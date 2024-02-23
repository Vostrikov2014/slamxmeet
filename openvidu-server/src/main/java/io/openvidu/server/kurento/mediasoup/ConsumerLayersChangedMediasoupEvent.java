//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import java.util.List;

import io.openvidu.server.kurento.mediasoup.ConsumerLayers;
import org.kurento.client.MediaEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.Tag;
import org.kurento.client.internal.server.Param;

public class ConsumerLayersChangedMediasoupEvent extends MediaEvent {
    private ConsumerLayers layers;

    public ConsumerLayersChangedMediasoupEvent(@Param("source") MediaObject source, @Param("timestampMillis") String timestampMillis, @Param("tags") List<Tag> tags, @Param("type") String type, @Param("layers") ConsumerLayers layers) {
        super(source, timestampMillis, tags, type);
        this.layers = layers;
    }

    public ConsumerLayers getLayers() {
        return this.layers;
    }

    public void setLayers(ConsumerLayers layers) {
        this.layers = layers;
    }

    public String toString() {
        return "ConsumerLayersChangedMedisoupEvent [layers=" + this.layers + "]";
    }
}
