//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config.logback;

import io.openvidu.server.config.OpenviduConfigPro;

public class HostElasticSearchLogbackProperty extends ElasticSearchLogbackProperty {
    public HostElasticSearchLogbackProperty() {
    }

    public String getPropertyValue() {
        if (!this.isOpenViduConfigReady()) {
            return this.DEFAULT_UNCONFIGURED_VALUE;
        } else {
            OpenviduConfigPro openViduConfigPro = this.getOpenViduConfigPro();
            String esHost = openViduConfigPro.getElasticsearchHost();
            String esUserName = openViduConfigPro.getElasticsearchUserName();
            String esPassword = openViduConfigPro.getElasticsearchPassword();
            if (esHost != null && !esHost.isEmpty()) {
                boolean isESSecured = esUserName != null && !esUserName.isEmpty() && esPassword != null && !esPassword.isEmpty();
                if (isESSecured) {
                    boolean isHttps = esHost.matches("^https://.*");
                    String auxUrl = esHost.replaceAll("http://|https://", "");
                    esHost = "http" + (isHttps ? "s" : "") + "://" + esUserName + ":" + esPassword + "@" + auxUrl;
                }
            }

            return esHost;
        }
    }
}
