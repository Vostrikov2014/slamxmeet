//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.cdr;

import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDREventName;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.pro.infrastructure.Instance;
import io.openvidu.server.pro.infrastructure.InstanceStatus;

public class CDREventMediaNodeStatus extends CDREvent {
    private Kms kms;
    private Instance mediaNode;
    private InstanceStatus newStatus;
    private InstanceStatus oldStatus;
    private String clusterId;
    private String uri;
    private String environmentId;

    public CDREventMediaNodeStatus(long timestamp, Kms kms, String environmentId, InstanceStatus newStatus, InstanceStatus oldStatus, String clusterId) {
        super(CDREventName.mediaNodeStatusChanged, (String)null, (String)null, timestamp);
        this.kms = kms;
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
        this.environmentId = environmentId;
        this.clusterId = clusterId;
    }

    public CDREventMediaNodeStatus(Instance mediaNode, String uri, InstanceStatus newStatus, InstanceStatus oldStatus, String clusterId) {
        super(CDREventName.mediaNodeStatusChanged, (String)null, (String)null, System.currentTimeMillis());
        this.mediaNode = mediaNode;
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
        this.uri = uri;
        this.clusterId = clusterId;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        if (this.mediaNode != null) {
            json.addProperty("id", this.mediaNode.getId());
            json.addProperty("environmentId", this.mediaNode.getEnvironmentId());
            json.addProperty("ip", this.mediaNode.getIp());
            json.addProperty("uri", this.uri);
        } else {
            json.addProperty("id", this.kms.getId());
            json.addProperty("environmentId", this.environmentId);
            json.addProperty("ip", this.kms.getIp());
            json.addProperty("uri", this.kms.getUri());
        }

        json.addProperty("newStatus", this.newStatus.toString());
        json.addProperty("oldStatus", this.oldStatus != null ? this.oldStatus.toString() : null);
        json.addProperty("clusterId", this.clusterId);
        return json;
    }

    public Instance getMediaNode() {
        return this.mediaNode;
    }

    public Kms getKms() {
        return this.kms;
    }

    public String getMediaNodeId() {
        return this.mediaNode != null ? this.mediaNode.getId() : this.kms.getId();
    }

    public String getMediaNodeEnvironmentId() {
        return this.mediaNode != null ? this.mediaNode.getEnvironmentId() : this.environmentId;
    }

    public String getMediaNodeIp() {
        return this.mediaNode != null ? this.mediaNode.getIp() : this.kms.getIp();
    }

    public InstanceStatus getNewMediaNodeStatus() {
        return this.newStatus;
    }

    public InstanceStatus getOldMediaNodeStatus() {
        return this.oldStatus;
    }
}
