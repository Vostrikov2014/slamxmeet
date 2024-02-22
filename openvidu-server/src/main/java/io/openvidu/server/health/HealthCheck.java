//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.health;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

public class HealthCheck {
    private Status status;
    private List<String> disconnectedMediaNodes;

    public HealthCheck(Status status, List<String> disconnectedMediaNodes) {
        this.status = status;
        this.disconnectedMediaNodes = disconnectedMediaNodes;
    }

    public Status getStatus() {
        return this.status;
    }

    public List<String> getDisconnectedMediaNodes() {
        return this.disconnectedMediaNodes;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("status", this.status.name());
        JsonArray disconnectedMediaNodes = new JsonArray();
        this.disconnectedMediaNodes.forEach((mediaNode) -> {
            disconnectedMediaNodes.add(mediaNode);
        });
        json.add("disconnectedMediaNodes", disconnectedMediaNodes);
        return json;
    }

    public static enum Status {
        UP,
        UNSTABLE,
        DOWN;

        private Status() {
        }
    }
}
