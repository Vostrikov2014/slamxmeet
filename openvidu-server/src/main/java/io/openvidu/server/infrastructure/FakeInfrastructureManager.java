//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.config.AdditionalLogAggregator;
import io.openvidu.server.infrastructure.mncontroller.config.MediaNodeKurentoConfig;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FakeInfrastructureManager extends InfrastructureManager {
    private final int DEFAULT_millisLaunchTime = 3000;
    private final int DEFAULT_millisDropTime = 1500;
    private final boolean DEFAULT_randomizeTimes = true;
    private int millisLaunchTime = 3000;
    private int millisDropTime = 1500;
    private int provisionTime = 3000;
    private boolean randomizeTimes = true;

    public FakeInfrastructureManager(InfrastructureInstanceData infrastructureInstanceData) {
        super(infrastructureInstanceData);
    }

    public int getMillisLaunchTime() {
        return this.millisLaunchTime;
    }

    public void setMillisLaunchTime(int millisLaunchTime) {
        this.millisLaunchTime = millisLaunchTime;
    }

    public int getMillisProvisionTime() {
        return this.provisionTime;
    }

    public void setMillisProvisionTime(int provisionTime) {
        this.provisionTime = provisionTime;
    }

    public int getMillisDropTime() {
        return this.millisDropTime;
    }

    public void setMillisDropTime(int millisDropTime) {
        this.millisDropTime = millisDropTime;
    }

    public boolean getRandomizeTimes() {
        return this.randomizeTimes;
    }

    public void setRandomizeTimes(boolean randomizeTimes) {
        this.randomizeTimes = randomizeTimes;
    }

    public void resetToDefaultValues() {
        this.millisLaunchTime = 3000;
        this.millisDropTime = 1500;
        this.randomizeTimes = true;
    }

    public Instance launchInstance(LaunchInstanceOptions launchInstanceOptions) throws Exception {
        String fakeId = KmsManager.generateKmsId();
        Instance instance = new Instance(InstanceType.mediaServer, fakeId);
        this.addInstance(instance);
        this.recordMediaNodeEvent(instance, (String)null, InstanceStatus.launching, (InstanceStatus)null);
        if (this.randomizeTimes) {
            Thread.sleep((long)(Math.random() * (double)this.millisLaunchTime));
        } else {
            Thread.sleep((long)this.millisLaunchTime);
        }

        return instance;
    }

    public void provisionInstance(Instance instance) throws Exception {
        if (this.randomizeTimes) {
            Thread.sleep((long)(Math.random() * (double)this.millisLaunchTime));
        } else {
            Thread.sleep((long)this.millisLaunchTime);
        }

    }

    public void dropInstance(Instance instance) throws Exception {
        if (this.randomizeTimes) {
            Thread.sleep((long)(Math.random() * (double)this.millisDropTime));
        } else {
            Thread.sleep((long)this.millisDropTime);
        }

        super.dropInstance(instance);
    }

    public List<Instance> autodiscoverInstances(boolean ignoreAddedInstances, boolean dropNotReachableInstances) throws Exception {
        return null;
    }

    public String provisionMediaNode(String mediaNodeIp, InstanceType type, String openviduSecret, AdditionalLogAggregator additionalLogAggregator, String configuredKmsImage, String configuredMediasoupImage, MediaNodeKurentoConfig kmsConfig, String recordingImageTag) throws IOException {
        return null;
    }

    public List<String> provisionBeats(String mediaNodeIp, String clusterId, String openviduSecret, int loadInterval, String esHost, String esUserName, String esPassword, String configuredMetricBeatImage, String configuredFileBeatImage, String nodeId) throws IOException {
        return null;
    }

    public String provisionDataDog(String mediaNodeIp, String openviduSecret, Map<String, String> datadogProperties) throws IOException {
        return null;
    }

    protected boolean isLunchingInDinD() {
        return false;
    }

    public Instance getInstanceByIp(String ip) {
        return null;
    }
}
