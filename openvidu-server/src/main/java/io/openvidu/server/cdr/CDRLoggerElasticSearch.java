//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.cdr;

import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDREventName;
import io.openvidu.server.cdr.CDREventParticipant;
import io.openvidu.server.cdr.CDRLogger;
import io.openvidu.server.cdr.WebrtcDebugEvent;
import io.openvidu.server.core.FinalUser;
import io.openvidu.server.kurento.endpoint.KmsEvent;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.monitoring.KmsWebrtcStats;
import io.openvidu.server.monitoring.NetworkQualityStats;
import io.openvidu.server.summary.SessionSummary;
import io.openvidu.server.utils.GeoLocation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CDRLoggerElasticSearch implements CDRLogger {
    List<String> geoPoints = new ArrayList();
    private static final Logger log = LoggerFactory.getLogger(CDRLoggerElasticSearch.class);
    private RestHighLevelClient client;
    public static final String ELASTICSEARCH_OPENVIDU_INDEX = "openvidu";
    public static final String ELASTICSEARCH_CLUSTER_ID_LEGACY = "clusterId";
    public static final String ELASTICSEARCH_CLUSTER_ID = "cluster_id";
    public static final String ELASTICSEARCH_MASTER_NODE_ID = "master_node_id";
    public static final String ELASTICSEARCH_MEDIA_NODE_ID = "media_node_id";
    public static final String ELASTICSEARCH_CUSTOM_TYPE_FIELD = "elastic_type";
    public static final String ELASTICSEARCH_TIMESTAMP_FIELD = "timestamp";
    public static final List<String> ELASTICSEARCH_DATE_FIELDS = Arrays.asList("timestamp", "createdAt", "destroyedAt", "startTime");
    public static final List<String> ELASTICSEARCH_GEOPOINTS_FIELDS = Arrays.asList("geoPoints");
    private String clusterId;
    private String masterNodeId;

    public CDRLoggerElasticSearch(OpenviduConfigPro openviduConfigPro) {
        this.populateTestGeoPoints();
        String esHost = openviduConfigPro.getElasticsearchHost();
        boolean esSecured = openviduConfigPro.isElasticSearchSecured();
        String esUserName = openviduConfigPro.getElasticsearchUserName();
        String esPassword = openviduConfigPro.getElasticsearchPassword();

        try {
            this.client = createClient(esHost, esSecured, esUserName, esPassword);
        } catch (MalformedURLException var17) {
            log.error("Property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is not a valid URI: {}", esHost);
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

        this.clusterId = openviduConfigPro.getClusterId();
        this.masterNodeId = openviduConfigPro.getOpenViduProMasterNodeId();

        try {
            if (this.client.ping(RequestOptions.DEFAULT)) {
                log.info("Elasticsearch is accessible at {}", esHost);
                MainResponse.Version version = this.client.info(RequestOptions.DEFAULT).getVersion();
                log.info("Elasticsearch version is {}", version.getNumber());
                openviduConfigPro.setElasticsearchVersion(version.getNumber());
                GetIndexRequest getRequest = new GetIndexRequest(new String[]{"openvidu"});
                boolean exists = this.client.indices().exists(getRequest, RequestOptions.DEFAULT);
                if (exists) {
                    log.info("Elasticsearch index \"{}\" already exists", "openvidu");
                } else {
                    try {
                        if (this.createElasticsearchIndex("openvidu")) {
                            PutMappingRequest request = new PutMappingRequest(new String[]{"openvidu"});
                            Map<String, Object> jsonMap = new HashMap();
                            Map<String, Object> properties = new HashMap();
                            Map<String, Object> message1 = new HashMap();
                            message1.put("type", "date");
                            message1.put("format", "epoch_millis");
                            ELASTICSEARCH_DATE_FIELDS.forEach((field) -> {
                                properties.put(field, message1);
                            });
                            Map<String, Object> message2 = new HashMap();
                            message2.put("type", "geo_point");
                            ELASTICSEARCH_GEOPOINTS_FIELDS.forEach((field) -> {
                                properties.put(field, message2);
                            });
                            jsonMap.put("properties", properties);
                            request.source(jsonMap);
                            AcknowledgedResponse putMappingResponse = this.client.indices().putMapping(request, RequestOptions.DEFAULT);
                            if (putMappingResponse.isAcknowledged()) {
                                log.info("Elasticsearch index \"{}\" has been created", "openvidu");
                            } else {
                                log.error("Error creating index \"{}\"", "openvidu");
                                Runtime.getRuntime().halt(1);
                            }
                        } else {
                            log.warn("Index creation could not complete successfully");
                        }
                    } catch (Exception var15) {
                        log.error("Error creating index \"{}\": {}", "openvidu", var15.getMessage());
                        Runtime.getRuntime().halt(1);
                    }
                }
            } else {
                log.error("Ping to Elasticsearch failed");
                Runtime.getRuntime().halt(1);
            }
        } catch (IOException var16) {
            log.error("Connection to Elasticsearch failed at {} ({})", esHost, var16.getMessage());
            log.error("If property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is defined, then it is mandatory that OpenVidu Server Pro is able to connect to it");
            log.error("Terminating OpenVidu Server Pro");
            Runtime.getRuntime().halt(1);
        }

    }

    public static RestHighLevelClient createClient(String elasticsearchHost, boolean isELKSecured, String esUserName, String esPassword) throws MalformedURLException {
        URL url = new URL(elasticsearchHost);
        HttpHost host = new HttpHost(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()));
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost[]{host});
        if (url.getPath() != null && !url.getPath().isEmpty()) {
            restClientBuilder.setPathPrefix(url.getPath());
        }

        if (isELKSecured) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(esUserName, esPassword));
            restClientBuilder.setHttpClientConfigCallback((httpClientBuilder) -> {
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        }

        return new RestHighLevelClient(restClientBuilder);
    }

    public RestHighLevelClient getClient() {
        return this.client;
    }

    private boolean createElasticsearchIndex(String index) throws Exception {
        log.info("Creating Elasticsearch index  \"{}\"", index);
        CreateIndexRequest createRequest = new CreateIndexRequest(index);

        try {
            CreateIndexResponse response = this.client.indices().create(createRequest, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (ElasticsearchStatusException var4) {
            log.error("ElasticsearchStatusException when creating index \"{}\" in Elasticsearch: {}", index, var4.getMessage());
            return false;
        } catch (Exception var5) {
            log.error("Error creating index \"{}\" in Elasticsearch: {}", index, var5.getMessage());
            throw var5;
        }
    }

    public void log(CDREvent event) {
        JsonObject jsonEvent = event.toJson();
        jsonEvent.addProperty("event", event.getEventName().name());
        if (event instanceof CDREventParticipant) {
            this.addPropertyGeoPointsForKibana(jsonEvent, ((CDREventParticipant)event).getParticipant().getLocation());
        }

        boolean logEventContentInServerOutput = !CDREventName.signalSent.equals(event.getEventName());
        this.logToOpenViduIndex(jsonEvent, CDRLoggerElasticSearch.ElasticsearchType.cdr.name(), logEventContentInServerOutput);
    }

    public void log(KmsEvent event) {
        JsonObject jsonKmsEvent = event.toJson();
        this.logToOpenViduIndex(jsonKmsEvent, CDRLoggerElasticSearch.ElasticsearchType.kms.name(), true);
    }

    public void log(SessionSummary sessionSummary) {
        JsonObject jsonSessionSummary = sessionSummary.toJson();
        Long DATE = sessionSummary.getEventSessionEnd().getTimestamp();
        this.addPropertyTimestampForKibanaIfNotExists(jsonSessionSummary, DATE);
        Map<String, GeoLocation> geoLocations = new HashMap();
        sessionSummary.getUsers().entrySet().forEach((entry) -> {
            geoLocations.put((String)entry.getKey(), ((FinalUser)entry.getValue()).getLocation());
        });
        String SESSION_ID = jsonSessionSummary.get("sessionId").getAsString();
        String UNIQUE_SESSION_ID = jsonSessionSummary.get("uniqueSessionId").getAsString();
        String MEDIA_NODE_ID = sessionSummary.getEventSessionEnd().getSession().getMediaNodeId();
        jsonSessionSummary.get("users").getAsJsonObject().get("content").getAsJsonArray().forEach((userJson) -> {
            String USER_ID = userJson.getAsJsonObject().get("id").getAsString();
            this.addPropertyGeoPointsForKibana(userJson.getAsJsonObject(), (GeoLocation)geoLocations.get(USER_ID));
            this.addPropertiesForDenormalizingData(userJson.getAsJsonObject(), DATE, SESSION_ID, UNIQUE_SESSION_ID, (String)null, (String)null, MEDIA_NODE_ID);
            this.logToOpenViduIndex(userJson.getAsJsonObject(), CDRLoggerElasticSearch.ElasticsearchType.userSummary.name(), true);
            userJson.getAsJsonObject().get("connections").getAsJsonObject().get("content").getAsJsonArray().forEach((connectionJson) -> {
                String CONNECTION_ID = connectionJson.getAsJsonObject().get("connectionId").getAsString();
                this.addPropertiesForDenormalizingData(connectionJson.getAsJsonObject(), DATE, SESSION_ID, UNIQUE_SESSION_ID, USER_ID, (String)null, MEDIA_NODE_ID);
                this.logToOpenViduIndex(connectionJson.getAsJsonObject(), CDRLoggerElasticSearch.ElasticsearchType.connectionSummary.name(), true);
                connectionJson.getAsJsonObject().get("publishers").getAsJsonObject().get("content").getAsJsonArray().forEach((publisherJson) -> {
                    this.addPropertiesForDenormalizingData(publisherJson.getAsJsonObject(), DATE, SESSION_ID, UNIQUE_SESSION_ID, USER_ID, CONNECTION_ID, MEDIA_NODE_ID);
                    this.logToOpenViduIndex(publisherJson.getAsJsonObject(), CDRLoggerElasticSearch.ElasticsearchType.publisherSummary.name(), true);
                });
                connectionJson.getAsJsonObject().get("subscribers").getAsJsonObject().get("content").getAsJsonArray().forEach((subscriberJson) -> {
                    this.addPropertiesForDenormalizingData(subscriberJson.getAsJsonObject(), DATE, SESSION_ID, UNIQUE_SESSION_ID, USER_ID, CONNECTION_ID, MEDIA_NODE_ID);
                    this.logToOpenViduIndex(subscriberJson.getAsJsonObject(), CDRLoggerElasticSearch.ElasticsearchType.subscriberSummary.name(), true);
                });
            });
        });
        jsonSessionSummary.get("recordings").getAsJsonObject().get("content").getAsJsonArray().forEach((recordingJson) -> {
            this.addPropertyMediaNodeIdIfNotExists(recordingJson.getAsJsonObject(), MEDIA_NODE_ID);
            this.logToOpenViduIndex(recordingJson.getAsJsonObject(), CDRLoggerElasticSearch.ElasticsearchType.recordingSummary.name(), true);
        });
        this.addPropertyMediaNodeIdIfNotExists(jsonSessionSummary, MEDIA_NODE_ID);
        this.logToOpenViduIndex(jsonSessionSummary, CDRLoggerElasticSearch.ElasticsearchType.sessionSummary.name(), true);
    }

    public void log(KmsWebrtcStats stats) {
        JsonObject jsonStatsEvent = stats.toJson();
        this.logToOpenViduIndex(jsonStatsEvent, CDRLoggerElasticSearch.ElasticsearchType.webrtcStats.name(), false);
    }

    public void log(WebrtcDebugEvent event) {
        JsonObject jsonEvent = event.toJson();
        this.logToOpenViduIndex(jsonEvent, CDRLoggerElasticSearch.ElasticsearchType.webrtcDebug.name(), false);
    }

    public void log(NetworkQualityStats stats) {
        JsonObject jsonStatsEvent = stats.toJson();
        this.logToOpenViduIndex(jsonStatsEvent, CDRLoggerElasticSearch.ElasticsearchType.networkQualityStats.name(), false);
    }

    private void logToOpenViduIndex(final JsonObject object, final String type, final boolean logEnabled) {
        object.addProperty("elastic_type", type);
        object.addProperty("clusterId", this.clusterId);
        object.addProperty("cluster_id", this.clusterId);
        object.addProperty("master_node_id", this.masterNodeId);
        IndexRequest request = new IndexRequest("openvidu");
        request.source(object.toString(), XContentType.JSON);
        this.client.indexAsync(request, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            public void onResponse(IndexResponse indexResponse) {
                CDRLoggerElasticSearch.log.info("New event of type \"{}\" sent to Elasticsearch{}", type, logEnabled ? ": " + object.toString() : "");
            }

            public void onFailure(Exception e) {
                CDRLoggerElasticSearch.log.error("Sending event of type \"{}\" to Elasticsearch failure: {}", type, e.getMessage());
            }
        });
    }

    private void addPropertyTimestampForKibanaIfNotExists(JsonObject json, long date) {
        if (!json.has("timestamp")) {
            json.addProperty("timestamp", date);
        }

    }

    private void addPropertyMediaNodeIdIfNotExists(JsonObject json, String mediaNodeId) {
        if (mediaNodeId != null && !json.has("media_node_id")) {
            json.addProperty("media_node_id", mediaNodeId);
        }

    }

    private void addPropertyGeoPointsForKibana(JsonObject json, GeoLocation location) {
        if (location != null) {
            Double var10002 = location.getLatitude();
            json.addProperty("geoPoints", "" + var10002 + "," + location.getLongitude());
        }

    }

    private void addPropertiesForDenormalizingData(JsonObject json, Long date, String sessionId, String uniqueSessionId, String user, String connection, String mediaNodeId) {
        if (sessionId != null && !json.has("sessionId")) {
            json.addProperty("sessionId", sessionId);
        }

        if (uniqueSessionId != null && !json.has("uniqueSessionId")) {
            json.addProperty("uniqueSessionId", uniqueSessionId);
        }

        if (user != null && !json.has("user")) {
            json.addProperty("user", user);
        }

        if (connection != null && !json.has("connectionId")) {
            json.addProperty("connection", connection);
            json.addProperty("connectionId", connection);
        }

        this.addPropertyMediaNodeIdIfNotExists(json, mediaNodeId);
        this.addPropertyTimestampForKibanaIfNotExists(json, date);
    }

    @PreDestroy
    public void preDestroy() {
        if (this.client != null) {
            try {
                this.client.close();
                log.info("Elasticsearch client closed");
            } catch (IOException var2) {
                log.error("Error closing Elasticsearch client: {}", var2.getMessage());
            }
        }

    }

    private void populateTestGeoPoints() {
        this.geoPoints.add("19.09,72.87");
        this.geoPoints.add("41.90,12.47");
        this.geoPoints.add("-36.85,174.76");
        this.geoPoints.add("41.15,-8.62");
        this.geoPoints.add("-23.55,-46.63");
        this.geoPoints.add("35.68,139.76");
        this.geoPoints.add("34.054170, -118.246567");
    }

    public static enum ElasticsearchType {
        cdr,
        kms,
        webrtcStats,
        webrtcDebug,
        networkQualityStats,
        sessionSummary,
        userSummary,
        connectionSummary,
        publisherSummary,
        subscriberSummary,
        recordingSummary;

        private ElasticsearchType() {
        }
    }
}
