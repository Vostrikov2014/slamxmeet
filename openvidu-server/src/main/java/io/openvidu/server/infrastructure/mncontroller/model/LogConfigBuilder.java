//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.model;

import java.util.HashMap;
import java.util.Map;

public class LogConfigBuilder {
    private String maxSize;
    private String type;

    public LogConfigBuilder() {
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LogConfig build() {
        if (this.type == null) {
            this.type = "json-file";
        }

        Map<String, String> logConfigMap = new HashMap();
        if (this.maxSize == null) {
            logConfigMap.put("max-size", "100M");
        } else {
            logConfigMap.put("max-size", this.maxSize);
        }

        return new LogConfig(this.type, logConfigMap);
    }
}
