//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.cdr;

import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDREventName;

public class CDREventNodeRecovered extends CDREvent {
    private String id;
    private String environmentId;
    private String ip;
    private String uri;
    private NodeRole nodeRole;
    private String clusterId;

    public CDREventNodeRecovered(Long timeStamp, String id, String environmentId, String ip, String uri, String clusterId) {
        super(CDREventName.nodeRecovered, (String)null, (String)null, timeStamp);
        this.id = id;
        this.environmentId = environmentId;
        this.ip = ip;
        this.uri = uri;
        this.nodeRole = NodeRole.medianode;
        this.clusterId = clusterId;
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
        json.addProperty("clusterId", this.clusterId);
        return json;
    }
}
