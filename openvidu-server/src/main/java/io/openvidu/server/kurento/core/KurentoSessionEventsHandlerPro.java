//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.core;

import com.google.gson.JsonObject;
import io.openvidu.server.cdr.CDREventNodeCrashed;
import io.openvidu.server.cdr.CDRLogger;
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSessionEventsHandler;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.cdr.CDREventNodeCrashed;
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.cdr.CallDetailRecordPro;
import io.openvidu.server.monitoring.NetworkQualityStats;
import io.openvidu.server.stt.grpc.autogenerated.SpeechToTextDefinitions;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KurentoSessionEventsHandlerPro extends KurentoSessionEventsHandler {
    private static final Logger log = LoggerFactory.getLogger(KurentoSessionEventsHandlerPro.class);
    private CDRLoggerElasticSearch elasticsearchLogger;

    public KurentoSessionEventsHandlerPro() {
    }

    public void onNetworkQualityLevelChanged(Session session, JsonObject params) {
        if (this.elasticsearchLogger != null) {
            this.elasticsearchLogger.log(new NetworkQualityStats(params, session.getSessionId()));
        }

        session.getParticipants().forEach((p) -> {
            this.rpcNotificationService.sendNotification(p.getParticipantPrivateId(), "networkQualityLevelChanged", params);
        });
    }

    public void onConnectionPropertyChanged(Participant participant, String property, Object newValue) {
        JsonObject params = new JsonObject();
        params.addProperty("property", property);
        params.addProperty("newValue", newValue.toString());
        this.rpcNotificationService.sendNotification(participant.getParticipantPrivateId(), "connectionPropertyChanged", params);
    }

    public void onSpeechToTextMessage(String sessionId, String connectionId, long timestamp, String text, SpeechToTextDefinitions.Reason reason, String raw, String lang, Set<Participant> subscribedParticipants) {
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        params.addProperty("connectionId", connectionId);
        params.addProperty("timestamp", timestamp);
        params.addProperty("text", text);
        params.addProperty("reason", reason.toString());
        params.addProperty("raw", raw);
        params.addProperty("lang", lang);
        Iterator var11 = subscribedParticipants.iterator();

        while(var11.hasNext()) {
            Participant p = (Participant)var11.next();
            log.debug("Sending speech-to-text message (session {}, connection {}) to {} with reason {}: {}", new Object[]{sessionId, connectionId, p.getParticipantPublicId(), reason.toString(), text});
            this.rpcNotificationService.sendNotification(p.getParticipantPrivateId(), "speechToTextMessage", params);
        }

    }

    public void onMediaNodeCrashed(Kms kms, String environmentId, long timeOfDisconnection, List<String> sessionIds, List<String> recordingIds, List<String> broadcasts) {
        ((CallDetailRecordPro)this.CDR).recordMediaNodeCrashed(kms, environmentId, timeOfDisconnection, sessionIds, recordingIds, broadcasts);
    }

    public void onMediaNodeRecovered(Kms kms, String environmentId, long timeOfConnection) {
        ((CallDetailRecordPro)this.CDR).recordMediaNodeRecovered(kms, environmentId, timeOfConnection);
    }

    public void onMasterNodeCrashed(CDREventNodeCrashed event) {
        ((CallDetailRecordPro)this.CDR).recordMasterNodeCrashed(event);
    }

    public void onForciblyReconnectSubscriber(Participant participant, String senderPublicId, String streamId, String sdpOffer) {
        JsonObject params = new JsonObject();
        params.addProperty("connectionId", senderPublicId);
        params.addProperty("streamId", streamId);
        params.addProperty("sdpOffer", sdpOffer);
        this.rpcNotificationService.sendNotification(participant.getParticipantPrivateId(), "forciblyReconnectSubscriber", params);
    }

    public void onSpeechToTextDisconnection(Set<Participant> participants, String errorMessage) {
        Iterator var3 = participants.iterator();

        while(var3.hasNext()) {
            Participant p = (Participant)var3.next();
            log.debug("Sending speech-to-text disconnected message to {} with message {}", p.getParticipantPublicId(), errorMessage);
            JsonObject params = new JsonObject();
            params.addProperty("message", errorMessage);
            this.rpcNotificationService.sendNotification(p.getParticipantPrivateId(), "speechToTextDisconnected", params);
        }

    }

    public void onBroadcastStarted(Session session) {
        session.getParticipants().forEach((p) -> {
            this.rpcNotificationService.sendNotification(p.getParticipantPrivateId(), "broadcastStarted", new JsonObject());
        });
    }

    public void onBroadcastStopped(Session session) {
        session.getParticipants().forEach((p) -> {
            this.rpcNotificationService.sendNotification(p.getParticipantPrivateId(), "broadcastStopped", new JsonObject());
        });
    }

    @PostConstruct
    private void init() {
        Optional<CDRLogger> opt = this.CDR.getLoggers().stream().filter((logger) -> {
            return logger instanceof CDRLoggerElasticSearch;
        }).findFirst();
        if (opt.isPresent()) {
            this.elasticsearchLogger = (CDRLoggerElasticSearch)opt.get();
        }

    }

    public boolean addMediaNodeInfoToSessionEntity() {
        return true;
    }
}
