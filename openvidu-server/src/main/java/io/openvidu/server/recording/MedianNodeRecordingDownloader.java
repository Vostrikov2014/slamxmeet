//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.recording;

import io.openvidu.java.client.RecordingProperties;
import io.openvidu.java.client.Recording.OutputMode;
import io.openvidu.java.client.Recording.Status;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.recording.RecorderEndpointWrapper;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.recording.RecordingDownloader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MedianNodeRecordingDownloader implements RecordingDownloader {
    private static final Logger log = LoggerFactory.getLogger(MedianNodeRecordingDownloader.class);
    final String KMS_RECORDINGS_PATH = "/media-node/recordings/";
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private InfrastructureManager infrastructureManager;
    private final Map<String, Collection<Thread>> downloadThreads = new ConcurrentHashMap();
    private final int MEDIA_NODE_CONTROLLER_PORT = 3000;

    public MedianNodeRecordingDownloader() {
    }

    public void downloadRecording(Recording recording, Collection<RecorderEndpointWrapper> wrappers, Runnable callback) throws IOException {
        if (!this.openviduConfigPro.isCluster()) {
            callback.run();
        } else {
            String mediaNodeIp = this.infrastructureManager.getMediaNodeIpForRecording(recording);
            log.info("Downloading recording {} from {}", recording.getId(), mediaNodeIp);
            String httpEndpoint = "http://" + mediaNodeIp + ":3000/media-node/recordings/";
            this.downloadThreads.put(recording.getId(), new ArrayList());
            if (OutputMode.INDIVIDUAL.equals(recording.getOutputMode())) {
                this.downloadMultipleFiles(httpEndpoint, recording, (List)wrappers.stream().map((w) -> {
                    return w.getNameWithExtension();
                }).collect(Collectors.toList()), callback);
            } else if (RecordingProperties.IS_COMPOSED(recording.getOutputMode())) {
                if (!recording.hasVideo()) {
                    this.downloadSingleFile(httpEndpoint, recording, recording.getName() + ".webm", callback);
                } else {
                    if (!this.openviduConfigPro.isRecordingComposedExternal()) {
                        callback.run();
                        return;
                    }

                    List<String> files = List.of(recording.getName() + ".mp4", recording.getId() + ".info", recording.getId() + ".jpg", ".recording." + recording.getId());
                    this.downloadMultipleFiles(httpEndpoint, recording, files, callback);
                }
            }

        }
    }

    public void cancelDownload(String recordingId) {
        if (this.downloadThreads.containsKey(recordingId)) {
            Iterator var2 = ((Collection)this.downloadThreads.get(recordingId)).iterator();

            while(var2.hasNext()) {
                Thread thread = (Thread)var2.next();
                thread.interrupt();
            }
        } else {
            log.warn("There were no download threads available for recording {}", recordingId);
        }

    }

    private void downloadSingleFile(final String httpEndpoint, final Recording recording, final String fileNameWithExtension, final Runnable callback) {
        Thread downloadThread = new Thread() {
            public void run() {
                try {
                    MedianNodeRecordingDownloader.this.downloadRecordingFile(httpEndpoint, fileNameWithExtension, recording);
                } catch (IOException var6) {
                    MedianNodeRecordingDownloader.log.error(var6.getMessage());
                    recording.setStatus(Status.failed);
                } catch (InterruptedException var7) {
                    MedianNodeRecordingDownloader.log.error(var7.getMessage());
                    recording.setStatus(Status.failed);
                } finally {
                    MedianNodeRecordingDownloader.this.downloadThreads.remove(recording.getId());
                    callback.run();
                }

            }
        };
        ((Collection)this.downloadThreads.get(recording.getId())).add(downloadThread);
        downloadThread.start();
    }

    private void downloadMultipleFiles(final String httpEndpoint, final Recording recording, List<String> files, final Runnable callback) {
        int MINUTES_WAIT = true;
        Collection<Thread> downloadFileThreads = new HashSet();
        final CountDownLatch downloadLatch = new CountDownLatch(files.size());
        Thread joinThread = new Thread() {
            public void run() {
                try {
                    if (!downloadLatch.await(15L, TimeUnit.MINUTES)) {
                        MedianNodeRecordingDownloader.log.error("The download process of all files of INDIVIDUAL recording {} couldn't be completed in {} minutes", recording.getId(), 15);
                        recording.setStatus(Status.failed);
                    }
                } catch (InterruptedException var5) {
                    MedianNodeRecordingDownloader.log.error("Exception while waiting for all files of recording {} to be downloaded: {}", recording.getId(), var5.getMessage());
                    recording.setStatus(Status.failed);
                } finally {
                    MedianNodeRecordingDownloader.this.downloadThreads.remove(recording.getId());
                    callback.run();
                }

            }
        };
        ((Collection)this.downloadThreads.get(recording.getId())).add(joinThread);
        joinThread.start();
        Iterator var9 = files.iterator();

        while(var9.hasNext()) {
            final String fileNameWithExtension = (String)var9.next();
            Thread downloadThread = new Thread() {
                public void run() {
                    try {
                        MedianNodeRecordingDownloader.this.downloadRecordingFile(httpEndpoint, fileNameWithExtension, recording);
                        downloadLatch.countDown();
                    } catch (IOException var6) {
                        MedianNodeRecordingDownloader.log.error(var6.getMessage());
                        recording.setStatus(Status.failed);
                    } catch (InterruptedException var7) {
                        MedianNodeRecordingDownloader.log.error(var7.getMessage());
                        recording.setStatus(Status.failed);
                    } finally {
                        if (MedianNodeRecordingDownloader.this.downloadThreads.containsKey(recording.getId()) && this != null) {
                            ((Collection)MedianNodeRecordingDownloader.this.downloadThreads.get(recording.getId())).remove(this);
                        }

                    }

                }
            };
            downloadFileThreads.add(downloadThread);
            ((Collection)this.downloadThreads.get(recording.getId())).add(downloadThread);
        }

        downloadFileThreads.forEach((t) -> {
            t.start();
        });
    }

    private void downloadRecordingFile(String httpEndpoint, String fileName, Recording recording) throws IOException, InterruptedException {
        httpEndpoint = httpEndpoint + recording.getId() + "/";
        log.info("Downloading file from {}", httpEndpoint + fileName);
        String var10000 = this.openviduConfigPro.getOpenViduRecordingPath();
        String outputFileInLocalHost = var10000 + recording.getId() + "/" + fileName;
        URL url = null;
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel fileChannel = null;

        try {
            url = new URL(httpEndpoint + fileName);
            urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", this.getBasicAuth());
            urlConnection.connect();
            long fileSize = urlConnection.getContentLengthLong();
            log.info("File {} is {} KB in size", httpEndpoint + fileName, fileSize / 1024L);
            inputStream = urlConnection.getInputStream();
            outputStream = new FileOutputStream(outputFileInLocalHost);
            byte[] buffer = new byte[1024];
            int bytesRead = false;

            int bytesRead;
            while((bytesRead = inputStream.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                outputStream.write(buffer, 0, bytesRead);
            }

            log.info("File successfully downloaded from {}. Deleting it", httpEndpoint + fileName);
            this.deleteRecordingFile(httpEndpoint + fileName);
        } catch (MalformedURLException var19) {
            log.error("Endpoint [{}] is not a valid URL", httpEndpoint + fileName);
            throw var19;
        } catch (FileNotFoundException var20) {
            log.error("Cannot create a FileOutputStream to file {}: {}", outputFileInLocalHost, var20.getMessage());
            throw var20;
        } catch (IOException var21) {
            log.error("Error while downloading recording from {}: {}", httpEndpoint + fileName, var21.getMessage());
            throw var21;
        } finally {
            if (fileChannel != null) {
                ((FileChannel)fileChannel).close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (inputStream != null) {
                inputStream.close();
            }

        }

    }

    private void deleteRecordingFile(String uri) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Authorization", this.getBasicAuth());
        ResponseHandler<String> responseHandler = (response) -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
            } else {
                throw new ClientProtocolException("Unexpected response status. Expected 204, actual " + status);
            }
        };

        try {
            httpclient.execute(httpDelete, responseHandler);
            log.info("File located at {} successfully deleted", uri);
        } catch (Exception var14) {
            log.error("Error while deleting file at {}: {}", uri, var14.getMessage());
        } finally {
            try {
                httpclient.close();
            } catch (IOException var13) {
                log.error("Error while closing HttpClient to delete file at {}", uri);
            }

        }

    }

    private String getBasicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(("OPENVIDUAPP:" + this.openviduConfigPro.getOpenViduSecret()).getBytes());
    }
}
