//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerContainerCreateOptions {
    private String Image;
    private String name;
    private List<String> Env;
    private String User;
    private DockerHostConfig HostConfig;
    private List<String> Cmd;
    private String Id;
    private Map<String, String> Labels;
    private Map<String, Map<String, String>> ExposedPorts;

    public DockerContainerCreateOptions(String image, String name, List<String> env, String user, DockerHostConfig hostConfig, List<String> cmd, Map<String, String> labels) {
        this.Image = image;
        this.name = name;
        this.Env = env;
        this.User = user;
        this.HostConfig = hostConfig;
        this.Cmd = cmd;
        this.Labels = labels;
    }

    public DockerContainerCreateOptions(String image) {
        this.Image = image;
    }

    public String getImage() {
        return this.Image;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.Id;
    }

    public Map<String, String> getLabels() {
        return this.Labels;
    }

    public List<String> getEnv() {
        return this.Env;
    }

    public void addExposedPorts(String exposedPort) {
        if (this.ExposedPorts == null) {
            this.ExposedPorts = new HashMap();
        }

        this.ExposedPorts.put(exposedPort, new HashMap());
    }
}
