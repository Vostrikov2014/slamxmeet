//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.core;

import io.openvidu.java.client.IceServerProperties;
import io.openvidu.java.client.KurentoOptions;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.core.Token;
import io.openvidu.server.core.TokenGenerator;
import io.openvidu.server.config.OpenviduConfigPro;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class TokenGeneratorPro extends TokenGenerator {
    @Autowired
    private OpenviduConfigPro openviduConfigPro;

    public TokenGeneratorPro() {
    }

    public Token generateToken(String sessionId, String serverMetadata, boolean record, OpenViduRole role, KurentoOptions kurentoOptions, List<IceServerProperties> customIceServers) throws Exception {
        Token t = super.generateToken(sessionId, serverMetadata, record, role, kurentoOptions, customIceServers);
        String var10000 = this.openviduConfigPro.getOpenViduEdition().name();
        String additionalParams = "&edition=" + var10000 + "&webrtcStatsInterval=" + this.openviduConfigPro.getOpenviduProStatsWebrtcInterval() + "&sendBrowserLogs=" + this.openviduConfigPro.getSendBrowserLogs();
        String var10001 = t.getToken();
        t.setToken(var10001 + additionalParams);
        return t;
    }
}
