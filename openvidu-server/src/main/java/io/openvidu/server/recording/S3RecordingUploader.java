//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.recording;

import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import io.openvidu.java.client.Recording.Status;
import io.openvidu.server.aws.s3.S3Handler;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.recording.service.RecordingManagerUtilsS3;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.recording.RecordingUploader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class S3RecordingUploader implements RecordingUploader {
    private static final Logger log = LoggerFactory.getLogger(S3RecordingUploader.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    private Map<String, Boolean> uploadingRecordings = new ConcurrentHashMap();

    public S3RecordingUploader() {
    }

    public void uploadRecording(Recording recording, Runnable successCallback, Runnable errorCallback) {
        recording.setStatus(Status.stopped);
        (new Thread(() -> {
            String recId = recording.getId();
            log.info("Uploading recording {} to S3 bucket path {}", recId, S3Handler.AWS_BUCKET_AND_PATH);
            TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(S3Handler.getS3Client()).build();
            String var10002 = this.openviduConfigPro.getOpenViduRecordingPath();
            File recordingFolder = new File(var10002 + recId);
            String pathInBucket = S3Handler.AWS_BUCKET_PATH + recId;

            label47: {
                try {
                    MultipleFileUpload transfer = transferManager.uploadDirectory(S3Handler.AWS_BUCKET_NAME, pathInBucket, recordingFolder, true, (file, meta) -> {
                        this.openviduConfigPro.getAwsS3Headers().entrySet().forEach((header) -> {
                            meta.setHeader((String)header.getKey(), header.getValue());
                        });
                    });
                    transfer.waitForCompletion();
                    log.info("Recording {} successfully uploaded to S3 bucket path {}", recId, S3Handler.AWS_BUCKET_AND_PATH);
                    break label47;
                } catch (Exception var12) {
                    log.error("Error uploading recording {} to S3 bucket path {}: {}", new Object[]{recId, S3Handler.AWS_BUCKET_AND_PATH, var12.getMessage()});
                    if (HttpStatus.NO_CONTENT.equals(RecordingManagerUtilsS3.deleteRecordingInS3(recId))) {
                        log.warn("Some partial files were deleted from S3 bucket path {} for recording {}", S3Handler.AWS_BUCKET_AND_PATH, recId);
                    }

                    this.uploadingRecordings.remove(recId);
                    errorCallback.run();
                } finally {
                    transferManager.shutdownNow(false);
                }

                return;
            }

            this.deleteRecordingFolder(recordingFolder);
            log.info("Recording {} deleted from local storage", recId);
            this.uploadingRecordings.remove(recId);
            recording.setStatus(Status.ready);
            successCallback.run();
        })).start();
    }

    public void storeAsUploadingRecording(String recordingId) {
        this.uploadingRecordings.putIfAbsent(recordingId, true);
    }

    public boolean isBeingUploaded(String recordingId) {
        return this.uploadingRecordings.containsKey(recordingId);
    }

    private void deleteRecordingFolder(File recordingFolder) {
        try {
            FileUtils.deleteDirectory(recordingFolder);
        } catch (IOException var3) {
            log.error("Couldn't delete recording folder {}", recordingFolder.getAbsolutePath());
        }

    }
}
