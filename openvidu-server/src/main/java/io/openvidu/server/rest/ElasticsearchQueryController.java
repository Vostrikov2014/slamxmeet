//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.openvidu.server.cdr.WebrtcDebugEvent.WebrtcDebugEventIssuer;
import io.openvidu.server.cdr.WebrtcDebugEvent.WebrtcDebugEventType;
import io.openvidu.server.pro.cdr.CDRLoggerElasticSearch.ElasticsearchType;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.MultiValuedMap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/elk"})
public class ElasticsearchQueryController {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchQueryController.class);
    private static final int ONE_HUNDRED_SIZE = 100;
    private static final int ONE_THOUSAND_FIVE_HUNDRED_SIZE = 1500;
    private static final int TEN_THOUSAND_SIZE = 10000;
    @Autowired
    private ElasticsearchQueryService elasticSrv;

    public ElasticsearchQueryController() {
    }

    @GetMapping({"/session-summary"})
    public ResponseEntity<String> getSessionSummaries(@RequestParam(value = "from",required = false) Long from, @RequestParam(value = "load-more",required = false) Boolean loadMore, @RequestParam(value = "only-problems",required = false) Boolean onlyProblems, HttpServletRequest request) throws IOException {
        log.info("REST API: GET {}/session-summary", "/openvidu/elk");
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            List<QueryBuilder> queries = new ArrayList();
            queries.add(QueryBuilders.queryStringQuery("elastic_type:sessionSummary"));
            boolean searchAfter = false;
            if (from != null) {
                queries.add(QueryBuilders.rangeQuery("date").lt(from));
            }

            if (loadMore != null) {
                searchAfter = loadMore;
            }

            if (onlyProblems != null) {
                queries.add(QueryBuilders.queryStringQuery("hadProblems:true"));
            }

            QueryBuilder[] array = new QueryBuilder[queries.size()];
            return this.getElasticResponse(100, searchAfter, true, (QueryBuilder[])((QueryBuilder[])queries.toArray(array)));
        }
    }

    @GetMapping({"/session-summary/{uniqueSessionId}"})
    public ResponseEntity<String> getSessionSummary(@PathVariable("uniqueSessionId") String uniqueSessionId) throws IOException, ParseException {
        log.info("REST API: GET {}/session-summary/{}", "/openvidu/elk", uniqueSessionId);
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            JsonObject sessionSummaryResponse = this.getElasticResponseFromStringQuery(1500, false, false, "uniqueSessionId:\"" + uniqueSessionId + "\" AND elastic_type:sessionSummary");
            this.joinRecordingObjects(sessionSummaryResponse);
            this.joinTroubleshooting(sessionSummaryResponse);
            sessionSummaryResponse.addProperty("total", sessionSummaryResponse.get("hits").getAsJsonArray().size());
            return this.getResponseFromHits(sessionSummaryResponse);
        }
    }

    @GetMapping({"/session-summary/{uniqueSessionId}/timeline"})
    public ResponseEntity<String> getSessionTimeline(@PathVariable("uniqueSessionId") String uniqueSessionId) throws Exception {
        log.info("REST API: GET {}/session-summary/{}/timeline", "/openvidu/elk", uniqueSessionId);
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            JsonObject timelineResponse = this.elasticSrv.getTimelineEvents(uniqueSessionId, 10000);
            return this.getResponseFromHits(timelineResponse);
        }
    }

    @GetMapping({"/session-summary/status"})
    public ResponseEntity<String> getSessionsStatus(@RequestParam(value = "analyze-sessions",required = false) Boolean analyzeSessions) throws Exception {
        log.info("REST API: GET {}/session-summary/status", "/openvidu/elk");
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("elastic_type:sessionSummary");
            if (analyzeSessions != null && analyzeSessions) {
                BoolQueryBuilder boolQuery = new BoolQueryBuilder();
                boolQuery.mustNot(QueryBuilders.existsQuery("hadProblems"));
                boolQuery.must(queryBuilder);
                JsonObject hitsResponse = this.getElasticResponse(100, false, false, (QueryBuilder)boolQuery);
                this.elasticSrv.analyzeSessions(hitsResponse);
            }

            return this.getResponseFromHits(this.elasticSrv.getPercentageValue(queryBuilder));
        }
    }

    private JsonObject joinRecordingObjects(JsonObject sessionSummaryResponse) throws IOException {
        JsonObject sessionSummary = sessionSummaryResponse.getAsJsonArray("hits").get(0).getAsJsonObject().getAsJsonObject("_source");
        String uniqueSessionId = sessionSummary.get("uniqueSessionId").getAsString();
        JsonObject recordingResponse = this.getElasticResponseFromStringQuery(1500, false, false, "uniqueSessionId:\"" + uniqueSessionId + "\" AND elastic_type:cdr AND event:recordingStatusChanged AND (status:ready OR status:stopped)");
        JsonArray recordingsArray = recordingResponse.get("hits").getAsJsonArray();
        Map<String, JsonObject> recordings = new HashMap();
        Iterator var7 = recordingsArray.iterator();

        while(var7.hasNext()) {
            JsonElement recording = (JsonElement)var7.next();
            JsonObject source = recording.getAsJsonObject().get("_source").getAsJsonObject();
            String recordingId = source.get("id").getAsString();
            if ("ready".equals(source.get("status").getAsString())) {
                recordings.put(recordingId, source);
            } else {
                recordings.putIfAbsent(recordingId, source);
            }
        }

        JsonArray recordingSummaries = sessionSummary.get("recordings").getAsJsonObject().get("content").getAsJsonArray();
        Iterator var15 = recordingSummaries.iterator();

        while(var15.hasNext()) {
            JsonElement recordingSummary = (JsonElement)var15.next();
            JsonObject recSummary = recordingSummary.getAsJsonObject();
            String recordingId = recSummary.get("id").getAsString();
            JsonObject rec = (JsonObject)recordings.get(recordingId);
            recSummary.add("size", rec.get("size"));
            recSummary.add("duration", rec.get("duration"));
            recSummary.add("status", rec.get("status"));
        }

        return sessionSummaryResponse;
    }

    private JsonObject joinTroubleshooting(JsonObject sessionSummaryResponse) throws IOException {
        Map<String, JsonObject> streams = new HashMap();
        JsonObject sessionSummary = sessionSummaryResponse.getAsJsonArray("hits").get(0).getAsJsonObject().getAsJsonObject("_source");
        String uniqueSessionId = sessionSummary.get("uniqueSessionId").getAsString();
        JsonArray users = sessionSummary.getAsJsonObject("users").getAsJsonArray("content");
        Iterator var6 = users.iterator();

        while(var6.hasNext()) {
            JsonElement user = (JsonElement)var6.next();
            Iterator var8 = user.getAsJsonObject().getAsJsonObject("connections").getAsJsonArray("content").iterator();

            while(var8.hasNext()) {
                JsonElement connection = (JsonElement)var8.next();
                Iterator var10 = connection.getAsJsonObject().getAsJsonObject("publishers").getAsJsonArray("content").iterator();

                while(var10.hasNext()) {
                    JsonElement publisher = (JsonElement)var10.next();
                    streams.put(publisher.getAsJsonObject().get("streamId").getAsString(), publisher.getAsJsonObject());
                }
            }
        }

        MultiValuedMap<String, JsonObject> kmsEvents = this.elasticSrv.getStreamIdKmsEventsMap(uniqueSessionId);
        Iterator var14 = kmsEvents.asMap().entrySet().iterator();

        while(var14.hasNext()) {
            Map.Entry<String, Collection<JsonObject>> connectionKmsEvents = (Map.Entry)var14.next();
            JsonObject stream = (JsonObject)streams.get(connectionKmsEvents.getKey());
            if (stream != null) {
                List<String> problemList = this.elasticSrv.checkSessionProblems((Collection)connectionKmsEvents.getValue());
                stream.add("troubleshootingInfo", (new Gson()).toJsonTree(problemList, (new TypeToken<List<String>>() {
                }).getType()));
            }
        }

        return sessionSummaryResponse;
    }

    @DeleteMapping({"/session-summary"})
    public ResponseEntity<String> deleteSessionSummaries(@RequestBody(required = true) Map<String, ?> params) throws IOException {
        log.info("REST API: DELETE {}/session-summary", "/openvidu/elk");
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            ArrayList documentIds;
            ArrayList sessions;
            try {
                documentIds = (ArrayList)params.get("documentIds");
                sessions = (ArrayList)params.get("sessions");
            } catch (ClassCastException var7) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            List<String> queries = new ArrayList();
            sessions.forEach((session) -> {
                String query = "(uniqueSessionId:\"" + session.get("uniqueSessionId") + "\" AND (elastic_type:userSummary OR elastic_type:connectionSummary OR elastic_type:publisherSummary OR elastic_type:subscriberSummary OR elastic_type:recordingSummary OR elastic_type:kms OR elastic_type:cdr OR elastic_type:webrtcDebug OR elastic_type:webrtcStats))";
                queries.add(query);
            });
            JsonObject hits = this.getElasticResponseFromMultipleStringQueries(10000, false, false, queries);
            List<String> otherList = new LinkedList();
            hits.get("hits").getAsJsonArray().forEach((summaryDoc) -> {
                String docId = summaryDoc.getAsJsonObject().get("_id").getAsString();
                otherList.add(docId);
            });
            return this.elasticSrv.deleteDocuments(documentIds, otherList);
        }
    }

    @DeleteMapping({"/session-summary/status"})
    public ResponseEntity<String> deleteSessionStatus() throws Exception {
        log.info("REST API: DELETE {}/session-summary/status", "/openvidu/elk");
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("elastic_type:sessionSummary");
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.must(QueryBuilders.existsQuery("hadProblems"));
            boolQuery.must(queryBuilder);
            JsonObject hitsResponse = this.getElasticResponse(100, false, false, (QueryBuilder)boolQuery);
            this.elasticSrv.restoreSesssionsProblems(hitsResponse);
            return this.getResponseFromHits(this.elasticSrv.getPercentageValue(queryBuilder));
        }
    }

    @GetMapping({"/cdr"})
    public ResponseEntity<String> getCdrEvents() throws IOException {
        log.info("REST API: GET {}/cdr", "/openvidu/elk");
        return !this.elasticSrv.isElasticsearchDefined() ? new ResponseEntity(HttpStatus.NOT_IMPLEMENTED) : this.getResponseFromHits(this.getElasticResponseFromStringQuery(1500, false, false, "elastic_type:cdr"));
    }

    @GetMapping({"/kms/session/{uniqueSessionId}/stream/{streamId}"})
    public ResponseEntity<String> getKmsEvents(@PathVariable("uniqueSessionId") String uniqueSessionId, @PathVariable("streamId") String streamId) throws IOException {
        log.info("REST API: GET {}/kms", "/openvidu/elk");
        if (!this.elasticSrv.isElasticsearchDefined()) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        } else {
            JsonObject hits = this.getElasticResponseFromStringQuery(1500, false, false, "uniqueSessionId:\"" + uniqueSessionId + "\" AND endpoint:\"" + streamId + "\" AND (elastic_type:kms OR elastic_type:webrtcDebug)");
            JsonObject response = new JsonObject();
            JsonArray kmsEvents = new JsonArray();
            JsonArray clientIceCandidates = new JsonArray();
            JsonArray serverIceCandidates = new JsonArray();
            Iterator var8 = hits.getAsJsonArray("hits").iterator();

            while(var8.hasNext()) {
                JsonElement event = (JsonElement)var8.next();
                JsonObject eventObj = event.getAsJsonObject().getAsJsonObject("_source");
                String elasticType = eventObj.get("elastic_type").getAsString();
                if (ElasticsearchType.webrtcDebug.name().equals(elasticType)) {
                    if (WebrtcDebugEventType.iceCandidate.name().equals(eventObj.get("type").getAsString())) {
                        if (WebrtcDebugEventIssuer.client.name().equals(eventObj.get("issuer").getAsString())) {
                            clientIceCandidates.add(this.formatIceCandidateFromEvent(eventObj));
                        } else {
                            serverIceCandidates.add(this.formatIceCandidateFromEvent(eventObj));
                        }
                    } else {
                        this.removeUnwantedElasticsearchFields(eventObj);
                        kmsEvents.add(eventObj);
                    }
                } else if (ElasticsearchType.kms.name().equals(elasticType)) {
                    this.removeUnwantedElasticsearchFields(eventObj);
                    kmsEvents.add(eventObj);
                }
            }

            response.add("events", kmsEvents);
            response.add("clientIceCandidates", clientIceCandidates);
            response.add("serverIceCandidates", serverIceCandidates);
            return this.getResponseFromHits(response);
        }
    }

    private void removeUnwantedElasticsearchFields(JsonObject event) {
        event.remove("sessionId");
        event.remove("uniqueSessionId");
        event.remove("user");
        event.remove("connection");
        event.remove("connectionId");
        event.remove("endpoint");
        event.remove("elastic_type");
        event.remove("clusterId");
        event.remove("cluster_id");
        event.remove("master_node_id");
        event.remove("media_node_id");
    }

    private JsonObject formatIceCandidateFromEvent(JsonObject webrtcDebugEvent) {
        return JsonParser.parseString(webrtcDebugEvent.get("content").getAsString()).getAsJsonObject();
    }

    @GetMapping({"/monitoring-stats"})
    public ResponseEntity<String> getMontoringStats() throws IOException {
        log.info("REST API: GET {}/monitoring-stats", "/openvidu/elk");
        return !this.elasticSrv.isElasticsearchDefined() ? new ResponseEntity(HttpStatus.NOT_IMPLEMENTED) : this.getResponseFromHits(this.getElasticResponseFromStringQuery(1500, false, false, "elastic_type:monitoringStats"));
    }

    @GetMapping({"/webrtc-stats"})
    public ResponseEntity<String> getWebrtcStats() throws IOException {
        log.info("REST API: GET {}/webrtc-stats", "/openvidu/elk");
        return !this.elasticSrv.isElasticsearchDefined() ? new ResponseEntity(HttpStatus.NOT_IMPLEMENTED) : this.getResponseFromHits(this.getElasticResponseFromStringQuery(1500, false, false, "elastic_type:webrtcStats"));
    }

    private ResponseEntity<String> getElasticResponse(int size, boolean searchAfter, boolean storeAsLastSearch, QueryBuilder... matches) throws IOException {
        SearchResponse elasticResponse = this.elasticSrv.getDocuments(size, searchAfter, storeAsLastSearch, matches);
        JsonObject json = JsonParser.parseString(elasticResponse.toString()).getAsJsonObject();
        JsonObject hits = json.get("hits").getAsJsonObject();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity(hits.toString(), responseHeaders, HttpStatus.OK);
    }

    private JsonObject getElasticResponseFromStringQuery(int size, boolean searchAfter, boolean storeAsLastSearch, String stringQuery) throws IOException {
        QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(stringQuery);
        return this.getElasticResponse(size, searchAfter, storeAsLastSearch, (QueryBuilder)queryBuilder);
    }

    private JsonObject getElasticResponseFromMultipleStringQueries(int size, boolean searchAfter, boolean storeAsLastSearch, List<String> queries) throws IOException {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queries.forEach((stringQuery) -> {
            queryBuilder.should(QueryBuilders.queryStringQuery(stringQuery));
        });
        return this.getElasticResponse(size, searchAfter, storeAsLastSearch, (QueryBuilder)queryBuilder);
    }

    private JsonObject getElasticResponse(int size, boolean searchAfter, boolean storeAsLastSearch, QueryBuilder queryBuilder) throws IOException {
        SearchResponse elasticResponse = this.elasticSrv.getDocuments(size, searchAfter, storeAsLastSearch, queryBuilder);
        JsonObject json = JsonParser.parseString(elasticResponse.toString()).getAsJsonObject();
        return json.get("hits").getAsJsonObject();
    }

    private ResponseEntity<String> getResponseFromHits(JsonObject hits) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity(hits.toString(), responseHeaders, HttpStatus.OK);
    }
}
