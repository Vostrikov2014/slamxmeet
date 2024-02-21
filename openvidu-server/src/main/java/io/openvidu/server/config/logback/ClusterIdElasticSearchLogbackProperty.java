//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config.logback;

import io.openvidu.server.config.OpenviduConfigPro;

public class ClusterIdElasticSearchLogbackProperty extends ElasticSearchLogbackProperty {
    public ClusterIdElasticSearchLogbackProperty() {
    }

    public String getPropertyValue() {
        if (!this.isOpenViduConfigReady()) {
            return this.DEFAULT_UNCONFIGURED_VALUE;
        } else {
            OpenviduConfigPro openViduConfigPro = this.getOpenViduConfigPro();
            return openViduConfigPro.getClusterId();
        }
    }
}
