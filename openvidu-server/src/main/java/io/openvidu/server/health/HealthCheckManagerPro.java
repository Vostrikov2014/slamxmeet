//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.health;

import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.health.HealthCheck.Status;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class HealthCheckManagerPro implements HealthCheckManager {
    @Autowired
    protected KmsManager kmsManager;
    @Autowired
    private InfrastructureManager infrastructureManager;

    public HealthCheckManagerPro() {
    }

    public HealthCheck health() {
        Collection<Kms> kmsList = this.kmsManager.getKmss();
        List<String> disconnectedMediaNodes = new ArrayList();
        int numRegisteredKms = 0;
        boolean allMediaNodesConnected = true;
        Iterator var5 = kmsList.iterator();

        while(var5.hasNext()) {
            Kms kms = (Kms)var5.next();
            Instance instance = this.infrastructureManager.getInstance(kms.getId());
            if (instance != null) {
                if (kms.isKurentoClientConnected()) {
                    ++numRegisteredKms;
                } else {
                    allMediaNodesConnected = false;
                    disconnectedMediaNodes.add(kms.getId());
                }
            }
        }

        if (numRegisteredKms == 0) {
            return new HealthCheck(Status.DOWN, disconnectedMediaNodes);
        } else if (allMediaNodesConnected) {
            return new HealthCheck(Status.UP, disconnectedMediaNodes);
        } else {
            return new HealthCheck(Status.UNSTABLE, disconnectedMediaNodes);
        }
    }
}
