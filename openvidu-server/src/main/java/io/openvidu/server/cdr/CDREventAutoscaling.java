//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.cdr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDREventName;
import io.openvidu.server.pro.infrastructure.autoscaling.AutoscalingResult;

public class CDREventAutoscaling extends CDREvent {
    private AutoscalingResult autoscalingResult;
    private String clusterId;

    public CDREventAutoscaling(AutoscalingResult autoscalingResult, String clusterId) {
        super(CDREventName.autoscaling, (String)null, (String)null, System.currentTimeMillis());
        this.autoscalingResult = autoscalingResult;
        this.clusterId = clusterId;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("clusterId", this.clusterId);
        JsonObject result = this.autoscalingResult.toJson();
        result.entrySet().forEach((entry) -> {
            json.add((String)entry.getKey(), (JsonElement)entry.getValue());
        });
        return json;
    }
}
