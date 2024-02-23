//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import java.util.List;

import io.openvidu.server.kurento.mediasoup.ProducerScore;
import org.kurento.client.MediaEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.Tag;
import org.kurento.client.internal.server.Param;

public class ProducerScoresChangedMediasoupEvent extends MediaEvent {
    private List<ProducerScore> scores;

    public ProducerScoresChangedMediasoupEvent(@Param("source") MediaObject source, @Param("timestampMillis") String timestampMillis, @Param("tags") List<Tag> tags, @Param("type") String type, @Param("scores") List<ProducerScore> scores) {
        super(source, timestampMillis, tags, type);
        this.scores = scores;
    }

    public List<ProducerScore> getScores() {
        return this.scores;
    }

    public void setScores(List<ProducerScore> scores) {
        this.scores = scores;
    }

    public String toString() {
        return "ProduceScoresChangedMediasoupEvent [scores=" + this.scores.toString() + "]";
    }
}
