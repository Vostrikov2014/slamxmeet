//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import java.util.List;

import io.openvidu.server.kurento.mediasoup.ConsumerScore;
import org.kurento.client.MediaEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.Tag;
import org.kurento.client.internal.server.Param;

public class ConsumerScoreChangedMediasoupEvent extends MediaEvent {
    private ConsumerScore score;

    public ConsumerScoreChangedMediasoupEvent(@Param("source") MediaObject source, @Param("timestampMillis") String timestampMillis, @Param("tags") List<Tag> tags, @Param("type") String type, @Param("score") ConsumerScore score) {
        super(source, timestampMillis, tags, type);
        this.score = score;
    }

    public ConsumerScore getScore() {
        return this.score;
    }

    public void setScore(ConsumerScore score) {
        this.score = score;
    }

    public String toString() {
        return "ConsumerScoreChangedMediasoupEvent [score=" + this.score + "]";
    }
}
