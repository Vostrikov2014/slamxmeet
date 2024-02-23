//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.kms;

import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.kurento.kms.KmsProperties;
import io.openvidu.server.kurento.kms.LoadManager;
import io.openvidu.server.account.ExceededCoresException;
import io.openvidu.server.cdr.CallDetailRecordPro;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.PublicIpAutodiscovery;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.infrastructure.InstanceType;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeControllerDockerManager;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeProvisioner;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.kurento.client.KurentoClient;
import org.kurento.commons.exception.KurentoException;
import org.springframework.beans.factory.annotation.Autowired;

public class MultipleKmsManager extends KmsManager {
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private InfrastructureManager infrastructureManager;
    @Autowired
    private CallDetailRecord CDR;

    public MultipleKmsManager(SessionManager sessionManager, LoadManager loadManager) {
        super(sessionManager, loadManager);
    }

    public List<Kms> initializeKurentoClients(List<KmsProperties> kmsProperties, boolean disconnectUponFailure) throws Exception {
        CountDownLatch latch = new CountDownLatch(kmsProperties.size());
        AtomicLong numberOfConnectedKmss = new AtomicLong(0L);
        List<Kms> successfullyConnectedKmss = new ArrayList();
        Iterator var6 = kmsProperties.iterator();

        while(var6.hasNext()) {
            KmsProperties kmsProperty = (KmsProperties)var6.next();
            KurentoClient kClient = null;

            try {
                Kms kms = new Kms(kmsProperty, this.loadManager, this);
                kClient = this.createKurentoClient(kms.getId(), kmsProperty.getUri());
                kms.setKurentoClient(kClient);

                try {
                    kms.setKurentoClientConnected(true, false);
                } catch (ExceededCoresException var13) {
                    log.error("Error adding Media Node {}: {}", var13.getKms().getId(), var13.getClass().getSimpleName());
                    Instance i = this.infrastructureManager.getInstance(var13.getKms().getId());
                    if (i != null) {
                        this.infrastructureManager.modifyMediaNode(InstanceStatus.canceled, i, false);
                    }

                    this.addKms(var13.getKms());
                    this.infrastructureManager.removeMediaNode(var13.getKms().getId(), "now", false, false, false);
                    latch.countDown();
                    continue;
                }

                this.addKms(kms);
                successfullyConnectedKmss.add(kms);
                numberOfConnectedKmss.incrementAndGet();
                latch.countDown();
            } catch (KurentoException var14) {
                log.error("OpenVidu Server couldn't connect to KMS with uri {}", kmsProperty.getUri());
                if (kClient != null) {
                    kClient.destroy();
                }

                latch.countDown();
            }
        }

        List<String> kmsUris = (List)kmsProperties.stream().map(KmsProperties::getUri).collect(Collectors.toList());

        try {
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                log.error("Timeout while waiting for KurentoClients to connect to KMSs");
            }
        } catch (InterruptedException var12) {
            log.error("Error while waiting for KurentoClients to connect to KMSs: {}", var12.getMessage());
        }

        if (kmsUris.isEmpty()) {
            log.info("No KMS uris were defined");
        } else if (numberOfConnectedKmss.get() != (long)kmsUris.size()) {
            if (numberOfConnectedKmss.get() == 0L) {
                log.error("None of the KMSs in {} are within reach of OpenVidu Server", kmsUris);
                throw new Exception();
            }

            log.error("Not all KMSs in {} are within reach of OpenVidu Server. Only the following KMSs are: {}", kmsUris, this.kmss.values());
            if (disconnectUponFailure) {
                log.warn("Disconnecting from successfully connected KMSs: {}", successfullyConnectedKmss);
                successfullyConnectedKmss.forEach((kmsx) -> {
                    this.removeKms(kmsx.getId()).getKurentoClient().destroy();
                    if (this.openviduConfigPro != null && this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
                        this.openviduConfigPro.getCoturnConfig().removeCoturnIp(kmsx.getUri());
                    }

                    if (this.openviduConfigPro != null && this.openviduConfigPro.getRecordingsConfig().isRecordingComposedExternal()) {
                        this.openviduConfigPro.getRecordingsConfig().removeRecordingsVolumePathByKmsUri(kmsx.getUri());
                    }

                });
                throw new Exception();
            }

            log.info("Only some KMSs in {} are within reach of OpenVidu Server: {}", kmsUris, successfullyConnectedKmss);
        } else {
            log.info("All KMSs in {} are within reach of OpenVidu Server", kmsUris);
        }

        return this.processSuccessfullyConnectedKmss(successfullyConnectedKmss);
    }

    public List<Kms> processSuccessfullyConnectedKmss(List<Kms> successfullyConnectedKmss) {
        Iterator it;
        Kms kms;
        if (!successfullyConnectedKmss.isEmpty()) {
            try {
                if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                    try {
                        it = successfullyConnectedKmss.iterator();

                        while(it.hasNext()) {
                            kms = (Kms)it.next();
                            Instance instance = this.infrastructureManager.getInstance(kms.getId());
                            InstanceStatus oldStatus = instance.getStatus();
                            if (InstanceStatus.canceled.equals(oldStatus)) {
                                this.infrastructureManager.removeMediaNode(instance.getId(), "now", false, false, true);
                                it.remove();
                            } else {
                                instance.setStatus(InstanceStatus.running);
                                ((CallDetailRecordPro)this.CDR).recordMediaNodeStatusChanged(kms.getTimeOfKurentoClientConnection(), kms, instance.getEnvironmentId(), InstanceStatus.running, oldStatus);
                            }
                        }
                    } finally {
                        KmsManager.selectAndRemoveKmsLock.unlock();
                    }
                } else {
                    log.error("selectAndRemoveKmsLock couldn't be acquired within {} seconds to change the status of successfully connected Media Nodes", 15);
                    log.error("The following Media Nodes will be disconnected and (if possible) dropped: {}", successfullyConnectedKmss);
                    this.cleanConnectedMediaNodes(successfullyConnectedKmss);
                }
            } catch (InterruptedException var19) {
                log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock to change the status of successfully connected Media Nodes: {}", var19.getMessage());
                log.error("The following Media Nodes will be disconnected and (if possible) dropped: {}", successfullyConnectedKmss);
                this.cleanConnectedMediaNodes(successfullyConnectedKmss);
            }
        }

        String kmsUri;
        if (this.openviduConfigPro != null && this.openviduConfigPro.getCoturnConfig().isDeployedOnMediaNodes()) {
            PublicIpAutodiscovery autodiscoverIpMode = this.openviduConfigPro.getMediaNodePublicIpAutodiscoveryMode();

            Kms kms;
            for(Iterator var21 = successfullyConnectedKmss.iterator(); var21.hasNext(); this.openviduConfigPro.getCoturnConfig().putCoturnIp(kms.getUri(), kmsUri)) {
                kms = (Kms)var21.next();
                MediaNodeProvisioner dockerProvisioner = new MediaNodeProvisioner(this.openviduConfigPro);
                MediaNodeControllerDockerManager dockerClient = dockerProvisioner.getMediaNodeControllerDockerManager(kms.getIp());
                kmsUri = null;

                try {
                    kmsUri = dockerClient.getPublicIp(autodiscoverIpMode);
                } catch (IOException var17) {
                    log.error("Error while auto discovering public ip of {}", kms.getIp());
                }

                if (kmsUri == null) {
                    log.warn("Public IP of the media node can not be auto discovered in media node. Clients may have problems connecting to media nodes");
                }
            }
        }

        if (this.openviduConfigPro != null && this.openviduConfigPro.isRecordingComposedExternal()) {
            it = successfullyConnectedKmss.iterator();

            while(it.hasNext()) {
                kms = (Kms)it.next();
                MediaNodeProvisioner dockerProvisioner = new MediaNodeProvisioner(this.openviduConfigPro);
                MediaNodeControllerDockerManager dockerClient = dockerProvisioner.getMediaNodeControllerDockerManager(kms.getIp());
                String mediaNodeIp = kms.getIp();
                kmsUri = kms.getUri();
                Instance instance = this.infrastructureManager.getInstanceByIp(mediaNodeIp);
                if (instance != null) {
                    String mediaNodeId = instance.getId();
                    String recordingsVolumePath = null;

                    try {
                        recordingsVolumePath = dockerClient.getRecordingsVolumePath();
                    } catch (IOException var16) {
                        log.error("Error while getting recordings volume path from: {}", kms.getIp());
                    }

                    this.openviduConfigPro.getRecordingsConfig().putRecordingsVolumePath(kmsUri, mediaNodeId, recordingsVolumePath);
                }
            }
        }

        return successfullyConnectedKmss;
    }

    private void cleanConnectedMediaNodes(List<Kms> successfullyConnectedKmss) {
        successfullyConnectedKmss.forEach((kms) -> {
            this.infrastructureManager.removeMediaNode(kms.getId(), "now", false, false, true);
            Instance instance = this.infrastructureManager.getInstance(kms.getId());
            this.infrastructureManager.recordMediaNodeEvent(instance, (String)null, InstanceStatus.failed, instance.getStatus());
        });
    }

    @PostConstruct
    protected void postConstructInitKurentoClients() {
        if (!this.openviduConfigPro.isCluster() || this.openviduConfigPro.isCluster() && OpenViduClusterMode.manual.equals(this.openviduConfigPro.getClusterMode())) {
            if (this.openviduConfigPro.isCluster()) {
                log.info("OpenVidu Server Pro is deployed with 'OPENVIDU_PRO_CLUSTER_MODE' set to 'manual'. Initializing Media Nodes defined in parameter 'KMS_URIS': {}", this.openviduConfig.getKmsUris());
            }

            Map<String, String> forceKmsUrisToHaveKmsIds = new HashMap();
            this.openviduConfig.getKmsUris().forEach((kmsUrix) -> {
                String parsedUri = kmsUrix.replaceAll("^ws://", "http://").replaceAll("^wss://", "https://");
                String ip = null;

                try {
                    ip = (new URL(parsedUri)).toURI().getHost();
                } catch (URISyntaxException | MalformedURLException var6) {
                    var6.printStackTrace();
                }

                Instance instance = new Instance(InstanceType.mediaServer, "media_" + ip);
                instance.setIp(ip);
                this.infrastructureManager.addInstance(instance);
                forceKmsUrisToHaveKmsIds.put(kmsUrix, instance.getId());
                ((CallDetailRecordPro)this.CDR).recordMediaNodeStatusChanged(instance, kmsUrix, InstanceStatus.launching, (InstanceStatus)null);
            });
            List<Kms> newKmss = new ArrayList();
            List<KmsProperties> kmsProps = new ArrayList();
            Iterator var4 = this.openviduConfig.getKmsUris().iterator();

            while(var4.hasNext()) {
                String kmsUri = (String)var4.next();
                String kmsId = (String)forceKmsUrisToHaveKmsIds.get(kmsUri);
                kmsProps.add(new KmsProperties(kmsId, kmsUri));
            }

            try {
                newKmss = this.initializeKurentoClients(kmsProps, true);
            } catch (Exception var7) {
                log.error("Shutting down OpenVidu Server");
                Runtime.getRuntime().halt(1);
            }

            Set<String> newKmssIds = (Set)((List)newKmss).stream().map(Kms::getId).collect(Collectors.toSet());
            Set<String> instancesToRemove = (Set)forceKmsUrisToHaveKmsIds.values().stream().filter((id) -> {
                return !newKmssIds.contains(id);
            }).collect(Collectors.toSet());
            instancesToRemove.forEach((instanceId) -> {
                this.infrastructureManager.removeInstance(instanceId);
            });
        } else {
            log.info("OpenVidu Server Pro is deployed with 'OPENVIDU_PRO_CLUSTER_MODE' set to 'auto'. Ignoring uris defined in 'KMS_URIS'");
        }

    }

    public void incrementActiveRecordings(RecordingProperties properties, String recordingId, Session session) throws OpenViduException {
        String mediaNodeId = this.infrastructureManager.getMediaNodeIdForRecordingOrBroadcast(properties, session);

        try {
            if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                try {
                    Kms kms = this.getKms(mediaNodeId);
                    if (kms == null) {
                        throw new OpenViduException(Code.MEDIA_NODE_NOT_FOUND, "Media Node " + mediaNodeId + " does not exist");
                    }

                    kms.incrementActiveRecordings(session.getSessionId(), recordingId, properties);
                    log.info("Incremented active recordings in Media Node {}. New total: {} ({})", new Object[]{mediaNodeId, kms.getActiveRecordings().size(), kms.getActiveRecordings()});
                    if (!this.infrastructureManager.isMediaNodeAvailableForRecordingOrBroadcast(mediaNodeId)) {
                        throw new OpenViduException(Code.MEDIA_NODE_STATUS_WRONG, "Media Node " + mediaNodeId + " does not allow starting new recordings");
                    }
                } finally {
                    KmsManager.selectAndRemoveKmsLock.unlock();
                }

            } else {
                throw new OpenViduException(Code.GENERIC_ERROR_CODE, "selectAndRemoveKmsLock couldn't be acquired within 15 seconds  when incrementing active recordings of Media Node " + mediaNodeId);
            }
        } catch (InterruptedException var10) {
            throw new OpenViduException(Code.GENERIC_ERROR_CODE, "InterruptedException waiting to acquire selectAndRemoveKmsLock when incrementing active recordings of Media Node " + mediaNodeId + ": " + var10.getMessage());
        }
    }

    public void decrementActiveRecordings(RecordingProperties properties, String recordingId, Session session) throws OpenViduException {
        String mediaNodeId = this.infrastructureManager.getMediaNodeIdForRecordingOrBroadcast(properties, session);

        try {
            if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                try {
                    Kms kms = this.getKms(mediaNodeId);
                    if (kms != null) {
                        kms.decrementActiveRecordings(recordingId, properties);
                        log.info("Decremented active recordings in Media Node {}. Remaining: {}", mediaNodeId, kms.getActiveRecordings());
                    } else {
                        log.warn("Trying to decrement active recordings of Media Node {} but cannot be found", mediaNodeId);
                    }
                } finally {
                    KmsManager.selectAndRemoveKmsLock.unlock();
                }

            } else {
                throw new OpenViduException(Code.GENERIC_ERROR_CODE, "selectAndRemoveKmsLock couldn't be acquired within 15 seconds  when decrementing active recordings of Media Node " + mediaNodeId);
            }
        } catch (InterruptedException var10) {
            throw new OpenViduException(Code.GENERIC_ERROR_CODE, "InterruptedException waiting to acquire selectAndRemoveKmsLock when decrementing active recordings of Media Node " + mediaNodeId + ": " + var10.getMessage());
        }
    }

    public void incrementActiveBroadcasts(RecordingProperties properties, Session session) {
        String mediaNodeId = this.infrastructureManager.getMediaNodeIdForRecordingOrBroadcast(properties, session);

        try {
            if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                try {
                    Kms kms = this.getKms(mediaNodeId);
                    if (kms == null) {
                        throw new OpenViduException(Code.MEDIA_NODE_NOT_FOUND, "Media Node " + mediaNodeId + " does not exist");
                    }

                    kms.incrementActiveBroadcasts(session.getSessionId());
                    log.info("Incremented active braodcasts in Media Node {}. New total: {} ({})", new Object[]{mediaNodeId, kms.getActiveBroadcasts().size(), kms.getActiveBroadcasts()});
                    if (!this.infrastructureManager.isMediaNodeAvailableForRecordingOrBroadcast(mediaNodeId)) {
                        throw new OpenViduException(Code.MEDIA_NODE_STATUS_WRONG, "Media Node " + mediaNodeId + " does not allow starting new broadcasts");
                    }
                } finally {
                    KmsManager.selectAndRemoveKmsLock.unlock();
                }

            } else {
                throw new OpenViduException(Code.GENERIC_ERROR_CODE, "selectAndRemoveKmsLock couldn't be acquired within 15 seconds  when incrementing active broadcasts of Media Node " + mediaNodeId);
            }
        } catch (InterruptedException var9) {
            throw new OpenViduException(Code.GENERIC_ERROR_CODE, "InterruptedException waiting to acquire selectAndRemoveKmsLock when incrementing active broadcasts of Media Node " + mediaNodeId + ": " + var9.getMessage());
        }
    }

    public void decrementActiveBroadcasts(RecordingProperties properties, Session session) {
        String mediaNodeId = this.infrastructureManager.getMediaNodeIdForRecordingOrBroadcast(properties, session);

        try {
            if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                try {
                    Kms kms = this.getKms(mediaNodeId);
                    if (kms != null) {
                        kms.decrementActiveBroadcasts(session.getSessionId());
                        log.info("Decremented active broadcasts in Media Node {}. Remaining: {}", mediaNodeId, kms.getActiveBroadcasts());
                    } else {
                        log.warn("Trying to decrement active broadcasts of Media Node {} but cannot be found", mediaNodeId);
                    }
                } finally {
                    KmsManager.selectAndRemoveKmsLock.unlock();
                }

            } else {
                throw new OpenViduException(Code.GENERIC_ERROR_CODE, "selectAndRemoveKmsLock couldn't be acquired within 15 seconds  when decrementing active broadcasts of Media Node " + mediaNodeId);
            }
        } catch (InterruptedException var9) {
            throw new OpenViduException(Code.GENERIC_ERROR_CODE, "InterruptedException waiting to acquire selectAndRemoveKmsLock when decrementing active broadcasts of Media Node " + mediaNodeId + ": " + var9.getMessage());
        }
    }

    public void removeMediaNodeUponCrash(String mediaNodeId) {
        log.warn("Removing Media Node {} after node crash", mediaNodeId);
        this.infrastructureManager.removeMediaNode(mediaNodeId, "now", false, false, false);
    }

    protected String getEnvironmentId(String mediaNodeId) {
        String environmentId = null;
        Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
        if (instance != null) {
            environmentId = instance.getEnvironmentId();
        }

        return environmentId;
    }
}
