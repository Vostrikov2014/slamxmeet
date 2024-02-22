//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import io.openvidu.server.recording.OpenViduRecordingStorage;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordingsConfig {
    private static final Logger log = LoggerFactory.getLogger(RecordingsConfig.class);
    private OpenViduRecordingStorage recordingStorage;
    private boolean recordingComposedExternal;
    private String masterRecordingsVolumePath;
    private ConcurrentHashMap<String, String> mapRecordingsVolumePathByKmsUri = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> mapRecordingsVolumePathByMNodeId = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> mapKmsUriByMNodeId = new ConcurrentHashMap();

    public RecordingsConfig(OpenViduRecordingStorage recordingStorage, boolean recordingComposedExternal, String masterRecordingsVolumePath) {
        this.recordingStorage = recordingStorage;
        this.recordingComposedExternal = recordingComposedExternal;
        this.masterRecordingsVolumePath = masterRecordingsVolumePath;
    }

    public OpenViduRecordingStorage getRecordingStorage() {
        return this.recordingStorage;
    }

    public boolean isRecordingComposedExternal() {
        return this.recordingComposedExternal;
    }

    public String getRecordingsVolumePath(String key) {
        if (this.isRecordingComposedExternal()) {
            String recordingsVolumePath = (String)this.mapRecordingsVolumePathByKmsUri.get(key);
            if (recordingsVolumePath == null) {
                recordingsVolumePath = (String)this.mapRecordingsVolumePathByMNodeId.get(key);
            }

            return recordingsVolumePath;
        } else {
            return this.masterRecordingsVolumePath;
        }
    }

    public void putRecordingsVolumePath(String kmsUri, String mediaNodeId, String recordingsVolumePath) {
        log.info("Adding as Recording Volume the path: '{}' to Kms uri: '{}'", recordingsVolumePath, kmsUri);
        this.mapRecordingsVolumePathByKmsUri.put(kmsUri, recordingsVolumePath);
        this.mapRecordingsVolumePathByMNodeId.put(mediaNodeId, recordingsVolumePath);
        this.mapKmsUriByMNodeId.put(kmsUri, mediaNodeId);
    }

    public void removeRecordingsVolumePathByKmsUri(String kmsUri) {
        if (kmsUri != null) {
            log.info("Removing Recording Volume for Kms uri: '{}'", kmsUri);
            String mediaNodeId = (String)this.mapKmsUriByMNodeId.get(kmsUri);
            this.mapRecordingsVolumePathByKmsUri.remove(kmsUri);
            this.mapKmsUriByMNodeId.remove(kmsUri);
            if (mediaNodeId != null) {
                this.mapRecordingsVolumePathByMNodeId.remove(mediaNodeId);
            }
        }

    }
}
