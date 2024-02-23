//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import java.util.List;
import org.kurento.client.internal.ModuleName;
import org.kurento.client.internal.server.Param;

@ModuleName("mediasoup")
public class ConsumerScore {
    private Number score;
    private Number producerScore;
    private List<Number> producerScores;

    public ConsumerScore(@Param("score") Number score, @Param("producerScore") Number producerScore, @Param("producerScores") List<Number> producerScores) {
        this.score = score;
        this.producerScore = producerScore;
        this.producerScores = producerScores;
    }

    public Number getScore() {
        return this.score;
    }

    public void setScore(Number score) {
        this.score = score;
    }

    public Number getProducerScore() {
        return this.producerScore;
    }

    public void setProducerScore(Number producerScore) {
        this.producerScore = producerScore;
    }

    public List<Number> getProducerScores() {
        return this.producerScores;
    }

    public void setProducerScores(List<Number> producerScores) {
        this.producerScores = producerScores;
    }

    public String toString() {
        return "ConsumerScore [score=" + this.score + ", producerScore=" + this.producerScore + ", producerScores=" + this.producerScores + "]";
    }
}
