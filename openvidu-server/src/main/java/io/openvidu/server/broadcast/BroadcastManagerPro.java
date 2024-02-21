//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.broadcast;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.broadcast.BroadcastManager;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionEventsHandler;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.pro.cdr.CallDetailRecordPro;
import io.openvidu.server.pro.kurento.core.KurentoSessionEventsHandlerPro;
import io.openvidu.server.pro.utils.RemoteDockerManager;
import io.openvidu.server.recording.service.RecordingManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BroadcastManagerPro implements BroadcastManager {
    private static final Logger log = LoggerFactory.getLogger(BroadcastManagerPro.class);
    private static final String BROADCAST_PREFIX = "broadcast_";
    private final String BROADCAST_COMMAND = String.join(" ", "ffmpeg -hide_banner -loglevel level+info -progress pipe:1 -stats_period 1", "-f pulse -sample_rate 48000 -channels 2 -channel_layout stereo", "-thread_queue_size 512", "-i default", "-f x11grab -video_size $RESOLUTION -framerate $FRAMERATE -draw_mouse 0", "-probesize 166M", "-thread_queue_size 128", "-i :$DISPLAY_NUM.0+0,0", "-c:a aac -ab 128k", "-c:v libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p", "-crf 17 -maxrate 6M -bufsize 12M", "-force_key_frames 'expr:gte(t,n_forced*2)'", "-copyts", "-f flv $BROADCAST_URL", "<./stop");
    private final String BROADCAST_ONLY_VIDEO_COMMAND = String.join(" ", "ffmpeg -hide_banner -loglevel level+info -progress pipe:1 -stats_period 1", "-f x11grab -video_size $RESOLUTION -framerate $FRAMERATE -draw_mouse 0", "-probesize 166M", "-thread_queue_size 128", "-i :$DISPLAY_NUM.0+0,0", "-c:v libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p", "-crf 17 -maxrate 6M -bufsize 12M", "-force_key_frames 'expr:gte(t,n_forced*2)'", "-copyts", "-f flv $BROADCAST_URL", "<./stop");
    private final String BROADCAST_ONLY_AUDIO_COMMAND = String.join(" ", "ffmpeg -hide_banner -loglevel level+info -progress pipe:1 -stats_period 1", "-f pulse -sample_rate 48000 -channels 2 -channel_layout stereo", "-thread_queue_size 512", "-i default", "-c:a aac -ab 128k", "-copyts", "-f flv $BROADCAST_URL", "<./stop");
    private final String BROADCAST_STARTED_REGEX = "frame=\\s*([2-9]|\\d{2,}).+bitrate=\\s*[\\d.]+kbits/s";
    private final String BROADCAST_FAILED = "Input/output error";
    private RemoteDockerManager remoteDockerManager;
    @Autowired
    private OpenviduConfig openviduConfig;
    @Autowired
    private KmsManager kmsManager;
    @Autowired
    private SessionEventsHandler sessionEventsHandler;
    @Autowired
    private CallDetailRecord CDR;
    protected Map<String, RecordingProperties> sessionsBroadcasting = new ConcurrentHashMap();

    public BroadcastManagerPro(RemoteDockerManager remoteDockerManager) {
        this.remoteDockerManager = remoteDockerManager;
        this.remoteDockerManager.init();
    }

    public void startBroadcast(Session session, RecordingProperties properties, String broadcastUrl) throws Exception {
        try {
            this.kmsManager.incrementActiveBroadcasts(properties, session);

            try {
                if (!session.recordingLock.tryLock(15L, TimeUnit.SECONDS)) {
                    throw new OpenViduException(Code.BROADCAST_START_ERROR_CODE, "Timeout waiting for recording Session lock to be available for session " + session.getSessionId());
                }

                try {
                    if (this.sessionIsBeingBroadcasted(session.getSessionId())) {
                        log.warn("Concurrent start of broadcast for session " + session.getSessionId());
                        this.kmsManager.decrementActiveBroadcasts(properties, session);
                        throw new OpenViduException(Code.BROADCAST_CONCURRENT_ERROR_CODE, "Concurrent start of broadcast for session " + session.getSessionId());
                    }

                    this.sessionsBroadcasting.put(session.getSessionId(), properties);
                    log.info("Starting broadcast of session {}", session.getSessionId());
                    List<String> envs = new ArrayList();
                    String layoutUrl = this.openviduConfig.getLayoutUrl(properties, session.getSessionId());
                    boolean onlyVideo = !properties.hasAudio();
                    envs.add("DEBUG_MODE=" + this.openviduConfig.isOpenViduRecordingDebug());
                    envs.add("CONTAINER_WORKING_MODE=BROADCAST");
                    envs.add("BROADCAST_URL=" + broadcastUrl);
                    envs.add("URL=" + layoutUrl);
                    envs.add("ONLY_VIDEO=" + onlyVideo);
                    envs.add("RESOLUTION=" + properties.resolution());
                    envs.add("FRAMERATE=" + properties.frameRate());
                    String var10001 = onlyVideo ? this.BROADCAST_ONLY_VIDEO_COMMAND : this.BROADCAST_COMMAND;
                    envs.add("BROADCAST_COMMAND=" + var10001);
                    log.info("Broadcasting url {} to {}", layoutUrl, broadcastUrl);
                    String var10000 = this.openviduConfig.getOpenviduRecordingImageRepo();
                    String container = var10000 + ":" + this.openviduConfig.getOpenViduRecordingVersion();
                    String containerName = "broadcast_" + session.getSessionId();
                    Volume volume = new Volume("/recordings");
                    List<Volume> volumes = new ArrayList();
                    volumes.add(volume);
                    Bind bind = new Bind(this.openviduConfig.getOpenViduRecordingPath(properties.mediaNode()), volume);
                    List<Bind> binds = new ArrayList();
                    binds.add(bind);

                    String containerId;
                    try {
                        containerId = this.remoteDockerManager.runContainer(properties.mediaNode(), container, containerName, (String)null, volumes, binds, "host", envs, (List)null, properties.shmSize(), false, (Map)null, false, this.openviduConfig.isOpenviduRecordingGPUEnabled());
                        log.info("Broadcast container successfully started ({})", containerId);
                    } catch (Exception var22) {
                        log.error("Error starting broadcast container for session {}: {}", session.getSessionId(), var22.getMessage());
                        throw new OpenViduException(Code.BROADCAST_START_ERROR_CODE, var22.getMessage());
                    }

                    try {
                        this.waitForBroadcastRunning(properties.mediaNode(), containerId);
                        log.info("Broadcast successfully started");
                    } catch (Exception var23) {
                        String errorMsg = var23 instanceof HttpResponseException ? ((HttpResponseException)var23).getReasonPhrase() : var23.getMessage();
                        log.error("Error waiting for broadcast for session {} to start: {}", session.getSessionId(), errorMsg);
                        throw new OpenViduException(Code.BROADCAST_START_ERROR_CODE, errorMsg);
                    }
                } finally {
                    session.recordingLock.unlock();
                }
            } catch (InterruptedException var25) {
                throw new OpenViduException(Code.BROADCAST_START_ERROR_CODE, "InterruptedException waiting for recording Session lock to be available for session " + session.getSessionId());
            }
        } catch (Exception var26) {
            if (var26 instanceof OpenViduException && ((OpenViduException)var26).getCodeValue() == Code.BROADCAST_START_ERROR_CODE.getValue()) {
                this.stopBroadcast(session, properties, (EndReason)null);
            }

            throw var26;
        }

        ((CallDetailRecordPro)this.CDR).recordBroadcastStartedEvent(session, properties, broadcastUrl);
        ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onBroadcastStarted(session);
    }

    public void stopBroadcast(Session session, RecordingProperties properties, EndReason reason) {
        RecordingProperties storedProperties = (RecordingProperties)this.sessionsBroadcasting.remove(session.getSessionId());
        RecordingProperties finalProperties;
        if (storedProperties != null) {
            finalProperties = storedProperties;
        } else {
            log.warn("Session {} is not being broadcasted. Just cleaning collections", session.getSessionId());
            finalProperties = properties;
        }

        this.kmsManager.decrementActiveBroadcasts(finalProperties, session);
        if (!EndReason.nodeCrashed.equals(reason) && storedProperties != null) {
            try {
                this.remoteDockerManager.removeContainer(storedProperties.mediaNode(), "broadcast_" + session.getSessionId(), true);
            } catch (OpenViduException var7) {
                if (var7.getCode() == Code.MEDIA_NODE_NOT_FOUND.getValue()) {
                    log.warn("Error stopping broadcast. Media Node {} does not exist", storedProperties.mediaNode());
                }
            }
        }

        if (reason != null && storedProperties != null) {
            ((CallDetailRecordPro)this.CDR).recordBroadcastStoppedEvent(session, RecordingManager.finalReason(reason));
            ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onBroadcastStopped(session);
        }

    }

    public boolean sessionIsBeingBroadcasted(String sessionId) {
        return this.sessionsBroadcasting.containsKey(sessionId);
    }

    public void waitForBroadcastRunning(String mediaNodeId, String containerId) throws InterruptedException, IOException {
        this.remoteDockerManager.waitForContainerLog(mediaNodeId, containerId, "frame=\\s*([2-9]|\\d{2,}).+bitrate=\\s*[\\d.]+kbits/s", true, "Input/output error", false, 20);
    }
}
