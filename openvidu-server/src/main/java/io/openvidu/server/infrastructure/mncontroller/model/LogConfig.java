//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.model;

import java.util.Map;

public class LogConfig {
    private String Type;
    private Map<String, String> Config;

    public LogConfig(String type, Map<String, String> config) {
        this.Type = type;
        this.Config = config;
    }
}
