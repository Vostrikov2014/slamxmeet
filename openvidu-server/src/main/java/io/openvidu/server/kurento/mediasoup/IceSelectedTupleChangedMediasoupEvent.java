//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import java.util.List;
import org.kurento.client.MediaEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.Tag;
import org.kurento.client.internal.server.Param;

public class IceSelectedTupleChangedMediasoupEvent extends MediaEvent {
    private TransportTuple tuple;

    public IceSelectedTupleChangedMediasoupEvent(@Param("source") MediaObject source, @Param("timestampMillis") String timestampMillis, @Param("tags") List<Tag> tags, @Param("type") String type, @Param("tuple") TransportTuple tuple) {
        super(source, timestampMillis, tags, type);
        this.tuple = tuple;
    }

    public TransportTuple getTuple() {
        return this.tuple;
    }

    public void setTuple(TransportTuple tuple) {
        this.tuple = tuple;
    }

    public String toString() {
        return "IceSelectedTupleChangedMediasoupEvent [tuple=" + this.tuple + "]";
    }
}
