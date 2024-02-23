//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.config.AdditionalMonitoring;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.AdditionalMonitoring.Type;
import io.openvidu.server.infrastructure.InfrastructureInstanceData;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.infrastructure.InstanceType;
import io.openvidu.server.infrastructure.LaunchInstanceOptions;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.utils.LocalDockerManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerInfrastructureManager extends InfrastructureManager {
    private static final Logger log = LoggerFactory.getLogger(DockerInfrastructureManager.class);
    private final String DEV_CONTAINERS_CACHED_PATH = "var-lib-docker";
    private final String DEV_CONTAINERS_MEDIA_NODE_PATH = "openvidu-pro-clustering/media-node";
    private String esHost;
    private String esUserName;
    private String esPassword;

    public DockerInfrastructureManager(InfrastructureInstanceData infrastructureInstanceData) {
        super(infrastructureInstanceData);
    }

    public DockerInfrastructureManager(OpenviduConfigPro config, InfrastructureInstanceData infrastructureInstanceData) {
        super(infrastructureInstanceData);
        this.openviduConfigPro = config;
    }

    @PostConstruct
    public void init() {
        this.openViduIp = this.openviduConfigPro.getOpenViduPrivateIp();
        this.esHost = this.openviduConfigPro.getElasticsearchHost();
        this.esUserName = this.openviduConfigPro.getElasticsearchUserName();
        this.esPassword = this.openviduConfigPro.getElasticsearchPassword();
        super.init();
    }

    protected boolean isLunchingInDinD() {
        return this.openviduConfigPro == null ? true : OpenViduClusterEnvironment.docker.equals(this.openviduConfigPro.getClusterEnvironment());
    }

    public Instance launchInstance(LaunchInstanceOptions launchInstanceOptions) throws Exception {
        if (this.newMediaNodesNotAllowed()) {
            throw new IllegalStateException("Can not launch more than One Media Nodes with your current deployment");
        } else {
            InstanceType type = launchInstanceOptions.getInstanceType();
            switch (type) {
                case mediaServer:
                    Instance instance = null;

                    try {
                        String image = this.openviduConfigPro.getDindImage();
                        String instanceId = this.runMediaNode(image);
                        instance = new Instance(InstanceType.mediaServer, "media_" + instanceId);
                        instance.setEnvironmentId(instanceId);
                        this.addInstance(instance);
                        this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.launching, (InstanceStatus)null);
                        LocalDockerManager dockerManager = null;

                        try {
                            dockerManager = new LocalDockerManager(true);
                            String instanceIp = dockerManager.getContainerIp(instanceId);
                            instance.setIp(instanceIp);
                        } finally {
                            dockerManager.close();
                        }

                        return instance;
                    } catch (Exception var12) {
                        if (instance != null) {
                            this.removeInstance(instance.getId());
                            this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.failed, instance.getStatus());
                        }

                        var12.printStackTrace();
                        throw var12;
                    }
                default:
                    return null;
            }
        }
    }

    public void provisionInstance(Instance instance) throws IOException, TimeoutException {
        this.waitUntilMediaNodeControllerIsReady(instance.getIp(), 250, 600);
        this.checkAndConfigMediaNode(instance.getIp(), this.openviduConfigPro.getDockerRegistries());
        AdditionalLogAggregator additionalLogAggregator = this.openviduConfigPro.getAdditionalLogAggregator();
        AdditionalMonitoring additionalMonitoring = this.openviduConfigPro.getAdditionalMonitoring();
        boolean pullRecordingImage = this.openviduConfigPro.isRecordingModuleEnabled() && this.openviduConfigPro.isRecordingComposedExternal();
        String var10000 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
        String recordingImage = var10000 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion();
        this.provisionMediaNode(instance.getIp(), InstanceType.mediaServer, this.openviduConfigPro.getOpenViduSecret(), additionalLogAggregator, this.openviduConfigPro.getKmsImage(), this.openviduConfigPro.getMediasoupImage(), this.openviduConfigPro.getMediaNodeKurentoConfig(), pullRecordingImage ? recordingImage : "NONE");
        log.info("New instance launched: {}", instance.toString());
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

    }

    public void dropInstance(Instance instance) throws Exception {
        try {
            log.info("Dropping instance {} of type {} and ip {}", new Object[]{instance.getId(), instance.getType().name(), instance.getIp()});
            this.stopMediaNode(instance);
        } catch (Exception var6) {
            log.error("Error stopping Docker container {}: {}", instance.getEnvironmentId(), var6.getMessage());
        } finally {
            super.dropInstance(instance);
        }

        log.info("Instance {} with environment ID {} of type {} and ip {} has been dropped", new Object[]{instance.getId(), instance.getEnvironmentId(), instance.getType().name(), instance.getIp()});
    }

    public List<Instance> autodiscoverInstances(boolean ignoreAddedInstances, boolean dropNotReachableInstances) throws Exception {
        LocalDockerManager dockerManager = null;
        Map<String, Instance> possibleNewInstances = new HashMap();

        try {
            dockerManager = new LocalDockerManager(true);
            String dindImage = this.openviduConfigPro.getDindImage();
            List<String> containerIds = dockerManager.getRunningContainers(dindImage);
            Iterator var7 = containerIds.iterator();

            while(var7.hasNext()) {
                String containerId = (String)var7.next();
                String possibleIp = dockerManager.getContainerIp(containerId);
                String possibleId = "media_" + containerId;
                if ((!ignoreAddedInstances || !this.mediaNodeAlreadyAdded(possibleIp)) && !this.newMediaNodesNotAllowed()) {
                    try {
                        log.info("Provisioning media-node {} if containers are not running", possibleIp);
                        AdditionalLogAggregator additionalLogAggregator = this.openviduConfigPro.getAdditionalLogAggregator();
                        AdditionalMonitoring additionalMonitoring = this.openviduConfigPro.getAdditionalMonitoring();
                        this.checkAndConfigMediaNode(possibleIp, this.openviduConfigPro.getDockerRegistries());
                        boolean pullRecordingImage = this.openviduConfigPro.isRecordingModuleEnabled() && this.openviduConfigPro.isRecordingComposedExternal();
                        String var10000 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
                        String recordingImage = var10000 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion();
                        this.provisionMediaNode(possibleIp, InstanceType.mediaServer, this.openviduConfigPro.getOpenViduSecret(), additionalLogAggregator, this.openviduConfigPro.getKmsImage(), this.openviduConfigPro.getMediasoupImage(), this.openviduConfigPro.getMediaNodeKurentoConfig(), pullRecordingImage ? recordingImage : "NONE");
                        if (this.openviduConfigPro.isElasticsearchDefined()) {
                            this.provisionBeats(possibleIp, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getOpenViduSecret(), this.openviduConfigPro.getOpenviduProStatsMonitoringInterval(), this.esHost, this.esUserName, this.esPassword, this.openviduConfigPro.getMetricbeatImage(), this.openviduConfigPro.getFilebeatImage(), possibleId);
                        } else {
                            this.dropBeats(possibleIp);
                        }

                        if (this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
                            this.provisionCoturn(possibleIp, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getCoturnImage(), this.openviduConfigPro.getCoturnConfig().getCoturnPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMinPort(), this.openviduConfigPro.getCoturnConfig().getMediaNodeMaxPort(), this.openviduConfigPro.getCoturnConfig().getCoturnSharedSecretKey(), possibleId);
                        } else {
                            this.dropCoturn(possibleIp);
                        }

                        if (!SpeechToTextType.disabled.equals(this.openviduConfigPro.getSpeechToText())) {
                            this.provisionSpeechToTextService(possibleIp, this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getSpeechToTextImage(), this.openviduConfigPro.getSpeechToText(), this.openviduConfigPro.getSpeechToTextVoskModelLoadStrategy(), this.openviduConfigPro.getSpeechToTextPort(), this.openviduConfigPro.getSpeechToTextAzureKey(), this.openviduConfigPro.getSpeechToTextAzureRegion(), this.openviduConfigPro.getAwsAccessKey(), this.openviduConfigPro.getAwsSecretKey(), this.openviduConfigPro.getAwsRegion(), possibleId);
                        } else {
                            this.dropSpeechToText(possibleIp);
                        }

                        if (additionalMonitoring.getType() == Type.datadog) {
                            this.provisionDataDog(possibleIp, this.openviduConfigPro.getOpenViduSecret(), additionalMonitoring.getProperties());
                        }
                    } catch (Exception var19) {
                        log.error("Can't provision autodiscovered node. Probably media-node-controller is not running: {}", possibleIp);
                        continue;
                    }

                    Instance instance = new Instance(InstanceType.mediaServer, possibleId);
                    instance.setEnvironmentId(containerId);
                    instance.setIp(possibleIp);
                    this.addInstance(instance);
                    possibleNewInstances.put(instance.getIp(), instance);
                    this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.launching, (InstanceStatus)null);
                }
            }
        } finally {
            dockerManager.close();
        }

        log.info("Autodiscovered {} docker KMS instances listening on {}", possibleNewInstances.size(), possibleNewInstances.values().stream().map(Instance::getIp).collect(Collectors.toList()));
        List newKmss = this.initNewKmss(possibleNewInstances, false);
        Set newKmssIds = (Set)newKmss.stream().map(Kms::getId).collect(Collectors.toSet());
        ArrayList instancesToRemove = new ArrayList();
        ArrayList instancesAutodiscovered = new ArrayList();
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
        log.info("Autodiscovery process finished with {} existing docker KMS instances", newKmss.size());
        return instancesAutodiscovered;
    }

    public void checkDockerEnabled() throws OpenViduException {
        LocalDockerManager dockerManager = null;

        try {
            dockerManager = new LocalDockerManager(true);
            dockerManager.checkDockerEnabled();
        } finally {
            dockerManager.close();
        }

    }

    public String runMediaNode(String image) {
        LocalDockerManager dockerManager = null;
        String dindCachePath = (new File("var-lib-docker")).getAbsolutePath();
        String mediaNodeInstallationPath = (new File("openvidu-pro-clustering/media-node")).getAbsolutePath();
        String[] dockerDriverStorageList = new String[]{"btrfs", "zfs", "overlay2", "fuse-overlayfs", "aufs", "overlay", "devicemapper", "vfs"};

        try {
            dockerManager = new LocalDockerManager(true);
            if (!dockerManager.dockerImageExistsLocally(image)) {
                dockerManager.downloadDockerImage(image, 1800);
            }

            List<Volume> volumes = new ArrayList();
            List<Bind> binds = new ArrayList();
            String[] var8 = dockerDriverStorageList;
            int var9 = dockerDriverStorageList.length;

            Bind bindMediaNode;
            for(int var10 = 0; var10 < var9; ++var10) {
                String dockerDriver = var8[var10];
                Volume volume1 = new Volume("/var/lib/docker/image/" + dockerDriver);
                bindMediaNode = new Bind(dindCachePath + "/image/" + dockerDriver, volume1);
                Volume volume2 = new Volume("/var/lib/docker/" + dockerDriver);
                Bind bind2 = new Bind(dindCachePath + "/" + dockerDriver, volume2);
                volumes.addAll(Arrays.asList(volume1, volume2));
                binds.addAll(Arrays.asList(bindMediaNode, bind2));
            }

            Volume volumeProc = new Volume("/hostfs/proc");
            Volume volumeCgroup = new Volume("/hostfs/sys/fs/cgroup");
            Volume volumeMediaNode = new Volume("/opt/kms");
            volumes.addAll(Arrays.asList(volumeProc, volumeCgroup, volumeMediaNode));
            Bind bindProc = new Bind("/proc", volumeProc, AccessMode.ro);
            Bind bindCgroup = new Bind("/sys/fs/cgroup", volumeCgroup, AccessMode.ro);
            bindMediaNode = new Bind(mediaNodeInstallationPath, volumeMediaNode);
            binds.addAll(Arrays.asList(bindProc, bindCgroup, bindMediaNode));
            log.info("Running DinD Media Node container with this properties: \n Binds: {}", binds);
            String var26 = dockerManager.runContainer((String)null, image, (String)null, (String)null, volumes, binds, "bridge", new ArrayList(), (List)null, (Long)null, true, (Map)null, false);
            return var26;
        } catch (Exception var19) {
            log.error("Error running media-node-controller: {}", var19.getMessage());
            var19.printStackTrace();
            Runtime.getRuntime().halt(1);
        } finally {
            dockerManager.close();
        }

        return null;
    }

    private void stopMediaNode(Instance instance) {
        LocalDockerManager dockerManager = null;

        try {
            dockerManager = new LocalDockerManager(true);
            String containerId = instance.getEnvironmentId();
            List<InspectContainerResponse.Mount> mounts = dockerManager.getMountsForContainers(List.of(containerId));
            log.info("Removing container {}", containerId);
            dockerManager.removeContainer((String)null, containerId, true);
            Iterator var5 = mounts.iterator();

            while(var5.hasNext()) {
                InspectContainerResponse.Mount mount = (InspectContainerResponse.Mount)var5.next();
                if (mount.getName() != null && mount.getDestination().getPath().equals("/var/lib/docker")) {
                    try {
                        log.info("Removing volume {} from container {}", mount.getName(), containerId);
                        dockerManager.removeVolume(mount.getName());
                    } catch (NotFoundException var11) {
                        log.error("Can't remove volume: {}", mount.getName());
                        var11.printStackTrace();
                    }
                }
            }
        } finally {
            dockerManager.close();
        }

    }
}
