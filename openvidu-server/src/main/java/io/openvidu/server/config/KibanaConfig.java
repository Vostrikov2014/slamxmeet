//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

public class KibanaConfig {
    private static final Logger log = LoggerFactory.getLogger(KibanaConfig.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private ResourceLoader resourceLoader;
    private CookieStore cookieStore;
    private boolean isSecuredOpenDistro;
    private String kibanaHost;
    private final String API_GET_SAVED_DASHBOARDS = "/api/saved_objects/_find?type=dashboard&per_page=1000";
    private final String API_GET_SAVED_INDEX_PATTERNS = "/api/saved_objects/_find?type=index-pattern&per_page=1000";
    private final String API_IMPORT_OBJECTS = "/api/saved_objects/_import?overwrite=true";
    private final String API_GET_STATUS = "/api/status";
    private final String API_OPENDISTRO_USER_INFO = "/_opendistro/_security/authinfo";
    private final String[] POSSIBLE_OPENDISTRO_API_LOGIN = new String[]{"/api/v1/auth/login", "/auth/login"};
    private final Set<String> EXPECTED_DASHBOARD_TITLES = new HashSet(Arrays.asList("CPU vs Sessions/Connections/Streams/Recordings", "OpenVidu Sessions", "OpenVidu Recordings", "Server Application Metrics", "Server Monitoring Metrics", "[Metricbeat] Nginx Metrics", "[Metricbeat] Node Monitoring Metrics", "[Metricbeat] Cluster Monitoring Metrics"));
    private final Set<String> EXPECTED_INDEX_PATTERNS = new HashSet(Arrays.asList("openvidu", "filebeat-*", "filebeat-kurento*", "filebeat-mediasoup*", "filebeat-media-node-controller*", "filebeat-nginx*", "filebeat-openvidu-recording*", "filebeat-redis*", "metricbeat-*", "openvidu-browser-logs*", "openvidu-logs*", "server-metrics-*", "session-metrics-*"));
    final int KIBANA_TIMEOUT = 30000;

    public KibanaConfig() {
    }

    private void importKibanaDashboards() {
        try {
            log.info("Importing Kibana JSON files with saved objects from JAR resources");
            List<File> kibanaFiles = this.getKibanaObjectsFile();
            Iterator var2 = kibanaFiles.iterator();

            while(var2.hasNext()) {
                File file = (File)var2.next();
                this.importSavedObjects(file);
            }

            log.info("Kibana dashboards successfully uploaded");
        } catch (IOException var4) {
            log.error("Exception while importing Kibana dashboards ({}): {}", var4.getClass().getCanonicalName(), var4.getMessage());
        }

    }

    private void setUpKibana() throws IOException {
        if (this.isSecuredOpenDistro && this.openviduConfigPro.isElasticSearchSecured()) {
            this.loginOpenDistro();
        }

        JsonObject kibanaStatus = this.kibanaGetRequest("/api/status");
        String version;
        if (kibanaStatus.get("version") != null) {
            version = kibanaStatus.get("version").getAsJsonObject().get("number").getAsString();
        } else {
            version = this.openviduConfigPro.getElasticsearchVersion();
        }

        this.openviduConfigPro.setKibanaVersion(version);
    }

    private boolean dashboardsExist() throws IOException {
        JsonObject dashboards = this.kibanaGetRequest("/api/saved_objects/_find?type=dashboard&per_page=1000");
        JsonObject indexPatterns = this.kibanaGetRequest("/api/saved_objects/_find?type=index-pattern&per_page=1000");
        return this.expectedSavedObjects(dashboards, indexPatterns);
    }

    private boolean expectedSavedObjects(JsonObject jsonDashboards, JsonObject jsonIndexPatterns) {
        Set<String> expectedDashboards = new HashSet();
        expectedDashboards.addAll(this.EXPECTED_DASHBOARD_TITLES);
        JsonArray dashboardsArray = jsonDashboards.get("saved_objects").getAsJsonArray();
        Iterator<JsonElement> iteratorDashboards = dashboardsArray.iterator();

        while(iteratorDashboards.hasNext()) {
            JsonObject d = ((JsonElement)iteratorDashboards.next()).getAsJsonObject();
            String objectType = d.get("type").getAsString();
            String objectTitle = d.get("attributes").getAsJsonObject().get("title").getAsString();
            if (objectType.equals("dashboard")) {
                expectedDashboards.remove(objectTitle);
            }
        }

        Set<String> expectedIndexPatterns = new HashSet();
        expectedIndexPatterns.addAll(this.EXPECTED_INDEX_PATTERNS);
        JsonArray IndexPatternsArray = jsonIndexPatterns.get("saved_objects").getAsJsonArray();
        Iterator<JsonElement> iteratorIndexPatterns = IndexPatternsArray.iterator();

        while(iteratorIndexPatterns.hasNext()) {
            JsonObject d = ((JsonElement)iteratorIndexPatterns.next()).getAsJsonObject();
            String objectType = d.get("type").getAsString();
            String objectTitle = d.get("attributes").getAsJsonObject().get("title").getAsString();
            if (objectType.equals("index-pattern")) {
                expectedIndexPatterns.remove(objectTitle);
            }
        }

        return expectedDashboards.isEmpty() && expectedIndexPatterns.isEmpty();
    }

    private List<File> getKibanaObjectsFile() throws IOException {
        Resource[] kibanaResourcesList = ResourcePatternUtils.getResourcePatternResolver(this.resourceLoader).getResources("classpath:kibana/*/*.ndjson");
        List<File> kibanaFileList = new ArrayList();
        Resource[] var3 = kibanaResourcesList;
        int var4 = kibanaResourcesList.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Resource resource = var3[var5];
            InputStream in = null;

            try {
                in = resource.getInputStream();
            } catch (IOException var13) {
                log.error("Error reading Kibana JSON file from JAR resources: {}", var13.getMessage());
            }

            File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".ndjson");
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);

            try {
                byte[] buffer = new byte[1024];

                int bytesRead;
                while((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (Throwable var14) {
                try {
                    out.close();
                } catch (Throwable var12) {
                    var14.addSuppressed(var12);
                }

                throw var14;
            }

            out.close();
            kibanaFileList.add(tempFile);
        }

        return kibanaFileList;
    }

    private void importSavedObjects(File file) throws IOException {
        String PATH = this.kibanaHost + "/api/saved_objects/_import?overwrite=true";
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InputStream inputStream = null;

        try {
            HttpPost httpPost = new HttpPost(PATH);
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).setCookieSpec("standard").build());
            httpPost.addHeader("kbn-xsrf", "true");
            String esUserName = this.openviduConfigPro.getElasticsearchUserName();
            String esPassword = this.openviduConfigPro.getElasticsearchPassword();
            boolean securityEnabled = this.openviduConfigPro.isElasticSearchSecured();
            if (!this.isSecuredOpenDistro && securityEnabled) {
                httpPost.addHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
            }

            FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
            HttpEntity entity = MultipartEntityBuilder.create().addPart("file", fileBody).setContentType(ContentType.MULTIPART_FORM_DATA).build();
            httpPost.setEntity(entity);
            httpclient = HttpClients.custom().setDefaultCookieStore(this.cookieStore).build();
            HttpClientContext context = HttpClientContext.create();
            response = httpclient.execute(httpPost, context);
            entity.writeTo(stream);
            this.parseRestResult(response, "POST " + PATH, (InputStream)inputStream);
            this.cookieStore = context.getCookieStore();
        } catch (HttpHostConnectException var18) {
            log.warn("Kibana is not accessible at {}: {}", var18.getHost().toURI(), var18.getMessage());
            throw var18;
        } catch (IOException var19) {
            log.warn("IOException when reaching Kibana REST API with method POST at path {}: {}", PATH, var19.getMessage());
            throw var19;
        } finally {
            if (stream != null) {
                stream.close();
            }

            if (inputStream != null) {
                ((InputStream)inputStream).close();
            }

            if (response != null) {
                response.close();
            }

            if (httpclient != null) {
                httpclient.close();
            }

        }

    }

    private JsonObject kibanaGetRequest(String path) throws IOException {
        InputStream stream = null;
        String PATH = this.kibanaHost + path;
        boolean securityEnabled = this.openviduConfigPro.isElasticSearchSecured();
        String esUserName = this.openviduConfigPro.getElasticsearchUserName();
        String esPassword = this.openviduConfigPro.getElasticsearchPassword();
        HttpGet httpGet = new HttpGet(PATH);
        httpGet.setHeader("kbn-xsrf", "true");
        if (!this.isSecuredOpenDistro && securityEnabled) {
            httpGet.addHeader("Authorization", this.getBasicAuth(esUserName, esPassword));
        }

        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("standard").setConnectTimeout(30000).setSocketTimeout(30000).build();

        try {
            CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(this.cookieStore).setDefaultRequestConfig(requestConfig).build();

            JsonObject var13;
            try {
                HttpClientContext context = HttpClientContext.create();

                try {
                    CloseableHttpResponse response = httpClient.execute(httpGet, context);

                    try {
                        String result = this.parseRestResult(response, "GET " + PATH, (InputStream)stream);
                        this.cookieStore = context.getCookieStore();
                        var13 = ((JsonElement)(new Gson()).fromJson(result, JsonElement.class)).getAsJsonObject();
                    } catch (Throwable var24) {
                        if (response != null) {
                            try {
                                response.close();
                            } catch (Throwable var23) {
                                var24.addSuppressed(var23);
                            }
                        }

                        throw var24;
                    }

                    if (response != null) {
                        response.close();
                    }
                } catch (HttpHostConnectException var25) {
                    log.warn("Kibana is not accessible at {}: {}", var25.getHost().toURI(), var25.getMessage());
                    throw var25;
                } finally {
                    if (stream != null) {
                        ((InputStream)stream).close();
                    }

                }
            } catch (Throwable var27) {
                if (httpClient != null) {
                    try {
                        httpClient.close();
                    } catch (Throwable var22) {
                        var27.addSuppressed(var22);
                    }
                }

                throw var27;
            }

            if (httpClient != null) {
                httpClient.close();
            }

            return var13;
        } catch (IOException var28) {
            log.warn("IOException when reaching Kibana REST API with method GET at path {}: {}", PATH, var28.getMessage());
            throw var28;
        }
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

    private void loginOpenDistro() {
        List<Pair<String, Exception>> possibleExceptions = new ArrayList();
        String[] var2 = this.POSSIBLE_OPENDISTRO_API_LOGIN;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String path = var2[var4];
            String PATH = this.kibanaHost + path;
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("standard").build();

            try {
                CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(this.cookieStore).setDefaultRequestConfig(requestConfig).build();

                label62: {
                    try {
                        this.loginOpenDistroPostRequest(httpClient, PATH);
                        if (!this.isCookieStoreEmpty()) {
                            break label62;
                        }

                        possibleExceptions.add(Pair.of(path, new Exception("Cookie after login is empty using path: " + path)));
                    } catch (Throwable var12) {
                        if (httpClient != null) {
                            try {
                                httpClient.close();
                            } catch (Throwable var11) {
                                var12.addSuppressed(var11);
                            }
                        }

                        throw var12;
                    }

                    if (httpClient != null) {
                        httpClient.close();
                    }
                    continue;
                }

                if (httpClient != null) {
                    httpClient.close();
                }
                break;
            } catch (Exception var13) {
                possibleExceptions.add(Pair.of(path, var13));
            }
        }

        if (!possibleExceptions.isEmpty() && this.isCookieStoreEmpty()) {
            Iterator var14 = possibleExceptions.iterator();

            while(var14.hasNext()) {
                Pair<String, Exception> exceptionPair = (Pair)var14.next();
                String path = (String)exceptionPair.getLeft();
                Exception exception = (Exception)exceptionPair.getRight();
                log.warn("IOException when reaching Kibana REST API with method GET at path {}: {}", path, exception);
                exception.printStackTrace();
            }

            Runtime.getRuntime().halt(1);
        }

    }

    private void loginOpenDistroPostRequest(CloseableHttpClient httpClient, String url) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("kbn-xsrf", "true");
        String esUserName = this.openviduConfigPro.getElasticsearchUserName();
        String esPassword = this.openviduConfigPro.getElasticsearchPassword();
        List<NameValuePair> params = new ArrayList();
        params.add(new BasicNameValuePair("username", esUserName));
        params.add(new BasicNameValuePair("password", esPassword));
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        HttpClientContext context = HttpClientContext.create();

        try {
            CloseableHttpResponse response = httpClient.execute(httpPost, context);

            try {
                this.cookieStore = context.getCookieStore();
            } catch (Throwable var12) {
                if (response != null) {
                    try {
                        response.close();
                    } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                    }
                }

                throw var12;
            }

            if (response != null) {
                response.close();
            }

        } catch (HttpHostConnectException var13) {
            log.warn("Kibana is not accessible at {}: {}", var13.getHost().toURI(), var13.getMessage());
            throw var13;
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

    @PostConstruct
    private void init() {
        this.kibanaHost = this.openviduConfigPro.getKibanaHost().replaceAll("/$", "");

        try {
            this.isSecuredOpenDistro = this.isSecuredOpenDistro();
        } catch (IOException var6) {
            log.error("Error while checking Elasticsearch authinfo: {}", var6.getMessage());
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        try {
            this.setUpKibana();
            log.info("Kibana is accessible at {}", this.openviduConfigPro.getKibanaHost());
            log.info("Kibana version is {}", this.openviduConfigPro.getKibanaVersion());
        } catch (Exception var5) {
            log.error("If property 'OPENVIDU_PRO_KIBANA_HOST' is defined, then it is mandatory that OpenVidu Server Pro is able to connect to it");
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        boolean dashboardsAlreadyExists = false;

        try {
            dashboardsAlreadyExists = this.dashboardsExist();
        } catch (IOException var4) {
            log.error("Kibana error (cannot check if default dashboards exist): {}", var4.getMessage());
            Runtime.getRuntime().halt(1);
        }

        if (!dashboardsAlreadyExists) {
            log.info("Kibana doesn't have default dashboards imported");

            try {
                this.importKibanaDashboards();
            } catch (Exception var3) {
                log.error("Error importing default Kibana dashboards: {}", var3.getMessage());
                log.error("Terminating OpenVidu Server Pro");
                Runtime.getRuntime().halt(1);
            }
        } else {
            log.info("Kibana already has default dashboards imported {}", this.EXPECTED_DASHBOARD_TITLES);
            log.info("Kibana already has default index patterns imported {}", this.EXPECTED_INDEX_PATTERNS);
        }

    }

    private boolean isCookieStoreEmpty() {
        return this.cookieStore == null || this.cookieStore.getCookies() == null || this.cookieStore.getCookies().isEmpty();
    }
}
