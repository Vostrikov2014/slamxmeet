//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.onpremise;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.AdditionalMonitoring;
import io.openvidu.server.config.AdditionalMonitoring.Type;
import io.openvidu.server.infrastructure.InfrastructureInstanceData;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.infrastructure.InstanceType;
import io.openvidu.server.infrastructure.LaunchInstanceOptions;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.utils.CommandExecutor;
import io.openvidu.server.utils.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnpremiseInfrastructureManager extends InfrastructureManager {
    private static final Logger log = LoggerFactory.getLogger(OnpremiseInfrastructureManager.class);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-u_HH:mm:ss,SSS");
    private JsonUtils jsonUtils = new JsonUtils();
    private String esHost;
    private String esUserName;
    private String esPassword;

    public OnpremiseInfrastructureManager(InfrastructureInstanceData infrastructureInstanceData) {
        super(infrastructureInstanceData);
    }

    public Instance launchInstance(LaunchInstanceOptions launchInstanceOptions) throws Exception {
        if (this.newMediaNodesNotAllowed()) {
            throw new IllegalStateException("Can not launch more than One Media Nodes with your current deployment");
        } else {
            InstanceType type = launchInstanceOptions.getInstanceType();
            log.info("Launching new '{}' instance of type '{}'", this.openviduConfigPro.getClusterEnvironment().name(), type.name());
            Instance instance = null;
            String scriptName = "openvidu_launch_kms.sh";
            switch (type) {
                case mediaServer:
                    scriptName = "openvidu_launch_kms.sh";
                default:
                    String filesName = UUID.randomUUID().toString();
                    String command = "";
                    if (launchInstanceOptions.getInfrastructureInstanceType() != null) {
                        command = command + "export CUSTOM_INSTANCE_TYPE=\"" + launchInstanceOptions.getInfrastructureInstanceType() + "\"; ";
                    }

                    if (launchInstanceOptions.getVolumeSize() != null) {
                        command = command + "export CUSTOM_VOLUME_SIZE=\"" + launchInstanceOptions.getVolumeSize() + "\"; ";
                    }

                    command = command + this.openviduConfigPro.getClusterPath() + scriptName;
                    String var10000 = this.openviduConfigPro.getClusterPath();
                    String absoluteOutputName = var10000 + "output/" + scriptName + "/";
                    this.fileManager.createFolderIfNotExists(absoluteOutputName);
                    File standardOutput = new File(absoluteOutputName + filesName + ".json");
                    File errorOutput = new File(absoluteOutputName + filesName + "_ERROR.txt");
                    log.info("Running command \"{}\"", command);
                    CommandExecutor.execCommandRedirectStandardOutputAndError(300000L, standardOutput, errorOutput, new String[]{"/bin/sh", "-c", command});

                    JsonObject instanceJson;
                    String errorContent;
                    try {
                        instanceJson = this.jsonUtils.fromFileToJsonObject(standardOutput.getAbsolutePath());
                    } catch (JsonSyntaxException | IllegalStateException | JsonIOException var16) {
                        log.error("Script \"{}\" returned an invalid JSON string through the standard output. Looking for error output...", command);

                        try {
                            errorContent = FileUtils.readFileToString(errorOutput, StandardCharsets.UTF_8);
                            log.error("Error output found. Content:");
                            log.error(errorContent);
                            throw new Exception(errorContent);
                        } catch (IOException var14) {
                            var10000 = errorOutput.getAbsolutePath();
                            String errorContent = "Error reading from file \"" + var10000 + "\": " + var14.getMessage();
                            log.error(errorContent);
                            throw new Exception(errorContent);
                        }
                    } catch (IOException var17) {
                        errorContent = "Error reading file \"" + standardOutput.getAbsolutePath() + "\" after running script \"" + command + "\". Error: " + var17.getMessage();
                        log.error(errorContent);
                        throw new Exception(errorContent);
                    }

                    try {
                        standardOutput.delete();
                        errorOutput.delete();
                    } catch (Exception var15) {
                        log.error("Error deleting output script files: ", var15.getMessage());
                    }

                    try {
                        String environmentId = instanceJson.get("id").getAsString();
                        errorContent = instanceJson.get("ip").getAsString();
                        instance = new Instance(InstanceType.mediaServer, "media_" + environmentId);
                        instance.setEnvironmentId(environmentId);
                        instance.setIp(errorContent);
                        this.addInstance(instance);
                        this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.launching, (InstanceStatus)null);
                        log.info("New instance launched: {}", instance.toString());
                        return instance;
                    } catch (Exception var18) {
                        errorContent = "Error initializing Media Node instance: " + var18.getMessage();
                        log.error(errorContent);
                        var18.printStackTrace();
                        if (instance != null) {
                            this.removeInstance(instance.getId());
                            this.dropInstance(instance);
                            this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.failed, instance.getStatus());
                        }

                        throw new Exception(errorContent);
                    }
            }
        }
    }

    public void provisionInstance(Instance instance) throws Exception {
        InstanceType type = instance.getType();
        if (InstanceType.mediaServer.equals(type)) {
            try {
                this.waitUntilMediaNodeControllerIsReady(instance.getIp(), 250, 100);
                this.checkAndConfigMediaNode(instance.getIp(), this.openviduConfigPro.getDockerRegistries());
                AdditionalLogAggregator additionalLogAggregator = this.openviduConfigPro.getAdditionalLogAggregator();
                AdditionalMonitoring additionalMonitoring = this.openviduConfigPro.getAdditionalMonitoring();
                this.checkAndConfigMediaNode(instance.getIp(), this.openviduConfigPro.getDockerRegistries());
                boolean pullRecordingImage = this.openviduConfigPro.isRecordingModuleEnabled() && this.openviduConfigPro.isRecordingComposedExternal();
                String var10000 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
                String recordingImage = var10000 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion();
                this.provisionMediaNode(instance.getIp(), InstanceType.mediaServer, this.openviduConfigPro.getOpenViduSecret(), additionalLogAggregator, this.openviduConfigPro.getKmsImage(), this.openviduConfigPro.getMediasoupImage(), this.openviduConfigPro.getMediaNodeKurentoConfig(), pullRecordingImage ? recordingImage : "NONE");
                if (this.openviduConfigPro.isElasticsearchDefined()) {
                    this.provisionBeats(instance.getIp(), this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getOpenViduSecret(), this.openviduConfigPro.getOpenviduProStatsMonitoringInterval(), this.esHost, this.esUserName, this.esPassword, this.openviduConfigPro.getMetricbeatImage(), this.openviduConfigPro.getFilebeatImage(), instance.getId());
                } else {
                    this.dropBeats(instance.getIp());
                }

                if (this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
                    this.provisionCoturn(instance.getIp(), this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getCoturnImage(), this.openviduConfigPro.getCoturnConfig().getCoturnPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMinPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMaxPort(), this.openviduConfigPro.getCoturnConfig().getCoturnSharedSecretKey(), instance.getId());
                } else {
                    this.dropCoturn(instance.getIp());
                }

                if (!SpeechToTextType.disabled.equals(this.openviduConfigPro.getSpeechToText())) {
                    this.provisionSpeechToTextService(instance.getIp(), this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getSpeechToTextImage(), this.openviduConfigPro.getSpeechToText(), this.openviduConfigPro.getSpeechToTextVoskModelLoadStrategy(), this.openviduConfigPro.getSpeechToTextPort(), this.openviduConfigPro.getSpeechToTextAzureKey(), this.openviduConfigPro.getSpeechToTextAzureRegion(), this.openviduConfigPro.getAwsAccessKey(), this.openviduConfigPro.getAwsSecretKey(), this.openviduConfigPro.getAwsRegion(), instance.getId());
                } else {
                    this.dropSpeechToText(instance.getIp());
                }

                if (additionalMonitoring.getType() == Type.datadog) {
                    this.provisionDataDog(instance.getIp(), this.openviduConfigPro.getOpenViduSecret(), additionalMonitoring.getProperties());
                }
            } catch (Exception var7) {
                String errorContent = "Error provisioning Media Node: {}" + var7.getMessage();
                log.error(errorContent);
                var7.printStackTrace();
                this.removeInstance(instance.getId());
                this.dropInstance(instance);
                this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.failed, instance.getStatus());
                throw new Exception(errorContent);
            }
        }

    }

    public void dropInstance(Instance instance) throws Exception {
        log.info("Dropping instance {}", instance.toString());
        String filesName = UUID.randomUUID().toString();
        String absoluteScriptName = this.openviduConfigPro.getClusterPath() + "openvidu_drop.sh";
        String absoluteOutputName = this.openviduConfigPro.getClusterPath() + "output/openvidu_drop.sh/";
        this.fileManager.createFolderIfNotExists(absoluteOutputName);
        File errorOutput = new File(absoluteOutputName + filesName + "_ERROR.txt");
        CommandExecutor.execCommandRedirectError(300000L, errorOutput, new String[]{"/bin/sh", "-c", absoluteScriptName + " " + instance.getEnvironmentId()});
        super.dropInstance(instance);

        try {
            String errorContent = FileUtils.readFileToString(errorOutput, StandardCharsets.UTF_8);
            if (!errorContent.isEmpty()) {
                String errorMessage = "Script \"" + absoluteScriptName + "\" returned an error: " + errorContent;
                log.warn(errorMessage);
                log.error("Dropping instance may have failed. Check the state of your instances");
                throw new Exception(errorMessage);
            }
        } catch (IOException var8) {
            log.error("Error reading from file \"{}\": {}", errorOutput.getAbsolutePath(), var8.getMessage());
        }

        log.info("Instace {} dropped", instance.toString());
    }

    public List<Instance> autodiscoverInstances(boolean ignoreAddedInstances, boolean dropNotReachableInstances) throws Exception {
        log.info("Autodiscovering '{}' instances", this.openviduConfigPro.getClusterEnvironment().name());
        String filesName = UUID.randomUUID().toString();
        String absoluteScriptName = this.openviduConfigPro.getClusterPath() + "openvidu_autodiscover.sh";
        String absoluteOutputName = this.openviduConfigPro.getClusterPath() + "output/openvidu_autodiscover.sh/";
        this.fileManager.createFolderIfNotExists(absoluteOutputName);
        File standardOutput = new File(absoluteOutputName + filesName + "_AUTODISCOVER.json");
        File errorOutput = new File(absoluteOutputName + filesName + "_AUTODISCOVER_ERROR.txt");
        CommandExecutor.execCommandRedirectStandardOutputAndError(300000L, standardOutput, errorOutput, new String[]{"/bin/sh", "-c", absoluteScriptName});
        JsonArray autodiscoveredInstancesJson = null;

        String var10002;
        try {
            autodiscoveredInstancesJson = this.jsonUtils.fromFileToJsonArray(standardOutput.getAbsolutePath());
        } catch (JsonSyntaxException | IllegalStateException | JsonIOException var22) {
            log.error("Script \"{}\" returned an invalid JSON string through the standard output. Looking for error output...", absoluteScriptName);

            try {
                String errorContent = FileUtils.readFileToString(errorOutput, StandardCharsets.UTF_8);
                log.error("Error output found");
                throw new Exception("Script \"" + absoluteScriptName + "\" returned an error: " + errorContent);
            } catch (IOException var20) {
                var10002 = errorOutput.getAbsolutePath();
                throw new Exception("Error reading from file \"" + var10002 + "\": " + var20.getMessage());
            }
        } catch (IOException var23) {
            var10002 = standardOutput.getAbsolutePath();
            throw new Exception("Error reading file \"" + var10002 + "\" after running script \"" + absoluteScriptName + "\". Error: " + var23.getMessage());
        }

        Map<String, Instance> possibleNewInstances = new HashMap();
        Iterator var25 = autodiscoveredInstancesJson.iterator();

        while(var25.hasNext()) {
            JsonElement i = (JsonElement)var25.next();

            JsonObject instanceJson;
            try {
                instanceJson = i.getAsJsonObject();
            } catch (IllegalStateException var21) {
                throw new Exception("Standard output returned by script \"" + absoluteScriptName + "\" is a JSON array, but an element that is not a JSON object has been found inside of it. Wrong element: " + i.toString());
            }

            String ip = instanceJson.get("ip").getAsString();
            String environmentId = instanceJson.get("id").getAsString();
            String id = "media_" + environmentId;
            if ((!ignoreAddedInstances || !this.mediaNodeAlreadyAdded(ip)) && !this.newMediaNodesNotAllowed()) {
                try {
                    log.info("Provisioning media-node {} if containers are not running", ip);
                    AdditionalLogAggregator additionalLogAggregator = this.openviduConfigPro.getAdditionalLogAggregator();
                    AdditionalMonitoring additionalMonitoring = this.openviduConfigPro.getAdditionalMonitoring();
                    this.checkAndConfigMediaNode(ip, this.openviduConfigPro.getDockerRegistries());
                    boolean pullRecordingImage = this.openviduConfigPro.isRecordingModuleEnabled() && this.openviduConfigPro.isRecordingComposedExternal();
                    String var10000 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
                    String recordingImage = var10000 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion();
                    this.provisionMediaNode(ip, InstanceType.mediaServer, this.openviduConfigPro.getOpenViduSecret(), additionalLogAggregator, this.openviduConfigPro.getKmsImage(), this.openviduConfigPro.getMediasoupImage(), this.openviduConfigPro.getMediaNodeKurentoConfig(), pullRecordingImage ? recordingImage : "NONE");
                    if (this.openviduConfigPro.isElasticsearchDefined()) {
                        this.provisionBeats(ip, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getOpenViduSecret(), this.openviduConfigPro.getOpenviduProStatsMonitoringInterval(), this.esHost, this.esUserName, this.esPassword, this.openviduConfigPro.getMetricbeatImage(), this.openviduConfigPro.getFilebeatImage(), id);
                    } else {
                        this.dropBeats(ip);
                    }

                    if (this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
                        this.provisionCoturn(ip, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getCoturnImage(), this.openviduConfigPro.getCoturnPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMinPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMaxPort(), this.openviduConfigPro.getCoturnConfig().getCoturnSharedSecretKey(), id);
                    } else {
                        this.dropCoturn(ip);
                    }

                    if (!SpeechToTextType.disabled.equals(this.openviduConfigPro.getSpeechToText())) {
                        this.provisionSpeechToTextService(ip, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getSpeechToTextImage(), this.openviduConfigPro.getSpeechToText(), this.openviduConfigPro.getSpeechToTextVoskModelLoadStrategy(), this.openviduConfigPro.getSpeechToTextPort(), this.openviduConfigPro.getSpeechToTextAzureKey(), this.openviduConfigPro.getSpeechToTextAzureRegion(), this.openviduConfigPro.getAwsAccessKey(), this.openviduConfigPro.getAwsSecretKey(), this.openviduConfigPro.getAwsRegion(), id);
                    } else {
                        this.dropSpeechToText(ip);
                    }

                    if (additionalMonitoring.getType() == Type.datadog) {
                        this.provisionDataDog(ip, this.openviduConfigPro.getOpenViduSecret(), additionalMonitoring.getProperties());
                    }
                } catch (Exception var24) {
                    log.error("Can't provision autodiscovered node. Probably media-node-controller is not running: {}", ip);
                    var24.printStackTrace();
                    continue;
                }

                Instance instance = new Instance(InstanceType.mediaServer, id);
                instance.setEnvironmentId(environmentId);
                instance.setIp(ip);
                this.addInstance(instance);
                possibleNewInstances.put(instance.getIp(), instance);
                this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.launching, (InstanceStatus)null);
            }
        }

        log.info("Autodiscovered {} instances with ips {}", possibleNewInstances.size(), possibleNewInstances.values().stream().map(Instance::getIp).collect(Collectors.toList()));
        List<Kms> newKmss = this.initNewKmss(possibleNewInstances, false);
        Set<String> newKmssIds = (Set)newKmss.stream().map(Kms::getId).collect(Collectors.toSet());
        List<Instance> instancesToRemove = new ArrayList();
        List<Instance> instancesAutodiscovered = new ArrayList();
        possibleNewInstances.values().stream().forEach((instancex) -> {
            if (!newKmssIds.contains(instancex.getId())) {
                instancesToRemove.add(instancex);
            } else {
                instancesAutodiscovered.add(instancex);
            }

        });
        instancesToRemove.forEach((instancex) -> {
            this.removeInstance(instancex.getId());

            try {
                if (dropNotReachableInstances) {
                    this.dropInstance(instancex);
                }
            } catch (Exception var4) {
                var4.printStackTrace();
            }

        });
        log.info("Autodiscovery process finished with {} existing instances", newKmss.size());
        return instancesAutodiscovered;
    }

    protected boolean isLunchingInDinD() {
        return this.openviduConfigPro == null ? false : OpenViduClusterEnvironment.docker.equals(this.openviduConfigPro.getClusterEnvironment());
    }

    @PostConstruct
    public void init() {
        try {
            this.checkClusterScripts(this.openviduConfigPro.getClusterPath());
            this.openViduIp = this.openviduConfigPro.getOpenViduPrivateIp();
            this.esHost = this.openviduConfigPro.getElasticsearchHost();
            this.esUserName = this.openviduConfigPro.getElasticsearchUserName();
            this.esPassword = this.openviduConfigPro.getElasticsearchPassword();
        } catch (Exception var2) {
            log.error(var2.getMessage());
            log.error("Shutting down OpenVidu Server");
            Runtime.getRuntime().halt(1);
        }

        super.init();
    }
}
