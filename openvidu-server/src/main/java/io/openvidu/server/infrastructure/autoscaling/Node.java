//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import com.google.gson.JsonObject;
import io.openvidu.server.infrastructure.InstanceStatus;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Node {
    private double load;
    private long launchingTime;
    private InstanceStatus status;
    private String instanceId;
    private String instanceEnvironemntId;
    private String instanceIp;

    public Node(double load, InstanceStatus status, long launchingTime, String instanceId, String instanceEnvironemntId, String instanceIp) {
        this.load = load;
        this.status = status;
        this.launchingTime = launchingTime;
        this.instanceId = instanceId;
        this.instanceEnvironemntId = instanceEnvironemntId;
        this.instanceIp = instanceIp;
    }

    public double getLoad() {
        return this.load;
    }

    public long getLaunchingTime() {
        return this.launchingTime;
    }

    public InstanceStatus getStatus() {
        return this.status;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public String toString() {
        String var10000 = this.instanceId;
        return "{ id: " + var10000 + ", environmentId: " + this.instanceEnvironemntId + ", ip: " + this.instanceIp + ", status: " + this.status.name() + ", load: " + this.load + "%, launchingTime: " + (new SimpleDateFormat("MMM dd HH:mm:ss")).format(new Date(this.launchingTime)) + " }";
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.instanceId);
        json.addProperty("environmentId", this.instanceEnvironemntId);
        json.addProperty("ip", this.instanceIp);
        json.addProperty("load", this.load);
        json.addProperty("status", this.status.name());
        return json;
    }
}
