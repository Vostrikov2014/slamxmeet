//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller;

import com.google.gson.Gson;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.core.MediaServer;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeControllerDockerManager;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.DockerRegistryConfig;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.AdditionalLogAggregator.SplunkProperties;
import io.openvidu.server.config.AdditionalLogAggregator.Type;
import io.openvidu.server.config.AdditionalMonitoring.DataDogProperties;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeBeatConfig;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeCoturnConfig;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeSpeechToTextConfig;
import io.openvidu.server.infrastructure.mncontroller.model.LogConfig;
import io.openvidu.server.infrastructure.mncontroller.model.LogConfigBuilder;
import io.openvidu.server.infrastructure.mncontroller.model.RestartPolicy;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.SpeechToTextVoskModelLoadStrategy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaNodeProvisioner {
    private static final Logger log = LoggerFactory.getLogger(MediaNodeProvisioner.class);
    public static final String SYS_ENV_KMS_IMAGE = "KMS_IMAGE";
    public static final String SYS_ENV_MEDIASOUP_IMAGE = "MEDIASOUP_IMAGE";
    public static final String SYS_ENV_METRICBEAT_IMAGE = "METRICBEAT_IMAGE";
    public static final String SYS_ENV_FILEBEAT_IMAGE = "FILEBEAT_IMAGE";
    public static final String SYS_ENV_COTURN_IMAGE = "COTURN_IMAGE";
    public static final String SYS_ENV_SPEECH_TO_TEXT_IMAGE = "SPEECH_TO_TEXT_IMAGE";
    public static final String DATADOG_IMAGE = "datadog/agent:latest";
    private final String openViduIp;
    private OpenviduConfigPro openviduConfigPro;
    private final String METRICBEAT_CONFIG_FILE_ELASTICSEARCH = "/opt/openvidu/beats/metricbeat-elasticsearch.yml";
    private final String FILEBEAT_CONFIG_FILE = "/opt/openvidu/beats/filebeat.yml";
    private final String KMS_CONTAINER_NAME = "kms";
    private final String FILEBEAT_CONTAINER_NAME = "filebeat-elasticsearch";
    private final String METRICBEAT_CONTAINER_NAME = "metricbeat-elasticsearch";
    private final String COTURN_CONTAINER_NAME = "coturn";
    private final String SPEECH_TO_TEXT_CONTAINER_NAME = "speech-to-text-service";
    private final String DATADOG_CONTAINER_NAME = "datadog";

    public MediaNodeProvisioner(OpenviduConfigPro openviduConfigPro) {
        this.openviduConfigPro = openviduConfigPro;
        this.openViduIp = this.openviduConfigPro.getOpenViduPrivateIp();
    }

    public void checkAndConfig(String mediaNodeIp) throws OpenViduException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        mncDockerManager.initMediaNodeController();
    }

    public void checkAndConfig(String mediaNodeIp, OpenviduBuildInfo openviduBuildInfo, List<DockerRegistryConfig> dockerRegistryConfigList) throws OpenViduException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        mncDockerManager.initMediaNodeController(openviduBuildInfo, dockerRegistryConfigList);
    }

    public String launchKmsContainer(String mediaNodeIp, AdditionalLogAggregator additionalLogAggregator, String restartPolicyName, String configuredKmsImage, String configuredMediasoupImage, MediaNodeKurentoConfig kmsConfig, String recordingImage, boolean launchInDinD) throws IOException, TimeoutException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        List<String> envs = new ArrayList();
        Map<String, String> labels = new HashMap();
        Gson gson = new Gson();
        String containerName = "kms";
        String image;
        if (MediaServer.mediasoup.equals(this.openviduConfigPro.getMediaServer())) {
            if (configuredMediasoupImage != null && !configuredMediasoupImage.isEmpty()) {
                image = configuredMediasoupImage;
            } else {
                image = mncDockerManager.getEnvVariable("MEDIASOUP_IMAGE");
            }
        } else if (configuredKmsImage != null && !configuredKmsImage.isEmpty()) {
            image = configuredKmsImage;
        } else {
            image = mncDockerManager.getEnvVariable("KMS_IMAGE");
        }

        if (recordingImage != null) {
            if (!"NONE".equals(recordingImage) && !mncDockerManager.dockerImageExists(recordingImage)) {
                (new Thread(() -> {
                    try {
                        log.info("External composed recording is enabled. Image {} not available in Media Node {}. Downloading it", recordingImage, mediaNodeIp);
                        mncDockerManager.downloadDockerImage(recordingImage);
                    } catch (IOException var4) {
                        log.error("Error pulling {}: {}", recordingImage, var4.getMessage());
                    }

                })).start();
            }
        } else {
            log.warn("Trying to pull recording image but there's no tag");
        }

        if (!mncDockerManager.dockerImageExists(image)) {
            mncDockerManager.downloadDockerImage(image);
        }

        String networkMode = "host";
        String recordingsVolumePath = mncDockerManager.getRecordingsVolumePath();
        List<String> volumes = Arrays.asList(recordingsVolumePath + ":" + recordingsVolumePath, "/opt/openvidu/kms-crashes:/opt/openvidu/kms-crashes", "/opt/openvidu/kurento-logs:/opt/openvidu/kurento-logs");
        Iterator var19 = kmsConfig.getConfiguration().entrySet().iterator();

        while(var19.hasNext()) {
            Map.Entry<String, String> mapKmsConfig = (Map.Entry)var19.next();
            if (mapKmsConfig.getValue() != null && !((String)mapKmsConfig.getValue()).isEmpty()) {
                String var10001 = (String)mapKmsConfig.getKey();
                envs.add(var10001 + "=" + (String)mapKmsConfig.getValue());
            }
        }

        if (!kmsConfig.isDefined("KMS_MIN_PORT")) {
            kmsConfig.addEnvVariable("KMS_MIN_PORT", "40000");
            envs.add("KMS_MIN_PORT=" + kmsConfig.getEnvVariable("KMS_MIN_PORT"));
        }

        if (!kmsConfig.isDefined("KMS_MAX_PORT")) {
            kmsConfig.addEnvVariable("KMS_MAX_PORT", "65535");
            envs.add("KMS_MAX_PORT=" + kmsConfig.getEnvVariable("KMS_MAX_PORT"));
        }

        LogConfigBuilder logConfigBuilder = new LogConfigBuilder();
        if (!kmsConfig.isDefined("KURENTO_LOG_FILE_SIZE")) {
            logConfigBuilder.setMaxSize("100M");
            kmsConfig.addEnvVariable("KURENTO_LOG_FILE_SIZE", "100");
            envs.add("KURENTO_LOG_FILE_SIZE=" + kmsConfig.getEnvVariable("KURENTO_LOG_FILE_SIZE"));
        } else {
            logConfigBuilder.setMaxSize(kmsConfig.getEnvVariable("KURENTO_LOG_FILE_SIZE").concat("M"));
        }

        LogConfig logConfig = logConfigBuilder.build();
        if (!kmsConfig.isDefined("KURENTO_NUMBER_LOG_FILES")) {
            kmsConfig.addEnvVariable("KURENTO_NUMBER_LOG_FILES", "10");
            envs.add("KURENTO_NUMBER_LOG_FILES=" + kmsConfig.getEnvVariable("KURENTO_NUMBER_LOG_FILES"));
        }

        String dockerGatewayIp;
        String possibleRunningContainer;
        if (MediaServer.mediasoup.equals(this.openviduConfigPro.getMediaServer())) {
            kmsConfig.addEnvVariable("OPENVIDU_PRO_LICENSE", this.openviduConfigPro.getLicense());
            envs.add("OPENVIDU_PRO_LICENSE=" + this.openviduConfigPro.getLicense());
            kmsConfig.addEnvVariable("OPENVIDU_PRO_LICENSE_API", this.openviduConfigPro.getLicenseApiUrl());
            envs.add("OPENVIDU_PRO_LICENSE_API=" + this.openviduConfigPro.getLicenseApiUrl());
            kmsConfig.addEnvVariable("OPENVIDU_PRO_MASTER_NODE_IP", this.openviduConfigPro.getMasterNodeIp());
            envs.add("OPENVIDU_PRO_MASTER_NODE_IP=" + this.openviduConfigPro.getMasterNodeIp());
            if (this.openviduConfigPro != null && this.openviduConfigPro.isLicenseHttpProxyDefined()) {
                String var10000 = this.openviduConfigPro.getLicenseHttpProxyHost();
                possibleRunningContainer = var10000 + ":" + this.openviduConfigPro.getLicenseHttpProxyPort();
                kmsConfig.addEnvVariable("OPENVIDU_PRO_LICENSE_HTTP_PROXY", possibleRunningContainer);
                envs.add("OPENVIDU_PRO_LICENSE_HTTP_PROXY=" + possibleRunningContainer);
            }

            if (launchInDinD) {
                kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_0_ANNOUNCEDIP", mediaNodeIp);
                envs.add("WEBRTC_LISTENIPS_0_ANNOUNCEDIP=" + mediaNodeIp);
                kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_0_IP", mediaNodeIp);
                envs.add("WEBRTC_LISTENIPS_0_IP=" + mediaNodeIp);
            } else {
                int indexIP = 0;
                String publicIp = null;
                if (this.openviduConfigPro != null) {
                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP")) {
                        try {
                            publicIp = mncDockerManager.getPublicIp(this.openviduConfigPro.getMediaNodePublicIpAutodiscoveryMode());
                        } catch (Exception var27) {
                            log.warn("Public IP of the media node can not be auto discovered in media node. Clients may have problems connecting to media nodes");
                        }

                        if (publicIp != null) {
                            kmsConfig.setMediaNodePublicIp(publicIp);
                            kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP", publicIp);
                            envs.add("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP=" + publicIp);
                        } else {
                            kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP", mediaNodeIp);
                            envs.add("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP=" + mediaNodeIp);
                        }
                    }

                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_IP")) {
                        kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_IP", "0.0.0.0");
                        envs.add("WEBRTC_LISTENIPS_" + indexIP + "_IP=0.0.0.0");
                    }

                    ++indexIP;
                }

                if (!this.isLoopback(mediaNodeIp)) {
                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP")) {
                        kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP", mediaNodeIp);
                        envs.add("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP=" + mediaNodeIp);
                    }

                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_IP")) {
                        kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_IP", mediaNodeIp);
                        envs.add("WEBRTC_LISTENIPS_" + indexIP + "_IP=" + mediaNodeIp);
                    }

                    ++indexIP;
                }

                dockerGatewayIp = null;

                try {
                    dockerGatewayIp = mncDockerManager.getDockerGatewayIp();
                } catch (Exception var26) {
                    log.error("Error while getting docker gateway IP to configure as announced IP in mediasoup {}", var26.getMessage());
                }

                if (dockerGatewayIp != null) {
                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP")) {
                        kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP", dockerGatewayIp);
                        envs.add("WEBRTC_LISTENIPS_" + indexIP + "_ANNOUNCEDIP=" + dockerGatewayIp);
                    }

                    if (!kmsConfig.isDefined("WEBRTC_LISTENIPS_" + indexIP + "_IP")) {
                        kmsConfig.addEnvVariable("WEBRTC_LISTENIPS_" + indexIP + "_IP", dockerGatewayIp);
                        envs.add("WEBRTC_LISTENIPS_" + indexIP + "_IP=" + dockerGatewayIp);
                    }

                    ++indexIP;
                }
            }
        }

        kmsConfig.addEnvVariable("KURENTO_LOGS_PATH", "/opt/openvidu/kurento-logs");
        envs.add("KURENTO_LOGS_PATH=" + kmsConfig.getEnvVariable("KURENTO_LOGS_PATH"));
        kmsConfig.setKmsImage(image);
        kmsConfig.setMediaNodePrivateIp(mediaNodeIp);
        possibleRunningContainer = this.getRunningContainer(containerName, mncDockerManager);
        if (possibleRunningContainer != null) {
            log.info("Container {} is already possibly running at Media Node {}", containerName, mediaNodeIp);
            Map<String, String> kmsContainerLabels = mncDockerManager.getLabelsByName(containerName);
            dockerGatewayIp = (String)kmsContainerLabels.get("kms-configuration");
            if (dockerGatewayIp != null) {
                MediaNodeKurentoConfig oldKmsConfig = (MediaNodeKurentoConfig)gson.fromJson(dockerGatewayIp, MediaNodeKurentoConfig.class);
                if (kmsConfig.equals(oldKmsConfig)) {
                    log.info("The configuration properties of service {} have NOT changed at Media Node {}: {}", new Object[]{containerName, mediaNodeIp, oldKmsConfig.toString()});
                    log.info("It is NOT necessary to restart service {} of Media Node {}", containerName, mediaNodeIp);
                    return possibleRunningContainer;
                }

                log.info("The configuration properties of service {} have changed at Media Node {}. From {} to {}", new Object[]{containerName, mediaNodeIp, oldKmsConfig.toString(), kmsConfig.toString()});
                log.info("It is necessary to restart Media Node {} service {}", mediaNodeIp, containerName);
            }
        }

        log.info("Restarting service {} at Media Node {}", containerName, mediaNodeIp);
        this.removeContainerIfExists(containerName, mncDockerManager);
        labels.put("node", "Media Node");
        labels.put("media-node-private-ip", mediaNodeIp);
        labels.put("kms-configuration", gson.toJson(kmsConfig));
        if (additionalLogAggregator.getType() == Type.splunk) {
            Map<String, String> config = new HashMap();
            String splunkToken = additionalLogAggregator.getProperty(SplunkProperties.TOKEN_MEDIA_NODE.name());
            String splunkUrl = additionalLogAggregator.getProperty(SplunkProperties.SPLUNK_URL.name());
            String splunkInSecVer = additionalLogAggregator.getProperty(SplunkProperties.INSECURE_SKIP_VERIFY.name());
            config.put("splunk-token", splunkToken);
            config.put("splunk-url", splunkUrl);
            config.put("splunk-insecureskipverify", splunkInSecVer);
            config.put("labels", "node,media-node-private-ip");
            logConfig = new LogConfig(Type.splunk.name(), config);
        }

        dockerGatewayIp = null;

        try {
            dockerGatewayIp = mncDockerManager.runContainerAux(image, containerName, (String)null, volumes, "host", envs, (List)null, labels, logConfig, false, new RestartPolicy(restartPolicyName), false, (List)null);
        } catch (HttpResponseException var28) {
            if (var28.getStatusCode() != 409) {
                throw var28;
            }

            dockerGatewayIp = var28.getMessage();
            log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{containerName, mediaNodeIp, dockerGatewayIp});
        }

        OpenviduConfigPro.waitUntilKmsReady("ws://" + mediaNodeIp + ":8888/kurento", 333, 120);
        return dockerGatewayIp;
    }

    public String launchCoturn(String mediaNodeIp, String clusterId, String restartPolicyName, String configuredCoturnImage, int port, int minPort, int maxPort, String sharedSecretKey, String nodeId) throws IOException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        MediaNodeCoturnConfig coturnConfig = new MediaNodeCoturnConfig();
        Gson gson = new Gson();
        String containerName = "coturn";
        String image;
        if (configuredCoturnImage == null) {
            image = mncDockerManager.getEnvVariable("COTURN_IMAGE");
        } else {
            image = configuredCoturnImage;
        }

        String networkMode = "bridge";
        List<String> envs = List.of("COTURN_SHARED_SECRET_KEY=" + sharedSecretKey);
        List<String> containerCommand = new ArrayList(Arrays.asList("--log-file=stdout", "--fingerprint", "--realm=openvidu", "--verbose", "--use-auth-secret", "--listening-port=" + port, "--min-port=" + minPort, "--max-port=" + maxPort, "--static-auth-secret=" + sharedSecretKey));
        List<Pair<String, String>> portBindingList = List.of(Pair.of(Integer.toString(port), "" + port + "/tcp"), Pair.of(Integer.toString(port), "" + port + "/udp"));
        coturnConfig.setCoturnImage(image);
        coturnConfig.setCoturnPort(port);
        coturnConfig.setMinCoturnPort(minPort);
        coturnConfig.setMaxCoturnPort(maxPort);
        coturnConfig.setCoturnSharedSecretKey(sharedSecretKey);
        String possibleRunningContainer = this.getRunningContainer("coturn", mncDockerManager);
        MediaNodeCoturnConfig oldCoturnConfig;
        if (possibleRunningContainer != null) {
            log.info("Container {} is already possibly running at Media Node {}", "coturn", mediaNodeIp);
            Map<String, String> coturnContainerLabels = mncDockerManager.getLabelsByName("coturn");
            String oldCoturnJsonConfig = (String)coturnContainerLabels.get("coturn-configuration");
            if (oldCoturnJsonConfig != null) {
                oldCoturnConfig = (MediaNodeCoturnConfig)gson.fromJson(oldCoturnJsonConfig, MediaNodeCoturnConfig.class);
                if (coturnConfig.equals(oldCoturnConfig)) {
                    log.info("The configuration properties of service {} have NOT changed at Media Node {}: {}", new Object[]{"coturn", mediaNodeIp, oldCoturnConfig.toString()});
                    log.info("It is NOT necessary to restart service {} of Media Node {}", "coturn", mediaNodeIp);
                    return possibleRunningContainer;
                }

                log.info("The configuration properties of service {} have changed at Media Node {}. From {} to {}", new Object[]{"coturn", mediaNodeIp, oldCoturnConfig.toString(), coturnConfig.toString()});
                log.info("It is necessary to restart Media Node {} service {}", mediaNodeIp, "coturn");
            }
        }

        log.info("Restarting service {} at Media Node {}", "coturn", mediaNodeIp);
        this.removeContainerIfExists("coturn", mncDockerManager);
        Map<String, String> labels = new HashMap();
        labels.put("node", "Media Node");
        labels.put("cluster-id", clusterId);
        labels.put("node-id", nodeId);
        labels.put("coturn-configuration", gson.toJson(coturnConfig));
        oldCoturnConfig = null;

        String newContainerId;
        try {
            LogConfig logConfig = (new LogConfigBuilder()).build();
            newContainerId = mncDockerManager.runContainerAux(image, "coturn", (String)null, (List)null, "bridge", envs, containerCommand, labels, logConfig, false, new RestartPolicy(restartPolicyName), false, portBindingList);
        } catch (HttpResponseException var24) {
            if (var24.getStatusCode() != 409) {
                throw var24;
            }

            newContainerId = var24.getMessage();
            log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{"coturn", mediaNodeIp, newContainerId});
        }

        return newContainerId;
    }

    public void removeCoturn(String mediaNodeIp) throws IOException {
        this.removeContainer(mediaNodeIp, "coturn");
    }

    public void removeBeats(String ip) throws IOException {
        this.removeContainer(ip, "filebeat-elasticsearch");
        this.removeContainer(ip, "metricbeat-elasticsearch");
    }

    public String launchSpeechToTextService(String mediaNodeIp, String clusterId, String restartPolicyName, String speechToTextImage, SpeechToTextType speechToTextType, SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy, int port, String azureKey, String azureRegion, String awsAccessKeyId, String awsSecretKey, String awsRegion, String nodeId) throws IOException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        MediaNodeSpeechToTextConfig configInfo = new MediaNodeSpeechToTextConfig();
        Gson gson = new Gson();
        String containerName = "speech-to-text-service";
        String image;
        if (speechToTextImage != null && !speechToTextImage.isEmpty()) {
            image = speechToTextImage;
        } else {
            image = mncDockerManager.getEnvVariable("SPEECH_TO_TEXT_IMAGE");
        }

        if (!mncDockerManager.dockerImageExists(image)) {
            mncDockerManager.downloadDockerImage(image);
        }

        String networkMode = "bridge";
        List<String> envs = new ArrayList(List.of("STT_ENGINE=" + speechToTextType.toString(), "APP_GRPCPORT=" + port, "NODE_TLS_REJECT_UNAUTHORIZED=0"));
        if (SpeechToTextType.azure.equals(speechToTextType)) {
            envs.add("STT_AZURE_SUBSCRIPTIONKEY=" + azureKey);
            envs.add("STT_AZURE_SERVICEREGION=" + azureRegion);
        } else if (SpeechToTextType.aws.equals(speechToTextType)) {
            envs.add("STT_AWS_ACCESSKEY=" + awsAccessKeyId);
            envs.add("STT_AWS_SECRETKEY=" + awsSecretKey);
            envs.add("STT_AWS_REGION=" + awsRegion);
        } else if (SpeechToTextType.vosk.equals(speechToTextType)) {
            envs.add("STT_VOSK_MODELLOADSTRATEGY=" + speechToTextVoskModelLoadStrategy.toString());
        }

        List<Pair<String, String>> portBindingList = List.of(Pair.of(Integer.toString(port), Integer.toString(port)));
        configInfo.setSpeechToTextImage(image);
        configInfo.setEngine(speechToTextType.toString());
        configInfo.setPort(port);
        configInfo.setAzureRegion(azureKey);
        configInfo.setAzureRegion(azureRegion);
        configInfo.setAwsKey(awsAccessKeyId);
        configInfo.setAwsSecret(awsSecretKey);
        configInfo.setAwsRegion(awsRegion);
        configInfo.setSpeechToTextVoskModelLoadStrategy(speechToTextVoskModelLoadStrategy);
        String possibleRunningContainer = this.getRunningContainer("speech-to-text-service", mncDockerManager);
        MediaNodeSpeechToTextConfig oldConfig;
        if (possibleRunningContainer != null) {
            log.info("Container {} is already possibly running at Media Node {}", "speech-to-text-service", mediaNodeIp);
            Map<String, String> containerLabels = mncDockerManager.getLabelsByName("speech-to-text-service");
            String oldJsonConfig = (String)containerLabels.get("speech-to-text-configuration");
            if (oldJsonConfig != null) {
                oldConfig = (MediaNodeSpeechToTextConfig)gson.fromJson(oldJsonConfig, MediaNodeSpeechToTextConfig.class);
                if (configInfo.equals(oldConfig)) {
                    log.info("The configuration properties of service {} have NOT changed at Media Node {}: {}", new Object[]{"speech-to-text-service", mediaNodeIp, oldConfig.toString()});
                    log.info("It is NOT necessary to restart service {} of Media Node {}", "speech-to-text-service", mediaNodeIp);
                    return possibleRunningContainer;
                }

                log.info("The configuration properties of service {} have changed at Media Node {}. From {} to {}", new Object[]{"speech-to-text-service", mediaNodeIp, oldConfig.toString(), configInfo.toString()});
                log.info("It is necessary to restart Media Node {} service {}", mediaNodeIp, "speech-to-text-service");
            }
        }

        log.info("Restarting service {} at Media Node {}", "speech-to-text-service", mediaNodeIp);
        this.removeContainerIfExists("speech-to-text-service", mncDockerManager);
        Map<String, String> labels = new HashMap();
        labels.put("node", "Media Node");
        labels.put("cluster-id", clusterId);
        labels.put("node-id", nodeId);
        labels.put("speech-to-text-configuration", gson.toJson(configInfo));
        oldConfig = null;

        String newContainerId;
        try {
            LogConfig logConfig = (new LogConfigBuilder()).build();
            newContainerId = mncDockerManager.runContainerAux(image, "speech-to-text-service", (String)null, (List)null, "bridge", envs, (List)null, labels, logConfig, false, new RestartPolicy(restartPolicyName), false, portBindingList);
        } catch (HttpResponseException var27) {
            if (var27.getStatusCode() != 409) {
                throw var27;
            }

            newContainerId = var27.getMessage();
            log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{"speech-to-text-service", mediaNodeIp, newContainerId});
        }

        return newContainerId;
    }

    public void removeSpeechToText(String mediaNodeIp) throws IOException {
        this.removeContainer(mediaNodeIp, "speech-to-text-service");
    }

    public String launchMetricBeatContainer(String mediaNodeIp, String clusterId, int loadInterval, boolean launchInDinD, String restartPolicyName, String esHost, String esUserName, String esPassword, String configuredMetricBeatImage, String nodeId) throws IOException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        MediaNodeBeatConfig beatConfig = new MediaNodeBeatConfig();
        Gson gson = new Gson();
        String containerName = "metricbeat-elasticsearch";
        String image;
        if (configuredMetricBeatImage == null) {
            image = mncDockerManager.getEnvVariable("METRICBEAT_IMAGE");
        } else {
            image = configuredMetricBeatImage;
        }

        String networkMode = "host";
        File configFile = new File("/opt/openvidu/beats/metricbeat-elasticsearch.yml");
        String configDir = configFile.getAbsolutePath();
        List<String> volumes = Arrays.asList(configDir + ":/usr/share/metricbeat/metricbeat.yml:ro", (launchInDinD ? "/hostfs" : "") + "/proc:/hostfs/proc:ro", (launchInDinD ? "/hostfs" : "") + "/sys/fs/cgroup:/hostfs/sys/fs/cgroup:ro", (launchInDinD ? "/hostfs" : "") + "/:/hostfs:ro", "/var/run/docker.sock:/var/run/docker.sock:rw");
        List<String> envs = new ArrayList();
        envs.add("OPENVIDU_PRO_STATS_MONITORING_INTERVAL=" + loadInterval);
        envs.add("MEDIA_NODE_IP=" + mediaNodeIp);
        envs.add("CLUSTER_ID=" + clusterId);
        envs.add("NODE_ID=" + nodeId);
        List<String> containerCommand = new ArrayList(Arrays.asList("metricbeat", "-e", "-strict.perms=false", "-e", "-system.hostfs=/hostfs"));
        String esHostForMediaNode;
        String possibleRunningContainer;
        if (esHost != null && !esHost.isEmpty()) {
            esHostForMediaNode = this.getESHostForMediaNode(esHost, mediaNodeIp);
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.hosts=['" + esHostForMediaNode + "']"));
        } else {
            possibleRunningContainer = mncDockerManager.getOpenViduIpForMediaNode();
            esHostForMediaNode = this.openViduIp != null && !this.openViduIp.isEmpty() ? this.openViduIp : possibleRunningContainer;
            envs.add("OPENVIDU_SERVER_PRO_IP=" + esHostForMediaNode);
        }

        beatConfig.setOutputHost(esHostForMediaNode);
        if (esUserName != null && !esUserName.isEmpty()) {
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.username='" + esUserName + "'"));
            beatConfig.setEsUserName(esUserName);
        }

        if (esPassword != null && !esPassword.isEmpty()) {
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.password='" + esPassword + "'"));
            beatConfig.setEsPassword(esPassword);
        }

        beatConfig.setBeatImage(image);
        beatConfig.setClusterId(clusterId);
        beatConfig.setNodeId(nodeId);
        beatConfig.setMediaNodePrivateIp(mediaNodeIp);
        beatConfig.setLoadInterval(Integer.toString(loadInterval));
        beatConfig.setVolumes(volumes);
        beatConfig.setEnvironmentVariables(envs);
        possibleRunningContainer = this.getRunningContainer("metricbeat-elasticsearch", mncDockerManager);
        String oldKmsJsonConfig;
        if (possibleRunningContainer != null) {
            log.info("Container {} is already possibly running at Media Node {}", "metricbeat-elasticsearch", mediaNodeIp);
            Map<String, String> metricbeatContainerLabels = mncDockerManager.getLabelsByName("metricbeat-elasticsearch");
            oldKmsJsonConfig = (String)metricbeatContainerLabels.get("metricbeat-configuration");
            if (oldKmsJsonConfig != null) {
                MediaNodeBeatConfig oldBeatConfig = (MediaNodeBeatConfig)gson.fromJson(oldKmsJsonConfig, MediaNodeBeatConfig.class);
                if (beatConfig.equals(oldBeatConfig)) {
                    log.info("The configuration properties of service {} have NOT changed at Media Node {}: {}", new Object[]{"metricbeat-elasticsearch", mediaNodeIp, oldBeatConfig.toString()});
                    log.info("It is NOT necessary to restart service {} of Media Node {}", "metricbeat-elasticsearch", mediaNodeIp);
                    return possibleRunningContainer;
                }

                log.info("The configuration properties of service {} have changed at Media Node {}. From {} to {}", new Object[]{"metricbeat-elasticsearch", mediaNodeIp, oldBeatConfig.toString(), beatConfig.toString()});
                log.info("It is necessary to restart Media Node {} service {}", mediaNodeIp, "metricbeat-elasticsearch");
            }
        }

        log.info("Restarting service {} at Media Node {}", "metricbeat-elasticsearch", mediaNodeIp);
        this.removeContainerIfExists("metricbeat-elasticsearch", mncDockerManager);
        oldKmsJsonConfig = "root";
        Map<String, String> labels = new HashMap();
        labels.put("node", "Media Node");
        labels.put("cluster-id", clusterId);
        labels.put("node-id", nodeId);
        labels.put("es-host", esHostForMediaNode);
        labels.put("es-username", esUserName);
        labels.put("es-password", esPassword);
        labels.put("metricbeat-configuration", gson.toJson(beatConfig));
        String newContainerId = null;

        try {
            LogConfig logConfig = (new LogConfigBuilder()).build();
            newContainerId = mncDockerManager.runContainerAux(image, "metricbeat-elasticsearch", "root", volumes, "host", envs, containerCommand, labels, logConfig, false, new RestartPolicy(restartPolicyName), false, (List)null);
        } catch (HttpResponseException var29) {
            if (var29.getStatusCode() != 409) {
                throw var29;
            }

            newContainerId = var29.getMessage();
            log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{"metricbeat-elasticsearch", mediaNodeIp, newContainerId});
        }

        return newContainerId;
    }

    public String launchFileBeatContainer(String mediaNodeIp, String clusterId, String restartPolicyName, String esHost, String esUserName, String esPassword, String configuredFileBeatImage, String nodeId) throws IOException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        MediaNodeBeatConfig beatConfig = new MediaNodeBeatConfig();
        Gson gson = new Gson();
        String containerName = "filebeat-elasticsearch";
        String image;
        if (configuredFileBeatImage == null) {
            image = mncDockerManager.getEnvVariable("FILEBEAT_IMAGE");
        } else {
            image = configuredFileBeatImage;
        }

        String networkMode = "bridge";
        File configFile = new File("/opt/openvidu/beats/filebeat.yml");
        String configDir = configFile.getAbsolutePath();
        List<String> volumes = Arrays.asList(configDir + ":/usr/share/filebeat/filebeat.yml", "/var/lib/docker:/var/lib/docker", "/var/run/docker.sock:/var/run/docker.sock", "/opt/openvidu/kurento-logs:/opt/openvidu/kurento-logs");
        List<String> envs = new ArrayList();
        envs.add("MEDIA_NODE_IP=" + mediaNodeIp);
        envs.add("CLUSTER_ID=" + clusterId);
        envs.add("NODE_ID=" + nodeId);
        List<String> containerCommand = new ArrayList(Arrays.asList("filebeat", "-e", "-strict.perms=false"));
        String esHostForMediaNode;
        String possibleRunningContainer;
        if (esHost != null && !esHost.isEmpty()) {
            esHostForMediaNode = this.getESHostForMediaNode(esHost, mediaNodeIp);
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.hosts=['" + esHostForMediaNode + "']"));
        } else {
            possibleRunningContainer = mncDockerManager.getOpenViduIpForMediaNode();
            esHostForMediaNode = this.openViduIp != null && !this.openViduIp.isEmpty() ? this.openViduIp : possibleRunningContainer;
            envs.add("OPENVIDU_SERVER_PRO_IP=" + esHostForMediaNode);
        }

        beatConfig.setOutputHost(esHostForMediaNode);
        if (esUserName != null && !esUserName.isEmpty()) {
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.username='" + esUserName + "'"));
            beatConfig.setEsUserName(esUserName);
        }

        if (esPassword != null && !esPassword.isEmpty()) {
            containerCommand.addAll(Arrays.asList("-E", "output.elasticsearch.password='" + esPassword + "'"));
            beatConfig.setEsPassword(esPassword);
        }

        beatConfig.setBeatImage(image);
        beatConfig.setClusterId(clusterId);
        beatConfig.setNodeId(nodeId);
        beatConfig.setMediaNodePrivateIp(mediaNodeIp);
        beatConfig.setVolumes(volumes);
        beatConfig.setEnvironmentVariables(envs);
        possibleRunningContainer = this.getRunningContainer("filebeat-elasticsearch", mncDockerManager);
        String oldKmsJsonConfig;
        if (possibleRunningContainer != null) {
            log.info("Container {} is already possibly running at Media Node {}", "filebeat-elasticsearch", mediaNodeIp);
            Map<String, String> filebeatContainerLabels = mncDockerManager.getLabelsByName("filebeat-elasticsearch");
            oldKmsJsonConfig = (String)filebeatContainerLabels.get("filebeat-configuration");
            if (oldKmsJsonConfig != null) {
                MediaNodeBeatConfig oldBeatConfig = (MediaNodeBeatConfig)gson.fromJson(oldKmsJsonConfig, MediaNodeBeatConfig.class);
                if (beatConfig.equals(oldBeatConfig)) {
                    log.info("The configuration properties of service {} have NOT changed at Media Node {}: {}", new Object[]{"filebeat-elasticsearch", mediaNodeIp, oldBeatConfig.toString()});
                    log.info("It is NOT necessary to restart service {} of Media Node {}", "filebeat-elasticsearch", mediaNodeIp);
                    return possibleRunningContainer;
                }

                log.info("The configuration properties of service {} have changed at Media Node {}. From {} to {}", new Object[]{"filebeat-elasticsearch", mediaNodeIp, oldBeatConfig.toString(), beatConfig.toString()});
                log.info("It is necessary to restart Media Node {} service {}", mediaNodeIp, "filebeat-elasticsearch");
            }
        }

        log.info("Restarting service {} at Media Node {}", "filebeat-elasticsearch", mediaNodeIp);
        this.removeContainerIfExists("filebeat-elasticsearch", mncDockerManager);
        oldKmsJsonConfig = "root";
        Map<String, String> labels = new HashMap();
        labels.put("node", "Media Node");
        labels.put("cluster-id", clusterId);
        labels.put("node-id", nodeId);
        labels.put("es-host", esHostForMediaNode);
        labels.put("es-username", esUserName);
        labels.put("es-password", esPassword);
        labels.put("filebeat-configuration", gson.toJson(beatConfig));
        String newContainerId = null;

        try {
            LogConfig logConfig = (new LogConfigBuilder()).build();
            newContainerId = mncDockerManager.runContainerAux(image, "filebeat-elasticsearch", "root", volumes, "bridge", envs, containerCommand, labels, logConfig, false, new RestartPolicy(restartPolicyName), false, (List)null);
        } catch (HttpResponseException var27) {
            if (var27.getStatusCode() != 409) {
                throw var27;
            }

            newContainerId = var27.getMessage();
            log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{"filebeat-elasticsearch", mediaNodeIp, newContainerId});
        }

        return newContainerId;
    }

    public String launchDatadogContainer(String mediaNodeIp, Map<String, String> datadogProperties, boolean launchInDinD, String restartPolicyName) throws IOException {
        String datadogApiKey = (String)datadogProperties.get(DataDogProperties.API_KEY.name());
        String datadogSite = (String)datadogProperties.get(DataDogProperties.DATADOG_SITE.name());
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        String containerName = "datadog";
        String possibleRunningContainer = this.getRunningContainer("datadog", mncDockerManager);
        if (possibleRunningContainer != null) {
            return possibleRunningContainer;
        } else {
            this.removeContainerIfExists("datadog", mncDockerManager);
            mncDockerManager.checkImages(Collections.singletonList("datadog/agent:latest"));
            String networkMode = "bridge";
            List<String> volumes = Arrays.asList((launchInDinD ? "/hostfs" : "") + "/proc/:/host/proc/:ro", (launchInDinD ? "/hostfs" : "") + "/sys/fs/cgroup/:/host/sys/fs/cgroup:ro", "/var/run/docker.sock:/var/run/docker.sock:ro");
            List<String> envs = Arrays.asList("DD_API_KEY=" + datadogApiKey, "DD_SITE=" + datadogSite);
            Map<String, String> labels = new HashMap();
            labels.put("node", "Media Node");
            String newContainerId = null;

            try {
                newContainerId = mncDockerManager.runContainerAux("datadog/agent:latest", "datadog", (String)null, volumes, "bridge", envs, (List)null, labels, (LogConfig)null, false, new RestartPolicy(restartPolicyName), false, (List)null);
            } catch (HttpResponseException var16) {
                if (var16.getStatusCode() != 409) {
                    throw var16;
                }

                newContainerId = var16.getMessage();
                log.warn("Container {} was already running at Media Node {} with id {}", new Object[]{"datadog", mediaNodeIp, newContainerId});
            }

            return newContainerId;
        }
    }

    public MediaNodeControllerDockerManager getMediaNodeControllerDockerManager(String mediaNodeIp) {
        return new MediaNodeControllerDockerManager(mediaNodeIp, this.openviduConfigPro.getOpenViduSecret(), this.calculateDockerClientTimeout());
    }

    private void removeContainerIfExists(String containerName, MediaNodeControllerDockerManager mncDockerManager) throws IOException {
        String possibleMediaNodeRunning = mncDockerManager.getContainerIdByName(containerName);
        if (possibleMediaNodeRunning != null && !possibleMediaNodeRunning.isEmpty()) {
            mncDockerManager.removeContainer(containerName, true);
        }

    }

    private String getRunningContainer(String containerName, MediaNodeControllerDockerManager mncDockerManager) throws IOException {
        boolean isRunningContainer = mncDockerManager.isContainerRunning(containerName);
        return isRunningContainer ? mncDockerManager.getContainerIdByName(containerName) : null;
    }

    private String getESHostForMediaNode(String configuredESHost, String mediaNodeIp) throws IOException {
        String esHost = configuredESHost;
        if (configuredESHost.matches("^(http|https)://(localhost|127\\.0\\.0\\.1).*")) {
            MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
            String esIp = mncDockerManager.getOpenViduIpForMediaNode();
            esHost = configuredESHost.replaceAll("localhost|127\\.0\\.0\\.1", esIp);
        }

        Pattern p = Pattern.compile("^(https?://)?([^:/]+)(:([0-9]+))?(/.*)?$");
        Matcher m = p.matcher(esHost);
        if (m.matches()) {
            String protocol = m.group(1);
            String host = m.group(2);
            String port = m.group(4);
            String path = m.group(5);
            if (port == null) {
                if (protocol.equals("https://")) {
                    port = "443";
                } else {
                    port = "80";
                }
            }

            esHost = protocol + host + ":" + port + (path != null ? path : "");
        }

        return esHost;
    }

    private void removeContainer(String mediaNodeIp, String containerName) throws IOException {
        MediaNodeControllerDockerManager mncDockerManager = this.getMediaNodeControllerDockerManager(mediaNodeIp);
        log.info("Removing service {} at Media Node {}", containerName, mediaNodeIp);
        this.removeContainerIfExists(containerName, mncDockerManager);
    }

    private boolean isLoopback(String host) {
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    public int calculateDockerClientTimeout() {
        if (this.openviduConfigPro == null) {
            return 60;
        } else {
            return OpenViduClusterEnvironment.docker.equals(this.openviduConfigPro.getClusterEnvironment()) ? 60 : 30;
        }
    }
}
