//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import java.util.HashMap;
import java.util.Map;

public class AdditionalMonitoring {
    private Type type;
    private Map<String, String> properties;

    public AdditionalMonitoring() {
        this.type = AdditionalMonitoring.Type.none;
        this.properties = new HashMap();
    }

    public AdditionalMonitoring(Type type, Map<String, String> properties) {
        this.type = type;
        this.properties = properties;
    }

    public Type getType() {
        return this.type;
    }

    public String getProperty(String propertyName) {
        return (String)this.properties.get(propertyName);
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public static enum DataDogProperties {
        API_KEY,
        DATADOG_SITE;

        private DataDogProperties() {
        }
    }

    public static enum Type {
        none,
        datadog;

        private Type() {
        }
    }
}
