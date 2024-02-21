//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.account.usage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.config.OpenViduEdition;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import java.util.Collection;

public class ClusterUsageInfo {
    private String clusterId;
    private OpenViduClusterEnvironment environment;
    private long quantity;
    private Collection<NodeUsageInfo> nodes;
    private long timestamp;
    private OpenViduEdition edition;
    private boolean isInitialUsage = false;

    public ClusterUsageInfo(String clusterId, OpenViduClusterEnvironment environment, long quantity, long timestamp, Collection<NodeUsageInfo> nodes, OpenViduEdition edition) {
        this.clusterId = clusterId;
        this.environment = environment;
        this.quantity = quantity;
        this.nodes = nodes;
        this.timestamp = timestamp;
        this.edition = edition;
    }

    public String toJsonString() {
        JsonArray jsonArray = new JsonArray();
        this.nodes.forEach((node) -> {
            jsonArray.add(node.toJson());
        });
        JsonObject clusterJson = new JsonObject();
        clusterJson.addProperty("id", this.clusterId);
        clusterJson.addProperty("environment", this.environment.name());
        clusterJson.add("nodes", jsonArray);
        JsonObject json = new JsonObject();
        json.add("cluster", clusterJson);
        json.addProperty("quantity", this.quantity);
        json.addProperty("timestamp", this.timestamp);
        json.addProperty("edition", this.edition.name());
        json.addProperty("isInitialUsage", this.isInitialUsage);
        return json.toString();
    }

    public long getQuantity() {
        return this.quantity;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public OpenViduEdition getEdition() {
        return this.edition;
    }

    public void asInitialUsage() {
        this.isInitialUsage = true;
    }

    public boolean isInitialUsage() {
        return this.isInitialUsage;
    }

    public Collection<NodeUsageInfo> getNodes() {
        return this.nodes;
    }
}
