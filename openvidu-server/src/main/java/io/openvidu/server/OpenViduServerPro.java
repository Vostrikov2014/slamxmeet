//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server;

import io.micrometer.core.lang.Nullable;
import io.micrometer.elastic.ElasticConfig;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.OpenViduServer;
import io.openvidu.server.broadcast.BroadcastManager;
import io.openvidu.server.cdr.CDREventName;
import io.openvidu.server.cdr.CDRLogger;
import io.openvidu.server.cdr.CDRLoggerFile;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.core.SessionEventsHandler;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.core.TokenGenerator;
import io.openvidu.server.kurento.core.KurentoParticipantEndpointConfig;
import io.openvidu.server.kurento.kms.DummyLoadManager;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.kurento.kms.LoadManager;
import io.openvidu.server.account.ClusterUsageService;
import io.openvidu.server.account.LambdaService;
import io.openvidu.server.aws.s3.S3Handler;
import io.openvidu.server.broadcast.BroadcastManagerPro;
import io.openvidu.server.cdr.CDRLoggerElasticSearch;
import io.openvidu.server.cdr.CallDetailRecordPro;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.AdditionalMonitoring;
import io.openvidu.server.config.DockerRegistryConfig;
import io.openvidu.server.config.ElasticSearchConfig;
import io.openvidu.server.config.KibanaConfig;
import io.openvidu.server.config.MicrometerSessionConfig;
import io.openvidu.server.config.OpenViduEdition;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.Status;
import io.openvidu.server.config.AdditionalMonitoring.Type;
import io.openvidu.server.core.TokenGeneratorPro;
import io.openvidu.server.health.HealthCheckManager;
import io.openvidu.server.health.HealthCheckManagerPro;
import io.openvidu.server.infrastructure.DummyInfrastructureManager;
import io.openvidu.server.infrastructure.FakeInfrastructureManager;
import io.openvidu.server.infrastructure.InfrastructureInstanceData;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.InstanceType;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.infrastructure.autoscaling.AutoscalingApplier;
import io.openvidu.server.infrastructure.docker.DockerInfrastructureManager;
import io.openvidu.server.infrastructure.metrics.MediaNodesCpuLoadCollector;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import io.openvidu.server.infrastructure.onpremise.OnpremiseInfrastructureManager;
import io.openvidu.server.kurento.core.KurentoParticipantEndpointConfigPro;
import io.openvidu.server.kurento.core.KurentoSessionEventsHandlerPro;
import io.openvidu.server.kurento.core.KurentoSessionManagerPro;
import io.openvidu.server.kurento.kms.CpuLoadManager;
import io.openvidu.server.kurento.kms.MultipleKmsManager;
import io.openvidu.server.recording.MedianNodeRecordingDownloader;
import io.openvidu.server.recording.OpenViduRecordingStorage;
import io.openvidu.server.recording.S3RecordingUploader;
import io.openvidu.server.recording.service.RecordingManagerUtilsS3;
import io.openvidu.server.rest.ApiRestPathRewriteFilterPro;
import io.openvidu.server.stt.SpeechToTextManager;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.SpeechToTextVoskModelLoadStrategy;
import io.openvidu.server.stt.grpc.SpeechToTextGrpcClient;
import io.openvidu.server.utils.GeoLocationByIpPro;
import io.openvidu.server.utils.MediaNodeManagerPro;
import io.openvidu.server.utils.RemoteCustomFileManager;
import io.openvidu.server.utils.RemoteDockerManager;
import io.openvidu.server.recording.DummyRecordingUploader;
import io.openvidu.server.recording.RecordingDownloader;
import io.openvidu.server.recording.RecordingUploader;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.recording.service.RecordingManagerUtils;
import io.openvidu.server.recording.service.RecordingManagerUtilsLocalStorage;
import io.openvidu.server.utils.CustomFileManager;
import io.openvidu.server.utils.DockerManager;
import io.openvidu.server.utils.GeoLocationByIp;
import io.openvidu.server.utils.LocalCustomFileManager;
import io.openvidu.server.utils.LocalDockerManager;
import io.openvidu.server.utils.MediaNodeManager;
import io.openvidu.server.webhook.CDRLoggerWebhook;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import javax.crypto.NoSuchPaddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@Configuration
@Import({OpenViduServer.class})
public class OpenViduServerPro {
    private static final Logger log = LoggerFactory.getLogger(OpenViduServerPro.class);
    private static ConfigurableApplicationContext context;
    public static OpenviduConfigPro INITIAL_CONFIG;

    public OpenViduServerPro() {
    }

    @Bean
    @DependsOn({"openviduConfig", "infrastructureInstanceData"})
    public LoadManager loadManager(OpenviduConfigPro openviduConfigPro) {
        return (LoadManager)(!openviduConfigPro.isCluster() ? new DummyLoadManager() : new CpuLoadManager());
    }

    @Bean
    @DependsOn({"openviduConfig", "infrastructureManager"})
    public AutoscalingApplier autoscalingApplier(OpenviduConfigPro openviduConfigPro) {
        log.info("OpenVidu Pro autoscaling service is {}", openviduConfigPro.isAutoscaling() ? "enabled" : "disabled");
        return new AutoscalingApplier();
    }

    @Bean
    @DependsOn({"infrastructureManager"})
    public MediaNodesCpuLoadCollector mediaNodesCpuLoadCollector() {
        return new MediaNodesCpuLoadCollector();
    }

    @Bean
    public InfrastructureInstanceData infrastructureInstanceData() {
        return new InfrastructureInstanceData();
    }

    @Bean
    @DependsOn({"openviduConfig", "sessionManager", "infrastructureInstanceData"})
    public InfrastructureManager infrastructureManager(OpenviduConfigPro openviduConfigPro, InfrastructureInstanceData infrastructureInstanceData) {
        return createInfrastructureManager(openviduConfigPro, infrastructureInstanceData);
    }

    @Bean
    @DependsOn({"openviduConfig", "infrastructureManager"})
    public RecordingManager recordingManager(OpenviduConfigPro openviduConfigPro, InfrastructureManager infrastructureManager) {
        Object dockerManager;
        Object fileManager;
        if (openviduConfigPro.isRecordingComposedExternal()) {
            RemoteDockerManager remoteDockerManager = new RemoteDockerManager(openviduConfigPro, infrastructureManager);
            fileManager = new RemoteCustomFileManager(remoteDockerManager);
            dockerManager = remoteDockerManager;
        } else {
            dockerManager = new LocalDockerManager(false);
            fileManager = new LocalCustomFileManager();
        }

        return new RecordingManager((DockerManager)dockerManager, (CustomFileManager)fileManager);
    }

    @Bean
    @DependsOn({"openviduConfig", "recordingManager"})
    public RecordingManagerUtils recordingManagerUtils(OpenviduConfigPro openviduConfigPro, RecordingManager recordingManager) {
        return (RecordingManagerUtils)(OpenViduRecordingStorage.s3.equals(openviduConfigPro.getRecordingStorage()) ? new RecordingManagerUtilsS3(openviduConfigPro, recordingManager) : new RecordingManagerUtilsLocalStorage(openviduConfigPro, recordingManager));
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public RecordingUploader recordingUpload(OpenviduConfigPro openviduConfigPro) {
        return (RecordingUploader)(OpenViduRecordingStorage.s3.equals(openviduConfigPro.getRecordingStorage()) ? new S3RecordingUploader() : new DummyRecordingUploader());
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public RecordingDownloader recordingDownload() {
        return new MedianNodeRecordingDownloader();
    }

    @Bean
    @DependsOn({"openviduConfig", "infrastructureManager"})
    public BroadcastManager broadcastManager(InfrastructureManager infrastructureManager, KmsManager kmsManager, OpenviduConfigPro openviduConfigPro) {
        RemoteDockerManager remoteDockerManager = new RemoteDockerManager(openviduConfigPro, infrastructureManager);
        return new BroadcastManagerPro(remoteDockerManager);
    }

    @Bean
    @DependsOn({"openviduConfig", "mediaNodesCpuLoadCollector"})
    public CallDetailRecord cdr(OpenviduConfigPro openviduConfigPro) {
        List<CDRLogger> loggers = new ArrayList();
        if (openviduConfigPro.isElasticsearchDefined()) {
            log.info("OpenVidu Pro Elasticsearch service is enabled");
            loggers.add(new CDRLoggerElasticSearch(openviduConfigPro));
        } else {
            log.info("OpenVidu Pro Elasticsearch service is disabled (may be enabled with 'OPENVIDU_PRO_ELASTICSEARCH_HOST=URL')");
        }

        if (openviduConfigPro.isCdrEnabled()) {
            log.info("OpenVidu Pro CDR service is enabled");
            loggers.add(new CDRLoggerFile());
        } else {
            log.info("OpenVidu Pro CDR service is disabled (may be enable with 'OPENVIDU_CDR=true')");
        }

        if (openviduConfigPro.isWebhookEnabled() || openviduConfigPro.isMultiMasterEnvironment()) {
            String webhookEndpoint = openviduConfigPro.getReplicationManagerWebhook();
            webhookEndpoint = webhookEndpoint != null && !webhookEndpoint.isBlank() ? webhookEndpoint : openviduConfigPro.getOpenViduWebhookEndpoint();
            List<CDREventName> events = openviduConfigPro.isMultiMasterEnvironment() ? Arrays.asList(CDREventName.values()) : openviduConfigPro.getOpenViduWebhookEvents();
            loggers.add(new CDRLoggerWebhook(webhookEndpoint, openviduConfigPro.getOpenViduWebhookHeaders(), events));
        }

        if (openviduConfigPro.isWebhookEnabled()) {
            log.info("OpenVidu Pro Webhook service is enabled");
        } else {
            log.info("OpenVidu Pro Webhook service is disabled (may be enabled with 'OPENVIDU_WEBHOOK=true')");
        }

        return new CallDetailRecordPro(loggers, openviduConfigPro);
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public KurentoParticipantEndpointConfig kurentoEndpointConfig(OpenviduConfigPro openviduConfigPro) {
        return (KurentoParticipantEndpointConfig)(openviduConfigPro.isElasticsearchDefined() ? new KurentoParticipantEndpointConfigPro() : new KurentoParticipantEndpointConfig());
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public SessionManager sessionManager(OpenviduConfigPro openviduConfigPro) {
        return new KurentoSessionManagerPro(openviduConfigPro);
    }

    @Bean
    @DependsOn({"openviduConfig", "sessionManager", "loadManager"})
    public KmsManager kmsManager(OpenviduConfigPro openviduConfigPro, SessionManager sessionManager, LoadManager loadManager) {
        if (!openviduConfigPro.isCluster()) {
            if (openviduConfigPro.getKmsUris().isEmpty()) {
                log.error("OpenVidu Pro cluster mode disabled ('OPENVIDU_PRO_CLUSTER' is false). Configuration parameter 'KMS_URIS' cannot be empty");
                log.error("Shutting down OpenVidu Server");
                Runtime.getRuntime().halt(1);
            }

            log.info("OpenVidu Pro cluster mode disabled. Using a single KMS located at {}", openviduConfigPro.getKmsUris().get(0));
        } else {
            log.info("OpenVidu Pro cluster mode enabled");
            String scriptsPath = openviduConfigPro.getClusterPath();
            scriptsPath = scriptsPath.endsWith("/") ? scriptsPath : scriptsPath + "/";
            openviduConfigPro.setFinalClusterPath(scriptsPath);
            initializeClusterIdAndMasterNodeId(openviduConfigPro);
        }

        return new MultipleKmsManager(sessionManager, loadManager);
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public SessionEventsHandler sessionEventsHandler() {
        return new KurentoSessionEventsHandlerPro();
    }

    @Bean
    @DependsOn({"openviduConfig"})
    public TokenGenerator tokenGenerator() {
        return new TokenGeneratorPro();
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${OPENVIDU_PRO_KIBANA_HOST:}') && ${OPENVIDU_PRO_ELASTICSEARCH:true}")
    @DependsOn({"openviduConfig"})
    public KibanaConfig kibanaConfig() {
        return new KibanaConfig();
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${OPENVIDU_PRO_ELASTICSEARCH_HOST:}') && ${OPENVIDU_PRO_ELASTICSEARCH:true}")
    @DependsOn({"openviduConfig"})
    public ElasticSearchConfig elasticSearchConfig() {
        return new ElasticSearchConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn({"openviduConfig", "clusterUsageService"})
    public LambdaService lambdaService(OpenviduConfigPro openviduConfigPro) {
        try {
            return new LambdaService(openviduConfigPro);
        } catch (InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException var3) {
            log.error("LambdaService couldn't load public key: {}", var3.getMessage());
            log.error("Shutting down OpenVidu Server");
            Runtime.getRuntime().halt(1);
            return null;
        }
    }

    @Bean(
            name = {"clusterUsageService"}
    )
    @ConditionalOnProperty(
            name = {"OPENVIDU_PRO_LICENSE_OFFLINE"},
            havingValue = "true"
    )
    @DependsOn({"openviduConfig", "specialLicenseConfig"})
    public ClusterUsageService clusterUsageService() {
        return new ClusterUsageService();
    }

    @Bean(
            name = {"clusterUsageService"}
    )
    @ConditionalOnMissingBean
    @DependsOn({"openviduConfig"})
    public ClusterUsageService clusterUsageServiceFallback() {
        return new ClusterUsageService();
    }

    @Bean
    @DependsOn({"clusterUsageService", "infrastructureManager"})
    public MediaNodeManager mediaNodeManager() {
        return new MediaNodeManagerPro();
    }

    @Bean
    public GeoLocationByIp geoLocationByIp() {
        return new GeoLocationByIpPro();
    }

    @Bean
    @ConditionalOnExpression("'${OPENVIDU_RECORDING}'=='true' && '${OPENVIDU_PRO_RECORDING_STORAGE}'=='s3'")
    @DependsOn({"openviduConfig", "lambdaService"})
    public S3Handler S3Handler(OpenviduConfigPro openviduConfigPro, LambdaService lambdaService) {
        log.info("AWS S3 recording storage is enabled");
        return new S3Handler(openviduConfigPro, lambdaService);
    }

    @Bean
    @DependsOn({"recordingManager"})
    public HealthCheckManager healthCheckManager() {
        return new HealthCheckManagerPro();
    }

    @Bean
    @ConditionalOnExpression("${OPENVIDU_PRO_STATS_SESSION_INTERVAL} > 0 && !T(org.springframework.util.StringUtils).isEmpty('${OPENVIDU_PRO_ELASTICSEARCH_HOST:}') && ${OPENVIDU_PRO_ELASTICSEARCH:true}")
    @DependsOn({"openviduConfig"})
    public MicrometerSessionConfig micrometerSessionConfig() {
        return new MicrometerSessionConfig();
    }

    @Bean
    @ConditionalOnExpression("${OPENVIDU_PRO_STATS_SERVER_INTERVAL} > 0 && !T(org.springframework.util.StringUtils).isEmpty('${OPENVIDU_PRO_ELASTICSEARCH_HOST:}')")
    @DependsOn({"openviduConfig"})
    public ElasticConfig elasticConfig(final OpenviduConfigPro openviduConfigPro) {
        log.info("Server Micrometer stats enabled. Gathering every {}s", openviduConfigPro.getOpenviduProStatsServerInterval());
        return new ElasticConfig() {
            @Nullable
            public String get(String k) {
                return null;
            }

            public String index() {
                return "server-metrics";
            }

            public String host() {
                return openviduConfigPro.getElasticsearchHost().replaceAll("/$", "");
            }

            public Duration step() {
                return Duration.ofSeconds((long)openviduConfigPro.getOpenviduProStatsServerInterval());
            }

            @Nullable
            public String userName() {
                return openviduConfigPro.isElasticSearchSecured() ? openviduConfigPro.getElasticsearchUserName() : null;
            }

            @Nullable
            public String password() {
                return openviduConfigPro.isElasticSearchSecured() ? openviduConfigPro.getElasticsearchPassword() : null;
            }

            public String indexDateFormat() {
                return "yyyy.MM.dd";
            }
        };
    }

    @Bean
    @ConditionalOnProperty(
            name = {"SUPPORT_DEPRECATED_API"},
            havingValue = "true"
    )
    public FilterRegistrationBean<ApiRestPathRewriteFilterPro> filterRegistrationBean() {
        FilterRegistrationBean<ApiRestPathRewriteFilterPro> registrationBean = new FilterRegistrationBean();
        ApiRestPathRewriteFilterPro apiRestPathRewriteFilter = new ApiRestPathRewriteFilterPro();
        registrationBean.setFilter(apiRestPathRewriteFilter);
        return registrationBean;
    }

    @Bean
    @DependsOn({"sessionEventsHandler", "sessionManager"})
    @ConditionalOnExpression("'${OPENVIDU_PRO_SPEECH_TO_TEXT}'!='disabled'")
    public SpeechToTextManager speechToTextManager() {
        return new SpeechToTextManager();
    }

    @Bean
    @DependsOn({"speechToTextManager"})
    @ConditionalOnExpression("'${OPENVIDU_PRO_SPEECH_TO_TEXT}'!='disabled'")
    public SpeechToTextGrpcClient speechToTextGrpcClient() {
        return new SpeechToTextGrpcClient();
    }

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext app = SpringApplication.run(OpenviduConfigPro.class, new String[]{"--spring.main.web-application-type=none", "--spring.main.banner-mode=off"});
        INITIAL_CONFIG = (OpenviduConfigPro)app.getBean(OpenviduConfigPro.class);
        INITIAL_CONFIG.checkConfiguration(true);
        initializeClusterIdAndMasterNodeId(INITIAL_CONFIG);
        setBannerBasedOnOpenViduEdition(INITIAL_CONFIG.getOpenViduEdition());
        app.close();
        Map<String, String> CONFIG_PROPS = OpenViduServer.checkConfigProperties(OpenviduConfigPro.class);
        provisionMediaNodesByUris(CONFIG_PROPS);
        checkKmsUrisConnections(CONFIG_PROPS);
        if (CONFIG_PROPS.get("SERVER_PORT") != null) {
            System.setProperty("server.port", (String)CONFIG_PROPS.get("SERVER_PORT"));
            Logger var10000 = log;
            String var10001 = (String)CONFIG_PROPS.get("SERVER_PORT");
            var10000.warn("You have set property server.port (or SERVER_PORT). This will serve OpenVidu Server Pro on your host at port " + var10001 + ". But property HTTPS_PORT (" + (String)CONFIG_PROPS.get("HTTPS_PORT") + ") still configures the port that should be used to connect to OpenVidu Server from outside. Bear this in mind when configuring a proxy in front of OpenVidu Server");
        } else if (CONFIG_PROPS.get("HTTPS_PORT") != null) {
            System.setProperty("server.port", (String)CONFIG_PROPS.get("HTTPS_PORT"));
        }

        enableOrDisableMicrometer(CONFIG_PROPS, (Integer)null, Boolean.valueOf((String)CONFIG_PROPS.get("OPENVIDU_PRO_ELASTICSEARCH")));
        String[] argsAux = new String[args.length + 2];
        System.arraycopy(args, 0, argsAux, 0, args.length);
        argsAux[argsAux.length - 2] = "--spring.main.banner-mode=off";
        argsAux[argsAux.length - 1] = "--spring.main.allow-circular-references=true";
        context = SpringApplication.run(OpenViduServerPro.class, argsAux);
    }

    private static void enableOrDisableMicrometer(Map<String, String> newAppliedProperties, Integer previousValue, boolean isElasticsearchEnabled) {
        System.setProperty("management.health.elasticsearch.enabled", Boolean.toString(false));
        if (!isElasticsearchEnabled) {
            System.setProperty("management.metrics.export.elastic.enabled", Boolean.toString(false));
            System.setProperty("management.health.elasticsearch.enabled", Boolean.toString(false));
        } else {
            String interval = (String)newAppliedProperties.get("OPENVIDU_PRO_STATS_SERVER_INTERVAL");
            boolean enabled;
            if (interval != null) {
                enabled = Integer.parseInt(interval) != 0;
            } else {
                enabled = previousValue != 0;
            }

            System.setProperty("management.metrics.export.elastic.enabled", Boolean.toString(enabled));
        }
    }

    public void restart(Map<String, String> properties) {
        ApplicationArguments args = (ApplicationArguments)context.getBean(ApplicationArguments.class);
        Thread thread = new Thread(() -> {
            int previousStatsServerInterval = ((OpenviduConfigPro)context.getBean(OpenviduConfigPro.class)).getOpenviduProStatsServerInterval();
            boolean previousElasticsearchEnabled = ((OpenviduConfigPro)context.getBean(OpenviduConfigPro.class)).isElasticsearchDefined();
            context.close();
            properties.entrySet().forEach((entry) -> {
                System.setProperty((String)entry.getKey(), ((String)entry.getValue()).toString());
            });
            enableOrDisableMicrometer(properties, previousStatsServerInterval, previousElasticsearchEnabled);
            context = SpringApplication.run(OpenViduServerPro.class, args.getSourceArgs());
        });
        thread.setDaemon(false);
        thread.start();
    }

    private static void initializeClusterIdAndMasterNodeId(OpenviduConfigPro openviduConfigPro) {
        String clusterId = openviduConfigPro.getClusterId();
        if (clusterId != null && !clusterId.isEmpty()) {
            log.info("Cluster identifier found in OPENVIDU_PRO_CLUSTER_ID: {}", clusterId);
        } else {
            clusterId = openviduConfigPro.getDomainOrPublicIp();
            log.info("Cluster identifier not configured in OPENVIDU_PRO_CLUSTER_ID. Using DOMAIN_OR_PUBLIC_IP as cluster identifier: {}", clusterId);
        }

        openviduConfigPro.setClusterId(clusterId);
        openviduConfigPro.loadOpenViduProMasterNodeId();
    }

    private static void setBannerBasedOnOpenViduEdition(OpenViduEdition edition) {
        if (OpenViduEdition.pro.equals(edition)) {
            System.setProperty("spring.banner.location", "classpath:banner-pro.txt");
        } else if (OpenViduEdition.enterprise.equals(edition)) {
            System.setProperty("spring.banner.location", "classpath:banner-enterprise.txt");
        }

    }

    private static InfrastructureManager createInfrastructureManager(OpenviduConfigPro openviduConfigPro, InfrastructureInstanceData infrastructureInstanceData) {
        if (openviduConfigPro.isCluster()) {
            log.info("OpenVidu Pro is deployed in '{}' environment", openviduConfigPro.getClusterEnvironment().name());
            if (!openviduConfigPro.getClusterMode().equals(OpenViduClusterMode.auto)) {
                log.warn("OpenVidu Pro cluster mode is '{}'. There will be no automatic instances management", openviduConfigPro.getClusterMode().name());
                return new DummyInfrastructureManager(openviduConfigPro, infrastructureInstanceData);
            } else {
                Object manager;
                switch (openviduConfigPro.getClusterEnvironment()) {
                    case docker:
                        manager = new DockerInfrastructureManager(infrastructureInstanceData);

                        try {
                            ((DockerInfrastructureManager)manager).checkDockerEnabled();
                        } catch (OpenViduException var5) {
                            log.error(var5.getMessage() + ": configuration 'OPENVIDU_PRO_CLUSTER_ENVIRONMENT' set to 'docker' requires Docker in the host machine");
                            log.error("Shutting down OpenVidu Server");
                            Runtime.getRuntime().halt(1);
                        }
                        break;
                    case on_premise:
                        String scriptsPath = openviduConfigPro.getClusterPath();
                        scriptsPath = scriptsPath.endsWith("/") ? scriptsPath : scriptsPath + "/";
                        openviduConfigPro.setFinalClusterPath(scriptsPath);
                        manager = new OnpremiseInfrastructureManager(infrastructureInstanceData);
                        break;
                    case aws:
                        String awsScriptsPath = openviduConfigPro.getClusterPath();
                        awsScriptsPath = awsScriptsPath.endsWith("/") ? awsScriptsPath : awsScriptsPath + "/";
                        if (!awsScriptsPath.endsWith("/aws/")) {
                            awsScriptsPath = awsScriptsPath + "aws/";
                        }

                        openviduConfigPro.setFinalClusterPath(awsScriptsPath);
                        manager = new OnpremiseInfrastructureManager(infrastructureInstanceData);
                        break;
                    case fake:
                        manager = new FakeInfrastructureManager(infrastructureInstanceData);
                        break;
                    default:
                        manager = new DockerInfrastructureManager(infrastructureInstanceData);
                }

                return (InfrastructureManager)manager;
            }
        } else {
            log.warn("OpenVidu Pro is not running in cluster mode", openviduConfigPro.getClusterEnvironment().name());
            return new DummyInfrastructureManager(openviduConfigPro, infrastructureInstanceData);
        }
    }

    private static void checkKmsUrisConnections(Map<String, String> CONFIG_PROPS) throws InterruptedException {
        String OPENVIDU_PRO_CLUSTER_MODE = (String)CONFIG_PROPS.get("OPENVIDU_PRO_CLUSTER_MODE");
        String KMS_URIS = (String)CONFIG_PROPS.get("KMS_URIS");
        boolean mustCheckKmsUris = OPENVIDU_PRO_CLUSTER_MODE != null && OpenViduClusterMode.manual.name().equals(OPENVIDU_PRO_CLUSTER_MODE) && KMS_URIS != null && !KMS_URIS.isEmpty() && !"[]".equals(KMS_URIS);
        if (mustCheckKmsUris) {
            ConfigurableApplicationContext app = SpringApplication.run(OpenviduConfigPro.class, new String[]{"--spring.main.web-application-type=none", "--spring.main.banner-mode=off"});
            OpenviduConfigPro config = (OpenviduConfigPro)app.getBean(OpenviduConfigPro.class);
            log.info("Checking KMS_URIS {}", config.getKmsUris());
            Iterator var6 = config.getKmsUris().iterator();

            while(var6.hasNext()) {
                String kmsUri = (String)var6.next();

                try {
                    OpenviduConfigPro.waitUntilKmsReady(kmsUri, 1000, 120);
                } catch (TimeoutException var11) {
                    String errorMsg = "You have declared configuration property KMS_URIS=" + KMS_URIS + "\n   But OpenVidu Server Pro failed connecting to KMS with URI \"" + kmsUri + "\"";
                    String msg = "\n\n\n   Configuration errors\n   --------------------\n\n   " + errorMsg + "\n\n   Fix config errors\n   ---------------\n\n   1) Return to shell pressing Ctrl+C\n   2) Make sure OpenVidu Server Pro can connect to KMS " + kmsUri + "\n   3) Restart OpenVidu with:\n\n      $ ./openvidu restart\n\n";
                    log.info(msg);
                    (new Semaphore(0)).acquire();
                }
            }

            app.close();
        }

    }

    private static void provisionMediaNodesByUris(Map<String, String> CONFIG_PROPS) {
        String OPENVIDU_PRO_CLUSTER_MODE = (String)CONFIG_PROPS.get("OPENVIDU_PRO_CLUSTER_MODE");
        String KMS_URIS = (String)CONFIG_PROPS.get("KMS_URIS");
        boolean mustProvisionKmsUris = OpenViduClusterMode.manual.name().equals(OPENVIDU_PRO_CLUSTER_MODE) && KMS_URIS != null && !KMS_URIS.isEmpty() && !"[]".equals(KMS_URIS);
        if (mustProvisionKmsUris) {
            ConfigurableApplicationContext app = SpringApplication.run(OpenviduConfigPro.class, new String[]{"--spring.main.web-application-type=none", "--spring.main.banner-mode=off"});
            OpenviduConfigPro config = (OpenviduConfigPro)app.getBean(OpenviduConfigPro.class);
            InfrastructureInstanceData infrastructureInstanceData = new InfrastructureInstanceData();
            InfrastructureManager infrastructureManager = createInfrastructureManager(config, infrastructureInstanceData);
            if (config.getKmsUris().size() > 1 && !config.isMultiMasterEnvironment() && config.isMonoNode()) {
                log.error("Can not launch more than One Media Nodes with your current deployment");
                Runtime.getRuntime().halt(1);
            }

            Iterator var8 = config.getKmsUris().iterator();

            while(var8.hasNext()) {
                String kmsUri = (String)var8.next();
                String ip = null;

                try {
                    URI uri = new URI(kmsUri);
                    ip = uri.getHost();
                } catch (Exception var44) {
                    log.error("Not valid URI: {}", kmsUri);
                    Runtime.getRuntime().halt(1);
                }

                String configuredKmsImage = config.getKmsImage();
                String configureMediasoupImage = config.getMediasoupImage();
                String configuredMetricbeatImage = config.getMetricbeatImage();
                String configuredFilebeatImage = config.getFilebeatImage();
                String mnodeCoturnImage = config.getCoturnImage();
                String speechToTextImage = config.getSpeechToTextImage();
                int mnodeCoturnPort = config.getCoturnConfig().getCoturnPort();
                int mnodeCoturnMinPort = config.getCoturnConfig().getMediaNodeMinPort();
                int mnodeCoturnMaxPort = config.getCoturnConfig().getMediaNodeMaxPort();
                String mnodeCoturnSharedSecretKey = config.getCoturnConfig().getCoturnSharedSecretKey();
                int speechToTextPort = config.getSpeechToTextPort();
                SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy = config.getSpeechToTextVoskModelLoadStrategy();
                SpeechToTextType speechToTextType = config.getSpeechToText();
                String speechToTextAzureKey = config.getSpeechToTextAzureKey();
                String speechToTextAzureRegion = config.getSpeechToTextAzureRegion();
                String speechToTextAwsAccessKey = config.getAwsAccessKey();
                String speechToTextAwsSecret = config.getAwsSecretKey();
                String speechToTextAwsRegion = config.getAwsRegion();
                String openViduSecret = config.getOpenViduSecret();
                boolean pullRecordingImage = config.isRecordingModuleEnabled() && config.isRecordingComposedExternal();
                List<DockerRegistryConfig> dockerRegistryConfigList = config.getDockerRegistries();
                initializeClusterIdAndMasterNodeId(config);
                String clusterId = config.getClusterId();
                MediaNodeKurentoConfig kmsConfig = config.getMediaNodeKurentoConfig();
                int loadInterval = config.getOpenviduProStatsMonitoringInterval();
                String esHost = config.getElasticsearchHost();
                String esUserName = config.getElasticsearchUserName();
                String esPassword = config.getElasticsearchPassword();

                try {
                    log.info("Provisioning media-node {} if containers are not running", ip);
                    AdditionalLogAggregator additionalLogAggregator = config.getAdditionalLogAggregator();
                    AdditionalMonitoring additionalMonitoring = config.getAdditionalMonitoring();
                    infrastructureManager.checkAndConfigMediaNode(ip, dockerRegistryConfigList);
                    String var10000 = config.getOpenviduRecordingImageRepo();
                    String recordingImage = var10000 + ":" + config.getOpenViduRecordingVersion();
                    infrastructureManager.provisionMediaNode(ip, InstanceType.mediaServer, openViduSecret, additionalLogAggregator, configuredKmsImage, configureMediasoupImage, kmsConfig, pullRecordingImage ? recordingImage : "NONE");
                    String nodeId = "media_" + ip;
                    if (config.isElasticsearchDefined()) {
                        infrastructureManager.provisionBeats(ip, clusterId, openViduSecret, loadInterval, esHost, esUserName, esPassword, configuredMetricbeatImage, configuredFilebeatImage, nodeId);
                    } else {
                        infrastructureManager.dropBeats(ip);
                    }

                    if (config.getCoturnConfig().isDeployedOnMediaNodes()) {
                        infrastructureManager.provisionCoturn(ip, clusterId, mnodeCoturnImage, mnodeCoturnPort, mnodeCoturnMinPort, mnodeCoturnMaxPort, mnodeCoturnSharedSecretKey, nodeId);
                    } else {
                        infrastructureManager.dropCoturn(ip);
                    }

                    if (!SpeechToTextType.disabled.equals(config.getSpeechToText())) {
                        infrastructureManager.provisionSpeechToTextService(ip, clusterId, speechToTextImage, speechToTextType, speechToTextVoskModelLoadStrategy, speechToTextPort, speechToTextAzureKey, speechToTextAzureRegion, speechToTextAwsAccessKey, speechToTextAwsSecret, speechToTextAwsRegion, nodeId);
                    } else {
                        infrastructureManager.dropSpeechToText(ip);
                    }

                    if (config.getAdditionalMonitoring().getType() == Type.datadog) {
                        Map<String, String> datadogProperties = additionalMonitoring.getProperties();
                        infrastructureManager.provisionDataDog(ip, openViduSecret, datadogProperties);
                    }
                } catch (Exception var43) {
                    log.error("Can't provision autodiscovered node. Probably media-node-controller is not running: {}", ip);
                    var43.printStackTrace();
                }
            }

            app.close();
        }

    }

    @EventListener({ApplicationReadyEvent.class})
    public void doSomethingAfterStartup() {
        if (Status.startTime == 0L) {
            Status.startTime = System.currentTimeMillis();
        } else {
            ++Status.restartCounter;
            Status.lastRestartTime = System.currentTimeMillis();
        }

    }
}
