//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.config;

import com.google.gson.Gson;
import java.util.List;
import java.util.Objects;

public class MediaNodeBeatConfig {
    private String beatImage;
    private String outputHost;
    private String esUserName;
    private String esPassword;
    private String clusterId;
    private String nodeId;
    private String loadInterval;
    private String mediaNodePrivateIp;
    private List<String> volumes;
    private List<String> environmentVariables;

    public MediaNodeBeatConfig() {
    }

    public String getBeatImage() {
        return this.beatImage;
    }

    public void setBeatImage(String beatImage) {
        this.beatImage = beatImage;
    }

    public String getOutputHost() {
        return this.outputHost;
    }

    public void setOutputHost(String outputHost) {
        this.outputHost = outputHost;
    }

    public String getEsUserName() {
        return this.esUserName;
    }

    public void setEsUserName(String esUserName) {
        this.esUserName = esUserName;
    }

    public String getEsPassword() {
        return this.esPassword;
    }

    public void setEsPassword(String esPassword) {
        this.esPassword = esPassword;
    }

    public String getClusterId() {
        return this.clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String setNodeId(String nodeId) {
        return this.nodeId;
    }

    public String getLoadInterval() {
        return this.loadInterval;
    }

    public void setLoadInterval(String loadInterval) {
        this.loadInterval = loadInterval;
    }

    public String getMediaNodePrivateIp() {
        return this.mediaNodePrivateIp;
    }

    public void setMediaNodePrivateIp(String mediaNodePrivateIp) {
        this.mediaNodePrivateIp = mediaNodePrivateIp;
    }

    public List<String> getVolumes() {
        return this.volumes;
    }

    public void setVolumes(List<String> volumes) {
        this.volumes = volumes;
    }

    public List<String> getEnvironmentVariables() {
        return this.environmentVariables;
    }

    public void setEnvironmentVariables(List<String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String toString() {
        Gson gson = new Gson();
        String var10000 = this.beatImage;
        return "{'image': '" + var10000 + "', 'outputHost: '" + this.outputHost + "', 'esUserName': '" + this.esUserName + "', 'esPassword': '" + this.esPassword + "', 'clusterId': '" + this.clusterId + "', 'nodeId': '" + this.nodeId + "', 'loadInterval': " + this.loadInterval + ", 'mediaNodePrivateIp': '" + this.mediaNodePrivateIp + "', 'volumes': " + gson.toJson(this.volumes) + ", 'environmentVariables': " + gson.toJson(this.environmentVariables) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            MediaNodeBeatConfig that = (MediaNodeBeatConfig)o;
            return Objects.equals(this.beatImage, that.beatImage) && Objects.equals(this.outputHost, that.outputHost) && Objects.equals(this.esUserName, that.esUserName) && Objects.equals(this.esPassword, that.esPassword) && Objects.equals(this.clusterId, that.clusterId) && Objects.equals(this.loadInterval, that.loadInterval) && Objects.equals(this.nodeId, that.nodeId) && Objects.equals(this.mediaNodePrivateIp, that.mediaNodePrivateIp) && Objects.equals(this.volumes, that.volumes) && Objects.equals(this.environmentVariables, that.environmentVariables);
        } else {
            return false;
        }
    }
}
