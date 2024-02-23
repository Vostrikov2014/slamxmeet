//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.core;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.KurentoOptions;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.broadcast.BroadcastManager;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.MediaServer;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.core.KurentoSessionEventsHandlerPro;
import io.openvidu.server.kurento.core.KurentoSessionManager;
import io.openvidu.server.kurento.endpoint.MediaEndpoint;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.pro.broadcast.BroadcastManagerPro;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.monitoring.KmsWebrtcStats;
import io.openvidu.server.stt.SpeechToTextManager;
import io.openvidu.server.utils.MediaNodeManager;
import io.openvidu.server.utils.UpdatableTimerTask;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.kurento.client.Continuation;
import org.kurento.client.MediaType;
import org.kurento.client.RTCRTPStreamStats;
import org.kurento.client.Stats;
import org.kurento.client.StatsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class KurentoSessionManagerPro extends KurentoSessionManager {
    private static final Logger log = LoggerFactory.getLogger(KurentoSessionManagerPro.class);
    private static final int MAX_TARGET_BITRATE_KBPS = 2500;
    private static int NETWORK_QUALITY_INTERVAL_SECONDS;
    @Autowired
    protected MediaNodeManager mediaNodeManager;
    @Autowired(
            required = false
    )
    private SpeechToTextManager speechToTextManager;
    @Autowired
    private BroadcastManager broadcastManager;
    private ConcurrentMap<String, Double> oldNetworkValues = new ConcurrentHashMap();
    private ConcurrentMap<String, Integer> previousBytesSent = new ConcurrentHashMap();
    private OpenviduConfigPro openviduConfigPro;
    private UpdatableTimerTask networkQualityTimer;

    public KurentoSessionManagerPro(OpenviduConfigPro openviduConfigPro) {
        this.openviduConfigPro = openviduConfigPro;
    }

    @PostConstruct
    public void enableNetworkQualityEvent() {
        log.info("OpenVidu Pro network stat quality service is {}", this.openviduConfigPro.isNetworkQualityEnabled() ? "enabled" : "disabled");
        if (this.openviduConfigPro.isNetworkQualityEnabled()) {
            NETWORK_QUALITY_INTERVAL_SECONDS = this.openviduConfigPro.getNetworkQualityInterval() * 1000;
            this.networkQualityTimer = new UpdatableTimerTask(() -> {
                Set<Participant> participants = this.getParticipantsFiltered();
                participants.forEach((participant) -> {
                    long secondsPublished = ((new Timestamp(System.currentTimeMillis())).getTime() - participant.getPublishedAt()) / 1000L;
                    if (secondsPublished > 10L) {
                        this.checkNetwork(participant);
                    }

                });
            }, () -> {
                return (long)NETWORK_QUALITY_INTERVAL_SECONDS;
            });
            this.networkQualityTimer.updateTimer();
        }

    }

    public Set<Participant> getParticipantsFiltered() {
        Set<Participant> participants = new HashSet();
        this.sessions.forEach((key, session) -> {
            participants.addAll(session.getParticipants());
        });
        participants.removeIf((p) -> {
            return p.getPublishedAt() == null;
        });
        return participants;
    }

    public void checkNetwork(Participant p) {
        try {
            final KurentoParticipant kParticipant = (KurentoParticipant)p;
            final MediaEndpoint endpoint = kParticipant.getPublisher();
            endpoint.getEndpoint().getStats(MediaType.VIDEO, new Continuation<Map<String, Stats>>() {
                public void onSuccess(Map<String, Stats> stats) throws Exception {
                    stats.values().forEach((stat) -> {
                        if (stat instanceof RTCRTPStreamStats && StatsType.inboundrtp.equals(stat.getType())) {
                            KurentoSessionManagerPro.this.processStat(stat, kParticipant);
                        }

                    });
                }

                public void onError(Throwable cause) throws Exception {
                    KurentoSessionManagerPro.log.error("Error in network quality thread while retrieving video MediaEndpoint stats for stream {} of participant {} of session {}: {}", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), cause.getMessage()});
                }
            });
        } catch (OpenViduException var4) {
            log.error("Error in network quality thread getting video stats for participant {}: {}", p.getParticipantPublicId(), var4.toString());
        }

    }

    private void processStat(Stats stat, KurentoParticipant kParticipant) {
        KmsWebrtcStats webRtcStats = new KmsWebrtcStats(kParticipant.getPublisher().getOwner(), kParticipant.getPublisher().getEndpointName(), MediaType.VIDEO, (RTCRTPStreamStats)stat);
        int totalBytesSent = Integer.parseInt(webRtcStats.toJson().get("bytesReceived").toString());
        int previousBytesSent = this.previousBytesSent.get(kParticipant.getParticipantPrivateId()) != null ? (Integer)this.previousBytesSent.get(kParticipant.getParticipantPrivateId()) : 0;
        double bitrateSentKbps = (double)((totalBytesSent - previousBytesSent) * 8 / NETWORK_QUALITY_INTERVAL_SECONDS);
        boolean isScreen = kParticipant.getPublisherMediaOptions().getTypeOfVideo().equals("SCREEN");
        boolean hasDimensions = kParticipant.getVideoWidth() != 0 && kParticipant.getVideoHeight() != 0;
        double newPercentage = 0.0;
        double oldPercentage = 0.0;
        NetworkQualityLevel newQualityLevel = null;
        NetworkQualityLevel oldQualityLevel = null;
        if (!isScreen && kParticipant.isVideoActive() && hasDimensions) {
            newPercentage = this.calculateNetworkQualityFromBitrate(kParticipant.getParticipantPublicId(), kParticipant.getVideoWidth(), kParticipant.getVideoHeight(), bitrateSentKbps, -1.0, this.getMaxRecvBandwitdhFromParticipant(kParticipant));
        } else {
            double fractionOfPacketsLost = Double.parseDouble(webRtcStats.toJson().get("fractionLost").toString());
            if (!isScreen && !kParticipant.isVideoActive()) {
                fractionOfPacketsLost *= 0.5;
            }

            newPercentage = this.calculateNetworkQualityFromPacketsLost(fractionOfPacketsLost);
        }

        newQualityLevel = this.calculateNetworkQualityLevel(newPercentage);
        this.previousBytesSent.put(kParticipant.getParticipantPrivateId(), totalBytesSent);
        if (this.oldNetworkValues.get(kParticipant.getParticipantPrivateId()) != null) {
            oldPercentage = (Double)this.oldNetworkValues.get(kParticipant.getParticipantPrivateId());
            oldQualityLevel = this.calculateNetworkQualityLevel(oldPercentage);
        }

        if (oldQualityLevel == null || newQualityLevel.getValue() != oldQualityLevel.getValue()) {
            this.sendNetworkQualityEvent(kParticipant, newQualityLevel, oldQualityLevel);
        }

        this.oldNetworkValues.put(kParticipant.getParticipantPrivateId(), newPercentage);
    }

    public double calculateNetworkQualityFromBitrate(String pId, int width, int height, double bitrateSentKbps, double bitrateCapKbps, int maxRecvBandwidth) {
        double targetKbps = (double)this.getMaxDefaultVideoBitrateKbps(width, height);
        targetKbps = Math.min(targetKbps, (double)maxRecvBandwidth);
        if (bitrateCapKbps > 0.0) {
            targetKbps = Math.min(targetKbps, bitrateCapKbps);
        }

        double qualityPct = 100.0 * bitrateSentKbps / targetKbps;
        return qualityPct;
    }

    public double calculateNetworkQualityFromPacketsLost(double fractionOfPacketsLost) {
        if (fractionOfPacketsLost <= 2.0) {
            return 100.0;
        } else if (fractionOfPacketsLost <= 4.0) {
            return 70.0;
        } else if (fractionOfPacketsLost <= 6.0) {
            return 50.0;
        } else if (fractionOfPacketsLost <= 8.0) {
            return 30.0;
        } else {
            return fractionOfPacketsLost <= 12.0 ? 10.0 : 0.0;
        }
    }

    public NetworkQualityLevel calculateNetworkQualityLevel(Double percentage) {
        if (percentage > 80.0) {
            return NetworkQualityLevel.EXCELLENT;
        } else if (percentage > 55.0 && percentage <= 80.0) {
            return NetworkQualityLevel.GOOD;
        } else if (percentage > 30.0 && percentage <= 55.0) {
            return NetworkQualityLevel.NON_OPTIMAL;
        } else if (percentage > 15.0 && percentage <= 30.0) {
            return NetworkQualityLevel.POOR;
        } else {
            return percentage >= 1.0 && percentage <= 15.0 ? NetworkQualityLevel.BAD : NetworkQualityLevel.BROKEN;
        }
    }

    private void sendNetworkQualityEvent(KurentoParticipant kParticipant, NetworkQualityLevel newQualityLevel, NetworkQualityLevel oldQualityLevel) {
        JsonObject params = new JsonObject();
        params.addProperty("connectionId", kParticipant.getParticipantPublicId());
        params.addProperty("newValue", newQualityLevel.getValue());
        if (oldQualityLevel != null) {
            params.addProperty("oldValue", oldQualityLevel.getValue());
        } else {
            params.add("oldValue", JsonNull.INSTANCE);
        }

        log.debug("Network quality event has been sent. New quality: {} - Old quality: {}", newQualityLevel, oldQualityLevel);
        ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onNetworkQualityLevelChanged(kParticipant.getSession(), params);
    }

    private int getMaxDefaultVideoBitrateKbps(int width, int height) {
        short maxBitrate;
        if (width * height <= 76800) {
            maxBitrate = 600;
        } else if (width * height <= 307200) {
            maxBitrate = 1700;
        } else if (width * height <= 518400) {
            maxBitrate = 2000;
        } else {
            maxBitrate = 2500;
        }

        return maxBitrate;
    }

    private int getMaxRecvBandwitdhFromParticipant(KurentoParticipant kParticipant) {
        KurentoOptions kurentoTokenOptions = kParticipant.getToken().getKurentoOptions();
        int maxRecvBandwidth;
        if (kurentoTokenOptions != null) {
            maxRecvBandwidth = kurentoTokenOptions.getVideoMaxRecvBandwidth() != null ? kurentoTokenOptions.getVideoMaxRecvBandwidth() : this.openviduConfig.getVideoMaxRecvBandwidth();
        } else {
            maxRecvBandwidth = this.openviduConfig.getVideoMaxRecvBandwidth();
        }

        if (maxRecvBandwidth > 0) {
            maxRecvBandwidth = Math.min(maxRecvBandwidth, 2500);
        } else {
            maxRecvBandwidth = 2500;
        }

        return maxRecvBandwidth;
    }

    public void onSubscribeToSpeechToText(Participant participant, Integer transactionId, String lang, String connectionId) {
        if (!this.openviduConfigPro.isSpeechToTextEnabled()) {
            this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, new OpenViduException(Code.SERVICE_NOT_ENABLED_ERROR_CODE, "Speech To Text service is not enabled"));
        } else {
            OpenViduException err;
            try {
                KurentoSession session = ((KurentoParticipant)participant).getSession();
                this.speechToTextManager.subscribeToSpeechToTextMessage(participant.getSessionId(), participant.getParticipantPublicId(), lang, connectionId, session.getKms());
                this.sessionEventsHandler.onSubscribeToSpeechToText(participant, transactionId, (OpenViduException)null);
            } catch (OpenViduException var7) {
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, var7);
            } catch (ExecutionException | TimeoutException | InterruptedException var8) {
                err = new OpenViduException(Code.ROOM_GENERIC_ERROR_CODE, var8.getMessage());
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, err);
            } catch (IOException var9) {
                err = new OpenViduException(Code.MEDIA_NODE_CONNECTION_ERROR_CODE, var9.getMessage());
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, err);
            } catch (Exception var10) {
                err = new OpenViduException(Code.GENERIC_ERROR_CODE, var10.getMessage());
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, err);
            }

        }
    }

    public void onUnsubscribeFromSpeechToText(Participant participant, Integer transactionId, String connectionId) {
        if (!this.openviduConfigPro.isSpeechToTextEnabled()) {
            this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, new OpenViduException(Code.SERVICE_NOT_ENABLED_ERROR_CODE, "Speech To Text service is not enabled"));
        } else {
            OpenViduException err;
            try {
                KurentoSession session = ((KurentoParticipant)participant).getSession();
                this.speechToTextManager.unsubscribeFromSpeechToTextMessage(participant.getSessionId(), participant.getParticipantPublicId(), connectionId, session.getKms().getIp(), true);
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, (OpenViduException)null);
            } catch (OpenViduException var6) {
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, var6);
            } catch (TimeoutException | ExecutionException var7) {
                err = new OpenViduException(Code.ROOM_GENERIC_ERROR_CODE, var7.getMessage());
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, err);
            } catch (Exception var8) {
                err = new OpenViduException(Code.GENERIC_ERROR_CODE, var8.getMessage());
                this.sessionEventsHandler.onUnsubscribeToSpeechToText(participant, transactionId, err);
            }

        }
    }

    @PreDestroy
    public void close() {
        if (this.networkQualityTimer != null) {
            this.networkQualityTimer.cancelTimer();
        }

        super.close();
    }

    protected Kms selectMediaNode(Session sessionNotActive) throws OpenViduException {
        String mediaNodeId = sessionNotActive.getSessionProperties().mediaNode();
        if (mediaNodeId != null) {
            Kms forcedKms = this.kmsManager.getKms(mediaNodeId);
            OpenViduException.Code var10002;
            String var10003;
            if (forcedKms != null) {
                if (this.mediaNodeManager.isRunning(forcedKms.getId())) {
                    if (forcedKms.isKurentoClientConnected()) {
                        return forcedKms;
                    } else {
                        var10002 = Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE;
                        var10003 = sessionNotActive.getSessionId();
                        throw new OpenViduException(var10002, "Session '" + var10003 + "' must be initialized in Media Node '" + mediaNodeId + "' but OpenVidu Server Pro is disconnected from it");
                    }
                } else {
                    var10002 = Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE;
                    var10003 = sessionNotActive.getSessionId();
                    throw new OpenViduException(var10002, "Session '" + var10003 + "' must be initialized in Media Node '" + mediaNodeId + "' but its current status is not \"running\"");
                }
            } else {
                this.cleanCollections(sessionNotActive.getSessionId());
                var10002 = Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE;
                var10003 = sessionNotActive.getSessionId();
                throw new OpenViduException(var10002, "Session '" + var10003 + "' must be initialized in Media Node '" + mediaNodeId + "' but it does not exist");
            }
        } else {
            return super.selectMediaNode(sessionNotActive);
        }
    }

    protected void reconnectPublisher(KurentoSession kSession, KurentoParticipant kParticipant, String streamId, String sdpOffer, Integer transactionId) {
        super.reconnectPublisher(kSession, kParticipant, streamId, sdpOffer, transactionId);
        if (MediaServer.mediasoup.equals(this.openviduConfigPro.getMediaServer())) {
            Set<Participant> subscribedParticipants = kSession.getParticipantsSubscribedToParticipant(kParticipant.getParticipantPublicId());
            Iterator var7 = subscribedParticipants.iterator();

            while(var7.hasNext()) {
                Participant p = (Participant)var7.next();

                try {
                    String sdpOfferByServer = this.prepareForcedSubscription(p, kParticipant.getParticipantPublicId());
                    ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onForciblyReconnectSubscriber(p, kParticipant.getParticipantPublicId(), streamId, sdpOfferByServer);
                } catch (OpenViduException var10) {
                    log.error("PARTICIPANT {}: Error preparing forced resubscription to {}", new Object[]{p.getParticipantPublicId(), kParticipant.getParticipantPublicId(), var10});
                }
            }
        }

    }

    public void stopBroadcastIfNecessary(Session session, EndReason reason) {
        if (this.broadcastManager.sessionIsBeingBroadcasted(session.getSessionId())) {
            try {
                ((BroadcastManagerPro)this.broadcastManager).stopBroadcast(session, (RecordingProperties)null, reason);
            } catch (OpenViduException var4) {
                log.error("Error stopping broadcast of session {}: {}", session.getSessionId(), var4.getMessage());
            }
        }

    }

    public void closeSessionAndEmptyCollections(Session session, EndReason reason, boolean stopRecording) {
        this.stopBroadcastIfNecessary(session, reason);
        super.closeSessionAndEmptyCollections(session, reason, stopRecording);
        if (this.speechToTextManager != null) {
            try {
                KurentoSession kSession = (KurentoSession)session;
                this.speechToTextManager.reset(kSession.getSessionId(), kSession.getMediaNodeIp(), true);
            } catch (ClassCastException var5) {
                this.speechToTextManager.reset(session.getSessionId(), (String)null, false);
            }
        }

    }
}
