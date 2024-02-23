//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.recording.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.JsonObject;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.aws.s3.S3Handler;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.recording.service.RecordingManagerUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class RecordingManagerUtilsS3 extends RecordingManagerUtils {
    private static final Logger log = LoggerFactory.getLogger(RecordingManagerUtilsS3.class);
    final String METADATA_OBJECT_SUFFIX = "/.recording.";

    public RecordingManagerUtilsS3(OpenviduConfig openviduConfig, RecordingManager recordingManager) {
        super(openviduConfig, recordingManager);
    }

    public Recording getRecordingFromStorage(String recordingId) {
        File file = this.recordingManager.getRecordingEntityFileFromLocalStorage(recordingId);
        if (file.exists()) {
            return this.recordingManager.getRecordingFromEntityFile(file);
        } else {
            String entityObjectPath = S3Handler.AWS_BUCKET_PATH + recordingId + "/.recording." + recordingId;
            return this.getRecordingFromEntityObjectPath(entityObjectPath);
        }
    }

    public Set<Recording> getAllRecordingsFromStorage() {
        Set<Recording> localRecordings = this.recordingManager.getAllRecordingsFromLocalStorage();
        List<CompletableFuture<Recording>> futures = new ArrayList();
        Iterator var3 = this.listAllEntityObjectSummariesFromBucket((String)null).iterator();

        while(var3.hasNext()) {
            S3ObjectSummary objectSummary = (S3ObjectSummary)var3.next();
            futures.add(CompletableFuture.supplyAsync(() -> {
                return this.getRecordingFromEntityObjectPath(objectSummary.getKey());
            }));
        }

        CompletableFuture<Recording>[] cfs = (CompletableFuture[])futures.toArray(new CompletableFuture[futures.size()]);

        try {
            Set<Recording> recordings = new HashSet((Collection)CompletableFuture.allOf(cfs).thenApply((ignored) -> {
                return (List)futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            }).get());
            Set<String> remoteRecordingIds = (Set)recordings.stream().map(Recording::getId).collect(Collectors.toSet());
            localRecordings.removeIf((recording) -> {
                return remoteRecordingIds.contains(recording.getId());
            });
            recordings.addAll(localRecordings);
            return recordings;
        } catch (ExecutionException | InterruptedException var6) {
            log.error("Error listing all recordings from S3 bucket {}: {}", S3Handler.AWS_BUCKET_AND_PATH, var6.getMessage());
            return null;
        }
    }

    public HttpStatus deleteRecordingFromStorage(String recordingId) {
        HttpStatus localReturnValue = HttpStatus.NOT_FOUND;
        if (this.deleteRecordingInLocalStorage(recordingId)) {
            localReturnValue = HttpStatus.NO_CONTENT;
        }

        HttpStatus remoteReturnValue = deleteRecordingInS3(recordingId);
        return HttpStatus.NOT_FOUND.equals(remoteReturnValue) ? localReturnValue : remoteReturnValue;
    }

    private boolean deleteRecordingInLocalStorage(String recordingId) {
        if (HttpStatus.NO_CONTENT.equals(this.recordingManager.deleteRecordingFromLocalStorage(recordingId))) {
            log.warn("Recording {} has been deleted from local path even if S3 storage", recordingId);
            return true;
        } else {
            return false;
        }
    }

    public static HttpStatus deleteRecordingInS3(String recordingId) {
        List<String> keyList = (List)S3Handler.getS3Client().listObjectsV2(S3Handler.AWS_BUCKET_NAME, S3Handler.AWS_BUCKET_PATH + recordingId + "/").getObjectSummaries().stream().map((objectSummary) -> {
            return objectSummary.getKey();
        }).collect(Collectors.toList());
        String[] keys = new String[keyList.size()];
        keyList.toArray(keys);
        if (keys.length == 0) {
            return HttpStatus.NOT_FOUND;
        } else {
            DeleteObjectsRequest deleteObjectsRequest = (new DeleteObjectsRequest(S3Handler.AWS_BUCKET_NAME)).withKeys(keys);

            try {
                S3Handler.getS3Client().deleteObjects(deleteObjectsRequest);
                return HttpStatus.NO_CONTENT;
            } catch (MultiObjectDeleteException var5) {
                log.error("MultiObjectDeleteException when deleting recording {} from bucket {}: {}", new Object[]{recordingId, S3Handler.AWS_BUCKET_AND_PATH, var5.getErrorMessage()});
            } catch (AmazonServiceException var6) {
                log.error("AmazonServiceException when deleting recording {} from bucket {}: {}", new Object[]{recordingId, S3Handler.AWS_BUCKET_AND_PATH, var6.getErrorMessage()});
            } catch (SdkClientException var7) {
                log.error("SdkClientException when listing deletingr recording {} from bucket {}: {}", new Object[]{recordingId, S3Handler.AWS_BUCKET_AND_PATH, var7.getMessage()});
            }

            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    protected String getRecordingUrl(Recording recording) {
        OpenviduConfigPro configPro = (OpenviduConfigPro)this.openviduConfig;
        String url;
        if (configPro.getAwsS3ServiceEndpoint() != null) {
            URL serviceEndpointUrl = null;

            try {
                serviceEndpointUrl = new URL(configPro.getAwsS3ServiceEndpoint());
            } catch (MalformedURLException var6) {
            }

            String var10000 = serviceEndpointUrl.getProtocol();
            url = var10000 + "://" + S3Handler.AWS_BUCKET_NAME + "." + serviceEndpointUrl.getAuthority();
        } else {
            url = "https://" + S3Handler.AWS_BUCKET_NAME + ".s3." + S3Handler.AWS_REGION + ".amazonaws.com";
        }

        url = url + "/" + S3Handler.AWS_BUCKET_PATH + recording.getId() + "/" + recording.getName() + "." + this.getExtensionFromRecording(recording);
        return url;
    }

    protected Set<String> getAllRecordingIdsFromStorage(String sessionIdPrefix) {
        Set<String> recordingIds = this.recordingManager.getAllRecordingIdsFromLocalStorage();
        this.listAllEntityObjectSummariesFromBucket(sessionIdPrefix).forEach((object) -> {
            recordingIds.add(object.getKey().substring(object.getKey().lastIndexOf("/.recording.") + "/.recording.".length()));
        });
        return recordingIds;
    }

    private Recording getRecordingFromEntityObjectPath(String entityObjectPath) {
        S3Object fullObject = null;

        try {
            fullObject = S3Handler.getS3Client().getObject(new GetObjectRequest(S3Handler.AWS_BUCKET_NAME, entityObjectPath));
        } catch (AmazonS3Exception var4) {
            if (var4.getStatusCode() == 404) {
                return null;
            }
        }

        Recording recording = this.getRecordingFromS3EntityObject(fullObject);
        if (recording == null) {
            log.error("Error reading recording entity file from S3 bucket {} with path {}", S3Handler.AWS_BUCKET_NAME, entityObjectPath);
        }

        return recording;
    }

    private List<S3ObjectSummary> listAllEntityObjectSummariesFromBucket(String additionalPrefix) {
        if (additionalPrefix == null) {
            additionalPrefix = "";
        }

        List<S3ObjectSummary> list = new ArrayList();

        try {
            ListObjectsV2Request req = (new ListObjectsV2Request()).withBucketName(S3Handler.AWS_BUCKET_NAME).withPrefix(S3Handler.AWS_BUCKET_PATH + additionalPrefix).withMaxKeys(Integer.MAX_VALUE);

            ListObjectsV2Result result;
            do {
                result = S3Handler.getS3Client().listObjectsV2(req);
                list.addAll((Collection)result.getObjectSummaries().stream().filter((obj) -> {
                    return obj.getKey().contains("/.recording.");
                }).collect(Collectors.toSet()));
                String token = result.getNextContinuationToken();
                req.setContinuationToken(token);
            } while(result.isTruncated());
        } catch (AmazonServiceException var6) {
            log.error("AmazonServiceException when listing objects from bucket {}. S3 couldn't process the call: {}", S3Handler.AWS_BUCKET_NAME, var6.getErrorMessage());
        } catch (SdkClientException var7) {
            log.error("SdkClientException when listing objects from bucket {}. S3 couldn't be contacted for a response or the client couldn't parse the response from S3: {}", S3Handler.AWS_BUCKET_NAME, var7.getMessage());
        }

        return list;
    }

    private Recording getRecordingFromS3EntityObject(S3Object fullObject) {
        InputStreamReader input = null;
        Reader reader = null;

        Object var6;
        try {
            input = new InputStreamReader(fullObject.getObjectContent());
            reader = new BufferedReader(input);
            JsonObject recordingJson = this.jsonUtils.fromReaderToJsonObject(reader);
            return this.recordingManager.getRecordingFromJson(recordingJson);
        } catch (IOException var16) {
            log.error("Error reading recording entity file from S3 bucket {}", S3Handler.AWS_BUCKET_NAME);
            var6 = null;
        } finally {
            try {
                input.close();
                reader.close();
            } catch (IOException var15) {
                var15.printStackTrace();
            }

        }

        return (Recording)var6;
    }
}
