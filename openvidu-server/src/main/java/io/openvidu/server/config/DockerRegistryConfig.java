//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.google.gson.JsonObject;

public class DockerRegistryConfig {
    private String serverAddress;
    private String username;
    private String password;

    private DockerRegistryConfig(String serverAddress, String username, String password) {
        this.serverAddress = serverAddress;
        this.username = username;
        this.password = password;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("serveraddress", this.getServerAddress());
        json.addProperty("username", this.getUsername());
        json.addProperty("password", this.getPassword());
        return json;
    }

    public static class Builder {
        private String serverAddress;
        private String username;
        private String password;

        public Builder() {
        }

        public Builder serverAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public DockerRegistryConfig build() {
            if (this.serverAddress != null && !this.serverAddress.isEmpty() && this.username != null && !this.username.isEmpty() && this.password != null && !this.password.isEmpty()) {
                return new DockerRegistryConfig(this.serverAddress, this.username, this.password);
            } else {
                throw new IllegalStateException("'serveraddress', 'username' and 'password' are mandatory");
            }
        }
    }
}
