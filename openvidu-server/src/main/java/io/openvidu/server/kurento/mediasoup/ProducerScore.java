//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.mediasoup;

import org.kurento.client.internal.ModuleName;
import org.kurento.client.internal.server.Param;

@ModuleName("mediasoup")
public class ProducerScore {
    private Number encodingIdx;
    private Number ssrc;
    private String rid;
    private Number score;

    public ProducerScore(@Param("encodingIdx") Number encodingIdx, @Param("ssrc") Number ssrc, @Param("rid") String rid, @Param("score") Number score) {
        this.encodingIdx = encodingIdx;
        this.ssrc = ssrc;
        this.rid = rid;
        this.score = score;
    }

    public Number getEncodingIdx() {
        return this.encodingIdx;
    }

    public void setEncodingIdx(Number encodingIdx) {
        this.encodingIdx = encodingIdx;
    }

    public Number getSsrc() {
        return this.ssrc;
    }

    public void setSsrc(Number ssrc) {
        this.ssrc = ssrc;
    }

    public String getRid() {
        return this.rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public Number getScore() {
        return this.score;
    }

    public void setScore(Number score) {
        this.score = score;
    }

    public String toString() {
        return "ProducerScore [encodingIdx=" + this.encodingIdx + ", ssrc=" + this.ssrc + ", rid=" + this.rid + ", score=" + this.score + "]";
    }
}
