//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import io.openvidu.server.infrastructure.autoscaling.AutoscalingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AutoscalingManager {
    public AutoscalingManager() {
    }

    public AutoscalingResult evalAutoscaling(SystemStatus status, AutoscalingConfig config) {
        int numNodesToLaunch;
        List waitingNodes;
        List canceledNodes;
        if (status.getNumNodes() >= config.getMinNodes() && (!(status.getAvgLoad() > config.getMaxAvgLoad()) || status.getNumNodes() >= config.getMaxNodes())) {
            if (status.getNumNodes() > config.getMaxNodes() || status.getAvgLoad() < config.getMinAvgLoad() && status.getNumNodes() > config.getMinNodes()) {
                numNodesToLaunch = this.calculateNumNodesToTerminate(status, config);
                waitingNodes = this.sortedByLoadAsc(status.getRunningNodes());
                if (status.hasLaunchingNodes()) {
                    canceledNodes = this.sortedByLaunchDateAsc(status.getLaunchingNodes());
                    return canceledNodes.size() >= numNodesToLaunch ? (new AutoscalingResult(status, config)).terminateLaunchingNodes(canceledNodes.subList(0, numNodesToLaunch)) : (new AutoscalingResult(status, config)).terminateLaunchingNodes(canceledNodes).nodesToTerminate(waitingNodes.subList(0, numNodesToLaunch - canceledNodes.size()));
                } else {
                    return (new AutoscalingResult(status, config)).nodesToTerminate(waitingNodes.subList(0, numNodesToLaunch));
                }
            } else {
                return new AutoscalingResult(status, config);
            }
        } else {
            numNodesToLaunch = this.calculateNumNodesToLaunch(status, config);
            if (!status.hasWaitingIdleToTerminateNodes() && !status.hasCanceledNodes()) {
                return (new AutoscalingResult(status, config)).numNodesToLaunch(numNodesToLaunch);
            } else {
                waitingNodes = this.sortedByLoadDesc(status.getWaitingIdleToTerminateNodes());
                canceledNodes = this.sortedByLaunchDateAsc(status.getCanceledNodes());
                if (waitingNodes.size() + canceledNodes.size() >= numNodesToLaunch) {
                    return waitingNodes.size() >= numNodesToLaunch ? (new AutoscalingResult(status, config)).relaunchWaitingIdleToTerminateNodes(waitingNodes.subList(0, numNodesToLaunch)) : (new AutoscalingResult(status, config)).relaunchWaitingIdleToTerminateNodes(waitingNodes).relaunchCanceledNodes(canceledNodes.subList(0, numNodesToLaunch - waitingNodes.size()));
                } else {
                    return (new AutoscalingResult(status, config)).relaunchWaitingIdleToTerminateNodes(waitingNodes).relaunchCanceledNodes(canceledNodes).numNodesToLaunch(numNodesToLaunch - (waitingNodes.size() + canceledNodes.size()));
                }
            }
        }
    }

    private int calculateNumNodesToLaunch(SystemStatus status, AutoscalingConfig config) {
        int idealNumberOfRemainingNodes = (int)(status.getTotalLoad() / config.getMaxAvgLoad()) + (status.getTotalLoad() % config.getMaxAvgLoad() > 0.0 ? 1 : 0);
        int numNodesToLaunchIfInRange = Math.min(config.getMaxNodes(), idealNumberOfRemainingNodes) - status.getNumNodes();
        int minNumNodesToLaunchIfBelowRange = config.getMinNodes() - status.getNumNodes();
        return Math.max(numNodesToLaunchIfInRange, minNumNodesToLaunchIfBelowRange);
    }

    private int calculateNumNodesToTerminate(SystemStatus status, AutoscalingConfig config) {
        int idealNumberOfRemainingNodes = (int)(status.getTotalLoad() / config.getMinAvgLoad());
        int numNodesToTerminateIfInRange = status.getNumNodes() - Math.max(config.getMinNodes(), idealNumberOfRemainingNodes);
        int minNumNodesToTerminateIfAboveRange = status.getNumNodes() - config.getMaxNodes();
        return Math.max(numNodesToTerminateIfInRange, minNumNodesToTerminateIfAboveRange);
    }

    private List<Node> sortedByLoadAsc(List<Node> nodes) {
        List<Node> result = new ArrayList(nodes);
        nodes.sort(Comparator.comparingDouble(Node::getLoad));
        return result;
    }

    private List<Node> sortedByLoadDesc(List<Node> nodes) {
        List<Node> result = this.sortedByLoadAsc(nodes);
        Collections.reverse(result);
        return result;
    }

    private List<Node> sortedByLaunchDateAsc(List<Node> nodes) {
        List<Node> result = new ArrayList(nodes);
        nodes.sort(Comparator.comparing(Node::getLaunchingTime));
        return result;
    }
}
