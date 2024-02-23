//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.core.TokenRegister;
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.config.BrowserLog;
import io.openvidu.server.config.ElasticSearchConfig;
import io.openvidu.server.config.OpenviduConfigPro;
import java.net.MalformedURLException;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/elk"})
public class ElasticsearchController {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchController.class);
    private final String OPENVIDU_BROWSER_LOG_INDEX_PREFIX = "openvidu-browser-logs";
    private final int MAX_MESSAGE_LENGTH = 100000;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private TokenRegister tokenRegister;
    private RestHighLevelClient elasticsearchClient;
    private Gson gson = new Gson();

    public ElasticsearchController() {
    }

    @PostConstruct
    public void initialize() {
        String esHost = this.openviduConfigPro.getElasticsearchHost();
        boolean esSecured = this.openviduConfigPro.isElasticSearchSecured();
        String esUserName = this.openviduConfigPro.getElasticsearchUserName();
        String esPassword = this.openviduConfigPro.getElasticsearchPassword();
        if (this.openviduConfigPro.isElasticsearchDefined()) {
            try {
                this.elasticsearchClient = CDRLoggerElasticSearch.createClient(esHost, esSecured, esUserName, esPassword);
            } catch (MalformedURLException var6) {
                log.error("Property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is not a valid URI: {}", esHost);
                log.error("Terminating OpenVidu Server Pro");
                Runtime.getRuntime().halt(1);
            }
        }

    }

    @RequestMapping(
            value = {"/webrtc-stats"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> postWebrtcStats(@RequestBody(required = true) Map<String, ?> params) {
        log.debug("REST API: POST {}/webrtc-stats {}", "/openvidu/elk", params.toString());

        String sessionId;
        String participantPrivateId;
        try {
            sessionId = (String)params.get("sessionId");
            participantPrivateId = (String)params.get("participantPrivateId");
        } catch (ClassCastException var6) {
            return new ResponseEntity("Type error in parameter 'sessionId' or 'participantPrivateId' on POST /elasticsearch/webrtc-stats", HttpStatus.BAD_REQUEST);
        }

        try {
            this.sessionManager.getParticipant(sessionId, participantPrivateId);
        } catch (OpenViduException var5) {
            return new ResponseEntity("User is not authorized", HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/openvidu-browser-logs"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> postBrowser(@RequestHeader("OV-Final-User-Id") final String finalUserId, @RequestHeader("OV-Session-Id") final String sessionId, @RequestHeader("OV-Token") String token, @RequestBody(required = true) Map<String, ?> params) {
        if (this.openviduConfigPro.getSendBrowserLogs() == BrowserLog.disabled) {
            return new ResponseEntity("OpenVidu Browser debug logs is disabled", HttpStatus.UNAUTHORIZED);
        } else {
            String openviduBrowserIndex = ElasticSearchConfig.getIndexWithDate("openvidu-browser-logs");
            if (sessionId != null && finalUserId != null && token != null) {
                if (!this.tokenRegister.isTokenRegistered(token, finalUserId, sessionId)) {
                    return new ResponseEntity("Token, Connection Id and Session Id does not match", HttpStatus.UNAUTHORIZED);
                } else {
                    String uniqueSessionId = this.sessionManager.getSession(sessionId).getUniqueSessionId();
                    BulkRequest bulkRequest = new BulkRequest();
                    JsonArray jsonLogArray = this.gson.toJsonTree(params).getAsJsonObject().get("lg").getAsJsonArray();

                    for(int i = 0; i < jsonLogArray.size(); ++i) {
                        JsonObject jsonLog = jsonLogArray.get(i).getAsJsonObject();
                        JsonObject jsonDoc = new JsonObject();
                        jsonDoc.addProperty("timestamp", jsonLog.get("t").getAsLong());
                        jsonDoc.addProperty("@timestamp", jsonLog.get("t").getAsLong());
                        jsonDoc.addProperty("log_level", this.numericSeverityToNamedSeverity(jsonLog.get("l").getAsInt()));
                        String message = this.parseLogMessage(jsonLog.get("m").getAsString());
                        if (message.length() > 100000) {
                            message = message.substring(0, 100000);
                        }

                        jsonDoc.addProperty("message", message);
                        jsonDoc.addProperty("finalUserId", finalUserId);
                        jsonDoc.addProperty("sessionId", sessionId);
                        jsonDoc.addProperty("uniqueSessionId", uniqueSessionId);
                        IndexRequest indexRequest = (new IndexRequest(openviduBrowserIndex)).source(jsonDoc.toString(), XContentType.JSON);
                        bulkRequest.add(indexRequest);
                    }

                    this.elasticsearchClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
                        public void onResponse(BulkResponse bulkResponse) {
                            ElasticsearchController.log.debug("OpenVidu Browser logs sent to Elasticsearch: FinalUserId: {} and SessionId: {}", finalUserId, sessionId);
                        }

                        public void onFailure(Exception e) {
                            ElasticsearchController.log.error("Error on sending Browser logs to Elasticsearch: FinalUserId: {} and SessionId: {}. Error: {}", new Object[]{finalUserId, sessionId, e.getMessage()});
                        }
                    });
                    return new ResponseEntity(HttpStatus.OK);
                }
            } else {
                return new ResponseEntity("Headers not set correctly", HttpStatus.UNAUTHORIZED);
            }
        }
    }

    private String numericSeverityToNamedSeverity(int numericSeverity) {
        if (numericSeverity <= 1000) {
            return "TRACE";
        } else if (numericSeverity <= 2000) {
            return "DEBUG";
        } else if (numericSeverity <= 3000) {
            return "INFO";
        } else if (numericSeverity <= 4000) {
            return "WARN";
        } else {
            return numericSeverity <= 5000 ? "ERROR" : "FATAL";
        }
    }

    private String parseLogMessage(String messageString) {
        StringBuilder message = new StringBuilder();

        JsonObject possibleJsonObject;
        try {
            possibleJsonObject = (JsonObject)this.gson.fromJson(messageString, JsonObject.class);
        } catch (JsonSyntaxException var6) {
            possibleJsonObject = null;
        }

        if (possibleJsonObject == null) {
            message.append(messageString);
        } else {
            for(int i = 0; i < possibleJsonObject.size(); ++i) {
                JsonElement messageLineObject = possibleJsonObject.get(Integer.toString(i));
                if (messageLineObject != null) {
                    message.append(messageLineObject).append("\n");
                }
            }
        }

        return message.toString();
    }
}
