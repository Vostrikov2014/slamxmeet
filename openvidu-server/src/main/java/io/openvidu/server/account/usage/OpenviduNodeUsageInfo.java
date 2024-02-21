//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account.usage;

import com.google.gson.JsonObject;
import io.openvidu.server.pro.infrastructure.InstanceType;

public class OpenviduNodeUsageInfo extends NodeUsageInfo {
    private String version;

    public OpenviduNodeUsageInfo(String id, String environmentId, String ip, int cores, long initTime, String version) {
        super(id, environmentId, ip, InstanceType.openvidu, cores, initTime, -1L, -1L);
        this.version = version;
    }

    public OpenviduNodeUsageInfo(NodeUsageInfo info, long endTime, long quantity, String version) {
        super(info.getId(), info.getEnvironmentId(), info.getIp(), InstanceType.openvidu, info.getCores(), info.getInitTime(), endTime, quantity);
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonObject extraInfo = new JsonObject();
        extraInfo.addProperty("version", this.version);
        json.add("extraInfo", extraInfo);
        return json;
    }
}
