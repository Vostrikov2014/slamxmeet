//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import com.google.gson.JsonObject;

public class Status {
    public static volatile long startTime = 0L;
    public static volatile int restartCounter = 0;
    public static volatile long lastRestartTime = 0L;

    public Status() {
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("startTime", startTime);
        json.addProperty("restartCounter", restartCounter);
        json.addProperty("lastRestartTime", lastRestartTime);
        return json;
    }
}
