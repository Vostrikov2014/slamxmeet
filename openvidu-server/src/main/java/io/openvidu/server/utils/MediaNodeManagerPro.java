//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.utils;

import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.account.ClusterUsageService;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.utils.MediaNodeManager;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

public class MediaNodeManagerPro implements MediaNodeManager {
    private static final Logger log = LoggerFactory.getLogger(MediaNodeManagerPro.class);
    @Autowired
    private ClusterUsageService clusterUsageService;
    @Autowired
    private InfrastructureManager infrastructureManager;

    public MediaNodeManagerPro() {
    }

    public void mediaNodeUsageRegistration(Kms kms, long timeOfConnection, Collection<Kms> existingKmss, boolean nodeRecovered) {
        String environmentId = null;
        Instance instance = this.infrastructureManager.getInstance(kms.getId());
        if (instance != null) {
            environmentId = instance.getEnvironmentId();
        }

        this.clusterUsageService.registerMediaNode(kms, timeOfConnection, environmentId, existingKmss, nodeRecovered);
    }

    public void mediaNodeUsageDeregistration(String mediaNodeId, long timeOfDisconnection) {
        this.clusterUsageService.deregisterMediaNode(mediaNodeId, timeOfDisconnection);
    }

    public void dropIdleMediaNode(String mediaNodeId) {
        try {
            this.infrastructureManager.removeMediaNode(mediaNodeId, "if-no-sessions", false, true, true);
        } catch (ResponseStatusException var3) {
            log.warn("Could not terminate instance {}: {}", mediaNodeId, var3.getMessage());
        }

    }

    public boolean isLaunching(String mediaNodeId) {
        return this.infrastructureManager.getInstance(mediaNodeId) == null ? false : InstanceStatus.launching.equals(this.infrastructureManager.getInstance(mediaNodeId).getStatus());
    }

    public boolean isCanceled(String mediaNodeId) {
        return this.infrastructureManager.getInstance(mediaNodeId) == null ? false : InstanceStatus.canceled.equals(this.infrastructureManager.getInstance(mediaNodeId).getStatus());
    }

    public boolean isRunning(String mediaNodeId) {
        return this.infrastructureManager.getInstance(mediaNodeId) == null ? false : InstanceStatus.running.equals(this.infrastructureManager.getInstance(mediaNodeId).getStatus());
    }

    public boolean isTerminating(String mediaNodeId) {
        return this.infrastructureManager.getInstance(mediaNodeId) == null ? false : InstanceStatus.terminating.equals(this.infrastructureManager.getInstance(mediaNodeId).getStatus());
    }

    public boolean isWaitingIdleToTerminate(String mediaNodeId) {
        return this.infrastructureManager.getInstance(mediaNodeId) == null ? false : InstanceStatus.waitingIdleToTerminate.equals(this.infrastructureManager.getInstance(mediaNodeId).getStatus());
    }
}
