//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.recording.OpenViduRecordingStorage;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.rest.ConfigRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController("configRestControllerPro")
@CrossOrigin
public class ConfigRestControllerPro extends ConfigRestController {
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private OpenviduBuildInfo openviduBuildInfo;

    public ConfigRestControllerPro() {
    }

    protected ResponseEntity<String> getConfig() {
        ResponseEntity<String> response = super.getConfig();
        JsonObject json = JsonParser.parseString((String)response.getBody()).getAsJsonObject();
        json.addProperty("OPENVIDU_EDITION", this.openviduConfigPro.getOpenViduEdition().name());
        json.addProperty("VERSION", this.openviduBuildInfo.getVersion());
        json.addProperty("OPENVIDU_SERVER_DEPENDENCY_VERSION", this.openviduBuildInfo.getOpenViduServerVersion());
        JsonArray kmsUris = new JsonArray();
        this.openviduConfigPro.getKmsUris().forEach((uri) -> {
            kmsUris.add(uri);
        });
        json.add("KMS_URIS", kmsUris);
        json.addProperty("OPENVIDU_PRO_STATS_SESSION_INTERVAL", this.openviduConfigPro.getOpenviduProStatsSessionInterval());
        json.addProperty("OPENVIDU_PRO_STATS_SERVER_INTERVAL", this.openviduConfigPro.getOpenviduProStatsServerInterval());
        json.addProperty("OPENVIDU_PRO_STATS_MONITORING_INTERVAL", this.openviduConfigPro.getOpenviduProStatsMonitoringInterval());
        json.addProperty("OPENVIDU_PRO_STATS_WEBRTC_INTERVAL", this.openviduConfigPro.getOpenviduProStatsWebrtcInterval());
        if (this.openviduConfigPro.isCluster()) {
            json.addProperty("OPENVIDU_PRO_CLUSTER_ID", this.openviduConfigPro.getClusterId());
            if (this.openviduConfigPro.isMultiMasterEnvironment() && !this.openviduConfigPro.isMultimasterOnPremises()) {
                json.addProperty("OPENVIDU_PRO_CLUSTER_ENVIRONMENT", OpenViduClusterEnvironment.aws.name());
            } else {
                json.addProperty("OPENVIDU_PRO_CLUSTER_ENVIRONMENT", this.openviduConfigPro.getClusterEnvironment().name());
            }

            if (OpenViduClusterMode.auto.equals(this.openviduConfigPro.getClusterMode())) {
                json.addProperty("OPENVIDU_PRO_CLUSTER_MEDIA_NODES", this.openviduConfigPro.getMediaNodes());
            }

            if (OpenViduClusterEnvironment.on_premise.equals(this.openviduConfigPro.getClusterEnvironment())) {
                json.addProperty("OPENVIDU_PRO_PRIVATE_IP", this.openviduConfigPro.getOpenViduPrivateIp());
            }

            json.addProperty("OPENVIDU_PRO_CLUSTER_PATH", this.openviduConfigPro.getClusterPath());
            json.addProperty("OPENVIDU_PRO_CLUSTER_RECONNECTION_TIMEOUT", this.openviduConfigPro.getReconnectionTimeout());
        }

        json.addProperty("OPENVIDU_PRO_CLUSTER_AUTOSCALING", this.openviduConfigPro.isAutoscaling());
        if (this.openviduConfigPro.isAutoscaling()) {
            json.addProperty("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MAX_NODES", this.openviduConfigPro.getAutoscalingMaxNodes());
            json.addProperty("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_NODES", this.openviduConfigPro.getAutoscalingMinNodes());
            json.addProperty("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MAX_LOAD", this.openviduConfigPro.getAutoscalingMaxAvgLoad());
            json.addProperty("OPENVIDU_PRO_CLUSTER_AUTOSCALING_MIN_LOAD", this.openviduConfigPro.getAutoscalingMinAvgLoad());
        }

        json.addProperty("OPENVIDU_PRO_NETWORK_QUALITY", this.openviduConfigPro.isNetworkQualityEnabled());
        if (this.openviduConfigPro.isNetworkQualityEnabled()) {
            json.addProperty("OPENVIDU_PRO_NETWORK_QUALITY_INTERVAL", this.openviduConfigPro.getNetworkQualityInterval());
        }

        json.addProperty("OPENVIDU_PRO_SPEECH_TO_TEXT", this.openviduConfigPro.getSpeechToText().name());
        if (this.openviduConfigPro.isSpeechToTextEnabled()) {
            if (this.openviduConfigPro.getSpeechToTextImage() != null && !this.openviduConfigPro.getSpeechToTextImage().isEmpty()) {
                json.addProperty("OPENVIDU_PRO_SPEECH_TO_TEXT_IMAGE", this.openviduConfigPro.getSpeechToTextImage());
            }

            if (SpeechToTextType.vosk.equals(this.openviduConfigPro.getSpeechToText())) {
                json.addProperty("OPENVIDU_PRO_SPEECH_TO_TEXT_VOSK_MODEL_LOAD_STRATEGY", this.openviduConfigPro.getSpeechToTextVoskModelLoadStrategy().name());
            }

            if (SpeechToTextType.azure.equals(this.openviduConfigPro.getSpeechToText())) {
                String hiddenAzureKey = this.openviduConfigPro.hideSecret(this.openviduConfigPro.getSpeechToTextAzureKey(), "*", 6);
                json.addProperty("OPENVIDU_PRO_SPEECH_TO_TEXT_AZURE_KEY", hiddenAzureKey);
                json.addProperty("OPENVIDU_PRO_SPEECH_TO_TEXT_AZURE_REGION", this.openviduConfigPro.getSpeechToTextAzureRegion());
            } else if (SpeechToTextType.aws.equals(this.openviduConfigPro.getSpeechToText())) {
                this.addAwsCommonProperties(json);
            }
        }

        json.addProperty("OPENVIDU_PRO_ELASTICSEARCH", this.openviduConfigPro.isElasticsearchDefined());
        if (this.openviduConfigPro.isElasticsearchDefined()) {
            json.addProperty("OPENVIDU_PRO_ELASTICSEARCH_HOST", this.openviduConfigPro.getElasticsearchHost());
            json.addProperty("OPENVIDU_PRO_ELASTICSEARCH_VERSION", this.openviduConfigPro.getElasticsearchVersion());
        }

        json.addProperty("OPENVIDU_PRO_KIBANA", this.openviduConfigPro.isKibanaDefined());
        if (this.openviduConfigPro.isKibanaDefined()) {
            json.addProperty("OPENVIDU_PRO_KIBANA_HOST", this.openviduConfigPro.getKibanaHost());
            json.addProperty("OPENVIDU_PRO_KIBANA_VERSION", this.openviduConfigPro.getKibanaVersion());
        }

        if (this.openviduConfigPro.isRecordingModuleEnabled()) {
            json.addProperty("OPENVIDU_PRO_RECORDING_STORAGE", this.openviduConfigPro.getRecordingStorage().name());
            if (OpenViduRecordingStorage.s3.equals(this.openviduConfigPro.getRecordingStorage())) {
                json.addProperty("OPENVIDU_PRO_AWS_S3_BUCKET", this.openviduConfigPro.getAwsS3Bucket());
                this.addAwsCommonProperties(json);
                if (this.openviduConfigPro.getAwsS3ServiceEndpoint() != null) {
                    json.addProperty("OPENVIDU_PRO_AWS_S3_SERVICE_ENDPOINT", this.openviduConfigPro.getAwsS3ServiceEndpoint());
                } else {
                    json.add("OPENVIDU_PRO_AWS_S3_SERVICE_ENDPOINT", (JsonElement)null);
                }
            }
        }

        if (this.openviduConfigPro.isMultiMasterEnvironment()) {
            json.addProperty("MULTI_MASTER_REPLICATION_MANAGER_WEBHOOK", this.openviduConfigPro.filterOpenViduSecret(this.openviduConfigPro.getReplicationManagerWebhook()));
            json.addProperty("MULTI_MASTER_NODE_ID", this.openviduConfigPro.getOpenViduProMasterNodeId());
        }

        return new ResponseEntity(json.toString(), response.getHeaders(), response.getStatusCode());
    }

    private void addAwsCommonProperties(JsonObject json) {
        if (!json.has("OPENVIDU_PRO_AWS_REGION")) {
            if (this.openviduConfigPro.getAwsRegion() != null) {
                json.addProperty("OPENVIDU_PRO_AWS_REGION", this.openviduConfigPro.getAwsRegion());
            } else {
                json.add("OPENVIDU_PRO_AWS_REGION", (JsonElement)null);
            }
        }

        String hiddenAwsSecretKey;
        if (!json.has("OPENVIDU_PRO_AWS_ACCESS_KEY")) {
            if (this.openviduConfigPro.getAwsAccessKey() != null) {
                hiddenAwsSecretKey = this.openviduConfigPro.hideSecret(this.openviduConfigPro.getAwsAccessKey(), "*", 4);
                json.addProperty("OPENVIDU_PRO_AWS_ACCESS_KEY", hiddenAwsSecretKey);
            } else {
                json.add("OPENVIDU_PRO_AWS_ACCESS_KEY", (JsonElement)null);
            }
        }

        if (!json.has("OPENVIDU_PRO_AWS_SECRET_KEY")) {
            if (this.openviduConfigPro.getAwsSecretKey() != null) {
                hiddenAwsSecretKey = this.openviduConfigPro.hideSecret(this.openviduConfigPro.getAwsSecretKey(), "*", 4);
                json.addProperty("OPENVIDU_PRO_AWS_SECRET_KEY", hiddenAwsSecretKey);
            } else {
                json.add("OPENVIDU_PRO_AWS_SECRET_KEY", (JsonElement)null);
            }
        }

    }
}
