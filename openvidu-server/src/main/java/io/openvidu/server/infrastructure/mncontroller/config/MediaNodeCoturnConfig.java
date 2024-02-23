//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.config;

import java.util.Objects;

public class MediaNodeCoturnConfig {
    private String coturnImage;
    private int coturnPort;
    private int minCoturnPort;
    private int maxCoturnPort;
    private String coturnSharedSecretKey;

    public MediaNodeCoturnConfig() {
    }

    public String getCoturnImage() {
        return this.coturnImage;
    }

    public void setCoturnImage(String coturnImage) {
        this.coturnImage = coturnImage;
    }

    public int getMinCoturnPort() {
        return this.minCoturnPort;
    }

    public void setMinCoturnPort(int minCoturnPort) {
        this.minCoturnPort = minCoturnPort;
    }

    public int getMaxCoturnPort() {
        return this.maxCoturnPort;
    }

    public void setMaxCoturnPort(int maxCoturnPort) {
        this.maxCoturnPort = maxCoturnPort;
    }

    public String getCoturnSharedSecretKey() {
        return this.coturnSharedSecretKey;
    }

    public void setCoturnSharedSecretKey(String coturnSharedSecretKey) {
        this.coturnSharedSecretKey = coturnSharedSecretKey;
    }

    public int getCoturnPort() {
        return this.coturnPort;
    }

    public void setCoturnPort(int coturnPort) {
        this.coturnPort = coturnPort;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            MediaNodeCoturnConfig that = (MediaNodeCoturnConfig)o;
            return this.coturnPort == that.coturnPort && this.minCoturnPort == that.minCoturnPort && this.maxCoturnPort == that.maxCoturnPort && Objects.equals(this.coturnImage, that.coturnImage) && Objects.equals(this.coturnSharedSecretKey, that.coturnSharedSecretKey);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.coturnImage, this.coturnPort, this.minCoturnPort, this.maxCoturnPort, this.coturnSharedSecretKey});
    }

    public String toString() {
        return "{'image': '" + this.coturnImage + "', 'coturnPort': '" + this.coturnPort + "', 'minCoturnPort': '" + this.minCoturnPort + "', 'maxCoturnPort': " + this.maxCoturnPort + "'}";
    }
}
