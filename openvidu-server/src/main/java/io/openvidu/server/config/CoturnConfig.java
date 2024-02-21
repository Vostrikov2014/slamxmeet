//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoturnConfig {
    private static final Logger log = LoggerFactory.getLogger(CoturnConfig.class);
    private String masterNodeCoturnIp;
    private int masterNodeCoturnPort;
    private int mediaNodeCoturnPort;
    private ConcurrentHashMap<String, String> mapKmsUrisCoturnIps = new ConcurrentHashMap();
    private boolean isDeployedOnMediaNodes;
    private int mediaNodeMinPort;
    private int mediaNodeMaxPort;
    private String coturnSharedSecretKey;

    public CoturnConfig(String masterNodeCoturnIp, int masterNodeCoturnPort, int mediaNodeCoturnPort, boolean isDeployedOnMediaNodes, int mediaNodeMinPort, int mediaNodeMaxPort, String coturnSharedSecretKey) {
        this.isDeployedOnMediaNodes = isDeployedOnMediaNodes;
        this.masterNodeCoturnIp = masterNodeCoturnIp;
        this.masterNodeCoturnPort = masterNodeCoturnPort;
        this.mediaNodeCoturnPort = mediaNodeCoturnPort;
        this.mediaNodeMinPort = mediaNodeMinPort;
        this.mediaNodeMaxPort = mediaNodeMaxPort;
        this.coturnSharedSecretKey = coturnSharedSecretKey;
    }

    public boolean isDeployedOnMediaNodes() {
        return this.isDeployedOnMediaNodes;
    }

    public String getCoturnIp(String kmsUri) {
        return this.isDeployedOnMediaNodes ? (String)this.mapKmsUrisCoturnIps.get(kmsUri) : this.masterNodeCoturnIp;
    }

    public int getCoturnPort() {
        return this.isDeployedOnMediaNodes ? this.mediaNodeCoturnPort : this.masterNodeCoturnPort;
    }

    public int getMediaNodeMinPort() {
        return this.mediaNodeMinPort;
    }

    public int getMediaNodeMaxPort() {
        return this.mediaNodeMaxPort;
    }

    public String getCoturnSharedSecretKey() {
        return this.coturnSharedSecretKey;
    }

    public void putCoturnIp(String kmsUri, String coturnIp) {
        log.info("Adding as Coturn IP '{}' to Kms uri: '{}'", coturnIp, kmsUri);
        this.mapKmsUrisCoturnIps.put(kmsUri, coturnIp);
    }

    public void removeCoturnIp(String kmsUri) {
        if (kmsUri != null) {
            log.info("Removing Coturn IP public IP from Kms uri: '{}'", kmsUri);
            this.mapKmsUrisCoturnIps.remove(kmsUri);
        }

    }
}
