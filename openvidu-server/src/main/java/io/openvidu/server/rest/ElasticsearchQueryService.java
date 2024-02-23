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
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.monitoring.model.SessionProblem;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.kurento.client.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchQueryService {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchQueryService.class);
    private static final int MEDIA_FLOWING_BACK_MS_LIMIT = 500;
    private static final int MEDIA_FLOWING_READY_MS_LIMIT = 5000;
    private static final int MEDIA_FLOWING_NOT_FOUND_MS_LIMIT = 1000;
    private static final int ONE_THOUSAND_FIVE_HUNDRED_SIZE = 1500;
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    private RestHighLevelClient defaultClient;
    private SearchResponse lastSessionSummarySearch;
    @Autowired
    HttpServletRequest request;

    public ElasticsearchQueryService() {
    }

    @PostConstruct
    public void initialize() {
        String esHost = this.openviduConfigPro.getElasticsearchHost();
        boolean esSecured = this.openviduConfigPro.isElasticSearchSecured();
        String esUserName = this.openviduConfigPro.getElasticsearchUserName();
        String esPassword = this.openviduConfigPro.getElasticsearchPassword();
        if (this.openviduConfigPro.isElasticsearchDefined()) {
            try {
                this.defaultClient = CDRLoggerElasticSearch.createClient(esHost, esSecured, esUserName, esPassword);
            } catch (MalformedURLException var6) {
                log.error("Property 'OPENVIDU_PRO_ELASTICSEARCH_HOST' is not a valid URI: {}", esHost);
                log.error("Terminating OpenVidu Server Pro");
                Runtime.getRuntime().halt(1);
            }
        }

    }

    private RestHighLevelClient getESClient() throws MalformedURLException {
        String esHost = this.request.getHeader("OV-ElasticSearch-URL");
        String esUserName = this.request.getHeader("OV-ElasticSearch-User");
        String esPassword = this.request.getHeader("OV-ElasticSearch-Password");
        return esHost == null ? this.defaultClient : CDRLoggerElasticSearch.createClient(esHost, esUserName != null, esUserName, esPassword);
    }

    public ResponseEntity<String> deleteDocuments(Collection<String> sessionSummaryIds, Collection<String> otherIds) throws IOException {
        RestHighLevelClient client = this.getESClient();
        BulkRequest request = new BulkRequest();
        sessionSummaryIds.forEach((id) -> {
            request.add(new DeleteRequest("openvidu", id));
        });
        otherIds.forEach((id) -> {
            request.add(new DeleteRequest("openvidu", id));
        });
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        int numberOfSessionSummaryDeletions = 0;
        int numberOfOtherDeletions = 0;

        for(int i = 0; i < response.getItems().length; ++i) {
            DeleteResponse delResponse = (DeleteResponse)response.getItems()[i].getResponse();
            if (delResponse.getResult() == Result.DELETED) {
                if (sessionSummaryIds.contains(delResponse.getId())) {
                    ++numberOfSessionSummaryDeletions;
                } else {
                    ++numberOfOtherDeletions;
                }
            }
        }

        JsonObject deletions = new JsonObject();
        deletions.addProperty("sessionSummaries", numberOfSessionSummaryDeletions);
        deletions.addProperty("otherSummaries", numberOfOtherDeletions);
        if (client != this.defaultClient) {
            client.close();
        }

        return new ResponseEntity(deletions.toString(), HttpStatus.OK);
    }

    public SearchResponse getDocuments(int size, boolean searchAfter, boolean storeAsLastSearch, QueryBuilder... matches) throws IOException {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

        for(int i = 0; i < matches.length; ++i) {
            queryBuilder.must(matches[i]);
        }

        return this.getDocuments(size, searchAfter, storeAsLastSearch, (QueryBuilder)queryBuilder);
    }

    public SearchResponse getDocuments(int size, boolean searchAfter, boolean storeAsLastSearch, QueryBuilder queryBuilder) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);
        sourceBuilder.from(0);
        sourceBuilder.size(size);
        sourceBuilder.timeout(new TimeValue(15L, TimeUnit.SECONDS));
        sourceBuilder.sort((new FieldSortBuilder("timestamp")).order(SortOrder.DESC));
        if (searchAfter && this.lastSessionSummarySearch != null && this.lastSessionSummarySearch.getHits().getHits().length > 0) {
            int lastHit = this.lastSessionSummarySearch.getHits().getHits().length - 1;
            Object[] lastSortValues = this.lastSessionSummarySearch.getHits().getHits()[lastHit].getSortValues();
            sourceBuilder.searchAfter(lastSortValues);
        }

        SearchRequest searchRequest = new SearchRequest(new String[]{"openvidu"});
        searchRequest.source(sourceBuilder);
        RestHighLevelClient client = null;
        SearchResponse res = null;

        try {
            client = this.getESClient();
            res = client.search(searchRequest, RequestOptions.DEFAULT);
            if (storeAsLastSearch && res.getHits().getHits().length > 0) {
                this.lastSessionSummarySearch = res;
            }
        } catch (Exception var13) {
            if (var13 != null && !var13.getMessage().contains("Timeout connecting")) {
                throw var13;
            }

            log.error(var13.getMessage());
        } finally {
            if (client != this.defaultClient) {
                client.close();
            }

        }

        return res;
    }

    public void restoreSesssionsProblems(JsonObject hitsResponse) throws Exception {
        JsonArray hitsArr = hitsResponse.getAsJsonArray("hits");

        for(int i = 0; i < hitsArr.size(); ++i) {
            String jsonString = "" + "{\"hadProblems\":null,\"problems\":[]}";
            this.updateDoc("openvidu", hitsArr.get(i).getAsJsonObject().get("_id").getAsString(), jsonString);
        }

    }

    public void analyzeSessions(JsonObject hitsResponse) throws Exception {
        JsonArray hitsArr = hitsResponse.getAsJsonArray("hits");

        for(int i = 0; i < hitsArr.size(); ++i) {
            JsonObject jsonObj = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(hitsArr.get(i).getAsJsonObject());
            jsonObj.add("hits", jsonArray);
            List<String> problemsArray = this.getSessionProblems(jsonObj);
            boolean hadProblems = !problemsArray.isEmpty();
            String jsonString = "{\"hadProblems\":" + hadProblems + ",\"problems\":" + (new Gson()).toJson(problemsArray) + "}";
            this.updateDoc("openvidu", hitsArr.get(i).getAsJsonObject().get("_id").getAsString(), jsonString);
        }

    }

    public JsonObject getPercentageValue(QueryBuilder queryBuilder) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);
        TermsAggregationBuilder aggregationTerms = (TermsAggregationBuilder)AggregationBuilders.terms("status").field("hadProblems");
        sourceBuilder.aggregation(aggregationTerms);
        sourceBuilder.size(0);
        SearchRequest searchRequest = new SearchRequest(new String[]{"openvidu"});
        searchRequest.source(sourceBuilder);
        RestHighLevelClient client = null;
        JsonObject jsonResponse = new JsonObject();

        try {
            client = this.getESClient();
            SearchResponse res = client.search(searchRequest, RequestOptions.DEFAULT);
            double totalSessions = (double)res.getHits().getTotalHits().value;
            Terms statusAggregation = (Terms)res.getAggregations().get("status");
            Terms.Bucket problemsBucket = statusAggregation.getBucketByKey("true");
            Terms.Bucket healthyBucket = statusAggregation.getBucketByKey("false");
            double sessionsWithProblems = problemsBucket != null ? (double)problemsBucket.getDocCount() : 0.0;
            double healthySessions = healthyBucket != null ? (double)healthyBucket.getDocCount() : 0.0;
            double sessionsNotAnalyzed = totalSessions - (sessionsWithProblems + healthySessions);
            jsonResponse.addProperty("totalSessions", totalSessions);
            jsonResponse.addProperty("sessionsWithProblemsPct", df.format(sessionsWithProblems / totalSessions * 100.0));
            jsonResponse.addProperty("healthySessionsPct", df.format(healthySessions / totalSessions * 100.0));
            jsonResponse.addProperty("sessionsNotAnalyzedPct", df.format(sessionsNotAnalyzed / totalSessions * 100.0));
        } catch (IOException var23) {
        } finally {
            if (client != this.defaultClient) {
                client.close();
            }

        }

        return jsonResponse;
    }

    private void updateDoc(String index, String docId, String jsonString) throws Exception {
        RestHighLevelClient client = null;

        try {
            client = this.getESClient();
            UpdateRequest request = new UpdateRequest(index, docId);
            request.doc(jsonString, XContentType.JSON);
            request.timeout(TimeValue.timeValueSeconds(20L));
            client.update(request, RequestOptions.DEFAULT);
        } catch (Exception var9) {
            if (var9 != null && !var9.getMessage().contains("Timeout connecting")) {
                throw var9;
            }

            log.error(var9.getMessage());
        } finally {
            if (client != this.defaultClient) {
                client.close();
            }

        }

    }

    public MultiValuedMap<String, JsonObject> getStreamIdKmsEventsMap(String uniqueSessionId) throws IOException {
        MultiValuedMap<String, JsonObject> kmsEvents = new ArrayListValuedHashMap();
        SearchResponse response = this.getPosibleErrors(1500, false, false, uniqueSessionId);
        if (response != null) {
            JsonObject responsePossibleErrors = JsonParser.parseString(response.toString()).getAsJsonObject().get("hits").getAsJsonObject();
            Iterator var5 = responsePossibleErrors.getAsJsonArray("hits").iterator();

            while(var5.hasNext()) {
                JsonElement possibleError = (JsonElement)var5.next();
                JsonObject event = possibleError.getAsJsonObject().getAsJsonObject("_source");
                String streamId = event.get("endpoint").getAsString();
                kmsEvents.put(streamId, event);
            }
        }

        return kmsEvents;
    }

    private JsonObject getConnectionSummary(String connectionId) throws IOException {
        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery("elastic_type:connectionSummary AND connectionId:" + connectionId);
        SearchResponse res = this.getDocuments(1, false, false, (QueryBuilder)queryBuilder);
        JsonObject json = JsonParser.parseString(res.toString()).getAsJsonObject();
        return json.get("hits").getAsJsonObject().get("hits").getAsJsonArray().get(0).getAsJsonObject().get("_source").getAsJsonObject();
    }

    private List<String> getSessionProblems(JsonObject sessionSummaryResponse) throws IOException {
        JsonObject sessionSummary = sessionSummaryResponse.getAsJsonArray("hits").get(0).getAsJsonObject().getAsJsonObject("_source");
        String uniqueSessionId = sessionSummary.get("uniqueSessionId").getAsString();
        MultiValuedMap<String, JsonObject> kmsEvents = this.getStreamIdKmsEventsMap(uniqueSessionId);
        List<String> problemList = new ArrayList();
        Iterator var6 = kmsEvents.asMap().entrySet().iterator();

        while(var6.hasNext()) {
            Map.Entry<String, Collection<JsonObject>> connectionKmsEvents = (Map.Entry)var6.next();
            problemList.addAll(this.checkSessionProblems((Collection)connectionKmsEvents.getValue()));
        }

        return problemList;
    }

    public JsonObject getTimelineEvents(String uniqueSessionId, int size) throws Exception {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.must(QueryBuilders.queryStringQuery("uniqueSessionId:\"" + uniqueSessionId + "\" AND (elastic_type:kms OR elastic_type:cdr OR elastic_type:webrtcDebug)"));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        sourceBuilder.from(0);
        sourceBuilder.size(size);
        sourceBuilder.timeout(new TimeValue(15L, TimeUnit.SECONDS));
        sourceBuilder.sort((new FieldSortBuilder("timestamp")).order(SortOrder.ASC));
        SearchRequest searchRequest = new SearchRequest(new String[]{"openvidu"});
        searchRequest.source(sourceBuilder);
        RestHighLevelClient client = null;
        SearchResponse res = null;

        try {
            client = this.getESClient();
            res = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (Exception var12) {
            if (var12 != null && !var12.getMessage().contains("Timeout connecting")) {
                throw var12;
            }

            log.error(var12.getMessage());
        } finally {
            if (client != this.defaultClient) {
                client.close();
            }

        }

        JsonObject json = JsonParser.parseString(res.toString()).getAsJsonObject();
        return json.get("hits").getAsJsonObject();
    }

    public List<String> checkSessionProblems(Collection<JsonObject> connectionKmsEvents) throws IOException {
        List<String> problemList = new ArrayList();
        boolean isVideoFlowingAnalyzed = false;
        boolean isAudioFlowingAnalyzed = false;

        for(int i = connectionKmsEvents.size() - 1; i >= 0; --i) {
            JsonObject kmsEvent = (JsonObject)connectionKmsEvents.toArray()[i];
            long kmsTimestamp = kmsEvent.get("timestamp").getAsLong();
            String eventState = kmsEvent.get("state").getAsString();
            String mediaType;
            String connectionId;
            if (eventState.equals("FLOWING") && (!isVideoFlowingAnalyzed || !isAudioFlowingAnalyzed)) {
                mediaType = kmsEvent.get("mediaType").getAsString();
                int msSinceEndpointCreation = kmsEvent.get("msSinceEndpointCreation").getAsInt();
                isVideoFlowingAnalyzed = isVideoFlowingAnalyzed || mediaType.equals(MediaType.VIDEO.name());
                isAudioFlowingAnalyzed = isAudioFlowingAnalyzed || mediaType.equals(MediaType.AUDIO.name());
                if (msSinceEndpointCreation > 5000) {
                    connectionId = Integer.toString(msSinceEndpointCreation / 1000) + "s";
                    problemList.add(SessionProblem.MEDIA_FLOWING_READY_TOO_LATE.getDescription(mediaType, connectionId));
                }
            } else if (!eventState.equals("NOT_FLOWING")) {
                if (eventState.equals(SessionProblem.ICE_FAILED.getValue())) {
                    problemList.add(SessionProblem.ICE_FAILED.getDescription());
                }
            } else {
                mediaType = kmsEvent.get("mediaType").getAsString();
                boolean flowingBackFound = false;

                JsonObject nextKmsEvent;
                String time;
                for(int j = i - 1; j >= 0; --j) {
                    nextKmsEvent = (JsonObject)connectionKmsEvents.toArray()[j];
                    String nextEventType = nextKmsEvent.get("type").getAsString();
                    if (nextEventType.equals("MediaFlowOutStateChanged")) {
                        time = nextKmsEvent.get("state").getAsString();
                        String nextMediaType = nextKmsEvent.get("mediaType").getAsString();
                        boolean isSameMediaType = mediaType.equals(nextMediaType);
                        if (isSameMediaType && time.equals("FLOWING")) {
                            flowingBackFound = true;
                            long nextTimestamp = nextKmsEvent.get("timestamp").getAsLong();
                            int diff = (int)(nextTimestamp - kmsTimestamp);
                            if (diff > 500) {
                                String time = diff > 1000 ? diff / 1000 + "s" : "" + diff + "ms";
                                problemList.add(SessionProblem.MEDIA_FLOWING_BACK_TOO_LATE.getDescription(mediaType, time));
                            }
                            break;
                        }
                    }
                }

                if (!flowingBackFound) {
                    connectionId = kmsEvent.get("connectionId").getAsString();
                    nextKmsEvent = this.getConnectionSummary(connectionId);
                    int diff = (int)(nextKmsEvent.get("destroyedAt").getAsLong() - kmsTimestamp);
                    if (diff > 1000) {
                        time = Integer.toString(diff / 1000) + "s";
                        problemList.add(SessionProblem.MEDIA_FLOWING_NOT_FOUND.getDescription(mediaType, time));
                    }
                }
            }
        }

        return problemList;
    }

    private SearchResponse getPosibleErrors(int size, boolean searchAfter, boolean storeAsLastSearch, String uniqueSessionId) throws IOException {
        String queryErrors = "uniqueSessionId:\"" + uniqueSessionId + "\" AND elastic_type:kms AND ((type:MediaFlowOutStateChanged) OR (type:IceComponentStateChanged AND state:FAILED))";
        QueryBuilder queryBuilder = QueryBuilders.queryStringQuery(queryErrors);
        return this.getDocuments(size, searchAfter, storeAsLastSearch, (QueryBuilder)queryBuilder);
    }

    public boolean isElasticsearchDefined() {
        return this.openviduConfigPro.isElasticsearchDefined();
    }
}
