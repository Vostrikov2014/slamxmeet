//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.kms;

import io.openvidu.server.infrastructure.InfrastructureInstanceData;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.LoadManager;
import io.openvidu.server.infrastructure.InfrastructureInstanceData;
import io.openvidu.server.infrastructure.Instance;
import org.springframework.beans.factory.annotation.Autowired;

public class CpuLoadManager implements LoadManager {
    @Autowired
    private InfrastructureInstanceData infrastructureInstanceData;

    public CpuLoadManager() {
    }

    public double calculateLoad(Kms kms) {
        return this.countCpuLoad(kms);
    }

    private synchronized double countCpuLoad(Kms kms) {
        Instance instance = this.infrastructureInstanceData.getInstance(kms.getId());
        return instance != null ? instance.getCpuLoad() : Double.MAX_VALUE;
    }
}
