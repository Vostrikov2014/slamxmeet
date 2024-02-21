//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account.usage;

import com.google.gson.JsonObject;
import io.openvidu.server.pro.infrastructure.InstanceType;

public abstract class NodeUsageInfo {
    private String id;
    private String environmentId;
    private String ip;
    private InstanceType type;
    private int cores = -1;
    private long initTime = -1L;
    private long endTime = -1L;
    private long quantity = -1L;
    private boolean reconnected = false;

    public NodeUsageInfo(String id, String environmentId, String ip, InstanceType type, int cores, long initTime, long endTime, long quantity) {
        this.id = id;
        this.environmentId = environmentId;
        this.ip = ip;
        this.type = type;
        this.cores = cores;
        this.initTime = initTime;
        this.endTime = endTime;
        this.quantity = quantity;
    }

    public String getId() {
        return this.id;
    }

    public String getEnvironmentId() {
        return this.environmentId;
    }

    public String getIp() {
        return this.ip;
    }

    public InstanceType getType() {
        return this.type;
    }

    public int getCores() {
        return this.cores;
    }

    public long getInitTime() {
        return this.initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getQuantity() {
        return this.quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public boolean isReconnected() {
        return this.reconnected;
    }

    public void setReconnected(boolean reconnected) {
        this.reconnected = reconnected;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("environmentId", this.environmentId);
        json.addProperty("ip", this.ip);
        json.addProperty("type", this.type.name());
        json.addProperty("cores", this.cores);
        json.addProperty("initTime", this.initTime);
        json.addProperty("endTime", this.endTime);
        json.addProperty("duration", (this.endTime - this.initTime) / 1000L);
        json.addProperty("quantity", this.quantity);
        return json;
    }
}
