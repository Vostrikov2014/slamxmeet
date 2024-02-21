//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account.usage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.pro.infrastructure.InstanceType;
import org.kurento.client.ServerInfo;

public class KmsNodeUsageInfo extends NodeUsageInfo {
    private ServerInfo kmsServerInfo;

    public KmsNodeUsageInfo(String id, String environmentId, String ip, int cores, long initTime, ServerInfo kmsServerInfo) {
        super(id, environmentId, ip, InstanceType.mediaServer, cores, initTime, -1L, -1L);
        this.kmsServerInfo = kmsServerInfo;
    }

    public KmsNodeUsageInfo(NodeUsageInfo info, long endTime, long quantity, ServerInfo kmsServerInfo) {
        super(info.getId(), info.getEnvironmentId(), info.getIp(), InstanceType.mediaServer, info.getCores(), info.getInitTime(), endTime, quantity);
        this.kmsServerInfo = kmsServerInfo;
    }

    public ServerInfo getServerInfo() {
        return this.kmsServerInfo;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonObject extraInfo = new JsonObject();
        extraInfo.addProperty("version", this.kmsServerInfo.getVersion());
        JsonArray modules = new JsonArray();
        this.kmsServerInfo.getModules().forEach((module) -> {
            modules.add(module.getName());
        });
        extraInfo.add("modules", modules);
        json.add("extraInfo", extraInfo);
        return json;
    }
}
