//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.common.util.concurrent.Striped;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.AdditionalMonitoring;
import io.openvidu.server.config.DockerRegistryConfig;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.AdditionalMonitoring.Type;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.infrastructure.InstanceType;
import io.openvidu.server.infrastructure.LaunchInstanceOptions;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.SpeechToTextVoskModelLoadStrategy;
import io.openvidu.server.utils.RestUtils;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/api"})
public class InfrastructureRestControllerPro {
    private static final Logger log = LoggerFactory.getLogger(InfrastructureRestControllerPro.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private KmsManager kmsManager;
    @Autowired
    private InfrastructureManager infrastructureManager;
    private final int MAX_SECONDS_ADD_MEDIA_NODE_LOCK_WAIT = 60;
    private final Striped<Lock> stripedLock = Striped.lazyWeakLock(128);

    public InfrastructureRestControllerPro() {
    }

    @RequestMapping(
            value = {"/media-nodes/{id}"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<String> getMediaNode(@PathVariable("id") String kmsId, @RequestParam(value = "sessions",defaultValue = "false",required = false) boolean withSessions, @RequestParam(value = "recordings",defaultValue = "false",required = false) boolean withRecordings, @RequestParam(value = "extra-info",defaultValue = "false",required = false) boolean withExtraInfo) {
        log.info("REST API: GET {}/media-nodes/{}", "/openvidu/api", kmsId);
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster mode is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            Kms kms = this.kmsManager.getKms(kmsId);
            Instance instance = this.infrastructureManager.getInstance(kmsId);
            JsonObject json = null;
            if (kms == null) {
                if (instance == null) {
                    return RestUtils.getErrorResponse("Media Node \"" + kmsId + "\" does not exist", HttpStatus.NOT_FOUND);
                }

                json = instance.toJson();
            } else {
                KmsManager var10002 = this.kmsManager;
                Objects.requireNonNull(var10002);
                KmsManager.KmsLoad kmsLoad = new KmsManager.KmsLoad(var10002, kms, kms.getLoad());
                json = kmsLoad.toJsonExtended(withSessions, withRecordings, withExtraInfo);
                json = this.getMediaNodeCombinedResponse(json, instance);
            }

            return new ResponseEntity(json.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
        }
    }

    @RequestMapping(
            value = {"/media-nodes"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<String> listMediaNodes(@RequestParam(value = "sessions",defaultValue = "false",required = false) boolean withSessions, @RequestParam(value = "recordings",defaultValue = "false",required = false) boolean withRecordings, @RequestParam(value = "extra-info",defaultValue = "false",required = false) boolean withExtraInfo) {
        log.info("REST API: GET {}/media-nodes", "/openvidu/api");
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster mode is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            JsonArray mediaNodes = new JsonArray();
            Set<String> kmssIds = new HashSet();
            Collection<KmsManager.KmsLoad> kmsLoads = this.kmsManager.getKmssSortedByLoad();
            Iterator var7 = kmsLoads.iterator();

            while(var7.hasNext()) {
                KmsManager.KmsLoad kmsLoad = (KmsManager.KmsLoad)var7.next();
                Instance instance = this.infrastructureManager.getInstance(kmsLoad.getKms().getId());
                JsonObject json = kmsLoad.toJsonExtended(withSessions, withRecordings, withExtraInfo);
                json = this.getMediaNodeCombinedResponse(json, instance);
                mediaNodes.add(json);
                if (instance != null) {
                    kmssIds.add(instance.getId());
                }
            }

            this.infrastructureManager.getInstances().stream().filter((inst) -> {
                return !kmssIds.contains(inst.getId());
            }).forEach((inst) -> {
                mediaNodes.add(inst.toJson());
            });
            JsonObject json = new JsonObject();
            json.addProperty("numberOfElements", mediaNodes.size());
            json.add("content", mediaNodes);
            return new ResponseEntity(json.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
        }
    }

    @RequestMapping(
            value = {"/media-nodes"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<?> newMediaNode(@RequestParam(value = "wait",defaultValue = "false",required = false) boolean waitForRunning, @RequestBody(required = false) Map<String, ?> params) {
        log.info("REST API: POST  {}/media-nodes ({})", "/openvidu/api", waitForRunning ? "sync" : "async");
        if (this.infrastructureManager.newMediaNodesNotAllowed()) {
            return RestUtils.getErrorResponse("Can not launch more than One Media Nodes with your current deployment", HttpStatus.BAD_REQUEST);
        } else if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            String kmsUri = null;
            Instance instance = null;
            switch (this.openviduConfigPro.getClusterMode()) {
                case auto:
                    String instanceType = null;
                    Integer volumeSize = null;
                    if (params != null) {
                        if (params.get("uri") != null) {
                            log.warn("\"uri\" parameter will be ignored. OpenVidu is configured with 'OPENVIDU_PRO_CLUSTER_MODE' to 'auto'");
                        }

                        if (params.get("instanceType") != null) {
                            instanceType = (String)params.get("instanceType");
                            log.info("Custom instance type: {}", instanceType);
                        }

                        if (params.get("volumeSize") != null) {
                            volumeSize = (Integer)params.get("volumeSize");
                            log.info("Custom Volume size: {}", volumeSize);
                        }
                    }

                    LaunchInstanceOptions launchInstanceOptions = new LaunchInstanceOptions(InstanceType.mediaServer);
                    launchInstanceOptions.setInfrastructureInstanceType(instanceType);
                    launchInstanceOptions.setVolumeSize(volumeSize);
                    if (waitForRunning) {
                        try {
                            instance = this.infrastructureManager.launchInstance(launchInstanceOptions);
                            this.infrastructureManager.provisionInstance(instance);
                        } catch (Exception var52) {
                            log.error("Error launching KMS instance");
                            return RestUtils.getErrorResponse("Error launching new instance: " + var52.getMessage(), HttpStatus.BAD_GATEWAY);
                        }

                        kmsUri = "ws://" + instance.getIp() + ":8888/kurento";
                        return this.sharedNewMediaNodeResponse(kmsUri, instance.getId(), instance);
                    } else {
                        try {
                            instance = this.infrastructureManager.asyncNewMediaNode(launchInstanceOptions);
                        } catch (Exception var53) {
                            return RestUtils.getErrorResponse("Error while launching media node: " + var53.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }

                        JsonObject responseJson = this.getMediaNodeCombinedResponse(this.getEmptyMediaNodeJson(instance.getId(), instance.getIp()), instance);
                        return new ResponseEntity(responseJson.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                    }
                case manual:
                    if (params != null && params.get("uri") != null) {
                        try {
                            kmsUri = (String)params.get("uri");
                        } catch (Exception var51) {
                            return RestUtils.getErrorResponse("Invalid parameter \"uri\". Must exist and be a string with a valid URI format", HttpStatus.BAD_REQUEST);
                        }

                        String mediaNodeIp;
                        try {
                            URI uri = this.openviduConfigPro.checkWebsocketUri(kmsUri);
                            mediaNodeIp = uri.getHost();
                            instance = new Instance(InstanceType.mediaServer, "media_" + mediaNodeIp);
                        } catch (Exception var50) {
                            return RestUtils.getErrorResponse("Invalid parameter \"uri\". It must be a string with a valid URI format", HttpStatus.BAD_REQUEST);
                        }

                        if (params.get("environmentId") != null) {
                            String environmentId = (String)params.get("environmentId");
                            log.info("Custom environmentId: {}", environmentId);
                            instance.setEnvironmentId(environmentId);
                        }

                        Lock stringBasedLock = (Lock)this.stripedLock.get(kmsUri);

                        try {
                            if (!stringBasedLock.tryLock(60L, TimeUnit.SECONDS)) {
                                log.error("Lock couldn't be acquired within {} seconds when adding new Media Node", 60);
                                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock couldn't be acquired within 60 seconds when adding new Media Node");
                            } else {
                                ResponseEntity var10;
                                try {
                                    if (!this.kmsManager.kmsWithUriExists(kmsUri)) {
                                        ResponseEntity var11;
                                        try {
                                            String openViduSecret = this.openviduConfigPro.getOpenViduSecret();
                                            int loadInterval = this.openviduConfigPro.getOpenviduProStatsMonitoringInterval();
                                            String clusterId = this.openviduConfigPro.getClusterId();
                                            MediaNodeKurentoConfig kmsConfig = this.openviduConfigPro.getMediaNodeKurentoConfig();
                                            String configuredKmsImage = this.openviduConfigPro.getKmsImage();
                                            String configuredMediasoupImage = this.openviduConfigPro.getMediasoupImage();
                                            List<DockerRegistryConfig> dockerRegistryConfigList = this.openviduConfigPro.getDockerRegistries();
                                            AdditionalLogAggregator additionalLogAggregator = this.openviduConfigPro.getAdditionalLogAggregator();
                                            AdditionalMonitoring additionalMonitoring = this.openviduConfigPro.getAdditionalMonitoring();
                                            this.infrastructureManager.checkAndConfigMediaNode(mediaNodeIp, dockerRegistryConfigList);
                                            boolean pullRecordingImage = this.openviduConfigPro.isRecordingModuleEnabled() && this.openviduConfigPro.isRecordingComposedExternal();
                                            String var10000 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
                                            String recordingImage = var10000 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion();
                                            this.infrastructureManager.provisionMediaNode(mediaNodeIp, InstanceType.mediaServer, openViduSecret, additionalLogAggregator, configuredKmsImage, configuredMediasoupImage, kmsConfig, pullRecordingImage ? recordingImage : "NONE");
                                            String esHost = this.openviduConfigPro.getElasticsearchHost();
                                            String esUserName = this.openviduConfigPro.getElasticsearchUserName();
                                            String esPassword = this.openviduConfigPro.getElasticsearchPassword();
                                            String configuredMetricbeatImage = this.openviduConfigPro.getMetricbeatImage();
                                            String configuredFilebeatImage = this.openviduConfigPro.getFilebeatImage();
                                            String mnodeCoturnImage = this.openviduConfigPro.getCoturnImage();
                                            String speechToTextImage = this.openviduConfigPro.getSpeechToTextImage();
                                            int mnodeCoturnPort = this.openviduConfigPro.getCoturnConfig().getCoturnPort();
                                            int mnodeCoturnMinPort = this.openviduConfigPro.getCoturnConfig().getMediaNodeMinPort();
                                            int mnodeCoturnMaxPort = this.openviduConfigPro.getCoturnConfig().getMediaNodeMaxPort();
                                            String mnodeCoturnSharedSecretKey = this.openviduConfigPro.getCoturnConfig().getCoturnSharedSecretKey();
                                            int speechToTextPort = this.openviduConfigPro.getSpeechToTextPort();
                                            SpeechToTextType speechToTextType = this.openviduConfigPro.getSpeechToText();
                                            SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy = this.openviduConfigPro.getSpeechToTextVoskModelLoadStrategy();
                                            String speechToTextAzureKey = this.openviduConfigPro.getSpeechToTextAzureKey();
                                            String speechToTextAzureRegion = this.openviduConfigPro.getSpeechToTextAzureRegion();
                                            String speechToTextAwsAccessKey = this.openviduConfigPro.getAwsAccessKey();
                                            String speechToTextAwsSecretKey = this.openviduConfigPro.getAwsSecretKey();
                                            String speechToTextAwsRegion = this.openviduConfigPro.getAwsRegion();
                                            if (this.openviduConfigPro.isElasticsearchDefined()) {
                                                this.infrastructureManager.provisionBeats(mediaNodeIp, clusterId, openViduSecret, loadInterval, esHost, esUserName, esPassword, configuredMetricbeatImage, configuredFilebeatImage, instance.getId());
                                            } else {
                                                this.infrastructureManager.dropBeats(mediaNodeIp);
                                            }

                                            if (this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
                                                this.infrastructureManager.provisionCoturn(mediaNodeIp, clusterId, mnodeCoturnImage, mnodeCoturnPort, mnodeCoturnMinPort, mnodeCoturnMaxPort, mnodeCoturnSharedSecretKey, instance.getId());
                                            } else {
                                                this.infrastructureManager.dropCoturn(mediaNodeIp);
                                            }

                                            if (!SpeechToTextType.disabled.equals(this.openviduConfigPro.getSpeechToText())) {
                                                this.infrastructureManager.provisionSpeechToTextService(mediaNodeIp, clusterId, speechToTextImage, speechToTextType, speechToTextVoskModelLoadStrategy, speechToTextPort, speechToTextAzureKey, speechToTextAzureRegion, speechToTextAwsAccessKey, speechToTextAwsSecretKey, speechToTextAwsRegion, instance.getId());
                                            } else {
                                                this.infrastructureManager.dropSpeechToText(mediaNodeIp);
                                            }

                                            if (additionalMonitoring.getType() == Type.datadog) {
                                                this.infrastructureManager.provisionDataDog(mediaNodeIp, openViduSecret, additionalMonitoring.getProperties());
                                            }

                                            instance.setIp(mediaNodeIp);
                                            this.infrastructureManager.recordMediaNodeEvent(instance, kmsUri, InstanceStatus.launching, (InstanceStatus)null);
                                            ResponseEntity var40 = this.sharedNewMediaNodeResponse(kmsUri, instance.getId(), instance);
                                            return var40;
                                        } catch (OpenViduException var54) {
                                            log.error(var54.getMessage());
                                            var11 = RestUtils.getErrorResponse(var54.getMessage(), HttpStatus.NOT_FOUND);
                                            return var11;
                                        } catch (Exception var55) {
                                            log.error("Error provisioning docker containers at {}: ({})", mediaNodeIp, var55.getMessage());
                                            var55.printStackTrace();
                                            var11 = RestUtils.getErrorResponse("Error while launching containers in media node with ip " + mediaNodeIp, HttpStatus.INTERNAL_SERVER_ERROR);
                                            return var11;
                                        }
                                    }

                                    var10 = RestUtils.getErrorResponse("Media Node at \"" + kmsUri + "\" is already registered", HttpStatus.CONFLICT);
                                } finally {
                                    stringBasedLock.unlock();
                                }

                                return var10;
                            }
                        } catch (InterruptedException var57) {
                            log.error("InterruptedException waiting to acquire lock when adding new Media Node: {}", var57.getMessage());
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "InterruptedException waiting to acquire lock when adding new Media Node: " + var57.getMessage());
                        }
                    } else {
                        return RestUtils.getErrorResponse("No parameter \"uri\" found in the body request. If 'OPENVIDU_PRO_CLUSTER_ENVIRONMENT' is 'on_premise', it must exist and be a string with a valid URI format", HttpStatus.NOT_IMPLEMENTED);
                    }
                default:
                    log.error("Unhandled OpenViduClusterMode");
                    return RestUtils.getErrorResponse("Unhandled OpenViduClusterMode", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private ResponseEntity<?> sharedNewMediaNodeResponse(String kmsUri, String kmsId, Instance instance) {
        Kms newKms = this.infrastructureManager.initNewKms(kmsUri, kmsId, instance);
        if (newKms == null) {
            return RestUtils.getErrorResponse("KMS " + kmsUri + " could not be added to OpenVidu Server", HttpStatus.NOT_FOUND);
        } else {
            JsonObject responseJson = newKms.toJson();
            responseJson = this.getMediaNodeCombinedResponse(responseJson, instance);
            return new ResponseEntity(responseJson.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
        }
    }

    @RequestMapping(
            value = {"/media-nodes/{id}"},
            method = {RequestMethod.DELETE}
    )
    public ResponseEntity<?> removeMediaNode(@PathVariable("id") String mediaNodeId, @RequestParam(value = "wait",defaultValue = "false",required = false) boolean waitForShutdown, @RequestParam(value = "deletion-strategy",defaultValue = "if-no-sessions",required = false) String deletionStrategy) {
        log.info("REST API: DELETE  {}/media-nodes/{} ({})", new Object[]{"/openvidu/api", mediaNodeId, waitForShutdown ? "sync" : "async"});
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            Kms removedKms = this.kmsManager.getKms(mediaNodeId);
            Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
            if (removedKms == null) {
                if (instance == null) {
                    return RestUtils.getErrorResponse("Media Node \"" + mediaNodeId + "\" does not exist", HttpStatus.NOT_FOUND);
                }

                if (InstanceStatus.launching.equals(instance.getStatus())) {
                    return RestUtils.getErrorResponse("Cannot remove Media Node " + mediaNodeId + "if status is \"launching\"", HttpStatus.BAD_REQUEST);
                }
            }

            try {
                instance = this.infrastructureManager.removeMediaNode(mediaNodeId, deletionStrategy, waitForShutdown, false, true);
                if (waitForShutdown) {
                    return new ResponseEntity(HttpStatus.NO_CONTENT);
                } else {
                    JsonObject responseJson = this.getMediaNodeCombinedResponse(removedKms.toJson(), instance);
                    return new ResponseEntity(responseJson.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                }
            } catch (ResponseStatusException var7) {
                return RestUtils.getErrorResponse(var7.getMessage(), var7.getStatus());
            }
        }
    }

    @RequestMapping(
            value = {"/media-nodes/{id}"},
            method = {RequestMethod.PATCH}
    )
    public ResponseEntity<?> modifyMediaNode(@PathVariable("id") String mediaNodeId, @RequestParam(value = "wait",defaultValue = "false",required = false) boolean waitForShutdown, @RequestBody(required = true) Map<String, ?> params) {
        log.info("REST API: PATCH  {}/media-nodes/{}", "/openvidu/api", mediaNodeId);
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            Kms modifiedKms = this.kmsManager.getKms(mediaNodeId);
            Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
            if (modifiedKms == null && instance == null) {
                return RestUtils.getErrorResponse("Media Node \"" + mediaNodeId + "\" does not exist", HttpStatus.NOT_FOUND);
            } else {
                InstanceStatus status = null;
                JsonObject responseJson;
                if (params.get("status") != null) {
                    try {
                        status = InstanceStatus.stringToStatus((String)params.get("status"));
                    } catch (IllegalArgumentException | ClassCastException var10) {
                        return RestUtils.getErrorResponse("Invalid parameter \"status\". Must be a string with a valid Media Node status value", HttpStatus.BAD_REQUEST);
                    }

                    try {
                        instance = this.infrastructureManager.modifyMediaNode(status, instance, waitForShutdown);
                        if (modifiedKms == null) {
                            responseJson = this.getEmptyMediaNodeJson(instance.getId(), instance.getIp());
                        } else {
                            responseJson = modifiedKms.toJson();
                        }

                        responseJson = this.getMediaNodeCombinedResponse(responseJson, instance);
                        return new ResponseEntity(responseJson.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                    } catch (ResponseStatusException var9) {
                        return RestUtils.getErrorResponse(var9.getMessage(), var9.getStatus());
                    }
                } else {
                    responseJson = this.getMediaNodeCombinedResponse(modifiedKms.toJson(), instance);
                    return new ResponseEntity(responseJson.toString(), RestUtils.getResponseHeaders(), HttpStatus.NO_CONTENT);
                }
            }
        }
    }

    @RequestMapping(
            value = {"/media-nodes"},
            method = {RequestMethod.PUT}
    )
    public ResponseEntity<String> autodiscover() {
        log.info("REST API: PUT {}/media-nodes", "/openvidu/api");
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster mode is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else if (!this.infrastructureManager.isAutodiscoveryScriptAvailable(this.openviduConfigPro.getClusterPath())) {
            return RestUtils.getErrorResponse("OpenVidu Pro cannot run autodiscovery process (instructions not implemented)", HttpStatus.METHOD_NOT_ALLOWED);
        } else {
            List autodiscoveredInstances;
            try {
                autodiscoveredInstances = this.infrastructureManager.autodiscoverInstances(true, false);
            } catch (Exception var4) {
                return RestUtils.getErrorResponse("Error manually autodiscovering instances: " + var4.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            JsonArray mediaNodes = new JsonArray();
            autodiscoveredInstances.forEach((instance) -> {
                Kms kms = this.kmsManager.getKms(instance.getId());
                if (kms != null) {
                    mediaNodes.add(this.getMediaNodeCombinedResponse(kms.toJson(), instance));
                } else {
                    mediaNodes.add(instance.toJson());
                }

            });
            JsonObject json = new JsonObject();
            json.addProperty("numberOfElements", mediaNodes.size());
            json.add("content", mediaNodes);
            return new ResponseEntity(json.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
        }
    }

    private JsonObject getMediaNodeCombinedResponse(JsonObject kmsJson, Instance instance) {
        if (instance == null) {
            kmsJson.add("environmentId", (JsonElement)null);
            kmsJson.add("status", (JsonElement)null);
        } else {
            kmsJson.addProperty("environmentId", instance.getEnvironmentId());
            kmsJson.addProperty("status", instance.getStatus() != null ? instance.getStatus().toString() : null);
            kmsJson.addProperty("launchingTime", instance.getLaunchingTime());
        }

        if (!kmsJson.has("load")) {
            kmsJson.addProperty("load", 0.0);
        }

        return kmsJson;
    }

    private JsonObject getEmptyMediaNodeJson(String id, String ip) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("object", "mediaNode");
        json.addProperty("ip", ip);
        json.add("uri", (JsonElement)null);
        json.addProperty("connected", false);
        json.add("connectionTime", (JsonElement)null);
        json.add("load", (JsonElement)null);
        return json;
    }
}
