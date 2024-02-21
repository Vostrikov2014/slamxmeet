//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.cdr;

import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.cdr.CDREvent;
import io.openvidu.server.cdr.CDRLogger;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.pro.config.MicrometerSessionConfig;
import io.openvidu.server.pro.config.OpenviduConfigPro;
import io.openvidu.server.pro.infrastructure.Instance;
import io.openvidu.server.pro.infrastructure.InstanceStatus;
import io.openvidu.server.pro.infrastructure.autoscaling.AutoscalingResult;
import io.openvidu.server.pro.infrastructure.metrics.MediaNodesCpuLoadCollector;
import io.openvidu.server.utils.MediaNodeManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;

public class CallDetailRecordPro extends CallDetailRecord {
    @Autowired
    private MediaNodeManager mediaNodeManager;
    @Autowired
    private MediaNodesCpuLoadCollector mediaNodesCpuLoadCollector;
    @Autowired(
            required = false
    )
    private MicrometerSessionConfig micrometerSessionConfig;
    private OpenviduConfigPro openviduConfigPro;
    private Map<String, CDREventBroadcast> broadcasts = new ConcurrentHashMap();

    public CallDetailRecordPro(Collection<CDRLogger> loggers, OpenviduConfigPro openviduConfigPro) {
        super(loggers);
        this.openviduConfigPro = openviduConfigPro;
    }

    public void recordMediaNodeStatusChanged(Kms mediaNode, String environmentId, InstanceStatus newStatus, InstanceStatus oldStatus) {
        this.recordMediaNodeStatusChanged(System.currentTimeMillis(), mediaNode, environmentId, newStatus, oldStatus);
    }

    public void recordMediaNodeStatusChanged(long timestamp, Kms mediaNode, String environmentId, InstanceStatus newStatus, InstanceStatus oldStatus) {
        CDREventMediaNodeStatus mediaNodeEvent = new CDREventMediaNodeStatus(timestamp, mediaNode, environmentId, newStatus, oldStatus, this.openviduConfigPro.getClusterId());
        this.performActionsOnMediaNodeStatusChanged(mediaNodeEvent);
    }

    public void recordMediaNodeStatusChanged(Instance mediaNode, String uri, InstanceStatus newStatus, InstanceStatus oldStatus) {
        CDREventMediaNodeStatus mediaNodeEvent = new CDREventMediaNodeStatus(mediaNode, uri, newStatus, oldStatus, this.openviduConfigPro.getClusterId());
        this.performActionsOnMediaNodeStatusChanged(mediaNodeEvent);
    }

    public void recordAutoscalingEvent(AutoscalingResult autoscalingResult, String clusterId) {
        this.log(new CDREventAutoscaling(autoscalingResult, clusterId));
    }

    public void recordMediaNodeCrashed(Kms kms, String environmentId, long timeOfDisconnection, List<String> sessionIds, List<String> recordingIds, List<String> broadcasts) {
        this.performActionsOnMediaNodeCrashed(kms, timeOfDisconnection);
        CDREvent event = new CDREventNodeCrashed(System.currentTimeMillis(), kms.getId(), environmentId, kms.getIp(), kms.getUri(), NodeRole.medianode, timeOfDisconnection, this.openviduConfigPro.getClusterId(), sessionIds, recordingIds, broadcasts);
        this.log(event);
    }

    public void recordMediaNodeRecovered(Kms kms, String environmentId, long timeOfConnection) {
        CDREvent event = new CDREventNodeRecovered(timeOfConnection, kms.getId(), environmentId, kms.getIp(), kms.getUri(), this.openviduConfigPro.getClusterId());
        this.log(event);
    }

    public void recordMasterNodeCrashed(CDREventNodeCrashed event) {
        this.log(event);
    }

    public void recordBroadcastStartedEvent(Session session, RecordingProperties properties, String broadcastUrl) {
        CDREventBroadcast event = new CDREventBroadcast(session.getSessionId(), session.getUniqueSessionId(), properties, broadcastUrl, System.currentTimeMillis());
        this.broadcasts.put(session.getUniqueSessionId(), event);
        this.log(event);
    }

    public void recordBroadcastStoppedEvent(Session session, EndReason reason) {
        CDREventBroadcast broadcastStartedEvent = (CDREventBroadcast)this.broadcasts.remove(session.getUniqueSessionId());
        CDREventBroadcast event = new CDREventBroadcast(broadcastStartedEvent, reason);
        this.log(event);
    }

    private void performActionsOnMediaNodeStatusChanged(CDREventMediaNodeStatus mediaNodeEvent) {
        this.updateClusterUsage(mediaNodeEvent);
        this.updateCpuLoadCollection(mediaNodeEvent);
        this.updateMicrometer(mediaNodeEvent);
        this.log(mediaNodeEvent);
    }

    private void performActionsOnMediaNodeCrashed(Kms kms, long timeOfDisconnection) {
        this.mediaNodeManager.mediaNodeUsageDeregistration(kms.getId(), timeOfDisconnection);
    }

    private void updateClusterUsage(CDREventMediaNodeStatus event) {
        if (this.toTerminating(event)) {
            this.mediaNodeManager.mediaNodeUsageDeregistration(event.getMediaNodeId(), event.getTimestamp());
        } else if (this.toLaunching(event)) {
            event.getMediaNode().setLaunchingTime(event.getTimestamp());
        }

    }

    private void updateCpuLoadCollection(CDREventMediaNodeStatus mediaNodeEvent) {
        if (this.toRunning(mediaNodeEvent)) {
            this.mediaNodesCpuLoadCollector.startCpuCollector(mediaNodeEvent.getMediaNodeIp(), mediaNodeEvent.getMediaNodeId());
        } else if (this.toTerminating(mediaNodeEvent)) {
            this.mediaNodesCpuLoadCollector.cancelCpuCollector(mediaNodeEvent.getMediaNodeId());
        }

    }

    private void updateMicrometer(CDREventMediaNodeStatus mediaNodeEvent) {
        if (this.micrometerSessionConfig != null) {
            if (this.toRunning(mediaNodeEvent)) {
                this.micrometerSessionConfig.registerNewMediaNode(mediaNodeEvent.getKms());
            } else if (this.toTerminating(mediaNodeEvent)) {
                this.micrometerSessionConfig.deregisterMediaNode(mediaNodeEvent.getMediaNodeId());
            }

        }
    }

    private boolean toRunning(CDREventMediaNodeStatus mediaNodeEvent) {
        return InstanceStatus.launching.equals(mediaNodeEvent.getOldMediaNodeStatus()) && InstanceStatus.running.equals(mediaNodeEvent.getNewMediaNodeStatus());
    }

    private boolean toTerminating(CDREventMediaNodeStatus mediaNodeEvent) {
        return (InstanceStatus.running.equals(mediaNodeEvent.getOldMediaNodeStatus()) || InstanceStatus.waitingIdleToTerminate.equals(mediaNodeEvent.getOldMediaNodeStatus()) || InstanceStatus.canceled.equals(mediaNodeEvent.getOldMediaNodeStatus())) && InstanceStatus.terminating.equals(mediaNodeEvent.getNewMediaNodeStatus());
    }

    private boolean toLaunching(CDREventMediaNodeStatus mediaNodeEvent) {
        return mediaNodeEvent.getOldMediaNodeStatus() == null && InstanceStatus.launching.equals(mediaNodeEvent.getNewMediaNodeStatus());
    }
}
