//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.infrastructure.autoscaling.AutoscalingConfig;

import java.util.ArrayList;
import java.util.List;

public class AutoscalingResult {
    private SystemStatus status;
    private AutoscalingConfig config;
    private boolean doNothing = true;
    private int numNodesToLaunch;
    private List<Node> relaunchWaitingIdleToTerminateNodes = new ArrayList();
    private List<Node> relaunchCanceledNodes = new ArrayList();
    private List<Node> terminateLaunchingNodes = new ArrayList();
    private List<Node> nodesToTerminate = new ArrayList();

    public AutoscalingResult(SystemStatus status, AutoscalingConfig config) {
        this.status = status;
        this.config = config;
    }

    public boolean isDoNothing() {
        return this.doNothing;
    }

    public List<Node> getRelaunchWaitingIdleToTerminateNodes() {
        return this.relaunchWaitingIdleToTerminateNodes;
    }

    public List<Node> getRelaunchCanceledNodes() {
        return this.relaunchCanceledNodes;
    }

    public int getNumNodesToLaunch() {
        return this.numNodesToLaunch;
    }

    public List<Node> getTerminateLaunchingNodes() {
        return this.terminateLaunchingNodes;
    }

    public List<Node> getNodesToTerminate() {
        return this.nodesToTerminate;
    }

    public AutoscalingResult relaunchWaitingIdleToTerminateNodes(List<Node> relaunchWaitingIdleToTerminateNodes) {
        this.relaunchWaitingIdleToTerminateNodes = relaunchWaitingIdleToTerminateNodes;
        this.doNothing = false;
        return this;
    }

    public AutoscalingResult relaunchCanceledNodes(List<Node> relaunchCanceledNodes) {
        this.relaunchCanceledNodes = relaunchCanceledNodes;
        this.doNothing = false;
        return this;
    }

    public AutoscalingResult numNodesToLaunch(int numNodesToLaunch) {
        this.numNodesToLaunch = numNodesToLaunch;
        this.doNothing = false;
        return this;
    }

    public AutoscalingResult terminateLaunchingNodes(List<Node> terminateLaunchingNodes) {
        this.terminateLaunchingNodes = terminateLaunchingNodes;
        this.doNothing = false;
        return this;
    }

    public AutoscalingResult nodesToTerminate(List<Node> nodesToTerminate) {
        this.nodesToTerminate = nodesToTerminate;
        this.doNothing = false;
        return this;
    }

    public String toString() {
        return "AutoscalingResult [doNothing=" + this.doNothing + ", numNodesToLaunch=" + this.numNodesToLaunch + ", relaunchWaitingIdleToTerminateNodes=" + this.relaunchWaitingIdleToTerminateNodes + ", relaunchCanceledNodes=" + this.relaunchCanceledNodes + ", terminateLaunchingNodes=" + this.terminateLaunchingNodes + ", nodesToTerminate=" + this.nodesToTerminate + "]";
    }

    public JsonObject toJson() {
        JsonObject mediaNodes = new JsonObject();
        JsonObject launchMediaNodes = new JsonObject();
        JsonObject terminateMediaNodes = new JsonObject();
        JsonArray launchIdleNodes = new JsonArray();
        JsonArray launchCanceledNodes = new JsonArray();
        JsonArray terminateLaunchingNodes = new JsonArray();
        JsonArray terminateRunningNodes = new JsonArray();
        this.relaunchWaitingIdleToTerminateNodes.forEach((n) -> {
            launchIdleNodes.add(n.toJson());
        });
        this.relaunchCanceledNodes.forEach((n) -> {
            launchCanceledNodes.add(n.toJson());
        });
        this.terminateLaunchingNodes.forEach((n) -> {
            terminateLaunchingNodes.add(n.toJson());
        });
        this.nodesToTerminate.forEach((n) -> {
            terminateRunningNodes.add(n.toJson());
        });
        launchMediaNodes.addProperty("total", this.numNodesToLaunch + this.relaunchWaitingIdleToTerminateNodes.size());
        launchMediaNodes.add("waitingIdleToTerminateNodes", launchIdleNodes);
        launchMediaNodes.add("canceledNodes", launchCanceledNodes);
        launchMediaNodes.addProperty("newNodes", this.numNodesToLaunch);
        terminateMediaNodes.addProperty("total", this.nodesToTerminate.size() + this.terminateLaunchingNodes.size());
        terminateMediaNodes.add("launchingNodes", terminateLaunchingNodes);
        terminateMediaNodes.add("runningNodes", terminateRunningNodes);
        mediaNodes.add("launch", launchMediaNodes);
        mediaNodes.add("terminate", terminateMediaNodes);
        JsonObject system = new JsonObject();
        system.add("config", this.config.toJson());
        system.add("status", this.status.toJson());
        JsonObject json = new JsonObject();
        json.addProperty("reason", this.generateReason());
        json.add("mediaNodes", mediaNodes);
        json.add("system", system);
        return json;
    }

    public String generateReason() {
        if (this.isDoNothing()) {
            return "Autoscaling doing nothing";
        } else {
            String reason = "";
            if (this.status.getNumNodes() < this.config.getMinNodes()) {
                reason = reason + "Minimum number of nodes (" + this.config.getMinNodes() + ") not reached. ";
            } else if (this.status.getNumNodes() > this.config.getMaxNodes()) {
                reason = reason + "Maximum number of nodes (" + this.config.getMaxNodes() + ") exceeded. ";
            }

            if (this.status.getAvgLoad() > this.config.getMaxAvgLoad()) {
                reason = reason + "The cluster average load (" + this.formatLoad(this.status.getAvgLoad()) + ") is above its limits [" + this.formatLoad(this.config.getMinAvgLoad()) + ", " + this.formatLoad(this.config.getMaxAvgLoad()) + "] ";
                if (this.status.getNumNodes() < this.config.getMaxNodes()) {
                    reason = reason + "and the upper limit of Media Nodes (" + this.config.getMaxNodes() + ") has not been reached. ";
                } else {
                    reason = reason + "but the upper limit of Media Nodes (" + this.config.getMaxNodes() + ") has already been reached. ";
                }
            } else if (this.status.getAvgLoad() < this.config.getMinAvgLoad()) {
                reason = reason + "The cluster average load (" + this.formatLoad(this.status.getAvgLoad()) + ") is below its limits [" + this.formatLoad(this.config.getMinAvgLoad()) + ", " + this.formatLoad(this.config.getMaxAvgLoad()) + "] ";
                if (this.status.getNumNodes() > this.config.getMinNodes()) {
                    reason = reason + "and the lower limit of Media Nodes (" + this.config.getMinNodes() + ") has not been reached. ";
                } else {
                    reason = reason + "but the lower limit of Media Nodes  (" + this.config.getMinNodes() + ") has already been reached. ";
                }
            } else {
                reason = reason + " The cluster average load is within the range [" + this.formatLoad(this.config.getMinAvgLoad()) + ", " + this.formatLoad(this.config.getMaxAvgLoad()) + "]. ";
            }

            reason = reason + "Current number of active nodes is " + this.status.getNumNodes() + " (" + this.status.getLaunchingNodes().size() + " launching and " + this.status.getRunningNodes().size() + " running). ";
            if (this.relaunchWaitingIdleToTerminateNodes.size() > 0) {
                reason = reason + this.relaunchWaitingIdleToTerminateNodes.size() + " waiting-idle-to-terminate Media Nodes will be returned to running status. ";
            }

            if (this.relaunchCanceledNodes.size() > 0) {
                reason = reason + this.relaunchCanceledNodes.size() + " canceled Media Nodes will be returned to launching status. ";
            }

            if (this.numNodesToLaunch > 0) {
                reason = reason + this.numNodesToLaunch + " new Media Nodes will be launched. ";
            }

            if (this.terminateLaunchingNodes.size() > 0) {
                reason = reason + this.terminateLaunchingNodes.size() + " launching Media Nodes will be canceled. ";
            }

            if (this.nodesToTerminate.size() > 0) {
                reason = reason + this.nodesToTerminate.size() + " Media Nodes will be terminated. ";
            }

            return reason.trim();
        }
    }

    private String formatLoad(double load) {
        Object[] var10001 = new Object[]{load};
        return String.format("%.2f", var10001) + "%";
    }
}
