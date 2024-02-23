//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.infrastructure.InstanceStatus;
import java.util.List;
import java.util.stream.Collectors;

public class SystemStatus {
    private int numNodes;
    private double totalLoad;
    private double avgLoad;
    private List<Node> runningNodes;
    private List<Node> launchingNodes;
    private List<Node> waitingIdleToTerminateNodes;
    private List<Node> canceledNodes;

    public SystemStatus(List<Node> nodes) {
        this.runningNodes = this.filterNodesWithStatus(nodes, InstanceStatus.running);
        this.launchingNodes = this.filterNodesWithStatus(nodes, InstanceStatus.launching);
        this.waitingIdleToTerminateNodes = this.filterNodesWithStatus(nodes, InstanceStatus.waitingIdleToTerminate);
        this.canceledNodes = this.filterNodesWithStatus(nodes, InstanceStatus.canceled);
        this.numNodes = this.getLaunchingNodes().size() + this.getRunningNodes().size();
        this.totalLoad = this.runningNodes.stream().mapToDouble(Node::getLoad).sum() + this.waitingIdleToTerminateNodes.stream().mapToDouble(Node::getLoad).sum();
        this.avgLoad = this.numNodes == 0 ? 0.0 : this.totalLoad / (double)this.numNodes;
    }

    private List<Node> filterNodesWithStatus(List<Node> nodes, InstanceStatus status) {
        return (List)nodes.stream().filter((n) -> {
            return n.getStatus() == status;
        }).collect(Collectors.toList());
    }

    public int getNumNodes() {
        return this.numNodes;
    }

    public double getTotalLoad() {
        return this.totalLoad;
    }

    public double getAvgLoad() {
        return this.avgLoad;
    }

    public List<Node> getRunningNodes() {
        return this.runningNodes;
    }

    public boolean hasLaunchingNodes() {
        return this.launchingNodes != null && !this.launchingNodes.isEmpty();
    }

    public List<Node> getLaunchingNodes() {
        return this.launchingNodes;
    }

    public boolean hasWaitingIdleToTerminateNodes() {
        return this.waitingIdleToTerminateNodes != null && !this.waitingIdleToTerminateNodes.isEmpty();
    }

    public List<Node> getWaitingIdleToTerminateNodes() {
        return this.waitingIdleToTerminateNodes;
    }

    public boolean hasCanceledNodes() {
        return this.canceledNodes != null && !this.canceledNodes.isEmpty();
    }

    public List<Node> getCanceledNodes() {
        return this.canceledNodes;
    }

    public String toString() {
        int var10000 = this.numNodes;
        return "SystemStatus [numNodes=" + var10000 + ", totalLoad=" + this.totalLoad + ", avgLoad=" + this.avgLoad + ", runningNodes=" + this.runningNodes.toString() + ", launchingNodes=" + this.launchingNodes.toString() + ", waitingIdleToTerminateNodes=" + this.waitingIdleToTerminateNodes.toString() + ", canceledNodes=" + this.canceledNodes.toString() + "]";
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("numNodes", this.numNodes);
        json.addProperty("totalLoad", this.totalLoad);
        json.addProperty("avgLoad", this.avgLoad);
        JsonArray jsonRunningNodes = new JsonArray();
        JsonArray jsonLaunchingNodes = new JsonArray();
        JsonArray jsonWaitingIdleToTerminateNodes = new JsonArray();
        JsonArray jsonCanceledNodes = new JsonArray();
        this.runningNodes.forEach((n) -> {
            jsonRunningNodes.add(n.toJson());
        });
        this.launchingNodes.forEach((n) -> {
            jsonLaunchingNodes.add(n.toJson());
        });
        this.waitingIdleToTerminateNodes.forEach((n) -> {
            jsonWaitingIdleToTerminateNodes.add(n.toJson());
        });
        this.canceledNodes.forEach((n) -> {
            jsonCanceledNodes.add(n.toJson());
        });
        json.add("runningNodes", jsonRunningNodes);
        json.add("launchingNodes", jsonLaunchingNodes);
        json.add("waitingIdleToTerminateNodes", jsonWaitingIdleToTerminateNodes);
        json.add("canceledNodes", jsonCanceledNodes);
        return json;
    }
}
