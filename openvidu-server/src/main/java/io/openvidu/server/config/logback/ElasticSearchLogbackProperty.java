//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config.logback;

import ch.qos.logback.core.PropertyDefinerBase;
import io.openvidu.server.OpenViduServerPro;
import io.openvidu.server.config.OpenviduConfigPro;

public abstract class ElasticSearchLogbackProperty extends PropertyDefinerBase {
    protected String DEFAULT_UNCONFIGURED_VALUE = "undefined";

    public ElasticSearchLogbackProperty() {
    }

    protected boolean isOpenViduConfigReady() {
        return OpenViduServerPro.INITIAL_CONFIG != null;
    }

    protected OpenviduConfigPro getOpenViduConfigPro() {
        return OpenViduServerPro.INITIAL_CONFIG;
    }
}
