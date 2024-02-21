//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.cdr;

import com.google.gson.JsonObject;
import io.openvidu.java.client.RecordingLayout;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.cdr.CDREventEnd;
import io.openvidu.server.cdr.CDREventName;
import io.openvidu.server.core.EndReason;

public class CDREventBroadcast extends CDREventEnd {
    private RecordingProperties properties;
    private String broadcastUrl;

    public CDREventBroadcast(String sessionId, String uniqueSessionId, RecordingProperties properties, String broadcastUrl, Long timestamp) {
        super(CDREventName.broadcastStarted, sessionId, uniqueSessionId, timestamp);
        this.properties = properties;
        this.broadcastUrl = broadcastUrl;
    }

    public CDREventBroadcast(CDREventBroadcast event, EndReason reason) {
        super(CDREventName.broadcastStopped, event.getSessionId(), event.getUniqueSessionId(), event.getTimestamp(), reason, System.currentTimeMillis());
        this.properties = event.properties;
        this.broadcastUrl = event.broadcastUrl;
    }

    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("broadcastUrl", this.broadcastUrl);
        json.addProperty("resolution", this.properties.resolution());
        json.addProperty("frameRate", this.properties.frameRate());
        json.addProperty("recordingLayout", this.properties.recordingLayout().name());
        if (RecordingLayout.CUSTOM.equals(this.properties.recordingLayout())) {
            json.addProperty("customLayout", this.properties.customLayout());
        }

        json.addProperty("hasAudio", this.properties.hasAudio());
        return json;
    }
}
