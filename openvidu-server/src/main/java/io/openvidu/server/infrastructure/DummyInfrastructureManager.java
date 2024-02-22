//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import io.openvidu.server.config.OpenviduConfigPro;
import java.util.List;

public class DummyInfrastructureManager extends InfrastructureManager {
    public DummyInfrastructureManager(OpenviduConfigPro config, InfrastructureInstanceData infrastructureInstanceData) {
        super(infrastructureInstanceData);
        this.openviduConfigPro = config;
    }

    public Instance launchInstance(LaunchInstanceOptions launchInstanceOptions) throws Exception {
        return null;
    }

    public void provisionInstance(Instance instance) throws Exception {
    }

    public void dropInstance(Instance instance) throws Exception {
    }

    public List<Instance> autodiscoverInstances(boolean ignoreAddedInstances, boolean dropNotReachableInstances) throws Exception {
        return null;
    }

    public void init() {
        this.startLambdaThreads();
    }

    protected boolean isLunchingInDinD() {
        return false;
    }
}
