//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.config;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MediaNodeKurentoConfig {
    private Map<String, String> configuration;
    public static final String PREFIX_CONFIG_NAME = "KMS_DOCKER_ENV_";
    private String kmsImage;
    private String mediaNodePrivateIp;
    private String mediaNodePublicIp;

    public MediaNodeKurentoConfig() {
        this.configuration = new HashMap();
    }

    public MediaNodeKurentoConfig(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public MediaNodeKurentoConfig(MediaNodeKurentoConfig mediaNodeKurentoConfig) {
        this.kmsImage = mediaNodeKurentoConfig.kmsImage;
        this.mediaNodePrivateIp = mediaNodeKurentoConfig.mediaNodePrivateIp;
        this.mediaNodePublicIp = mediaNodeKurentoConfig.mediaNodePublicIp;
        this.configuration = new HashMap(mediaNodeKurentoConfig.configuration);
    }

    public String getKmsImage() {
        return this.kmsImage;
    }

    public void setKmsImage(String kmsImage) {
        this.kmsImage = kmsImage;
    }

    public String getMediaNodePrivateIp() {
        return this.mediaNodePrivateIp;
    }

    public void setMediaNodePrivateIp(String mediaNodePrivateIp) {
        this.mediaNodePrivateIp = mediaNodePrivateIp;
    }

    public String getMediaNodePublicIp() {
        return this.mediaNodePublicIp;
    }

    public void setMediaNodePublicIp(String mediaNodePublicIp) {
        this.mediaNodePublicIp = mediaNodePublicIp;
    }

    public void addEnvVariable(String envVarName, String envVarValue) {
        this.configuration.put(envVarName, envVarValue);
    }

    public String getEnvVariable(String envVarName) {
        return (String)this.configuration.get(envVarName);
    }

    public boolean isDefined(String envVarName) {
        String envVarValue = (String)this.configuration.get(envVarName);
        return envVarValue != null && !envVarValue.isEmpty();
    }

    public Map<String, String> getConfiguration() {
        return this.configuration;
    }

    public String toString() {
        Gson gson = new Gson();
        String var10000 = gson.toJson(this.configuration);
        return "{'configuration': '" + var10000 + "', 'kmsImage: '" + this.kmsImage + "', 'mediaNodePrivateIp': '" + this.mediaNodePrivateIp + "'}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            MediaNodeKurentoConfig that = (MediaNodeKurentoConfig)o;
            return Objects.equals(this.configuration, that.configuration) && Objects.equals(this.kmsImage, that.kmsImage) && Objects.equals(this.mediaNodePrivateIp, that.mediaNodePrivateIp) && Objects.equals(this.mediaNodePublicIp, that.mediaNodePublicIp);
        } else {
            return false;
        }
    }
}
