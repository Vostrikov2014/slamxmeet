//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.amazonaws.regions.Regions;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.MediaServer;
import io.openvidu.server.config.AdditionalLogAggregator.SplunkProperties;
import io.openvidu.server.config.AdditionalLogAggregator.Type;
import io.openvidu.server.config.AdditionalMonitoring.DataDogProperties;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeProvisioner;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import io.openvidu.server.recording.OpenViduRecordingStorage;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.SpeechToTextVoskModelLoadStrategy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.kurento.client.KurentoClient;
import org.kurento.commons.exception.KurentoException;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

@Component
@Primary
@PropertySource({"classpath:application-pro.properties"})
public class OpenviduConfigPro extends OpenviduConfig {
    private String license;
    private String licenseApiUrl;
    private boolean isCluster;
    private String clusterId;
    private int mediaNodeLoadInterval;
    private OpenViduClusterEnvironment clusterEnvironment;
    private OpenViduClusterMode clusterMode;
    private String masterNodeId;
    private String masterNodeIp;
    private int mediaNodes;
    private Integer reconnectionTimeout;
    private String clusterPath;
    private boolean clusterTest;
    private RecordingsConfig recordingsConfig;
    private String replicationManagerWebhook;
    private boolean isMultimasterOnPremises;
    private boolean isMonoNode;
    private boolean isLicenseOffline;
    private String licenseHttpProxyHost;
    private String licenseHttpProxyPort;
    private CoturnConfig coturnConfig;
    private boolean autoscaling;
    private int autoscalingInterval;
    private int autoscalingMaxNodes;
    private int autoscalingMinNodes;
    private int autoscalingMaxAvgLoad;
    private int autoscalingMinAvgLoad;
    private OpenViduEdition openviduEdition;
    private MediaServer mediaServer;
    private PublicIpAutodiscovery mediaNodePublicIpAutodiscoveryMode;
    private boolean webrtcSimulcast;
    private int statsSessionInterval;
    private int statsServerInterval;
    private int statsMonitoringInterval;
    private int statsWebrtcInterval;
    private String awsS3Bucket;
    private Map<String, String> awsS3Headers;
    private String awsS3ServiceEndpoint;
    private Boolean awsS3PathStyleAccess;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsRegion;
    private boolean networkQuality;
    private int networkQualityInterval;
    private SpeechToTextType speechToText;
    private String speechToTextImage;
    private SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy;
    private int speechToTextPort;
    private String speechToTextAzureKey;
    private String speechToTextAzureRegion;
    private String elasticsearchVersion;
    private String elasticsearchHost;
    private boolean elasticsearchEnabled;
    private String kibanaHost;
    private String elasticsearchUserName;
    private String elasticsearchPassword;
    private int elasticsearchMaxDaysDelete;
    private String kibanaVersion;
    private String openViduPrivateIp;
    private String kmsImage;
    private String mediasoupImage;
    private String filebeatImage;
    private String metricbeatImage;
    private String coturnImage;
    private MediaNodeKurentoConfig kmsConfig;
    private boolean isAutodiscoveryPossible = true;
    private AdditionalLogAggregator additionalLogAggregator;
    private AdditionalMonitoring additionalMonitoring;
    private BrowserLog sendBrowserLogs;
    private List<DockerRegistryConfig> dockerRegistries;
    private String dindImage;

    public OpenviduConfigPro() {
        this.secretProps.addAll(Arrays.asList("OPENVIDU_PRO_LICENSE", "OPENVIDU_PRO_AWS_ACCESS_KEY", "OPENVIDU_PRO_AWS_SECRET_KEY", "OPENVIDU_PRO_SPEECH_TO_TEXT_AZURE_KEY", "ELASTICSEARCH_PASSWORD"));
    }

    public String getLicense() {
        return this.license;
    }

    public String getLicenseApiUrl() {
        return this.licenseApiUrl;
    }

    public String getLicenseHttpProxyHost() {
        return this.licenseHttpProxyHost;
    }

    public String getLicenseHttpProxyPort() {
        return this.licenseHttpProxyPort;
    }

    public int getOpenviduProStatsSessionInterval() {
        return this.statsSessionInterval;
    }

    public int getOpenviduProStatsServerInterval() {
        return this.statsServerInterval;
    }

    public int getOpenviduProStatsMonitoringInterval() {
        return this.statsMonitoringInterval;
    }

    public int getOpenviduProStatsWebrtcInterval() {
        return this.statsWebrtcInterval;
    }

    public String getOpenViduProMasterNodeId() {
        return this.masterNodeId;
    }

    public String getElasticsearchHost() {
        return this.elasticsearchHost;
    }

    public String getElasticsearchUserName() {
        return this.elasticsearchUserName;
    }

    public String getElasticsearchPassword() {
        return this.elasticsearchPassword;
    }

    public int getElasticsearchMaxDaysDelete() {
        return this.elasticsearchMaxDaysDelete;
    }

    public String getElasticsearchVersion() {
        return this.elasticsearchVersion;
    }

    public String getKibanaHost() {
        return this.kibanaHost;
    }

    public String getKibanaVersion() {
        return this.kibanaVersion;
    }

    public boolean isCluster() {
        return this.isCluster;
    }

    public String getClusterId() {
        return this.clusterId;
    }

    public int getMediaNodeLoadInterval() {
        return this.mediaNodeLoadInterval;
    }

    public PublicIpAutodiscovery getMediaNodePublicIpAutodiscoveryMode() {
        return this.mediaNodePublicIpAutodiscoveryMode;
    }

    public String getKmsImage() {
        return this.kmsImage;
    }

    public String getMediasoupImage() {
        return this.mediasoupImage;
    }

    public String getFilebeatImage() {
        return this.filebeatImage;
    }

    public String getMetricbeatImage() {
        return this.metricbeatImage;
    }

    public String getCoturnImage() {
        return this.coturnImage;
    }

    public MediaNodeKurentoConfig getMediaNodeKurentoConfig() {
        return new MediaNodeKurentoConfig(this.kmsConfig);
    }

    public OpenViduClusterEnvironment getClusterEnvironment() {
        return this.clusterEnvironment;
    }

    public OpenViduClusterMode getClusterMode() {
        return OpenViduClusterEnvironment.on_premise.equals(this.getClusterEnvironment()) ? OpenViduClusterMode.manual : OpenViduClusterMode.auto;
    }

    public int getMediaNodes() {
        return this.mediaNodes;
    }

    public int getReconnectionTimeout() {
        return this.reconnectionTimeout;
    }

    public int getAppliedReconnectionTimeout() {
        return this.reconnectionTimeout == -1 ? Integer.MAX_VALUE : this.reconnectionTimeout;
    }

    public String getCoturnIp(String kmsUri) {
        return this.coturnConfig.getCoturnIp(kmsUri);
    }

    public int getCoturnPort() {
        return this.coturnConfig.getCoturnPort();
    }

    public CoturnConfig getCoturnConfig() {
        return this.coturnConfig;
    }

    public boolean isMediaNodesAutodiscovery() {
        return this.isAutodiscoveryPossible && OpenViduClusterMode.auto.equals(this.getClusterMode());
    }

    public String getClusterPath() {
        return this.clusterPath;
    }

    public void setFinalClusterPath(String path) {
        this.clusterPath = path;
    }

    public boolean isClusterTestEnabled() {
        return this.clusterTest;
    }

    public boolean isAutoscaling() {
        return this.autoscaling;
    }

    public int getAutoscalingInterval() {
        return this.autoscalingInterval;
    }

    public int getAutoscalingMaxNodes() {
        return this.autoscalingMaxNodes;
    }

    public int getAutoscalingMinNodes() {
        return this.autoscalingMinNodes;
    }

    public int getAutoscalingMaxAvgLoad() {
        return this.autoscalingMaxAvgLoad;
    }

    public int getAutoscalingMinAvgLoad() {
        return this.autoscalingMinAvgLoad;
    }

    public OpenViduEdition getOpenViduEdition() {
        return this.openviduEdition;
    }

    public MediaServer getMediaServer() {
        return this.mediaServer;
    }

    public boolean isWebrtcSimulcast() {
        return this.webrtcSimulcast;
    }

    public RecordingsConfig getRecordingsConfig() {
        return this.recordingsConfig;
    }

    public OpenViduRecordingStorage getRecordingStorage() {
        return this.recordingsConfig.getRecordingStorage();
    }

    public boolean isRecordingComposedExternal() {
        return this.recordingsConfig.isRecordingComposedExternal();
    }

    public String getAwsS3Bucket() {
        return this.awsS3Bucket;
    }

    public Map<String, String> getAwsS3Headers() {
        return this.awsS3Headers;
    }

    public String getAwsS3ServiceEndpoint() {
        return this.awsS3ServiceEndpoint;
    }

    public Boolean getAwsS3PathStyleAccess() {
        return this.awsS3PathStyleAccess;
    }

    public String getAwsAccessKey() {
        return this.awsAccessKey;
    }

    public String getAwsSecretKey() {
        return this.awsSecretKey;
    }

    public String getAwsRegion() {
        return this.awsRegion;
    }

    public String getReplicationManagerWebhook() {
        return this.replicationManagerWebhook;
    }

    public boolean isLicenseOffline() {
        return this.isLicenseOffline;
    }

    public boolean isMultiMasterEnvironment() {
        return this.getReplicationManagerWebhook() != null && !this.getReplicationManagerWebhook().isBlank();
    }

    public boolean isMultimasterOnPremises() {
        return this.isMultimasterOnPremises;
    }

    public boolean isMonoNode() {
        return this.isMonoNode;
    }

    public boolean isMultiMasterMonoNode() {
        return this.isMultiMasterEnvironment() && this.isMonoNode();
    }

    public boolean isNetworkQualityEnabled() {
        return this.networkQuality;
    }

    public int getNetworkQualityInterval() {
        return this.networkQualityInterval;
    }

    public boolean isSpeechToTextEnabled() {
        return !SpeechToTextType.disabled.equals(this.speechToText);
    }

    public SpeechToTextType getSpeechToText() {
        return this.speechToText;
    }

    public int getSpeechToTextPort() {
        return this.speechToTextPort;
    }

    public String getSpeechToTextImage() {
        return this.speechToTextImage;
    }

    public String getSpeechToTextAzureKey() {
        return this.speechToTextAzureKey;
    }

    public String getSpeechToTextAzureRegion() {
        return this.speechToTextAzureRegion;
    }

    public SpeechToTextVoskModelLoadStrategy getSpeechToTextVoskModelLoadStrategy() {
        return this.speechToTextVoskModelLoadStrategy;
    }

    public String getOpenViduPrivateIp() {
        return this.openViduPrivateIp;
    }

    public String getMasterNodeIp() {
        return this.masterNodeIp;
    }

    public boolean isElasticsearchDefined() {
        return this.elasticsearchEnabled && this.elasticsearchHost != null && !this.elasticsearchHost.isEmpty();
    }

    public boolean isKibanaDefined() {
        return this.elasticsearchEnabled && this.kibanaHost != null && !this.kibanaHost.isEmpty();
    }

    public AdditionalLogAggregator getAdditionalLogAggregator() {
        return this.additionalLogAggregator;
    }

    public AdditionalMonitoring getAdditionalMonitoring() {
        return this.additionalMonitoring;
    }

    public String getOpenViduFrontendDefaultPath() {
        return "/inspector";
    }

    public String getDindImage() {
        return this.dindImage;
    }

    public List<DockerRegistryConfig> getDockerRegistries() {
        return this.dockerRegistries;
    }

    public BrowserLog getSendBrowserLogs() {
        return this.sendBrowserLogs;
    }

    public void cancelMediaNodesAutodiscovery() {
        this.isAutodiscoveryPossible = false;
    }

    public void setKibanaVersion(String version) {
        this.kibanaVersion = version;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setElasticsearchVersion(String version) {
        this.elasticsearchVersion = version;
    }

    protected OpenviduConfig newOpenviduConfig() {
        return new OpenviduConfigPro();
    }

    public String getOpenViduRecordingPath(String key) {
        return this.recordingsConfig.getRecordingsVolumePath(key);
    }

    protected void checkConfigurationProperties(boolean loadDotenv) {
        super.checkConfigurationProperties(loadDotenv);
        this.loadMediaNodePublicIpAutodiscoveryMode();
        this.loadCoturnConfig();
        this.licenseApiUrl = this.asNonEmptyString("OPENVIDU_PRO_LICENSE_API");
        this.asOptionalURL("OPENVIDU_PRO_LICENSE_API");
        this.isLicenseOffline = Boolean.valueOf(this.getValue("OPENVIDU_PRO_LICENSE_OFFLINE", false));
        this.loadLicenseHttpProxy("OPENVIDU_PRO_LICENSE_HTTP_PROXY");
        this.license = this.asNonEmptyString("OPENVIDU_PRO_LICENSE");
        this.statsSessionInterval = this.asNonNegativeInteger("OPENVIDU_PRO_STATS_SESSION_INTERVAL");
        this.statsServerInterval = this.asNonNegativeInteger("OPENVIDU_PRO_STATS_SERVER_INTERVAL");
        this.statsMonitoringInterval = this.asNonNegativeInteger("OPENVIDU_PRO_STATS_MONITORING_INTERVAL");
        this.statsWebrtcInterval = this.asNonNegativeInteger("OPENVIDU_PRO_STATS_WEBRTC_INTERVAL");
        this.isCluster = this.asBoolean("OPENVIDU_PRO_CLUSTER");
        this.clusterId = this.asOptionalString("OPENVIDU_PRO_CLUSTER_ID");
        this.mediaNodes = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_MEDIA_NODES");
        this.reconnectionTimeout = this.asOptionalIntegerBetweenRanges("OPENVIDU_PRO_CLUSTER_RECONNECTION_TIMEOUT", new Range[]{Range.between(-1, -1), Range.between(3, Integer.MAX_VALUE)});
        this.clusterEnvironment = (OpenViduClusterEnvironment)this.asEnumValue("OPENVIDU_PRO_CLUSTER_ENVIRONMENT", OpenViduClusterEnvironment.class);
        this.clusterMode = (OpenViduClusterMode)this.asEnumValue("OPENVIDU_PRO_CLUSTER_MODE", OpenViduClusterMode.class);
        this.mediaNodeLoadInterval = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_LOAD_INTERVAL");
        this.clusterTest = this.asBoolean("OPENVIDU_PRO_CLUSTER_TEST");
        this.clusterPath = this.asFileSystemPath("OPENVIDU_PRO_CLUSTER_PATH");
        this.autoscaling = this.asBoolean("OPENVIDU_PRO_CLUSTER_AUTOSCALING");
        this.autoscalingInterval = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_AUTOSCALING_INTERVAL");
        this.autoscalingMaxNodes = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MAX_NODES");
        this.autoscalingMinNodes = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_NODES");
        this.autoscalingMaxAvgLoad = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MAX_LOAD");
        this.autoscalingMinAvgLoad = this.asNonNegativeInteger("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_LOAD");
        OpenViduRecordingStorage recordingStorage = (OpenViduRecordingStorage)this.asEnumValue("OPENVIDU_PRO_RECORDING_STORAGE", OpenViduRecordingStorage.class);
        boolean recordingComposedExternal = this.asBoolean("OPENVIDU_PRO_RECORDING_COMPOSED_EXTERNAL");
        this.recordingsConfig = new RecordingsConfig(recordingStorage, recordingComposedExternal, this.getOpenViduRecordingPath());
        this.awsS3Bucket = this.asOptionalStringAndNullIfBlank("OPENVIDU_PRO_AWS_S3_BUCKET");
        this.awsS3Headers = this.asOptionalStringMap("OPENVIDU_PRO_AWS_S3_HEADERS");
        this.awsS3ServiceEndpoint = this.asOptionalURL("OPENVIDU_PRO_AWS_S3_SERVICE_ENDPOINT");
        this.awsS3PathStyleAccess = this.asOptionalBoolean("OPENVIDU_PRO_AWS_S3_WITH_PATH_STYLE_ACCESS", true);
        this.awsAccessKey = this.asOptionalStringAndNullIfBlank("OPENVIDU_PRO_AWS_ACCESS_KEY");
        this.awsSecretKey = this.asOptionalStringAndNullIfBlank("OPENVIDU_PRO_AWS_SECRET_KEY");
        this.awsRegion = this.asOptionalStringAndNullIfBlank("OPENVIDU_PRO_AWS_REGION");
        this.openviduEdition = (OpenViduEdition)this.asEnumValue("OPENVIDU_EDITION", OpenViduEdition.class);
        String mediaServerStr = this.asOptionalString("OPENVIDU_ENTERPRISE_MEDIA_SERVER");
        if (mediaServerStr == null || mediaServerStr.isBlank()) {
            if (OpenViduEdition.pro.equals(this.getOpenViduEdition())) {
                mediaServerStr = MediaServer.kurento.name();
            } else if (OpenViduEdition.enterprise.equals(this.getOpenViduEdition())) {
                mediaServerStr = MediaServer.mediasoup.name();
            }
        }

        this.mediaServer = (MediaServer)Enum.valueOf(MediaServer.class, mediaServerStr);
        this.webrtcSimulcast = this.asBoolean("OPENVIDU_WEBRTC_SIMULCAST");
        this.replicationManagerWebhook = this.asOptionalURL("MULTI_MASTER_REPLICATION_MANAGER_WEBHOOK");
        this.networkQuality = this.asBoolean("OPENVIDU_PRO_NETWORK_QUALITY");
        this.networkQualityInterval = this.asNonNegativeInteger("OPENVIDU_PRO_NETWORK_QUALITY_INTERVAL");
        this.speechToText = (SpeechToTextType)this.asEnumValue("OPENVIDU_PRO_SPEECH_TO_TEXT", SpeechToTextType.class);
        this.speechToTextPort = this.asOptionalIntegerBetweenRanges("OPENVIDU_PRO_SPEECH_TO_TEXT_PORT", new Range[]{Range.between(1, 65535)});
        if (!SpeechToTextType.disabled.equals(this.speechToText)) {
            this.speechToTextImage = this.asOptionalString("OPENVIDU_PRO_SPEECH_TO_TEXT_IMAGE");
            if (SpeechToTextType.azure.equals(this.speechToText)) {
                this.speechToTextAzureKey = this.asNonEmptyString("OPENVIDU_PRO_SPEECH_TO_TEXT_AZURE_KEY");
                this.speechToTextAzureRegion = this.asNonEmptyString("OPENVIDU_PRO_SPEECH_TO_TEXT_AZURE_REGION");
            } else if (SpeechToTextType.aws.equals(this.speechToText)) {
                this.awsAccessKey = this.asNonEmptyString("OPENVIDU_PRO_AWS_ACCESS_KEY");
                this.awsSecretKey = this.asNonEmptyString("OPENVIDU_PRO_AWS_SECRET_KEY");
                this.awsRegion = this.asNonEmptyString("OPENVIDU_PRO_AWS_REGION");
            } else if (SpeechToTextType.vosk.equals(this.speechToText)) {
                this.speechToTextVoskModelLoadStrategy = (SpeechToTextVoskModelLoadStrategy)this.asEnumValue("OPENVIDU_PRO_SPEECH_TO_TEXT_VOSK_MODEL_LOAD_STRATEGY", SpeechToTextVoskModelLoadStrategy.class);
            }
        }

        this.elasticsearchEnabled = this.asBoolean("OPENVIDU_PRO_ELASTICSEARCH");
        this.elasticsearchHost = this.asOptionalURL("OPENVIDU_PRO_ELASTICSEARCH_HOST");
        this.elasticsearchMaxDaysDelete = this.asNonNegativeInteger("OPENVIDU_PRO_ELASTICSEARCH_MAX_DAYS_DELETE");
        this.kibanaHost = this.asOptionalURL("OPENVIDU_PRO_KIBANA_HOST");
        this.openViduPrivateIp = this.asOptionalIPv4OrIPv6("OPENVIDU_PRO_PRIVATE_IP");
        this.elasticsearchUserName = this.asOptionalString("ELASTICSEARCH_USERNAME");
        this.elasticsearchPassword = this.asOptionalString("ELASTICSEARCH_PASSWORD");
        this.kmsImage = this.asOptionalString("KMS_IMAGE");
        this.mediasoupImage = this.asOptionalString("MEDIASOUP_IMAGE");
        this.filebeatImage = this.asOptionalString("FILEBEAT_IMAGE");
        this.metricbeatImage = this.asOptionalString("METRICBEAT_IMAGE");
        this.coturnImage = this.asOptionalString("COTURN_IMAGE");
        this.kmsConfig = this.loadKMSConfig();
        if (this.isMultiMasterEnvironment()) {
            if (OpenViduClusterEnvironment.on_premise.equals(this.clusterEnvironment)) {
                this.isMultimasterOnPremises = true;
            }

            this.clusterEnvironment = OpenViduClusterEnvironment.on_premise;
        }

        this.sendBrowserLogs = (BrowserLog)this.asEnumValue("OPENVIDU_BROWSER_LOGS", BrowserLog.class);
        this.dindImage = this.asOptionalString("DEV_CONTAINERS_DIND_IMAGE");
        this.dockerRegistries = this.loadDockerRegistries("OPENVIDU_PRO_DOCKER_REGISTRIES");
        this.defaultReconnectionTimeout();
        this.checkOpenViduEditionAndMediaServer();
        this.loadOpenViduProMasterNodeIp();
        this.loadAdditionalServices();
        this.checkMonoNode();
    }

    protected void postProcessConfigProps() {
        super.postProcessConfigProps();
        if (OpenViduEdition.pro.equals(this.openviduEdition)) {
            this.getConfigProps().remove("OPENVIDU_ENTERPRISE_MEDIA_SERVER");
        } else if (OpenViduEdition.enterprise.equals(this.openviduEdition)) {
            this.getConfigProps().put("OPENVIDU_ENTERPRISE_MEDIA_SERVER", this.mediaServer.name());
        }

        if (this.isMultiMasterEnvironment() && !this.isMultimasterOnPremises()) {
            this.getConfigProps().put("OPENVIDU_PRO_CLUSTER_ENVIRONMENT", OpenViduClusterEnvironment.aws.name());
        }

    }

    protected List<String> getNonUserProperties() {
        List<String> nonUserProperties = new ArrayList(super.getNonUserProperties());
        nonUserProperties.addAll(Arrays.asList("OPENVIDU_PRO_LICENSE_API", "OPENVIDU_PRO_ELASTICSEARCH", "OPENVIDU_PRO_ELASTICSEARCH_HOST", "OPENVIDU_PRO_KIBANA_HOST", "OPENVIDU_PRO_LICENSE_OFFLINE", "DEV_CONTAINERS_DIND_IMAGE"));
        return nonUserProperties;
    }

    protected List<String> getNonPrintablePropertiesIfEmpty() {
        List<String> nonPrintablePropertiesIfEmpty = new ArrayList(super.getNonPrintablePropertiesIfEmpty());
        nonPrintablePropertiesIfEmpty.addAll(Arrays.asList("FILEBEAT_IMAGE", "METRICBEAT_IMAGE", "KMS_IMAGE", "MEDIASOUP_IMAGE", "MULTI_MASTER_REPLICATION_MANAGER_WEBHOOK", "OPENVIDU_PRO_CLUSTER_ID", "OPENVIDU_PRO_LICENSE_HTTP_PROXY", "OPENVIDU_PRO_PRIVATE_IP"));
        return nonPrintablePropertiesIfEmpty;
    }

    public List<String> getNonModifiablePropertiesOnRestart() {
        List<String> nonModifiableProperties = new ArrayList(this.getNonUserProperties());
        nonModifiableProperties.addAll(Arrays.asList("DOMAIN_OR_PUBLIC_IP", "CERTIFICATE_TYPE", "HTTPS_PORT", "OPENVIDU_PRO_STATS_MONITORING_INTERVAL", "HTTP_PORT", "OPENVIDU_SECRET", "OPENVIDU_PRO_LICENSE", "OPENVIDU_PRO_CLUSTER", "OPENVIDU_PRO_CLUSTER_MODE", "OPENVIDU_PRO_CLUSTER_AUTOSCALING_INTERVAL", "OPENVIDU_PRO_CLUSTER_TEST", "ELASTICSEARCH_USERNAME", "ELASTICSEARCH_PASSWORD", "KMS_DOCKER_ENV_GST_DEBUG", "KMS_IMAGE", "MEDIASOUP_IMAGE", "FILEBEAT_IMAGE", "METRICBEAT_IMAGE", "MEDIA_NODES_PUBLIC_IPS", "OPENVIDU_EDITION", "OPENVIDU_ENTERPRISE_MEDIA_SERVER"));
        return nonModifiableProperties;
    }

    public void loadAdditionalServices() {
        AdditionalLogAggregator.Type logAggregatorType = (AdditionalLogAggregator.Type)this.asEnumValue("ADDITIONAL_LOG_AGGREGATOR", AdditionalLogAggregator.Type.class);
        AdditionalMonitoring.Type monitoringType = (AdditionalMonitoring.Type)this.asEnumValue("ADDITIONAL_MONITORING", AdditionalMonitoring.Type.class);
        String ddApiKey;
        String ddSite;
        if (logAggregatorType == Type.splunk) {
            ddApiKey = this.asNonEmptyString("SPLUNK_TOKEN_MEDIA_NODE_LOGS");
            ddSite = this.asNonEmptyString("SPLUNK_URL");
            String splInsecureSkipVerify = this.asNonEmptyString("SPLUNK_INSECURESKIPVERIFY");
            Map<String, String> props = new HashMap();
            props.put(SplunkProperties.TOKEN_MEDIA_NODE.name(), ddApiKey);
            props.put(SplunkProperties.SPLUNK_URL.name(), ddSite);
            props.put(SplunkProperties.INSECURE_SKIP_VERIFY.name(), splInsecureSkipVerify);
            this.additionalLogAggregator = new AdditionalLogAggregator(logAggregatorType, props);
        }

        if (monitoringType == io.openvidu.server.pro.config.AdditionalMonitoring.Type.datadog) {
            ddApiKey = this.asNonEmptyString("DD_API_KEY");
            ddSite = this.asNonEmptyString("DD_SITE");
            Map<String, String> props = new HashMap();
            props.put(DataDogProperties.API_KEY.name(), ddApiKey);
            props.put(DataDogProperties.DATADOG_SITE.name(), ddSite);
            this.additionalMonitoring = new AdditionalMonitoring(monitoringType, props);
        }

        if (this.additionalLogAggregator == null) {
            this.additionalLogAggregator = new AdditionalLogAggregator();
        }

        if (this.additionalMonitoring == null) {
            this.additionalMonitoring = new AdditionalMonitoring();
        }

    }

    public void loadOpenViduProMasterNodeId() {
        if (!this.isMultiMasterEnvironment()) {
            if (OpenViduClusterEnvironment.aws.equals(this.getClusterEnvironment())) {
                this.masterNodeId = "master_" + this.asOptionalString("AWS_INSTANCE_ID");
            } else {
                this.masterNodeId = "master_" + this.getClusterId();
                log.info("Master Node Id loaded: '{}'", this.masterNodeId);
            }
        } else {
            String id = this.asOptionalString("AWS_INSTANCE_ID");
            if (id == null || id.isEmpty()) {
                id = this.asOptionalString("MULTI_MASTER_NODE_ID");
            }

            this.masterNodeId = "master_" + id;
        }
    }

    public void loadOpenViduProMasterNodeIp() {
        try {
            URI uri = new URI(this.getFinalUrl());
            this.masterNodeIp = uri.getHost();
        } catch (URISyntaxException var2) {
            this.masterNodeIp = this.getOpenViduPublicUrl();
        }

    }

    public boolean isElasticSearchSecured() {
        return this.elasticsearchUserName != null && !this.elasticsearchUserName.isEmpty() && this.elasticsearchPassword != null && !this.elasticsearchPassword.isEmpty();
    }

    public static void waitUntilKmsReady(String kmsUri, int msIntervalWait, int secondsOfWait) throws TimeoutException {
        boolean ready = false;
        int attempts = 1;
        int attemptLimit = secondsOfWait * 1000 / msIntervalWait;
        log.info("Waiting for KMS instance {} to be ready for a maximum of {} seconds (maximum {} connection attempts with a wait interval of {} ms between them)", new Object[]{kmsUri, secondsOfWait, attemptLimit, msIntervalWait});

        while(!ready & attempts <= attemptLimit) {
            KurentoClient testClient = null;

            try {
                testClient = KurentoClient.create(kmsUri);
                log.info("KMS with URI {} is now ready after {} seconds at connection attempt {}", new Object[]{kmsUri, attempts * msIntervalWait / 1000, attempts});
                ready = true;
            } catch (KurentoException var14) {
                try {
                    Thread.sleep((long)msIntervalWait);
                } catch (InterruptedException var13) {
                    var13.printStackTrace();
                }

                log.warn("KMS with URI {} connection attempt {} failed. There are still {} connection attempts", new Object[]{kmsUri, attempts, attemptLimit - attempts});
                ++attempts;
            } finally {
                if (testClient != null) {
                    testClient.destroy();
                }

            }
        }

        if (attempts >= attemptLimit) {
            log.error("KMS with URI {} wasn't reachable after {} seconds", kmsUri, secondsOfWait);
            throw new TimeoutException();
        }
    }

    protected Regions asOptionalAwsRegion(String property) {
        String regionStr = this.asOptionalString(property);
        if (regionStr != null && !regionStr.isEmpty()) {
            try {
                Regions region = Regions.fromName(regionStr);
                return region;
            } catch (IllegalArgumentException var4) {
                this.addError(property, "Is not a valid AWS region");
                return null;
            }
        } else {
            return null;
        }
    }

    public MediaNodeKurentoConfig loadKMSConfig() {
        Map<String, String> kmsProperties = new HashMap();
        if (this.propertiesSource != null) {
            Iterator var2 = this.propertiesSource.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<String, ?> entry = (Map.Entry)var2.next();
                if (((String)entry.getKey()).startsWith("KMS_DOCKER_ENV_")) {
                    String propKey = ((String)entry.getKey()).replace("KMS_DOCKER_ENV_", "");
                    String propValue = (String)entry.getValue();
                    kmsProperties.put(propKey, propValue);
                }
            }
        }

        MutablePropertySources allEnvVars = ((AbstractEnvironment)this.env).getPropertySources();
        StreamSupport.stream(allEnvVars.spliterator(), false).filter((ps) -> {
            return ps instanceof EnumerablePropertySource;
        }).map((ps) -> {
            return ((EnumerablePropertySource)ps).getPropertyNames();
        }).flatMap(Arrays::stream).forEach((propName) -> {
            if (propName.startsWith("KMS_DOCKER_ENV_")) {
                String propValue = this.env.getProperty(propName);
                propName = propName.replace("KMS_DOCKER_ENV_", "");
                boolean isConfigured = kmsProperties.get(propName) != null && !((String)kmsProperties.get(propName)).isEmpty();
                if (!isConfigured) {
                    kmsProperties.put(propName, propValue);
                }
            }

        });
        return new MediaNodeKurentoConfig(kmsProperties);
    }

    public boolean isLicenseHttpProxyDefined() {
        return this.licenseHttpProxyHost != null && !this.licenseHttpProxyHost.isEmpty() && this.licenseHttpProxyPort != null && !this.licenseHttpProxyPort.isEmpty();
    }

    public String filterOpenViduSecret(String str) {
        String escapeSecret = Pattern.quote(this.getOpenViduSecret());
        return str.replaceAll(escapeSecret, "********");
    }

    private void loadLicenseHttpProxy(String propertyName) {
        String proxyEnvParameter = this.asOptionalString(propertyName);
        if (proxyEnvParameter != null && !proxyEnvParameter.isEmpty()) {
            String errorMsg = "The " + propertyName + " needs to separated a string separated by ':' like <host>:<port>. Example: proxy.host:1234";
            if (!proxyEnvParameter.contains(":")) {
                this.addError(propertyName, errorMsg);
                return;
            }

            String[] parts = proxyEnvParameter.split(":");
            if (parts.length != 2) {
                this.addError(propertyName, errorMsg);
                return;
            }

            this.licenseHttpProxyHost = parts[0];
            this.licenseHttpProxyPort = parts[1];
            this.checkHttpProxy(propertyName, this.licenseHttpProxyHost, this.licenseHttpProxyPort);
        }

    }

    private void checkHttpProxy(String propertyName, String proxyHost, String proxyPort) {
        DomainValidator domainValidator = DomainValidator.getInstance();
        InetAddressValidator ipValidator = InetAddressValidator.getInstance();
        if (!domainValidator.isValid(proxyHost) && !ipValidator.isValid(proxyHost)) {
            this.addError(propertyName, "Specified host is not valid");
        }

        try {
            int port = Integer.parseInt(proxyPort);
            if (port <= 0 || port > 65535) {
                this.addError(propertyName, "Port specified is out of valid ports range (0-65535)");
            }
        } catch (NumberFormatException var7) {
            this.addError(propertyName, "Can not convert port to integer: " + var7.getMessage());
        }

    }

    private void defaultReconnectionTimeout() {
        if (this.reconnectionTimeout == null) {
            if (!this.isMultiMasterEnvironment() && OpenViduClusterEnvironment.on_premise.equals(this.getClusterEnvironment()) && !this.isAutoscaling()) {
                this.reconnectionTimeout = -1;
            } else if (this.isMultiMasterEnvironment() && this.isMultimasterOnPremises()) {
                this.reconnectionTimeout = -1;
            } else {
                this.reconnectionTimeout = 3;
            }
        }

    }

    private void checkOpenViduEditionAndMediaServer() {
        if (OpenViduEdition.pro.equals(this.openviduEdition) && MediaServer.mediasoup.equals(this.mediaServer)) {
            this.addError("OPENVIDU_ENTERPRISE_MEDIA_SERVER", "You cannot use mediasoup with OPENVIDU_EDITION=pro");
        } else if (OpenViduEdition.enterprise.equals(this.openviduEdition) && MediaServer.kurento.equals(this.mediaServer) && !this.isMultiMasterEnvironment()) {
            this.addError("OPENVIDU_ENTERPRISE_MEDIA_SERVER", "Using kurento as media server with OpenVidu Enterprise Single Master is not allowed. Set OPENVIDU_EDITION=pro");
        }

    }

    private void loadMediaNodePublicIpAutodiscoveryMode() {
        String publicIpAutodiscoveryRaw = this.asOptionalString("OPENVIDU_PRO_MEDIA_NODE_PUBLIC_IP_AUTODISCOVER");
        if (publicIpAutodiscoveryRaw != null && !publicIpAutodiscoveryRaw.isBlank()) {
            if (publicIpAutodiscoveryRaw.equals("auto-ipv4")) {
                this.mediaNodePublicIpAutodiscoveryMode = PublicIpAutodiscovery.AUTO_IPV4;
            } else if (publicIpAutodiscoveryRaw.equals("auto-ipv6")) {
                this.mediaNodePublicIpAutodiscoveryMode = PublicIpAutodiscovery.AUTO_IPV6;
            } else {
                this.addError("OPENVIDU_PRO_MEDIA_NODE_PUBLIC_IP_AUTODISCOVER", "Value defined is not valid. Possible values: 'auto-ipv4', 'auto-ipv6'");
            }
        } else {
            this.addError("OPENVIDU_PRO_MEDIA_NODE_PUBLIC_IP_AUTODISCOVER", "Property can not be empty");
        }

    }

    private void loadCoturnConfig() {
        int mediaNodeCoturnPort = this.checkPort("OPENVIDU_PRO_COTURN_PORT_MEDIA_NODES");
        boolean isDeployedInMediaNodes = this.asBoolean("OPENVIDU_PRO_COTURN_IN_MEDIA_NODES");
        int mediaNodeMinPort = this.checkPort("OPENVIDU_PRO_COTURN_MIN_PORT_MEDIA_NODES");
        int mediaNodeMaxPort = this.checkPort("OPENVIDU_PRO_COTURN_MAX_PORT_MEDIA_NODES");
        String coturnSharedSecretKey = this.getCoturnSharedSecretKey();
        this.coturnConfig = new CoturnConfig(this.coturnIp, this.coturnPort, mediaNodeCoturnPort, isDeployedInMediaNodes, mediaNodeMinPort, mediaNodeMaxPort, coturnSharedSecretKey);
    }

    private void checkMonoNode() {
        try {
            MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this);
            provisioner.checkAndConfig("127.0.0.1");
            this.isMonoNode = true;
            log.info("Cluster distribution: Mono nodes");
        } catch (Exception var2) {
            if (this.isMultiMasterEnvironment()) {
                log.info("Cluster distribution: Multiple master nodes and media nodes");
            } else {
                log.info("Cluster distribution: Unique master node and multiple media nodes");
            }
        }

    }

    private List<DockerRegistryConfig> loadDockerRegistries(String property) {
        String rawDockerRegistries = this.asOptionalString(property);
        List<DockerRegistryConfig> dockerRegistryConfigList = new ArrayList();
        if (rawDockerRegistries != null && !rawDockerRegistries.isEmpty()) {
            List<String> arrayDockerRegistries = this.asJsonStringsArray(property);
            Iterator var5 = arrayDockerRegistries.iterator();

            while(var5.hasNext()) {
                String dockerRegistryString = (String)var5.next();

                try {
                    DockerRegistryConfig.Builder dockerRegistryBuilder = this.readDockerRegistry(property, dockerRegistryString);
                    dockerRegistryConfigList.add(dockerRegistryBuilder.build());
                } catch (Exception var8) {
                    this.addError(property, dockerRegistryString + " is not a valid docker registry: " + var8.getMessage());
                }
            }

            return dockerRegistryConfigList;
        } else {
            return dockerRegistryConfigList;
        }
    }

    private DockerRegistryConfig.Builder readDockerRegistry(String property, String dockerRegistryString) {
        String serveraddress = null;
        String username = null;
        String password = null;
        String[] dockerRegistryPropList = dockerRegistryString.split(",");
        String[] var7 = dockerRegistryPropList;
        int var8 = dockerRegistryPropList.length;

        for(int var9 = 0; var9 < var8; ++var9) {
            String dockerRegistryProp = var7[var9];
            if (dockerRegistryProp.startsWith("serveraddress=")) {
                serveraddress = StringUtils.substringAfter(dockerRegistryProp, "serveraddress=");
            } else if (dockerRegistryProp.startsWith("username=")) {
                username = StringUtils.substringAfter(dockerRegistryProp, "username=");
            } else if (dockerRegistryProp.startsWith("password=")) {
                password = StringUtils.substringAfter(dockerRegistryProp, "password=");
            } else {
                this.addError(property, "Wrong parameter: " + dockerRegistryProp);
            }
        }

        DockerRegistryConfig.Builder dockerRegistryBuilder = new DockerRegistryConfig.Builder();
        return dockerRegistryBuilder.serverAddress(serveraddress).username(username).password(password);
    }
}
