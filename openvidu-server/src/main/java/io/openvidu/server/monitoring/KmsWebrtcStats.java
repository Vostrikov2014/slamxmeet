//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.monitoring;

import com.google.gson.JsonObject;
import io.openvidu.server.core.Participant;
import org.kurento.client.MediaType;
import org.kurento.client.RTCInboundRTPStreamStats;
import org.kurento.client.RTCOutboundRTPStreamStats;
import org.kurento.client.RTCRTPStreamStats;

public class KmsWebrtcStats {
    private Participant participant;
    private String endpoint;
    private MediaType mediaType;
    private RTCRTPStreamStats stats;

    public KmsWebrtcStats(Participant participant, String endpoint, MediaType mediaType, RTCRTPStreamStats stats) {
        this.participant = participant;
        this.endpoint = endpoint;
        this.mediaType = mediaType;
        this.stats = stats;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("sessionId", this.participant.getSessionId());
        json.addProperty("uniqueSessionId", this.participant.getUniqueSessionId());
        json.addProperty("user", this.participant.getFinalUserId());
        json.addProperty("connection", this.participant.getParticipantPublicId());
        json.addProperty("connectionId", this.participant.getParticipantPublicId());
        json.addProperty("endpoint", this.endpoint);
        json.addProperty("mediaType", this.mediaType.name().toLowerCase());
        switch (this.stats.getType()) {
            case inboundrtp:
                RTCInboundRTPStreamStats inboudStats = (RTCInboundRTPStreamStats)this.stats;
                json.addProperty("jitter", inboudStats.getJitter());
                json.addProperty("bytesReceived", inboudStats.getBytesReceived());
                json.addProperty("packetsReceived", inboudStats.getPacketsReceived());
                json.addProperty("packetsLost", inboudStats.getPacketsLost());
                break;
            case outboundrtp:
                RTCOutboundRTPStreamStats outboundStats = (RTCOutboundRTPStreamStats)this.stats;
                json.addProperty("rtt", outboundStats.getRoundTripTime());
                json.addProperty("bytesSent", outboundStats.getBytesSent());
                json.addProperty("packetsSent", outboundStats.getPacketsSent());
                json.addProperty("targetBitrate", outboundStats.getTargetBitrate());
        }

        json.addProperty("timestamp", this.stats.getTimestampMillis());
        json.addProperty("fractionLost", this.stats.getFractionLost());
        json.addProperty("remb", this.stats.getRemb());
        json.addProperty("firCount", this.stats.getFirCount());
        json.addProperty("pliCount", this.stats.getPliCount());
        json.addProperty("nackCount", this.stats.getNackCount());
        json.addProperty("sliCount", this.stats.getSliCount());
        return json;
    }

    public long getTimestamp() {
        return this.stats.getTimestampMillis();
    }
}
