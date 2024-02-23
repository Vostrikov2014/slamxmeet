//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import com.google.gson.JsonObject;

public class AutoscalingConfig {
    private int maxNodes;
    private int minNodes;
    private double maxAvgLoad;
    private double minAvgLoad;

    public AutoscalingConfig(int maxNodes, int minNodes, double maxAvgLoad, double minAvgLoad) {
        this.maxNodes = maxNodes;
        this.minNodes = minNodes;
        this.maxAvgLoad = maxAvgLoad;
        this.minAvgLoad = minAvgLoad;
    }

    public int getMaxNodes() {
        return this.maxNodes;
    }

    public int getMinNodes() {
        return this.minNodes;
    }

    public double getMaxAvgLoad() {
        return this.maxAvgLoad;
    }

    public double getMinAvgLoad() {
        return this.minAvgLoad;
    }

    public String toString() {
        return "AutoscalingConfig [maxNodes=" + this.maxNodes + ", minNodes=" + this.minNodes + ", maxAvgLoad=" + this.maxAvgLoad + ", minAvgLoad=" + this.minAvgLoad + "]";
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("maxNodes", this.maxNodes);
        json.addProperty("minNodes", this.minNodes);
        json.addProperty("maxAvgLoad", this.maxAvgLoad);
        json.addProperty("minAvgLoad", this.minAvgLoad);
        return json;
    }
}
