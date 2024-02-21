//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.cdr;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDREventName;
import java.util.List;

public class CDREventNodeCrashed extends CDREvent {
    private String id;
    private String environmentId;
    private String ip;
    private String uri;
    private NodeRole nodeRole;
    private Long timeOfDisconnection;
    private String clusterId;
    private List<String> sessionIds;
    private List<String> recordingIds;
    private List<String> broadcasts;

    public CDREventNodeCrashed(Long timestamp, String id, String environmentId, String ip, String uri, NodeRole nodeRole, Long timeOfDisconnection, String clusterId, List<String> sessionIds, List<String> recordingIds, List<String> broadcasts) {
        super(CDREventName.nodeCrashed, (String)null, (String)null, timestamp);
        this.id = id;
        this.environmentId = environmentId;
        this.ip = ip;
        this.uri = uri;
        this.nodeRole = nodeRole;
        this.timeOfDisconnection = timeOfDisconnection;
        this.clusterId = clusterId;
        this.sessionIds = sessionIds;
        this.recordingIds = recordingIds;
        this.broadcasts = broadcasts;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("id", this.id);
        if (this.environmentId != null) {
            json.addProperty("environmentId", this.environmentId);
        }

        json.addProperty("ip", this.ip);
        json.addProperty("uri", this.uri);
        json.addProperty("nodeRole", this.nodeRole.name());
        json.addProperty("timeOfDisconnection", this.timeOfDisconnection);
        json.addProperty("clusterId", this.clusterId);
        JsonArray sIds = new JsonArray();
        this.sessionIds.forEach((sId) -> {
            sIds.add(sId);
        });
        json.add("sessionIds", sIds);
        JsonArray rIds = new JsonArray();
        this.recordingIds.forEach((rId) -> {
            rIds.add(rId);
        });
        json.add("recordingIds", rIds);
        JsonArray bs = new JsonArray();
        this.broadcasts.forEach((b) -> {
            bs.add(b);
        });
        json.add("broadcasts", bs);
        return json;
    }

    public Long getTimeOfDisconnection() {
        return this.timeOfDisconnection;
    }
}
