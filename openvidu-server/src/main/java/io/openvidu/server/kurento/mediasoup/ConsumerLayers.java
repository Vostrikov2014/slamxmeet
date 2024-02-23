//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import org.kurento.client.internal.ModuleName;
import org.kurento.client.internal.server.Param;

@ModuleName("mediasoup")
public class ConsumerLayers {
    private Number spatialLayer;
    private Number temporalLayer;

    public ConsumerLayers(@Param("spatialLayer") Number spatialLayer, @Param("temporalLayer") Number temporalLayer) {
        this.spatialLayer = spatialLayer;
        this.temporalLayer = temporalLayer;
    }

    public Number getSpatialLayer() {
        return this.spatialLayer;
    }

    public void setSpatialLayer(Number spatialLayer) {
        this.spatialLayer = spatialLayer;
    }

    public Number getTemporalLayer() {
        return this.temporalLayer;
    }

    public void setTemporalLayer(Number temporalLayer) {
        this.temporalLayer = temporalLayer;
    }

    public String toString() {
        return "ConsumerLayers [spatialLayer=" + this.spatialLayer + ", temporalLayer=" + this.temporalLayer + "]";
    }
}
