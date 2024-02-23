//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openvidu.server.infrastructure.mncontroller.model.DockerDeviceRequests;
import org.apache.commons.lang3.tuple.Pair;

public class DockerHostConfig {
    private boolean AutoRemove;
    private String networkMode;
    private List<String> Binds;
    private List<DockerUlimit> Ulimits;
    private LogConfig LogConfig;
    private RestartPolicy RestartPolicy;
    private List<DockerDeviceRequests> DeviceRequests;
    private Map<String, List<Map<String, String>>> PortBindings;

    public DockerHostConfig(boolean autoRemove, String networkMode, List<String> binds, List<DockerUlimit> ulimits, LogConfig logConfig, RestartPolicy restartPolicy) {
        this.AutoRemove = autoRemove;
        this.networkMode = networkMode;
        this.Binds = binds;
        this.Ulimits = ulimits;
        this.LogConfig = logConfig;
        this.RestartPolicy = restartPolicy;
    }

    public DockerHostConfig(boolean autoRemove, String networkMode, List<String> binds, List<DockerUlimit> ulimits, LogConfig logConfig, RestartPolicy restartPolicy, List<DockerDeviceRequests> dockerDeviceRequests) {
        this.AutoRemove = autoRemove;
        this.networkMode = networkMode;
        this.Binds = binds;
        this.Ulimits = ulimits;
        this.LogConfig = logConfig;
        this.RestartPolicy = restartPolicy;
        this.DeviceRequests = dockerDeviceRequests;
    }

    public void addPortBinding(Pair<String, String> portBindings) {
        if (this.PortBindings == null) {
            this.PortBindings = new HashMap();
        }

        Map<String, String> hostBinding = new HashMap();
        hostBinding.put("HostIp", "");
        hostBinding.put("HostPort", (String)portBindings.getLeft());
        List<Map<String, String>> listHostPort = List.of(hostBinding);
        this.PortBindings.put((String)portBindings.getRight(), listHostPort);
    }
}
