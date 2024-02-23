//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.core;

import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.core.MediaServer;
import io.openvidu.server.kurento.core.KurentoParticipantEndpointConfig;
import io.openvidu.server.kurento.endpoint.KmsEvent;
import io.openvidu.server.kurento.endpoint.MediaEndpoint;
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.kurento.mediasoup.ConsumerLayersChangedMediasoupEvent;
import io.openvidu.server.kurento.mediasoup.ConsumerScoreChangedMediasoupEvent;
import io.openvidu.server.kurento.mediasoup.IceSelectedTupleChangedMediasoupEvent;
import io.openvidu.server.kurento.mediasoup.ProducerScoresChangedMediasoupEvent;
import io.openvidu.server.monitoring.KmsWebrtcStats;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.kurento.client.Continuation;
import org.kurento.client.MediaType;
import org.kurento.client.RTCRTPStreamStats;
import org.kurento.client.Stats;
import org.kurento.jsonrpc.JsonRpcClientClosedException;
import org.springframework.beans.factory.annotation.Autowired;

public class KurentoParticipantEndpointConfigPro extends KurentoParticipantEndpointConfig {
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private CallDetailRecord cdr;
    private CDRLoggerElasticSearch elasticsearchLogger;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    final int MAX_NUMBER_OF_GETSTATS_ERRORS = 8;
    final int MAX_GETSTATS_SECONDS_OF_WAIT = 10;

    public KurentoParticipantEndpointConfigPro() {
    }

    @PostConstruct
    public void init() {
        this.elasticsearchLogger = (CDRLoggerElasticSearch)this.cdr.getLoggers().stream().filter((logger) -> {
            return logger instanceof CDRLoggerElasticSearch;
        }).findFirst().get();
    }

    public void addEndpointListeners(final MediaEndpoint endpoint, String typeOfEndpoint) {
        super.addEndpointListeners(endpoint, typeOfEndpoint);
        this.addMediasoupEndpointListeners(endpoint, typeOfEndpoint);
        if (this.openviduConfigPro.getOpenviduProStatsWebrtcInterval() != 0) {
            Runnable runnable = new Runnable() {
                public void run() {
                    if (!endpoint.cancelStatsLoop.get()) {
                        final CountDownLatch LATCH = new CountDownLatch(2);
                        long INIT_TIME = System.currentTimeMillis();
                        endpoint.getEndpoint().getStats(MediaType.AUDIO, new Continuation<Map<String, Stats>>() {
                            public void onSuccess(Map<String, Stats> result) throws Exception {
                                endpoint.statsNotFoundErrors.set(0);
                                result.values().forEach((stat) -> {
                                    if (stat instanceof RTCRTPStreamStats) {
                                        KmsWebrtcStats webRtcStats = new KmsWebrtcStats(endpoint.getOwner(), endpoint.getEndpointName(), MediaType.AUDIO, (RTCRTPStreamStats)stat);
                                        KurentoParticipantEndpointConfigPro.this.elasticsearchLogger.log(webRtcStats);
                                    } else {
                                        KurentoParticipantEndpointConfigPro.log.warn("MediaEndpoint audio stat is not of type \"inboundrtp\" or \"outboundrtp\": {}", stat.getType());
                                    }

                                });
                                LATCH.countDown();
                            }

                            public void onError(Throwable cause) throws Exception {
                                KurentoParticipantEndpointConfigPro.log.error("Error while retrieveing audio MediaEndpoint stats for stream {} of participant {} of session {}: {}", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), cause.getMessage()});
                                if (KurentoParticipantEndpointConfigPro.this.kurentoError(cause) && endpoint.statsNotFoundErrors.incrementAndGet() > 8) {
                                    endpoint.cancelStatsLoop.set(true);
                                    KurentoParticipantEndpointConfigPro.log.error("Too many errors thrown by getStats (AUDIO) thread of stream {} of participant {} of session {} with endpoint {}.Stats loop will be canceled", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName()});
                                    KurentoParticipantEndpointConfigPro.log.warn("This might be caused by a stranded getStats thread");
                                }

                                LATCH.countDown();
                            }
                        });
                        endpoint.getEndpoint().getStats(MediaType.VIDEO, new Continuation<Map<String, Stats>>() {
                            public void onSuccess(Map<String, Stats> result) throws Exception {
                                endpoint.statsNotFoundErrors.set(0);
                                result.values().forEach((stat) -> {
                                    if (stat instanceof RTCRTPStreamStats) {
                                        KmsWebrtcStats webRtcStats = new KmsWebrtcStats(endpoint.getOwner(), endpoint.getEndpointName(), MediaType.VIDEO, (RTCRTPStreamStats)stat);
                                        KurentoParticipantEndpointConfigPro.this.elasticsearchLogger.log(webRtcStats);
                                    } else {
                                        KurentoParticipantEndpointConfigPro.log.warn("MediaEndpoint video stat is not of type \"inboundrtp\" or \"outboundrtp\": {}", stat.getType());
                                    }

                                });
                                LATCH.countDown();
                            }

                            public void onError(Throwable cause) throws Exception {
                                KurentoParticipantEndpointConfigPro.log.error("Error while retrieveing video MediaEndpoint stats for stream {} of participant {} of session {}: {}", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), cause.getMessage()});
                                if (KurentoParticipantEndpointConfigPro.this.kurentoError(cause) && endpoint.statsNotFoundErrors.incrementAndGet() > 8) {
                                    endpoint.cancelStatsLoop.set(true);
                                    KurentoParticipantEndpointConfigPro.log.error("Too many errors thrown by getStats (VIDEO) thread of stream {} of participant {} of session {} with endpoint {}. Stats loop will be canceled", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName()});
                                    KurentoParticipantEndpointConfigPro.log.warn("This might be caused by a stranded getStats thread");
                                }

                                LATCH.countDown();
                            }
                        });

                        try {
                            if (LATCH.await(10L, TimeUnit.SECONDS)) {
                                if (endpoint.cancelStatsLoop.get()) {
                                    KurentoParticipantEndpointConfigPro.log.info("Loop for getStats of stream {} of participant {} of session {} with endpoint {} has been canceled during stats gathering", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName()});
                                } else {
                                    KurentoParticipantEndpointConfigPro.this.scheduleNewLoop(INIT_TIME, endpoint);
                                }
                            } else if (endpoint.statsNotFoundErrors.incrementAndGet() > 8) {
                                endpoint.cancelStatsLoop.set(true);
                                KurentoParticipantEndpointConfigPro.log.error("Call for getStats of stream {} of participant {} of session {} with endpoint {} didn't return in {} seconds. Loop canceled", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName(), 10});
                            } else {
                                KurentoParticipantEndpointConfigPro.this.scheduleNewLoop(INIT_TIME, endpoint);
                            }
                        } catch (InterruptedException var5) {
                            KurentoParticipantEndpointConfigPro.log.error("Stats thread of stream {} of participant {} of session {} with endpoint {} was interrupted while waiting for response. Stats loop canceled", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName()});
                        }
                    } else {
                        KurentoParticipantEndpointConfigPro.log.info("Loop for getStats of stream {} of participant {} of session {} with endpoint {} is now canceled", new Object[]{endpoint.getStreamId(), endpoint.getOwner().getParticipantPublicId(), endpoint.getOwner().getSessionId(), endpoint.getEndpointName()});
                    }

                }
            };
            endpoint.kmsWebrtcStatsRunnable = runnable;
            this.executorService.schedule(runnable, (long)this.openviduConfigPro.getOpenviduProStatsWebrtcInterval(), TimeUnit.SECONDS);
        }

    }

    private void addMediasoupEndpointListeners(MediaEndpoint endpoint, String typeOfEndpoint) {
        if (endpoint.getWebEndpoint() != null && MediaServer.mediasoup.equals(this.openviduConfigPro.getMediaServer())) {
            endpoint.getEndpoint().addEventListener("IceSelectedTupleChanged", (event) -> {
                String msg = "Mediasoup event [IceSelectedTupleChanged]: -> endpoint: " + endpoint.getEndpointName() + " (" + typeOfEndpoint + ") | tuple: " + event.getTuple().toString() + " | timestamp: " + event.getTimestampMillis();
                KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(), endpoint.createdAt());
                endpoint.kmsEvents.add(kmsEvent);
                this.CDR.log(kmsEvent);
                this.infoHandler.sendInfo(msg);
                log.info(msg);
            }, IceSelectedTupleChangedMediasoupEvent.class);
            endpoint.getEndpoint().addEventListener("ConsumerScoreChanged", (event) -> {
                String msg = "Mediasoup event [ConsumerScoreChanged]: -> endpoint: " + endpoint.getEndpointName() + " (" + typeOfEndpoint + ") | score: " + event.getScore().toString() + " | timestamp: " + event.getTimestampMillis();
                KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(), endpoint.createdAt());
                endpoint.kmsEvents.add(kmsEvent);
                this.CDR.log(kmsEvent);
                this.infoHandler.sendInfo(msg);
                log.info(msg);
            }, ConsumerScoreChangedMediasoupEvent.class);
            endpoint.getEndpoint().addEventListener("ConsumerLayersChanged", (event) -> {
                String msg = "Mediasoup event [ConsumerLayersChanged]: -> endpoint: " + endpoint.getEndpointName() + " (" + typeOfEndpoint + ") | layers: " + event.getLayers().toString() + " | timestamp: " + event.getTimestampMillis();
                KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(), endpoint.createdAt());
                endpoint.kmsEvents.add(kmsEvent);
                this.CDR.log(kmsEvent);
                this.infoHandler.sendInfo(msg);
                log.info(msg);
            }, ConsumerLayersChangedMediasoupEvent.class);
            endpoint.getEndpoint().addEventListener("ProducerScoresChanged", (event) -> {
                String msg = "Mediasoup event [ProducerScoresChanged]: -> endpoint: " + endpoint.getEndpointName() + " (" + typeOfEndpoint + ") | scores: " + event.getScores().toString() + " | timestamp: " + event.getTimestampMillis();
                KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(), endpoint.createdAt());
                endpoint.kmsEvents.add(kmsEvent);
                this.CDR.log(kmsEvent);
                this.infoHandler.sendInfo(msg);
                log.info(msg);
            }, ProducerScoresChangedMediasoupEvent.class);
        }

    }

    private void scheduleNewLoop(long initTime, MediaEndpoint endpoint) {
        long exactMillisWait = (long)(this.openviduConfigPro.getOpenviduProStatsWebrtcInterval() * 1000) - (System.currentTimeMillis() - initTime);
        exactMillisWait = exactMillisWait < 0L ? 0L : exactMillisWait;
        this.executorService.schedule(endpoint.kmsWebrtcStatsRunnable, exactMillisWait, TimeUnit.MILLISECONDS);
    }

    private boolean kurentoError(Throwable cause) {
        return cause.getMessage().contains("40101") || JsonRpcClientClosedException.class.equals(cause.getClass());
    }

    @PreDestroy
    private void preDestroy() {
        log.info("Shutting down any remaining MediaEndpoint statistics gathering thread");
        this.executorService.shutdownNow();
    }
}
