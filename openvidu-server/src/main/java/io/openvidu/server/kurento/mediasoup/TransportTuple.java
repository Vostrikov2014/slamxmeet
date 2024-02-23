//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import org.kurento.client.internal.ModuleName;
import org.kurento.client.internal.server.Param;

@ModuleName("mediasoup")
public class TransportTuple {
    private String localIp;
    private Integer localPort;
    private String remoteIp;
    private Integer remotePort;
    private String protocol;

    public TransportTuple(@Param("localIp") String localIp, @Param("localPort") Integer localPort, @Param("remoteIp") String remoteIp, @Param("remotePort") Integer remotePort, @Param("protocol") String protocol) {
        this.localIp = localIp;
        this.localPort = localPort;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.protocol = protocol;
    }

    public String getLocalIp() {
        return this.localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getLocalPort() {
        return this.localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getRemoteIp() {
        return this.remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Integer getRemotePort() {
        return this.remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String toString() {
        return "TransportTuple [localIp=" + this.localIp + ", localPort=" + this.localPort + ", remoteIp=" + this.remoteIp + ", remotePort=" + this.remotePort + ", protocol=" + this.protocol + "]";
    }
}
