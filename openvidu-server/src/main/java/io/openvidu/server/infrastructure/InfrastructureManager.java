//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.java.client.Recording.OutputMode;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.kurento.kms.KmsProperties;
import io.openvidu.server.account.ExceededCoresException;
import io.openvidu.server.account.LambdaService;
import io.openvidu.server.cdr.CallDetailRecordPro;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.DockerRegistryConfig;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeControllerDockerManager;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeProvisioner;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.SpeechToTextVoskModelLoadStrategy;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.utils.CustomFileManager;
import io.openvidu.server.utils.LocalCustomFileManager;
import io.openvidu.server.utils.MediaNodeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class InfrastructureManager {
    private static final Logger log = LoggerFactory.getLogger(InfrastructureManager.class);
    @Autowired
    protected OpenviduConfigPro openviduConfigPro;
    @Autowired
    protected OpenviduBuildInfo openviduBuildInfo;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    protected KmsManager kmsManager;
    @Autowired
    protected MediaNodeManager mediaNodeManager;
    @Autowired
    private CallDetailRecord CDR;
    @Autowired
    private LambdaService lambdaService;
    private InfrastructureInstanceData infrastructureInstanceData;
    protected String openViduIp;
    protected CustomFileManager fileManager = new LocalCustomFileManager();
    private ExecutorService parallelLaunchInstancesExecutor = Executors.newCachedThreadPool();
    protected static final String OUTPUT_FOLDER = "output";
    protected static final String LAUNCH_KMS_SCRIPT_NAME = "openvidu_launch_kms.sh";
    protected static final String DROP_SCRIPT_NAME = "openvidu_drop.sh";
    protected static final String AUTODISCOVER_SCRIPT_NAME = "openvidu_autodiscover.sh";
    private static final List<OpenViduClusterEnvironment> CLUSTER_ENVIRONMENT_WITH_SCRIPTS;

    public InfrastructureManager(InfrastructureInstanceData infrastructureInstanceData) {
        this.infrastructureInstanceData = infrastructureInstanceData;
    }

    public abstract Instance launchInstance(LaunchInstanceOptions var1) throws Exception;

    public abstract void provisionInstance(Instance var1) throws Exception;

    public void dropInstance(Instance instance) throws Exception {
        this.removeInstance(instance.getId());
    }

    public abstract List<Instance> autodiscoverInstances(boolean var1, boolean var2) throws Exception;

    public Collection<Instance> getInstances() {
        return this.infrastructureInstanceData.getInstancesCollection();
    }

    public Instance getInstance(String instanceId) {
        return this.infrastructureInstanceData.getInstance(instanceId);
    }

    public void addInstance(Instance instance) {
        this.infrastructureInstanceData.addInstance(instance);
    }

    public Instance removeInstance(String instanceId) {
        return this.infrastructureInstanceData.removeInstance(instanceId);
    }

    public Instance getInstanceByIp(String ip) {
        return this.infrastructureInstanceData.getInstanceByIp(ip);
    }

    public Kms getKmsByIp(String ip) {
        Instance instance = this.getInstanceByIp(ip);
        return instance != null ? this.kmsManager.getKms(instance.getId()) : null;
    }

    public Instance removeMediaNode(String mediaNodeId, String deletionStrategy, boolean waitForShutdown, boolean calledByQuarantineKiller, boolean closeSessionsAndRecordings) throws ResponseStatusException {
        try {
            if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                Kms removedKms;
                Instance instance;
                try {
                    removedKms = this.kmsManager.getKms(mediaNodeId);
                    instance = this.getInstance(mediaNodeId);
                    InstanceStatus oldStatus;
                    switch (deletionStrategy) {
                        case "now":
                            break;
                        case "when-no-sessions":
                            if (!InstanceStatus.waitingIdleToTerminate.equals(instance.getStatus())) {
                                oldStatus = instance.getStatus();
                                instance.setStatus(InstanceStatus.waitingIdleToTerminate);
                                this.recordMediaNodeEvent(instance, removedKms.getUri(), InstanceStatus.waitingIdleToTerminate, oldStatus);
                            }

                            if (removedKms.getKurentoSessions().size() > 0 || removedKms.getActiveRecordings().size() > 0 || removedKms.getActiveBroadcasts().size() > 0) {
                                log.info("Media Node {} has ongoing sessions/recordings/broadcasts and property 'deletion-strategy' is set to 'when-no-sessions'", mediaNodeId);
                                log.info("Media Node {} status is now waiting-idle-to-terminate and will be dropped after last session/recording/broadcast is closed", mediaNodeId);
                                throw new ResponseStatusException(HttpStatus.ACCEPTED);
                            }
                            break;
                        case "if-no-sessions":
                        default:
                            if (calledByQuarantineKiller && (instance == null || !InstanceStatus.waitingIdleToTerminate.equals(instance.getStatus()))) {
                                oldStatus = null;
                                return oldStatus;
                            }

                            if (removedKms.getKurentoSessions().size() > 0 || removedKms.getActiveRecordings().size() > 0 || removedKms.getActiveBroadcasts().size() > 0) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT, "Running KMS '" + mediaNodeId + "' has ongoing sessions/recordings/broadcasts");
                            }
                    }

                    InstanceStatus oldStatus = instance.getStatus();
                    instance.setStatus(InstanceStatus.terminating);
                    this.recordMediaNodeEvent(instance, removedKms.getUri(), InstanceStatus.terminating, oldStatus);
                } finally {
                    KmsManager.selectAndRemoveKmsLock.unlock();
                }

                if (waitForShutdown) {
                    return this.removeMediaNodeAux(removedKms, instance, closeSessionsAndRecordings);
                } else {
                    (new Thread(() -> {
                        this.removeMediaNodeAux(removedKms, instance, closeSessionsAndRecordings);
                    })).start();
                    return instance;
                }
            } else {
                log.error("selectAndRemoveKmsLock couldn't be acquired within {} seconds when removing Media Node", 15);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock couldn't be acquired within 15 seconds when removing Media Node");
            }
        } catch (InterruptedException var15) {
            log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock when removing Media Node: {}", var15.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "InterruptedException waiting to acquire selectAndRemoveKmsLock when removing Media Node: " + var15.getMessage());
        }
    }

    private Instance removeMediaNodeAux(Kms removedKms, Instance instance, boolean closeSessionsAndRecordings) {
        if (closeSessionsAndRecordings) {
            this.sessionManager.closeAllSessionsAndRecordingsOfKms(removedKms, EndReason.mediaServerDisconnect);
        }

        try {
            this.destroyKurentoClientAndDropInstance(instance);
            return instance;
        } catch (Exception var5) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error dropping instance: " + var5.getMessage());
        }
    }

    public Instance modifyMediaNode(InstanceStatus status, Instance instance, boolean waitForShutdown) {
        String mediaNodeId = instance.getId();
        InstanceStatus oldStatus2;
        switch (status) {
            case launching:
                try {
                    if (!KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                        log.error("selectAndRemoveKmsLock couldn't be acquired within {} seconds when modifying Media Node", 15);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock couldn't be acquired within 15 seconds when modifying Media Node");
                    }

                    try {
                        if (InstanceStatus.launching.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
                        }

                        if (!InstanceStatus.canceled.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"launching\" if current status is other than \"canceled\"");
                        }

                        oldStatus2 = InstanceStatus.valueOf(instance.getStatus().name());
                        instance.setStatus(InstanceStatus.launching);
                        this.recordMediaNodeEvent(instance, InstanceStatus.launching, oldStatus2);
                        break;
                    } finally {
                        KmsManager.selectAndRemoveKmsLock.unlock();
                    }
                } catch (InterruptedException var35) {
                    log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: {}", var35.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: " + var35.getMessage());
                }
            case canceled:
                try {
                    if (!KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                        log.error("selectAndRemoveKmsLock couldn't be acquired within {} seconds when modifying Media Node", 15);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock couldn't be acquired within 15 seconds when modifying Media Node");
                    }

                    try {
                        if (InstanceStatus.canceled.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
                        }

                        if (!InstanceStatus.launching.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"canceled\" if current status is other than \"launching\"");
                        }

                        oldStatus2 = InstanceStatus.valueOf(instance.getStatus().name());
                        instance.setStatus(InstanceStatus.canceled);
                        this.recordMediaNodeEvent(instance, InstanceStatus.canceled, oldStatus2);
                        break;
                    } finally {
                        KmsManager.selectAndRemoveKmsLock.unlock();
                    }
                } catch (InterruptedException var33) {
                    log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: {}", var33.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: " + var33.getMessage());
                }
            case running:
                try {
                    if (!KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                        log.error("selectAndRemoveKmsLock couldn't be acquired within {} seconds when modifying Media Node", 15);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lock couldn't be acquired within 15 seconds when modifying Media Node");
                    }

                    try {
                        if (InstanceStatus.running.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
                        }

                        if (!InstanceStatus.waitingIdleToTerminate.equals(instance.getStatus())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"running\" if current status is other than \"waiting-idle-to-terminate\"");
                        }

                        oldStatus2 = InstanceStatus.valueOf(instance.getStatus().name());
                        instance.setStatus(InstanceStatus.running);
                        this.recordMediaNodeEvent(instance, InstanceStatus.running, oldStatus2);
                        break;
                    } finally {
                        KmsManager.selectAndRemoveKmsLock.unlock();
                    }
                } catch (InterruptedException var31) {
                    log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: {}", var31.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "InterruptedException waiting to acquire selectAndRemoveKmsLock when modifying Media Node: " + var31.getMessage());
                }
            case terminating:
                if (InstanceStatus.terminating.equals(instance.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.NO_CONTENT);
                }

                if (InstanceStatus.launching.equals(instance.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"termintaing\" if current status is \"launching\"");
                }

                if (InstanceStatus.canceled.equals(instance.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"termintaing\" if current status is \"canceled\". Instance will transition to \"terminating\" status automatically when possible");
                }

                this.removeMediaNode(mediaNodeId, "now", waitForShutdown, false, true);
                break;
            case waitingIdleToTerminate:
                if (InstanceStatus.waitingIdleToTerminate.equals(instance.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.NO_CONTENT);
                }

                if (!InstanceStatus.running.equals(instance.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"waiting-idle-to-terminate\" if current status is other than \"running\"");
                }

                try {
                    this.removeMediaNode(mediaNodeId, "when-no-sessions", waitForShutdown, false, true);
                } catch (ResponseStatusException var36) {
                    if (!var36.getStatus().is2xxSuccessful()) {
                        throw var36;
                    }
                }
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter \"status\". Cannot be \"" + status + "\"");
        }

        return instance;
    }

    public Instance asyncNewMediaNode() throws Exception {
        LaunchInstanceOptions launchInstanceOptions = new LaunchInstanceOptions(InstanceType.mediaServer);
        return this.asyncNewMediaNode(launchInstanceOptions);
    }

    public Instance asyncNewMediaNode(LaunchInstanceOptions launchInstanceOptions) throws Exception {
        Instance instance = null;

        try {
            instance = this.launchInstance(launchInstanceOptions);
            this.addInstance(instance);
        } catch (Exception var5) {
            String msg = "Error launching Media Node instance";
            log.error(msg);
            if (instance != null) {
                this.removeInstance(instance.getId());
            }

            throw new Exception(msg);
        }

        (new Thread(() -> {
            try {
                this.provisionInstance(instance);
            } catch (Exception var3) {
                log.error("Error provisioning Media Node instance: {}", var3.getMessage());
                this.removeInstance(instance.getId());
                return;
            }

            String kmsUriAux = "ws://" + instance.getIp() + ":8888/kurento";
            this.initNewKms(kmsUriAux, instance.getId(), instance);
        })).start();
        return instance;
    }

    public Kms initNewKms(String kmsUri, String kmsId, Instance instance) {
        InstanceStatus previousStatus = InstanceStatus.valueOf(instance.getStatus().name());
        this.addInstance(instance);

        try {
            List<Kms> newKmss = this.kmsManager.initializeKurentoClients(Arrays.asList(new KmsProperties(kmsId, kmsUri)), true);
            if (InstanceStatus.canceled.equals(previousStatus) && InstanceStatus.terminating.equals(instance.getStatus())) {
                return null;
            } else if (newKmss.size() > 0) {
                return (Kms)newKmss.get(0);
            } else {
                throw new Exception("No new KMSs could be initialized");
            }
        } catch (ExceededCoresException var6) {
            log.error("Error adding Media Node {}: {}", var6.getKms().getId(), var6.getClass().getSimpleName());
            this.modifyMediaNode(InstanceStatus.canceled, instance, false);
            this.kmsManager.addKms(var6.getKms());
            this.removeMediaNode(var6.getKms().getId(), "now", false, false, false);
            return null;
        } catch (Exception var7) {
            var7.printStackTrace();
            this.removeInstance(instance.getId());
            return null;
        }
    }

    @PostConstruct
    public void init() {
        if (this.openviduConfigPro.isClusterTestEnabled()) {
            if (OpenViduClusterMode.auto.equals(this.openviduConfigPro.getClusterMode())) {
                try {
                    this.clusterTest();
                    log.info("CLUSTER TEST success!");
                    log.info("Shutting down OpenVidu Server Pro");
                    Runtime.getRuntime().halt(0);
                } catch (Exception var15) {
                    log.error("CLUSTER TEST failed. " + var15.getMessage());
                    log.error("Shutting down OpenVidu Server Pro");
                    Runtime.getRuntime().halt(1);
                }
            } else {
                log.error("CLUSTER TEST is enabled (configuration parameter 'OPENVIDU_PRO_CLUSTER_TEST' is true) but cluster mode is not set to 'auto'. To run the cluster test you must set configuration parameter 'OPENVIDU_PRO_CLUSTER_MODE' to 'auto'");
                log.error("Shutting down OpenVidu Server Pro");
                Runtime.getRuntime().halt(1);
            }
        }

        if (this.openviduConfigPro.isMediaNodesAutodiscovery()) {
            try {
                log.info("Autodiscovering Media Nodes...");
                this.autodiscoverInstances(false, false);
            } catch (Exception var14) {
                log.error("Error autodiscovering Media Nodes: {}", var14.getMessage());
            }
        } else {
            log.info("No autodiscovery of Media Nodes will be performed");
        }

        int desiredNumberOfInitialMediaNodes;
        if (this.openviduConfigPro.isAutoscaling()) {
            log.info("Autoscaling is enabled, so property OPENVIDU_PRO_CLUSTER_MEDIA_NODES will be ignored and the desired initial number of Media Nodes will be OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_NODES");
            desiredNumberOfInitialMediaNodes = this.openviduConfigPro.getAutoscalingMinNodes();
        } else {
            desiredNumberOfInitialMediaNodes = this.openviduConfigPro.getMediaNodes();
        }

        if (OpenViduClusterMode.manual.equals(this.openviduConfigPro.getClusterMode())) {
            log.info("No adjustments on the initial number of Media Nodes will be performed");
        } else {
            int adjustment = desiredNumberOfInitialMediaNodes - this.kmsManager.getKmss().size();
            if (adjustment > 0) {
                log.info("{} new Media Nodes will be launched so parameter '{}' ({}) is reached", new Object[]{adjustment, this.openviduConfigPro.isAutoscaling() ? "OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_NODES" : "OPENVIDU_PRO_CLUSTER_MEDIA_NODES", desiredNumberOfInitialMediaNodes});
                Map<String, Instance> newInstances = new HashMap();
                List<Callable<Instance>> launchInstanceThreads = new ArrayList();
                List<Future<Instance>> launchInstanceResults = null;

                for(int i = 0; i < adjustment; ++i) {
                    launchInstanceThreads.add(new Callable<Instance>() {
                        public Instance call() throws Exception {
                            Instance instance = null;

                            try {
                                LaunchInstanceOptions launchInstanceOptions = new LaunchInstanceOptions(InstanceType.mediaServer);
                                instance = InfrastructureManager.this.launchInstance(launchInstanceOptions);
                                InfrastructureManager.this.provisionInstance(instance);
                                return instance;
                            } catch (Exception var3) {
                                if (instance != null && instance.getId() != null) {
                                    InfrastructureManager.this.dropInstance(instance);
                                }

                                throw var3;
                            }
                        }
                    });
                }

                try {
                    launchInstanceResults = this.parallelLaunchInstancesExecutor.invokeAll(launchInstanceThreads);
                } catch (InterruptedException var13) {
                    log.error("Some instance launch thread was interrupted while waiting to be executed in the thread pool: {}", var13.getMessage());
                }

                Iterator var19 = launchInstanceResults.iterator();

                while(var19.hasNext()) {
                    Future<Instance> future = (Future)var19.next();
                    Instance newInstance = null;

                    try {
                        newInstance = (Instance)future.get();
                        newInstances.put(newInstance.getIp(), newInstance);
                    } catch (ExecutionException var11) {
                        log.error("Error launching Media Node: {}", var11.getMessage());
                    } catch (InterruptedException var12) {
                        log.error("Some instance launch thread was interrupted while running in the thread pool: {}", var12.getMessage());
                    }
                }

                this.parallelLaunchInstancesExecutor.shutdown();
                this.initNewKmss(newInstances, false);
            } else if (adjustment < 0) {
                log.info("{} existing Media Nodes will be dropped so parameter '{}' ({}) is reached", new Object[]{Math.abs(adjustment), this.openviduConfigPro.isAutoscaling() ? "OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_NODES" : "OPENVIDU_PRO_CLUSTER_MEDIA_NODES", desiredNumberOfInitialMediaNodes});
                Iterator<Map.Entry<String, Instance>> it = this.infrastructureInstanceData.getInstancesEntriesIterator();

                for(int i = 0; i > adjustment; --i) {
                    try {
                        Map.Entry<String, Instance> entry = (Map.Entry)it.next();
                        String kmsId = (String)entry.getKey();
                        Instance instance = (Instance)entry.getValue();
                        String uri = this.kmsManager.getKms(kmsId).getUri();
                        InstanceStatus oldStatus = instance.getStatus();
                        instance.setStatus(InstanceStatus.terminating);
                        this.recordMediaNodeEvent(instance, uri, InstanceStatus.terminating, oldStatus);
                        this.destroyKurentoClientAndDropInstance(instance);
                    } catch (Exception var10) {
                        log.error("Error dropping KMS instance: {}", var10.getMessage());
                    }
                }
            }
        }

        this.startLambdaThreads();
        this.openViduIp = this.openviduConfigPro.getOpenViduPrivateIp();
    }

    protected void startLambdaThreads() {
        if (!this.openviduConfigPro.isLicenseOffline()) {
            this.lambdaService.startLicenseThread();
            this.lambdaService.startUsageThread();
        }

    }

    public boolean isEnvironmentWithScripts(OpenViduClusterEnvironment environment) {
        return CLUSTER_ENVIRONMENT_WITH_SCRIPTS.contains(environment);
    }

    public void checkClusterScripts(String clusterPath) throws Exception {
        Path scriptsPath = Paths.get(clusterPath);
        String errorMessage1 = null;
        if (Files.exists(scriptsPath, new LinkOption[0]) && Files.isDirectory(scriptsPath, new LinkOption[0])) {
            if (!Files.isWritable(scriptsPath)) {
                errorMessage1 = "Cluster path \"" + clusterPath + "\" set with property \"OPENVIDU_PRO_CLUSTER_PATH\" is not valid. Reason: OpenVidu Server Pro needs write permissions over path \"" + clusterPath + "\"";
            }
        } else {
            errorMessage1 = "Cluster path \"" + clusterPath + "\" set with property \"OPENVIDU_PRO_CLUSTER_PATH\" is not valid. Reason: OpenVidu Server Pro cannot find path " + clusterPath;
        }

        if (errorMessage1 != null) {
            throw new Exception(errorMessage1);
        } else {
            log.info("Using path \"{}\" as clustering path (set with property \"OPENVIDU_PRO_CLUSTER_PATH\")", clusterPath);
            String launchScriptString = clusterPath + "openvidu_launch_kms.sh";
            String dropScriptString = clusterPath + "openvidu_drop.sh";
            Path launchScript = Paths.get(launchScriptString);
            Path dropScript = Paths.get(dropScriptString);
            String errorMessage2 = null;
            if (Files.notExists(launchScript, new LinkOption[0])) {
                errorMessage2 = "Cluster environment \"on_premise\" requires file \"" + launchScriptString + "\" to exist, but cannot be found";
            }

            if (errorMessage2 == null && Files.notExists(dropScript, new LinkOption[0])) {
                errorMessage2 = "Cluster environment \"on_premise\" requires file \"" + dropScriptString + "\" to exist, but cannot be found";
            }

            if (errorMessage2 == null && !Files.isExecutable(launchScript)) {
                errorMessage2 = "OpenVidu Server Pro must have execute permissions over existing file \"" + launchScriptString + "\"";
            }

            if (errorMessage2 == null && !Files.isExecutable(dropScript)) {
                errorMessage2 = "OpenVidu Server Pro must have execute permissions over existing file \"" + dropScriptString + "\"";
            }

            if (errorMessage2 != null) {
                throw new Exception(errorMessage2);
            } else {
                log.info("Scripts \"{}\" and \"{}\" found in path \"{}\". OpenVidu Server Pro has execute permissions over them", new Object[]{"openvidu_launch_kms.sh", "openvidu_drop.sh", clusterPath});
                if (!this.isAutodiscoveryScriptAvailable(clusterPath)) {
                    this.openviduConfigPro.cancelMediaNodesAutodiscovery();
                }

                String outputFolder = clusterPath + "output";
                boolean newFolderCreated = this.fileManager.createFolderIfNotExists(outputFolder);
                if (newFolderCreated) {
                    log.info("New folder \"{}\" created for storing on_premise custom scripts outputs", outputFolder);
                } else {
                    log.info("Folder \"{}\" for storing on_premise custom script outputs already existed", outputFolder);
                }

                Path outputPath = Paths.get(outputFolder);
                if (!Files.isWritable(outputPath)) {
                    throw new Exception("Output folder \"" + outputFolder + "\" is not valid. Reason: OpenVidu Server Pro needs write permissions over path \"" + outputFolder + "\"");
                }
            }
        }
    }

    public void destroyKurentoClientAndDropInstance(Instance instance) throws Exception {
        Kms kms = this.kmsManager.removeKms(instance.getId());
        kms.getKurentoClient().destroy();
        if (this.openviduConfigPro != null && this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
            this.openviduConfigPro.getCoturnConfig().removeCoturnIp(kms.getUri());
        }

        if (this.openviduConfigPro != null && this.openviduConfigPro.getRecordingsConfig().isRecordingComposedExternal()) {
            this.openviduConfigPro.getRecordingsConfig().removeRecordingsVolumePathByKmsUri(kms.getUri());
        }

        if (OpenViduClusterMode.auto.equals(this.openviduConfigPro.getClusterMode())) {
            this.dropInstance(instance);
        } else {
            log.warn("Media Node {} cannot be automatically terminated if 'OPENVIDU_PRO_CLUSTER_MODE' is not 'auto'", instance.getId());
            instance = this.removeInstance(instance.getId());
        }

        InstanceStatus oldStatus = instance.getStatus();
        instance.setStatus(InstanceStatus.terminated);
        this.recordMediaNodeEvent(kms, instance.getEnvironmentId(), InstanceStatus.terminated, oldStatus);
    }

    protected List<Kms> initNewKmss(Map<String, Instance> instancesByIp, boolean disconnectUponFailure) {
        List<KmsProperties> kmsProperties = (List)instancesByIp.values().stream().map((instance) -> {
            return new KmsProperties(instance.getId(), "ws://" + instance.getIp() + ":8888/kurento");
        }).collect(Collectors.toList());
        List<Kms> newKmss = new ArrayList();

        try {
            kmsProperties.stream().filter((kmsProp) -> {
                return this.kmsManager.kmsWithUriExists(kmsProp.getUri());
            });
            newKmss = this.kmsManager.initializeKurentoClients(kmsProperties, disconnectUponFailure);
        } catch (Exception var6) {
            log.error("Cannot establish connection to some KMS of {}", kmsProperties.stream().map((kmsProp) -> {
                return kmsProp.getUri();
            }).collect(Collectors.toList()));
        }

        return (List)newKmss;
    }

    private void clusterTest() throws Exception {
        log.info("CLUSTER TEST is enabled. Environment: {}", this.openviduConfigPro.getClusterEnvironment());
        int numberOfExistingInstances = this.infrastructureInstanceData.getNumberOfInstances();
        Instance testInstance = null;
        log.info("CLUSTER TEST: launching new Media Node instance");

        try {
            LaunchInstanceOptions launchInstanceOptions = new LaunchInstanceOptions(InstanceType.mediaServer);
            testInstance = this.launchInstance(launchInstanceOptions);
            this.provisionInstance(testInstance);
        } catch (Exception var13) {
            throw new Exception("Cannot launch a new Media Node instance: " + var13.getMessage());
        }

        Map<String, Instance> testInstances = new HashMap();
        testInstances.put(testInstance.getIp(), testInstance);
        List<Kms> newKms = this.initNewKmss(testInstances, true);
        if (newKms.size() != 1) {
            throw new Exception("Unexpected content of list of new Media Nodes. Expected 1, found " + newKms.size());
        } else {
            Kms kms = (Kms)newKms.get(0);
            if (this.infrastructureInstanceData.getNumberOfInstances() != numberOfExistingInstances + 1) {
                throw new Exception("Unexpected number of instances after launch. Expected " + (numberOfExistingInstances + 1) + ", found " + this.infrastructureInstanceData.getNumberOfInstances());
            } else {
                int secondsOfWait = true;
                int msInterval = true;
                int attempt = 0;

                boolean kmsConnectedEventDispatched;
                for(kmsConnectedEventDispatched = false; !kmsConnectedEventDispatched && attempt < 10; ++attempt) {
                    kmsConnectedEventDispatched = kms.isKurentoClientConnected();
                    if (!kmsConnectedEventDispatched) {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException var12) {
                            throw new Exception("Error while waiting to KurentoClient connected event: " + var12.getMessage());
                        }
                    }
                }

                if (!kmsConnectedEventDispatched) {
                    throw new Exception("There's no connection to the test Media Node instance");
                } else if (kms.getTimeOfKurentoClientConnection() <= 0L) {
                    throw new Exception("Wrong connection time to the test Media Node instance");
                } else {
                    log.info("CLUSTER TEST: new Media Node instance has been successfully launched");
                    log.info("CLUSTER TEST: dropping newly created Media Node instance");

                    try {
                        this.removeMediaNode(testInstance.getId(), "now", true, false, true);
                    } catch (Exception var11) {
                        String var10002 = var11.getMessage();
                        throw new Exception("Cannot drop the newly created Media Node instance: " + var10002 + System.lineSeparator() + "New Media Node instance " + testInstance.toString() + " is still alive");
                    }

                    log.info("CLUSTER TEST: the newly created Media Node instance has been successfully dropped");
                    if (this.infrastructureInstanceData.getNumberOfInstances() != numberOfExistingInstances) {
                        throw new Exception("Unexpected number of instances after drop. Expected " + numberOfExistingInstances + ", found " + this.infrastructureInstanceData.getNumberOfInstances());
                    }
                }
            }
        }
    }

    public void recordMediaNodeEvent(Instance instance, InstanceStatus newStatus, InstanceStatus oldStatus) {
        String uri = null;
        Kms kms = this.kmsManager.getKms(instance.getId());
        if (kms != null) {
            uri = kms.getUri();
        }

        ((CallDetailRecordPro)this.CDR).recordMediaNodeStatusChanged(instance, uri, newStatus, oldStatus);
    }

    public void recordMediaNodeEvent(Instance instance, String kmsUri, InstanceStatus newStatus, InstanceStatus oldStatus) {
        ((CallDetailRecordPro)this.CDR).recordMediaNodeStatusChanged(instance, kmsUri, newStatus, oldStatus);
    }

    public void recordMediaNodeEvent(Kms kms, String environmentId, InstanceStatus newStatus, InstanceStatus oldStatus) {
        ((CallDetailRecordPro)this.CDR).recordMediaNodeStatusChanged(kms, environmentId, newStatus, oldStatus);
    }

    protected boolean mediaNodeAlreadyAdded(String ip) {
        boolean added = false;
        Iterator var3 = this.infrastructureInstanceData.getInstancesCollection().iterator();

        while(var3.hasNext()) {
            Instance instance = (Instance)var3.next();
            added = ip.equals(instance.getIp()) && (InstanceStatus.launching.equals(instance.getStatus()) || InstanceStatus.running.equals(instance.getStatus()));
            if (added) {
                break;
            }
        }

        return added;
    }

    public boolean isAutodiscoveryScriptAvailable(String clusterPath) {
        if (!this.isEnvironmentWithScripts(this.openviduConfigPro.getClusterEnvironment())) {
            return true;
        } else {
            boolean isPossible = false;
            String autodiscoverScriptString = clusterPath + "openvidu_autodiscover.sh";
            Path autodiscoverScript = Paths.get(autodiscoverScriptString);
            if (Files.exists(autodiscoverScript, new LinkOption[0])) {
                if (Files.isExecutable(autodiscoverScript)) {
                    isPossible = true;
                    log.info("Script \"{}\" found in path \"{}\". OpenVidu Server Pro has execute permissions over it. Autodiscovery process is possible", "openvidu_autodiscover.sh", clusterPath);
                } else {
                    log.warn("OpenVidu Server Pro has no execute permissions over file \"{}\". Autodiscovery process is disabled", autodiscoverScriptString);
                }
            } else {
                log.warn("File \"{}\" cannot be found. Autodiscovery process is disabled", autodiscoverScriptString);
            }

            return isPossible;
        }
    }

    public void checkAndConfigMediaNode(String mediaNodeIp, List<DockerRegistryConfig> dockerRegistryConfigList) throws OpenViduException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        provisioner.checkAndConfig(mediaNodeIp, this.openviduBuildInfo, dockerRegistryConfigList);
    }

    public void waitUntilMediaNodeControllerIsReady(String mediaNodeIp, int msIntervalWait, int secondsOfWait) throws TimeoutException {
        MediaNodeProvisioner dockerProvisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        MediaNodeControllerDockerManager dockerClient = dockerProvisioner.getMediaNodeControllerDockerManager(mediaNodeIp);
        boolean ready = false;
        int attempts = 1;
        int attemptLimit = secondsOfWait * 1000 / msIntervalWait;
        log.info("Waiting for Media Node Controller {} to be ready for a maximum of {} seconds (maximum {} connection attempts with a wait interval of {} ms between them)", new Object[]{mediaNodeIp, secondsOfWait, attemptLimit, msIntervalWait});

        while(!ready & attempts <= attemptLimit) {
            try {
                dockerClient.isServiceAvailable();
                log.info("Media Node Controller with IP {} is now ready after {} seconds at connection attempt {}", new Object[]{mediaNodeIp, attempts * msIntervalWait / 1000, attempts});
                ready = true;
            } catch (Exception var12) {
                try {
                    Thread.sleep((long)msIntervalWait);
                } catch (InterruptedException var11) {
                    var11.printStackTrace();
                }

                log.warn("Media Node Controller with IP {} connection attempt {} failed. There are still {} connection attempts", new Object[]{mediaNodeIp, attempts, attemptLimit - attempts});
                ++attempts;
            }
        }

        if (attempts >= attemptLimit) {
            String msg = "Media Node Controller with IP" + mediaNodeIp + "wasn't reachable after" + secondsOfWait + " seconds";
            log.error(msg);
            throw new TimeoutException(msg);
        }
    }

    public String provisionMediaNode(String mediaNodeIp, InstanceType type, String openviduSecret, AdditionalLogAggregator additionalLogAggregator, String configuredKmsImage, String configuredMediasoupImage, MediaNodeKurentoConfig kmsConfig, String recordingImage) throws IOException, TimeoutException {
        switch (type) {
            case mediaServer:
                MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
                return provisioner.launchKmsContainer(mediaNodeIp, additionalLogAggregator, "always", configuredKmsImage, configuredMediasoupImage, kmsConfig, recordingImage, this.isLunchingInDinD());
            default:
                return null;
        }
    }

    public String provisionCoturn(String mediaNodeIp, String clusterId, String configuredCoturnImage, int port, int minPort, int maxPort, String sharedSecretKey, String nodeId) throws IOException, TimeoutException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        return provisioner.launchCoturn(mediaNodeIp, clusterId, "always", configuredCoturnImage, port, minPort, maxPort, sharedSecretKey, nodeId);
    }

    public String provisionSpeechToTextService(String mediaNodeIp, String clusterId, String speechToTextImage, SpeechToTextType speechToTextType, SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy, int port, String azureKey, String azureRegion, String awsAccessKeyId, String awsSecretKey, String awsRegion, String nodeId) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        return provisioner.launchSpeechToTextService(mediaNodeIp, clusterId, "always", speechToTextImage, speechToTextType, speechToTextVoskModelLoadStrategy, port, azureKey, azureRegion, awsAccessKeyId, awsSecretKey, awsRegion, nodeId);
    }

    public void dropCoturn(String mediaNodeIp) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        provisioner.removeCoturn(mediaNodeIp);
    }

    public void dropBeats(String ip) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        provisioner.removeBeats(ip);
    }

    public void dropSpeechToText(String mediaNodeIp) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        provisioner.removeSpeechToText(mediaNodeIp);
    }

    public List<String> provisionBeats(String mediaNodeIp, String clusterId, String openviduSecret, int loadInterval, String esHost, String esUserName, String esPassword, String configuredMetricBeatImage, String configuredFilebeatImage, String nodeId) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        List<String> containerBeats = new ArrayList();
        String metricBeatESContainerId = provisioner.launchMetricBeatContainer(mediaNodeIp, clusterId, loadInterval, this.isLunchingInDinD(), "always", esHost, esUserName, esPassword, configuredMetricBeatImage, nodeId);
        containerBeats.add(metricBeatESContainerId);
        String fileBeatContainerId = provisioner.launchFileBeatContainer(mediaNodeIp, clusterId, "always", esHost, esUserName, esPassword, configuredFilebeatImage, nodeId);
        containerBeats.add(fileBeatContainerId);
        return containerBeats;
    }

    public String provisionDataDog(String mediaNodeIp, String openviduSecret, Map<String, String> datadogProperties) throws IOException {
        MediaNodeProvisioner provisioner = new MediaNodeProvisioner(this.openviduConfigPro);
        return provisioner.launchDatadogContainer(mediaNodeIp, datadogProperties, this.isLunchingInDinD(), "always");
    }

    protected abstract boolean isLunchingInDinD();

    public String getMediaNodeIdForRecordingOrBroadcast(RecordingProperties properties, Session session) {
        try {
            return this.getMediaNodePropForRecordingOrBroadcast(properties, session, false);
        } catch (IOException var4) {
            return null;
        }
    }

    public String getMediaNodeIpForRecording(Recording recording) throws IOException {
        return this.getMediaNodePropForRecordingOrBroadcast(recording.getRecordingProperties(), this.sessionManager.getSession(recording.getSessionId()), true);
    }

    private String getMediaNodePropForRecordingOrBroadcast(RecordingProperties properties, Session session, boolean returnIp) throws IOException {
        if (OutputMode.INDIVIDUAL.equals(properties.outputMode()) || properties.hasAudio() && !properties.hasVideo()) {
            return returnIp ? ((KurentoSession)session).getKms().getIp() : session.getMediaNodeId();
        } else {
            String mediaNodeId = properties.mediaNode() != null ? properties.mediaNode() : session.getMediaNodeId();
            if (returnIp) {
                Instance instance = this.getInstance(mediaNodeId);
                if (instance == null) {
                    throw new IOException("Media Node " + properties.mediaNode() + " does not exist");
                } else {
                    return instance.getIp();
                }
            } else {
                return mediaNodeId;
            }
        }
    }

    public boolean isMediaNodeAvailableForSession(String mediaNodeId) {
        return this.getInstance(mediaNodeId) != null && this.mediaNodeManager.isRunning(mediaNodeId);
    }

    public boolean isMediaNodeAvailableForRecordingOrBroadcast(String mediaNodeId) {
        return this.getInstance(mediaNodeId) != null && (this.mediaNodeManager.isRunning(mediaNodeId) || this.mediaNodeManager.isWaitingIdleToTerminate(mediaNodeId));
    }

    public boolean newMediaNodesNotAllowed() {
        return !this.openviduConfigPro.isMultiMasterEnvironment() && this.openviduConfigPro.isMonoNode() && this.infrastructureInstanceData.getNumberOfInstances() >= 1;
    }

    static {
        CLUSTER_ENVIRONMENT_WITH_SCRIPTS = Arrays.asList(OpenViduClusterEnvironment.on_premise, OpenViduClusterEnvironment.aws);
    }
}
