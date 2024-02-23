//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.config;

import io.openvidu.server.pro.stt.SpeechToTextVoskModelLoadStrategy;
import java.util.Objects;

public class MediaNodeSpeechToTextConfig {
    private String speechToTextImage;
    private String engine;
    private int port;
    private String azureKey;
    private String azureRegion;
    private String awsKey;
    private String awsSecret;
    private String awsRegion;
    private SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy;

    public MediaNodeSpeechToTextConfig() {
    }

    public String getSpeechToTextImage() {
        return this.speechToTextImage;
    }

    public void setSpeechToTextImage(String speechToTextImage) {
        this.speechToTextImage = speechToTextImage;
    }

    public String getEngine() {
        return this.engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAzureKey() {
        return this.azureKey;
    }

    public void setAzureKey(String azureKey) {
        this.azureKey = azureKey;
    }

    public String getAzureRegion() {
        return this.azureRegion;
    }

    public String getAwsKey() {
        return this.awsKey;
    }

    public void setAwsKey(String awsKey) {
        this.awsKey = awsKey;
    }

    public String getAwsSecret() {
        return this.awsSecret;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAwsRegion() {
        return this.awsRegion;
    }

    public void setAwsSecret(String awsSecret) {
        this.awsSecret = awsSecret;
    }

    public void setAzureRegion(String azureRegion) {
        this.azureRegion = azureRegion;
    }

    public SpeechToTextVoskModelLoadStrategy getSpeechToTextVoskModelLoadStrategy() {
        return this.speechToTextVoskModelLoadStrategy;
    }

    public void setSpeechToTextVoskModelLoadStrategy(SpeechToTextVoskModelLoadStrategy speechToTextVoskModelLoadStrategy) {
        this.speechToTextVoskModelLoadStrategy = speechToTextVoskModelLoadStrategy;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MediaNodeSpeechToTextConfig)) {
            return false;
        } else {
            MediaNodeSpeechToTextConfig that = (MediaNodeSpeechToTextConfig)o;
            return this.port == that.port && Objects.equals(this.speechToTextImage, that.speechToTextImage) && Objects.equals(this.engine, that.engine) && Objects.equals(this.azureKey, that.azureKey) && Objects.equals(this.azureRegion, that.azureRegion) && Objects.equals(this.awsKey, that.awsKey) && Objects.equals(this.awsSecret, that.awsSecret) && Objects.equals(this.awsRegion, that.awsRegion) && this.speechToTextVoskModelLoadStrategy == that.speechToTextVoskModelLoadStrategy;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.speechToTextImage, this.engine, this.port, this.azureKey, this.azureRegion, this.awsKey, this.awsSecret, this.awsRegion, this.speechToTextVoskModelLoadStrategy});
    }

    public String toString() {
        return "MediaNodeSpeechToTextConfig{speechToTextImage='" + this.speechToTextImage + "', engine='" + this.engine + "', port=" + this.port + ", azureKey='" + this.azureKey + "', azureRegion='" + this.azureRegion + "', awsKey='" + this.awsKey + "', awsSecret='" + this.awsSecret + "', awsRegion='" + this.awsRegion + "', speechToTextVoskModelLoadStrategy=" + this.speechToTextVoskModelLoadStrategy + "}";
    }
}
