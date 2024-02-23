//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.monitoring;

import com.google.gson.JsonObject;

public class NetworkQualityStats {
    private String sessionId;
    private String connectionId;
    private Integer newValue;
    private Integer oldValue;
    private Long timestamp;

    public NetworkQualityStats(JsonObject params, String sessionId) {
        this.sessionId = sessionId;
        this.connectionId = params.get("connectionId").getAsString();
        this.newValue = params.get("newValue").getAsInt();
        if (!params.get("oldValue").isJsonNull()) {
            this.oldValue = params.get("oldValue").getAsInt();
        }

        this.timestamp = System.currentTimeMillis();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("sessionId", this.sessionId);
        json.addProperty("connectionId", this.connectionId);
        json.addProperty("newValue", this.newValue);
        json.addProperty("oldValue", this.oldValue);
        json.addProperty("timestamp", this.timestamp);
        return json;
    }
}
