//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openvidu.server.utils.UpdatableTimerTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.AcknowledgedResponse;
import org.elasticsearch.client.indexlifecycle.DeleteAction;
import org.elasticsearch.client.indexlifecycle.LifecycleAction;
import org.elasticsearch.client.indexlifecycle.LifecyclePolicy;
import org.elasticsearch.client.indexlifecycle.Phase;
import org.elasticsearch.client.indexlifecycle.PutLifecyclePolicyRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;

public class ElasticSearchConfig {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchConfig.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private ResourceLoader resourceLoader;
    private RestHighLevelClient client;
    private final String OPENVIDU_INDEX = "openvidu";
    private final String CLEANUP_POLICY_NAME = "openvidu_cleanup_policy";
    private final String PIPELINE_NAME = "kurento-pipeline";
    private final int DELETE_OLD_DOCS_INTERVAL = 3600;
    private final int DELETE_OLD_DOCS_TIMEOUT = 600;
    private final String KURENTO_LOG_PIPELINE_DIRFILE = "elasticsearch/kurento-pipeline.json";
    private static final String[] indexPrefixes = new String[]{"filebeat-nginx", "filebeat-redis", "filebeat-kurento", "filebeat-mediasoup", "filebeat-speech-to-text", "openvidu-logs", "openvidu-browser-logs", "metricbeat"};
    private static final String[] indexPatterns = new String[]{"filebeat*", "openvidu-logs*", "openvidu-browser-logs*", "metricbeat*", "server-metrics*", "session-metrics*"};
    private final String API_OPENDISTRO_USER_INFO = "/_opendistro/_security/authinfo";
    private final String API_OPENDISTRO_POLICY = "/_opendistro/_ism/policies/";
    private final String OPENDISTRO_POLICY_JSON = "elasticsearch/opensearch_lifecycle_policy.json";
    private boolean isSecuredOpenDistro;
    private UpdatableTimerTask removeOldDocumentsTimer;

    public ElasticSearchConfig() {
    }

    @PostConstruct
    private void init() {
        URL url = this.serializeUrl();
        HttpHost httpHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost[]{httpHost});
        boolean isELKSecured = this.openviduConfigPro.isElasticSearchSecured();
        if (url.getPath() != null && !url.getPath().isEmpty()) {
            restClientBuilder.setPathPrefix(url.getPath());
        }

        if (isELKSecured) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            String esUserName = this.openviduConfigPro.getElasticsearchUserName();
            String esPassword = this.openviduConfigPro.getElasticsearchPassword();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esUserName, esPassword));
            restClientBuilder.setHttpClientConfigCallback((httpClientBuilder) -> {
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        }

        this.client = new RestHighLevelClient(restClientBuilder);

        try {
            this.isSecuredOpenDistro = this.isSecuredOpenDistro();
        } catch (IOException var10) {
            log.error("Error while checking Elasticsearch authinfo: {}", var10.getMessage());
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        if (this.doPing()) {
            String[] var11 = indexPrefixes;
            int var12 = var11.length;

            for(int var13 = 0; var13 < var12; ++var13) {
                String prefix = var11[var13];
                String indexName = getIndexWithDate(prefix);
                this.createIndex(indexName);
            }

            this.importPipelines();
            this.importLifecyclePolicy();
            this.updateLifecyclePoliciesAndMappings();
            this.importTemplateIndex();
        } else {
            log.error("Ping to Elasticsearch failed");
            Runtime.getRuntime().halt(1);
        }

        this.removeOldDocs();
        this.initOldDocsRemoverTimerTask();
    }

    public static String getIndexWithDate(String prefix) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        Date date = new Date();
        String dateSuffix = dateFormat.format(date);
        return prefix + "-" + dateSuffix;
    }

    private void createIndex(String indexName) {
        GetIndexRequest getRequest = new GetIndexRequest(new String[]{indexName});

        try {
            boolean exists = this.client.indices().exists(getRequest, RequestOptions.DEFAULT);
            if (exists) {
                log.info("Elasticsearch index \"{}\" already exists", indexName);
            } else {
                log.info("Creating Elasticsearch index  \"{}\"", indexName);
                CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
                CreateIndexResponse createIndexResponse = this.client.indices().create(createRequest, RequestOptions.DEFAULT);
                if (createIndexResponse.isAcknowledged()) {
                    log.info("Elasticsearch index \"{}\" has been created", indexName);
                } else {
                    log.error("Error creating index \"{}\"", indexName);
                    Runtime.getRuntime().halt(1);
                }
            }
        } catch (IOException var6) {
            log.error("Error: IOException while creating index \"{}\": ({})", indexName, var6.getMessage());
            Runtime.getRuntime().halt(1);
        } catch (ElasticsearchStatusException var7) {
            log.error("Error while creating index {}, with exception: {}", var7.getIndex(), var7.getMessage());
            Runtime.getRuntime().halt(1);
        }

    }

    private void importLifecyclePolicy() {
        int maxDaysDelete = this.openviduConfigPro.getElasticsearchMaxDaysDelete();
        if (maxDaysDelete == 0) {
            maxDaysDelete = Integer.MAX_VALUE;
        }

        if (this.isSecuredOpenDistro) {
            JsonObject policyJson = null;
            String esUserName = this.openviduConfigPro.getElasticsearchUserName();
            String esPassword = this.openviduConfigPro.getElasticsearchPassword();
            Gson gson = new Gson();
            boolean policyExists = false;

            try {
                HttpGet httpGet = new HttpGet(this.openviduConfigPro.getElasticsearchHost() + "/_opendistro/_ism/policies/openvidu_cleanup_policy");
                httpGet.setHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
                httpGet.setHeader("Content-type", "application/json");
                File file = this.getFile("elasticsearch/opensearch_lifecycle_policy.json");
                BufferedReader br = new BufferedReader(new FileReader(file));
                policyJson = (JsonObject)gson.fromJson(br, JsonObject.class);
                JsonArray indexPatternsJsonArray = policyJson.get("policy").getAsJsonObject().get("ism_template").getAsJsonObject().get("index_patterns").getAsJsonArray();
                Stream var10000 = Arrays.stream(indexPatterns);
                Objects.requireNonNull(indexPatternsJsonArray);
                var10000.forEach(indexPatternsJsonArray::add);
                JsonObject deleteCondition = policyJson.get("policy").getAsJsonObject().get("states").getAsJsonArray().get(0).getAsJsonObject().get("transitions").getAsJsonArray().get(0).getAsJsonObject().get("conditions").getAsJsonObject();
                deleteCondition.addProperty("min_index_age", "" + maxDaysDelete + "d");
            } catch (Exception var18) {
                log.error("Error while loading policy json file at '{}'", "elasticsearch/opensearch_lifecycle_policy.json");
                Runtime.getRuntime().halt(1);
            }

            try {
                CloseableHttpClient httpclient = HttpClients.createDefault();

                try {
                    HttpPut httpPut = new HttpPut(this.openviduConfigPro.getElasticsearchHost() + "/_opendistro/_ism/policies/openvidu_cleanup_policy");
                    httpPut.setHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
                    httpPut.setHeader("Content-type", "application/json");
                    httpPut.setEntity(new StringEntity(policyJson.toString(), ContentType.APPLICATION_JSON));
                    CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpPut);
                    HttpStatus httpStatus = HttpStatus.valueOf(closeableHttpResponse.getStatusLine().getStatusCode());
                    if (httpStatus.is4xxClientError()) {
                        if (httpStatus.equals(HttpStatus.CONFLICT)) {
                            log.info("Policy '{}' currently exists in Elasticsearch...", "openvidu_cleanup_policy");
                            policyExists = true;
                        } else {
                            log.error("Error while importing policy to Elasticsearch: '{}'", "openvidu_cleanup_policy");
                            Runtime.getRuntime().halt(1);
                        }
                    }
                } catch (Throwable var25) {
                    if (httpclient != null) {
                        try {
                            httpclient.close();
                        } catch (Throwable var17) {
                            var25.addSuppressed(var17);
                        }
                    }

                    throw var25;
                }

                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (Exception var26) {
                log.error("Error while importing policy to Elasticsearch '{}'", var26.getMessage());
                var26.printStackTrace();
                Runtime.getRuntime().halt(1);
            }

            if (policyExists) {
                Integer seqNo = null;
                Integer primaryTerm = null;

                CloseableHttpClient httpclient;
                try {
                    httpclient = HttpClients.createDefault();

                    try {
                        HttpGet httpGet = new HttpGet(this.openviduConfigPro.getElasticsearchHost() + "/_opendistro/_ism/policies/openvidu_cleanup_policy");
                        httpGet.setHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
                        httpGet.setHeader("Content-type", "application/json");
                        CloseableHttpResponse httpResponse = httpclient.execute(httpGet);
                        JsonObject currentPolicy = JsonParser.parseString(EntityUtils.toString(httpResponse.getEntity())).getAsJsonObject();
                        seqNo = currentPolicy.get("_seq_no").getAsInt();
                        primaryTerm = currentPolicy.get("_primary_term").getAsInt();
                        log.info("Getting current policy '{}' info for updating...", "openvidu_cleanup_policy");
                    } catch (Throwable var22) {
                        if (httpclient != null) {
                            try {
                                httpclient.close();
                            } catch (Throwable var16) {
                                var22.addSuppressed(var16);
                            }
                        }

                        throw var22;
                    }

                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (HttpResponseException var23) {
                    log.error("Http exception occurred while getting current policy: '{}'", var23.getStatusCode());
                    var23.printStackTrace();
                    Runtime.getRuntime().halt(1);
                } catch (Exception var24) {
                    log.error("Error while getting current policy from Elasticsearch '{}'", var24.getMessage());
                    var24.printStackTrace();
                    Runtime.getRuntime().halt(1);
                }

                try {
                    httpclient = HttpClients.createDefault();

                    try {
                        String url = this.openviduConfigPro.getElasticsearchHost() + "/_opendistro/_ism/policies/openvidu_cleanup_policy?if_seq_no=" + seqNo + "&if_primary_term=" + primaryTerm;
                        HttpPut httpPut = new HttpPut(url);
                        httpPut.setHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
                        httpPut.setHeader("Content-type", "application/json");
                        httpPut.setEntity(new StringEntity(policyJson.toString(), ContentType.APPLICATION_JSON));
                        httpclient.execute(httpPut);
                        log.info("Policy '{}' updated in Elasticsearch", "openvidu_cleanup_policy");
                    } catch (Throwable var19) {
                        if (httpclient != null) {
                            try {
                                httpclient.close();
                            } catch (Throwable var13) {
                                var19.addSuppressed(var13);
                            }
                        }

                        throw var19;
                    }

                    if (httpclient != null) {
                        httpclient.close();
                    }
                } catch (HttpResponseException var20) {
                    log.error("Http exception occurred while updating current policy: '{}'", var20.getStatusCode());
                    var20.printStackTrace();
                    Runtime.getRuntime().halt(1);
                } catch (Exception var21) {
                    log.error("Unknown exception while getting updating policy from Elasticsearch '{}'", var21.getMessage());
                    var21.printStackTrace();
                    Runtime.getRuntime().halt(1);
                }
            }
        } else {
            Map<String, Phase> phases = new HashMap();
            Map<String, LifecycleAction> deleteActions = Collections.singletonMap("delete", new DeleteAction());
            phases.put("delete", new Phase("delete", new TimeValue((long)maxDaysDelete, TimeUnit.DAYS), deleteActions));
            LifecyclePolicy policy = new LifecyclePolicy("openvidu_cleanup_policy", phases);
            PutLifecyclePolicyRequest request = new PutLifecyclePolicyRequest(policy);

            try {
                AcknowledgedResponse response = this.client.indexLifecycle().putLifecyclePolicy(request, RequestOptions.DEFAULT);
                if (response.isAcknowledged()) {
                    log.info("Imported lifecycle policy \"{}\"", "openvidu_cleanup_policy");
                } else {
                    log.error("Error applying cleanup policy \"{}\"", "openvidu_cleanup_policy");
                    Runtime.getRuntime().halt(1);
                }
            } catch (ElasticsearchStatusException var14) {
                log.warn("Can't import lifecycle policy \"{}\"", "openvidu_cleanup_policy");
            } catch (IOException var15) {
                log.error("Error: IOException while import lifecycle policy \"{}\": ({})", "openvidu_cleanup_policy", var15.getMessage());
                Runtime.getRuntime().halt(1);
            }
        }

    }

    private void updateLifecyclePoliciesAndMappings() {
        UpdateSettingsRequest requestAddPolicyIndex;
        Settings settingsRemovePolicyIndex;
        if (!this.isSecuredOpenDistro) {
            try {
                GetIndexRequest getRequest = new GetIndexRequest(new String[]{"openvidu"});
                boolean existsOpenViduPolicy = this.client.indices().exists(getRequest, RequestOptions.DEFAULT);
                if (existsOpenViduPolicy) {
                    requestAddPolicyIndex = new UpdateSettingsRequest(new String[]{"openvidu"});
                    settingsRemovePolicyIndex = Settings.builder().put("index.lifecycle.name", "").build();
                    requestAddPolicyIndex.settings(settingsRemovePolicyIndex);
                    if (this.client.indices().putSettings(requestAddPolicyIndex, RequestOptions.DEFAULT).isAcknowledged()) {
                        log.info("openvidu index lifecycle removed");
                    } else {
                        log.error("Error creating lifecycle policy");
                        Runtime.getRuntime().halt(1);
                    }
                }
            } catch (ElasticsearchStatusException var8) {
                log.warn("Can't remove lifecycle from openvidu index");
            } catch (IOException var9) {
                log.error("Error: IOException while getting openvidu index to update it ({})", var9.getMessage());
                Runtime.getRuntime().halt(1);
            }
        }

        if (!this.isSecuredOpenDistro) {
            UpdateSettingsRequest requestRemovePolicyIndex = new UpdateSettingsRequest(indexPatterns);
            Settings settingsRemovePolicyIndex = Settings.builder().put("index.lifecycle.name", "").build();
            requestRemovePolicyIndex.settings(settingsRemovePolicyIndex);
            requestAddPolicyIndex = new UpdateSettingsRequest(indexPatterns);
            settingsRemovePolicyIndex = Settings.builder().put("index.lifecycle.name", "openvidu_cleanup_policy").build();
            requestAddPolicyIndex.settings(settingsRemovePolicyIndex);

            try {
                if (this.client.indices().putSettings(requestRemovePolicyIndex, RequestOptions.DEFAULT).isAcknowledged() && this.client.indices().putSettings(requestAddPolicyIndex, RequestOptions.DEFAULT).isAcknowledged()) {
                    log.info("Updated lifecycle to indices");
                } else {
                    log.error("Error creating lifecycle policy to indices");
                    Runtime.getRuntime().halt(1);
                }
            } catch (ElasticsearchStatusException var10) {
                log.warn("Can't update lifecycle to openvidu indices");
            } catch (IOException var11) {
                log.error("Error: IOException while updating lifecycle to indices ({})", var11.getMessage());
                Runtime.getRuntime().halt(1);
            }
        }

        PutMappingRequest requestMappingPut = new PutMappingRequest(new String[]{"openvidu-browser-logs*"});
        Map<String, Object> jsonMap = new HashMap();
        Map<String, Object> properties = new HashMap();
        Map<String, Object> mappingDate = new HashMap();
        mappingDate.put("type", "date");
        mappingDate.put("format", "epoch_millis");
        properties.put("timestamp", mappingDate);
        jsonMap.put("properties", properties);
        requestMappingPut.source(jsonMap);

        try {
            if (this.client.indices().putMapping(requestMappingPut, RequestOptions.DEFAULT).isAcknowledged()) {
                log.info("Updated mappings to indices");
            } else {
                log.error("Error updating mappings");
                Runtime.getRuntime().halt(1);
            }
        } catch (ElasticsearchStatusException var6) {
            log.warn("Can't update mappings to openvidu indices");
        } catch (IOException var7) {
            log.error("Error: IOException while updating mappings to indices ({})", var7.getMessage());
            Runtime.getRuntime().halt(1);
        }

    }

    private void importTemplateIndex() {
        if (!this.isSecuredOpenDistro) {
            PutIndexTemplateRequest requestLifecycleTemplate = (new PutIndexTemplateRequest("openvidu-lifecycle-template")).patterns(Arrays.asList(indexPatterns)).settings(Settings.builder().put("index.lifecycle.name", "openvidu_cleanup_policy"));

            try {
                if (this.client.indices().putTemplate(requestLifecycleTemplate, RequestOptions.DEFAULT).isAcknowledged()) {
                    log.info("Imported template for new indices");
                } else {
                    log.error("Error importing template for new indices");
                    Runtime.getRuntime().halt(1);
                }
            } catch (ElasticsearchStatusException var8) {
                log.warn("Can't import lifecycle template for new indices");
            } catch (IOException var9) {
                log.error("Error: IOException while importing template for new indices ({})", var9.getMessage());
                Runtime.getRuntime().halt(1);
            }
        }

        Map<String, Object> mappingsJson = new HashMap();
        Map<String, Object> properties = new HashMap();
        Map<String, Object> mappingDate = new HashMap();
        mappingDate.put("type", "date");
        mappingDate.put("format", "epoch_millis");
        properties.put("timestamp", mappingDate);
        mappingsJson.put("properties", properties);
        PutIndexTemplateRequest indexTemplateRequest = (new PutIndexTemplateRequest("openvidu-browser-template")).patterns(Arrays.asList("openvidu-browser-logs*")).mapping(mappingsJson);

        try {
            if (this.client.indices().putTemplate(indexTemplateRequest, RequestOptions.DEFAULT).isAcknowledged()) {
                log.info("Imported mappings on templates for new indices");
            } else {
                log.error("Error importing mappings on templates for new indices");
                Runtime.getRuntime().halt(1);
            }
        } catch (ElasticsearchStatusException var6) {
            log.warn("Can't import mappings on templates for new indices");
        } catch (IOException var7) {
            log.error("Error: IOException while importing mappings to templates for new indices ({})", var7.getMessage());
            Runtime.getRuntime().halt(1);
        }

    }

    private void importPipelines() {
        try {
            File file = this.getFile("elasticsearch/kurento-pipeline.json");
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(file));
            JsonObject pipelineJson = (JsonObject)gson.fromJson(br, JsonObject.class);
            String pipelineSource = gson.toJson(pipelineJson);
            PutPipelineRequest request = new PutPipelineRequest("kurento-pipeline", new BytesArray(pipelineSource.getBytes(StandardCharsets.UTF_8)), XContentType.JSON);
            if (this.client.ingest().putPipeline(request, RequestOptions.DEFAULT).isAcknowledged()) {
                log.info("Imported pipeline \"{}\" to ElasticSearch", "kurento-pipeline");
            } else {
                log.error("Error importing pipeline \"{}\" to ElasticSearch", "kurento-pipeline");
                Runtime.getRuntime().halt(1);
            }
        } catch (FileNotFoundException var7) {
            log.error("File \"{}\" not found in resource files. ({})", "elasticsearch/kurento-pipeline.json", var7.getMessage());
            Runtime.getRuntime().halt(1);
        } catch (IOException var8) {
            log.error("Error: IOException while importing pipeline to ElasticSearch. ({})", var8.getMessage());
            Runtime.getRuntime().halt(1);
        }

    }

    private URL serializeUrl() {
        URL url = null;

        try {
            url = new URL(this.openviduConfigPro.getElasticsearchHost());
        } catch (MalformedURLException var3) {
            log.error("Property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is not a valid URI: {}", this.openviduConfigPro.getElasticsearchHost());
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        return url;
    }

    private boolean doPing() {
        boolean pingSuccess = false;

        try {
            pingSuccess = this.client.ping(RequestOptions.DEFAULT);
        } catch (IOException var3) {
            log.error("Connection to Elasticsearch failed at {} ({})", this.openviduConfigPro.getElasticsearchHost(), var3.getMessage());
            log.error("If property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is defined, then it is mandatory that OpenVidu Server Pro is able to connect to it");
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        return pingSuccess;
    }

    public static String getElasticVersion() {
        Class<?> clazz = ElasticsearchClient.class;
        Package p = clazz.getPackage();
        return p.getImplementationVersion();
    }

    private File getFile(String filePath) throws IOException {
        Resource resource = this.resourceLoader.getResource("classpath:" + filePath);
        InputStream in = null;

        try {
            in = resource.getInputStream();
        } catch (IOException var10) {
            log.error("Error reading Kurento Pipeline JSON file from JAR resources: {}", var10.getMessage());
        }

        String generatedString = RandomStringUtils.randomAlphabetic(10);
        File tempFile = File.createTempFile(generatedString + "-" + in.hashCode(), ".json");
        FileOutputStream out = new FileOutputStream(tempFile);

        try {
            byte[] buffer = new byte[1024];

            int bytesRead;
            while((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (Throwable var11) {
            try {
                out.close();
            } catch (Throwable var9) {
                var11.addSuppressed(var9);
            }

            throw var11;
        }

        out.close();
        return tempFile;
    }

    private void initOldDocsRemoverTimerTask() {
        if (this.openviduConfigPro.getElasticsearchMaxDaysDelete() == 0) {
            log.info("Old docs remover from 'openvidu' index is disabled (property 'OPENVIDU_PRO_ELASTICSEARCH_MAX_DAYS_DELETE' is 0)");
        } else {
            this.removeOldDocumentsTimer = new UpdatableTimerTask(this::removeOldDocs, () -> {
                return 3600000L;
            });
            this.removeOldDocumentsTimer.updateTimer();
            log.info("Elasticsearch old docs remover from 'openvidu' index is initialized. Docs older than {} days will be deleted each {} seconds", this.openviduConfigPro.getElasticsearchMaxDaysDelete(), 3600);
        }
    }

    private boolean isSecuredOpenDistro() throws IOException {
        String PATH = this.openviduConfigPro.getElasticsearchHost() + "/_opendistro/_security/authinfo";

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            boolean var3;
            try {
                var3 = this.isSecuredOpenDistroGetRequest(httpClient, PATH);
            } catch (Throwable var6) {
                if (httpClient != null) {
                    try {
                        httpClient.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (httpClient != null) {
                httpClient.close();
            }

            return var3;
        } catch (IOException var7) {
            log.warn("IOException when reaching Elasticsearch REST API with method GET at path {}: {}", PATH, var7.getMessage());
            throw var7;
        }
    }

    private boolean isSecuredOpenDistroGetRequest(CloseableHttpClient httpClient, String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        String esUserName = this.openviduConfigPro.getElasticsearchUserName();
        String esPassword = this.openviduConfigPro.getElasticsearchPassword();
        httpGet.addHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
        InputStream stream = null;

        boolean var8;
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);

            try {
                this.parseRestResult(response, "GET", (InputStream)stream);
                log.info("ELK is OpenDistro");
                var8 = true;
            } catch (Throwable var16) {
                if (response != null) {
                    try {
                        response.close();
                    } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                    }
                }

                throw var16;
            }

            if (response != null) {
                response.close();
            }

            return var8;
        } catch (Exception var17) {
            log.info("ELK is not OpenDistro");
            var8 = false;
        } finally {
            if (stream != null) {
                ((InputStream)stream).close();
            }

        }

        return var8;
    }

    private String getBasicAuth(String username, String password) {
        Base64.Encoder var10000 = Base64.getEncoder();
        String var10001 = username + ":" + password;
        return "Basic " + var10000.encodeToString(var10001.getBytes());
    }

    private String parseRestResult(HttpResponse response, String method, InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        int status = response.getStatusLine().getStatusCode();
        switch (status) {
            case 200:
                stream = response.getEntity().getContent();
                break;
            default:
                log.warn("Kibana returned an unexpected response to {}: {}", method, status);
        }

        BufferedReader rd = new BufferedReader(new InputStreamReader(stream));

        try {
            String line;
            try {
                while((line = rd.readLine()) != null) {
                    result.append(line);
                }
            } catch (IOException var12) {
                log.error(var12.getMessage());
            }
        } finally {
            if (rd != null) {
                rd.close();
            }

        }

        return result.toString();
    }

    private void removeOldDocs() {
        final int maxDaysDelete = this.openviduConfigPro.getElasticsearchMaxDaysDelete();
        DeleteByQueryRequest request = new DeleteByQueryRequest(new String[]{"openvidu"});
        RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder("timestamp");
        rangeQueryBuilder.lte(String.format("now-%dd", maxDaysDelete));
        request.setQuery(rangeQueryBuilder);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(600000).setSocketTimeout(600000).build();
        RequestOptions options = RequestOptions.DEFAULT.toBuilder().setRequestConfig(requestConfig).build();
        this.client.deleteByQueryAsync(request, options, new ActionListener<BulkByScrollResponse>() {
            public void onResponse(BulkByScrollResponse bulkByScrollResponse) {
                ElasticSearchConfig.log.info("Docs older than {} days in 'openvidu' index have been removed", maxDaysDelete);
            }

            public void onFailure(Exception e) {
                ElasticSearchConfig.log.error("Error removing Elasticsearch docs of 'openvidu' index older than {} days: {}", maxDaysDelete, e.getMessage());
            }
        });
    }

    @PreDestroy
    public void preDestroy() {
        if (this.removeOldDocumentsTimer != null) {
            this.removeOldDocumentsTimer.cancelTimer();
        }

    }
}
