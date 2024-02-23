//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import com.google.gson.JsonObject;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import io.openvidu.server.infrastructure.InstanceStatus;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class Instance {
    private InstanceType type;
    private String id;
    private String environmentId;
    private String ip;
    private InstanceStatus status;
    private long launchingTime;
    private final int METRIC_BUFFER_SIZE = 1200;
    private CircularFifoQueue<Double> cpuLoad = new CircularFifoQueue(1200);

    public Instance(InstanceType type, String id) {
        this.type = type;
        this.id = id;
        this.status = InstanceStatus.launching;
    }

    public InstanceType getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public String getEnvironmentId() {
        return this.environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public InstanceStatus getStatus() {
        return this.status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public long getLaunchingTime() {
        return this.launchingTime;
    }

    public synchronized double getCpuLoad() {
        if (this.cpuLoad.isEmpty()) {
            return 0.0;
        } else {
            Double two = null;
            Double three = null;
            Double one = (Double)this.cpuLoad.get(this.cpuLoad.size() - 1);
            if (this.cpuLoad.size() > 1) {
                two = (Double)this.cpuLoad.get(this.cpuLoad.size() - 2);
            }

            if (this.cpuLoad.size() > 2) {
                three = (Double)this.cpuLoad.get(this.cpuLoad.size() - 3);
            }

            double returnedValue;
            if (three != null) {
                returnedValue = (one + two + three) / 3.0;
            } else if (two != null) {
                returnedValue = (one + two) / 2.0;
            } else {
                returnedValue = one;
            }

            return Double.parseDouble((new DecimalFormat("##.##")).format(returnedValue));
        }
    }

    public synchronized void addCpuLoad(Double load) {
        this.cpuLoad.add(load);
    }

    public void setLaunchingTime(long launchingTime) {
        this.launchingTime = launchingTime;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("object", "mediaNode");
        json.addProperty("environmentId", this.environmentId);
        json.addProperty("ip", this.ip);
        json.addProperty("status", this.status.toString());
        json.addProperty("launchingTime", this.launchingTime);
        return json;
    }

    public String toString() {
        String var10000 = this.type.name();
        return "{type: " + var10000 + ", ip: " + this.ip + ", id: " + this.id + ", environmentId: " + this.environmentId + ", status: " + this.status.toString() + ", launchingTime: " + LocalDateTime.ofInstant(Instant.ofEpochMilli(this.launchingTime), ZoneId.systemDefault()).toString() + "}";
    }
}
