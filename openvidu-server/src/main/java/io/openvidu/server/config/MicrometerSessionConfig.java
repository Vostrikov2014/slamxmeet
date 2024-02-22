//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.lang.Nullable;
import io.micrometer.elastic.ElasticConfig;
import io.micrometer.elastic.ElasticMeterRegistry;
import io.openvidu.server.kurento.kms.Kms;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MicrometerSessionConfig {
    private static final Logger log = LoggerFactory.getLogger(MicrometerSessionConfig.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    private ElasticConfig elasticConfig;
    private MeterRegistry registry;
    private Map<String, Set<Meter>> mediaNodeMeters = new HashMap();

    public MicrometerSessionConfig() {
    }

    @PostConstruct
    public void init() {
        log.info("Session Micrometer stats enabled. Gathering every {}s", this.openviduConfigPro.getOpenviduProStatsSessionInterval());
        this.elasticConfig = new ElasticConfig() {
            @Nullable
            public String get(String k) {
                return null;
            }

            public String index() {
                return "session-metrics";
            }

            public boolean autoCreateIndex() {
                return false;
            }

            public String host() {
                return MicrometerSessionConfig.this.openviduConfigPro.getElasticsearchHost().replaceAll("/$", "");
            }

            public Duration step() {
                return Duration.ofSeconds((long)MicrometerSessionConfig.this.openviduConfigPro.getOpenviduProStatsSessionInterval());
            }

            @Nullable
            public String userName() {
                return MicrometerSessionConfig.this.openviduConfigPro.isElasticSearchSecured() ? MicrometerSessionConfig.this.openviduConfigPro.getElasticsearchUserName() : null;
            }

            @Nullable
            public String password() {
                return MicrometerSessionConfig.this.openviduConfigPro.isElasticSearchSecured() ? MicrometerSessionConfig.this.openviduConfigPro.getElasticsearchPassword() : null;
            }

            public String indexDateFormat() {
                return "yyyy.MM.dd";
            }
        };
        this.registry = new ElasticMeterRegistry(this.elasticConfig, Clock.SYSTEM);
    }

    public void registerNewMediaNode(Kms kms) {
        log.info("Registering Micrometer metrics for Media Node {}", kms.getId());
        ToDoubleFunction<Kms> numSessionsFunction = (k) -> {
            return Double.valueOf((double)k.getKurentoSessions().size());
        };
        Gauge.Builder<Kms> builderSessions = Gauge.builder("num.sessions", kms, numSessionsFunction);
        builderSessions.description("Total number of sessions in a Media Node");
        this.applyCommonTagsToMeter(builderSessions, kms.getId());
        Gauge gaugeTotalSessions = builderSessions.register(this.registry);
        ToDoubleFunction<Kms> numConnectionsFunction = (k) -> {
            return Double.valueOf((double)k.getNumberOfConnections());
        };
        Gauge.Builder<Kms> builderConnections = Gauge.builder("num.connections", kms, numConnectionsFunction);
        builderConnections.description("Total number of connections in a Media Node");
        this.applyCommonTagsToMeter(builderConnections, kms.getId());
        Gauge gaugeTotalConnections = builderConnections.register(this.registry);
        ToDoubleFunction<Kms> numWebrtcConnectionsFunction = (k) -> {
            return Double.valueOf((double)k.getNumberOfWebrtcConnections());
        };
        Gauge.Builder<Kms> builderWebrtcConnections = Gauge.builder("num.webrtc.connections", kms, numWebrtcConnectionsFunction);
        builderWebrtcConnections.description("Total number of WebRTC connections in a Media Node");
        this.applyCommonTagsToMeter(builderWebrtcConnections, kms.getId());
        Gauge gaugeTotalWebrtcConnections = builderWebrtcConnections.register(this.registry);
        ToDoubleFunction<Kms> numRecordingsFunction = (k) -> {
            return Double.valueOf((double)k.getNumberOfRecordings());
        };
        Gauge.Builder<Kms> builderRecordings = Gauge.builder("num.recordings", kms, numRecordingsFunction);
        builderRecordings.description("Total number of recordings in a Media Node");
        this.applyCommonTagsToMeter(builderRecordings, kms.getId());
        Gauge gaugeTotalRecordings = builderRecordings.register(this.registry);
        ToDoubleFunction<Kms> numComposedRecordingsFunction = (k) -> {
            return Double.valueOf((double)k.getNumberOfComposedRecordings());
        };
        Gauge.Builder<Kms> builderComposedRecordings = Gauge.builder("num.recordings.composed", kms, numComposedRecordingsFunction);
        builderComposedRecordings.description("Total number of composed recordings in a Media Node");
        this.applyCommonTagsToMeter(builderComposedRecordings, kms.getId());
        Gauge gaugeTotalComposedRecordings = builderComposedRecordings.register(this.registry);
        ToDoubleFunction<Kms> numBroadcastsFunction = (k) -> {
            return Double.valueOf((double)k.getNumberOfBroadcasts());
        };
        Gauge.Builder<Kms> builderBroadcasts = Gauge.builder("num.broadcasts", kms, numBroadcastsFunction);
        builderBroadcasts.description("Total number of broadcasts in a Media Node");
        this.applyCommonTagsToMeter(builderBroadcasts, kms.getId());
        Gauge gaugeTotalBroadcasts = builderBroadcasts.register(this.registry);
        Set<Meter> setOfMeters = new HashSet();
        setOfMeters.add(gaugeTotalSessions);
        setOfMeters.add(gaugeTotalConnections);
        setOfMeters.add(gaugeTotalWebrtcConnections);
        setOfMeters.add(gaugeTotalRecordings);
        setOfMeters.add(gaugeTotalComposedRecordings);
        setOfMeters.add(gaugeTotalBroadcasts);
        this.mediaNodeMeters.putIfAbsent(kms.getId(), setOfMeters);
    }

    private void applyCommonTagsToMeter(Gauge.Builder<Kms> builderSessions, String kmsId) {
        builderSessions.tag("cluster_id", this.openviduConfigPro.getClusterId());
        builderSessions.tag("node_id", kmsId);
        builderSessions.tag("node_role", "medianode");
    }

    public void deregisterMediaNode(String mediaNodeId) {
        log.info("Deregistering Micrometer metrics for Media Node {}", mediaNodeId);
        Set<Meter> setOfMeters = (Set)this.mediaNodeMeters.remove(mediaNodeId);
        if (setOfMeters != null) {
            setOfMeters.forEach((meter) -> {
                this.registry.remove(meter);
            });
        }

    }

    @PreDestroy
    public void preDestroy() {
        this.registry.close();
    }
}
