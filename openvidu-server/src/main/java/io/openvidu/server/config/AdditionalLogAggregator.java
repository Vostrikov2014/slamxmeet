//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import java.util.HashMap;
import java.util.Map;

public class AdditionalLogAggregator {
    private Type type;
    private Map<String, String> properties;

    public AdditionalLogAggregator() {
        this.type = AdditionalLogAggregator.Type.none;
        this.properties = new HashMap();
    }

    public AdditionalLogAggregator(Type type, Map<String, String> properties) {
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

    public static enum SplunkProperties {
        TOKEN_MEDIA_NODE,
        SPLUNK_URL,
        INSECURE_SKIP_VERIFY;

        private SplunkProperties() {
        }
    }

    public static enum Type {
        none,
        splunk;

        private Type() {
        }
    }
}