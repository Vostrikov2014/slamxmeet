//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account;

import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.account.usage.ClusterUsageInfo;
import io.openvidu.server.account.usage.KmsNodeUsageInfo;
import io.openvidu.server.account.usage.NodeUsageInfo;
import io.openvidu.server.account.usage.NodeUsageInfoFactory;
import io.openvidu.server.account.usage.OpenviduNodeUsageInfo;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.SpecialLicenseConfig;
import io.openvidu.server.infrastructure.InstanceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.kurento.client.KurentoClient;
import org.kurento.client.ServerInfo;
import org.kurento.client.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ClusterUsageService {
    private static final Logger log = LoggerFactory.getLogger(ClusterUsageService.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private OpenviduBuildInfo openviduBuildInfo;
    private Map<String, NodeUsageInfo> nodeUsageEntries = new ConcurrentHashMap();
    private NodeUsageInfoFactory nodeUsageInfoFactory = new NodeUsageInfoFactory();
    final Deque<ClusterUsageInfo> failedUsages = new ConcurrentLinkedDeque();
    int currentUsageIntervalSleepSeconds;

    public ClusterUsageService() {
    }

    public synchronized ClusterUsageInfo obtainUsage() {
        long TIMESTAMP = System.currentTimeMillis();
        int totalQuantity = 0;
        Collection<NodeUsageInfo> mediaNodesCopiesReturnValue = new ArrayList();
        Iterator<Map.Entry<String, NodeUsageInfo>> it = this.nodeUsageEntries.entrySet().iterator();

        while(it.hasNext()) {
            NodeUsageInfo info = (NodeUsageInfo)((Map.Entry)it.next()).getValue();
            if (info.getQuantity() != -1L) {
                totalQuantity = (int)((long)totalQuantity + info.getQuantity());
                if (!info.isReconnected()) {
                    it.remove();
                }
            } else {
                info.setEndTime(TIMESTAMP);
                totalQuantity = (int)((long)totalQuantity + this.calculateQuantity(info));
            }

            NodeUsageInfo copy = this.nodeUsageInfoFactory.copyNodeUsageInfo(info);
            mediaNodesCopiesReturnValue.add(copy);
        }

        ClusterUsageInfo clusterUsageInfo = new ClusterUsageInfo(this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getClusterEnvironment(), (long)totalQuantity, TIMESTAMP, mediaNodesCopiesReturnValue, this.openviduConfigPro.getOpenViduEdition());
        if (this.openviduConfigPro.isMonoNode()) {
            Collection<NodeUsageInfo> monoNodeNodesCopiesReturnValue = (Collection)mediaNodesCopiesReturnValue.stream().filter((nodeUsageInfo) -> {
                return InstanceType.openvidu.equals(nodeUsageInfo.getType());
            }).collect(Collectors.toList());
            long monoNodeTotalQuantity = monoNodeNodesCopiesReturnValue.stream().mapToLong(NodeUsageInfo::getQuantity).sum();
            clusterUsageInfo = new ClusterUsageInfo(this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getClusterEnvironment(), monoNodeTotalQuantity, TIMESTAMP, monoNodeNodesCopiesReturnValue, this.openviduConfigPro.getOpenViduEdition());
        }

        this.resetUsage(TIMESTAMP + 1L);
        return clusterUsageInfo;
    }

    public synchronized ClusterUsageInfo obtainFirstUsage() {
        String masterNodeId = "opv_" + this.openviduConfigPro.getClusterId();
        NodeUsageInfo masterNodeUsageInfo = (NodeUsageInfo)this.nodeUsageEntries.get(masterNodeId);
        Collection<NodeUsageInfo> nodes = Arrays.asList(masterNodeUsageInfo);
        ClusterUsageInfo initialUsage = new ClusterUsageInfo(this.openviduConfigPro.getClusterId(), this.openviduConfigPro.getClusterEnvironment(), 0L, System.currentTimeMillis(), nodes, this.openviduConfigPro.getOpenViduEdition());
        initialUsage.asInitialUsage();
        return initialUsage;
    }

    private void resetUsage(long newInitTime) {
        this.nodeUsageEntries.values().forEach((info) -> {
            info.setInitTime(newInitTime);
            info.setEndTime(-1L);
            info.setQuantity(-1L);
            info.setReconnected(false);
        });
    }

    public void registerMediaNode(Kms kms, long timeOfConnection, String environmentId, Collection<Kms> kmss, boolean nodeRecovered) throws CpuCountException, ExceededCoresException {
        String mediaNodeId = kms.getId();
        String mediaNodeIp = kms.getIp();
        KmsNodeUsageInfo nodeUsage;
        if (nodeRecovered) {
            nodeUsage = (KmsNodeUsageInfo)this.nodeUsageEntries.get(mediaNodeId);
            if (nodeUsage != null) {
                nodeUsage.setReconnected(true);
                if (!this.mustOverwriteMediaNodeUsageAfterRecover(nodeUsage, timeOfConnection)) {
                    return;
                }
            }
        }

        int cores = true;
        KurentoClient kurentoClient = kms.getKurentoClient();
        if (kurentoClient != null) {
            ServerManager serverManager = kurentoClient.getServerManager();
            int cores = serverManager.getCpuCount();
            ServerInfo serverInfo = serverManager.getInfo();
            nodeUsage = new KmsNodeUsageInfo(mediaNodeId, environmentId, mediaNodeIp, cores, timeOfConnection, serverInfo);
            int totalCores = this.totalCores(kmss);
            int newCores = nodeUsage.getCores();
            if (this.openviduConfigPro.isLicenseOffline() && totalCores + newCores > SpecialLicenseConfig.numCores) {
                throw new ExceededCoresException(kms, SpecialLicenseConfig.numCores, totalCores, newCores);
            } else {
                this.nodeUsageEntries.put(mediaNodeId, nodeUsage);
                log.info("Media Node {} registered into usage service", mediaNodeId);
            }
        } else {
            String errorMessage = "No KurentoClient available for Kms " + mediaNodeId + ". Cannot get CPU count";
            log.error(errorMessage);
            throw new CpuCountException(errorMessage);
        }
    }

    public synchronized void deregisterMediaNode(String mediaNodeId, long timeOfDisconnection) {
        NodeUsageInfo mediaNodeCoreInfo = (NodeUsageInfo)this.nodeUsageEntries.get(mediaNodeId);
        if (mediaNodeCoreInfo != null) {
            if (mediaNodeCoreInfo.getEndTime() == -1L) {
                mediaNodeCoreInfo.setEndTime(timeOfDisconnection);
                this.calculateQuantity(mediaNodeCoreInfo);
                log.info("Media Node {} deregistered from usage service", mediaNodeId);
            } else {
                log.info("Media Node {} was deregistered from usage service {} seconds ago", mediaNodeId, timeOfDisconnection - mediaNodeCoreInfo.getEndTime());
            }
        } else {
            log.info("Media Node {} not registered in usage service", mediaNodeId);
        }

    }

    protected long calculateQuantity(NodeUsageInfo mediaNodeCoreInfo) {
        long totalQuantity = calculateQuantity(mediaNodeCoreInfo.getEndTime(), mediaNodeCoreInfo.getInitTime(), mediaNodeCoreInfo.getCores());
        mediaNodeCoreInfo.setQuantity(totalQuantity);
        return totalQuantity;
    }

    public static long calculateQuantity(long endTimeMillis, long initTimeMillis, int cores) {
        return (endTimeMillis - initTimeMillis) / 60000L * (long)cores;
    }

    @PostConstruct
    protected void init() {
        String id = "opv_" + this.openviduConfigPro.getClusterId();
        int cores = Runtime.getRuntime().availableProcessors();
        NodeUsageInfo openviduNodeUsage = new OpenviduNodeUsageInfo(id, id, this.openviduConfigPro.getMasterNodeIp(), cores, System.currentTimeMillis(), this.openviduBuildInfo.getVersion());
        this.nodeUsageEntries.put(openviduNodeUsage.getId(), openviduNodeUsage);
    }

    private int totalCores(Collection<Kms> kmss) {
        int cores = 0;
        Iterator<Kms> it = kmss.iterator();

        while(it.hasNext()) {
            String mediaNodeId = ((Kms)it.next()).getId();
            if (this.nodeUsageEntries.containsKey(mediaNodeId)) {
                cores += ((NodeUsageInfo)this.nodeUsageEntries.get(mediaNodeId)).getCores();
            }
        }

        cores += ((NodeUsageInfo)this.nodeUsageEntries.values().stream().filter((info) -> {
            return info.getType().equals(InstanceType.openvidu);
        }).findFirst().get()).getCores();
        return cores;
    }

    private boolean mustOverwriteMediaNodeUsageAfterRecover(KmsNodeUsageInfo nodeUsage, long timeOfConnectionRecovery) {
        if (this.currentUsageIntervalSleepSeconds == 0) {
            return true;
        } else {
            long lastStoredQuantity = nodeUsage.getQuantity();
            if (lastStoredQuantity == -1L) {
                return true;
            } else {
                long lastTimeObtainUsageWasCalled = ((NodeUsageInfo)this.nodeUsageEntries.values().stream().filter((info) -> {
                    return info.getType().equals(InstanceType.openvidu);
                }).findFirst().get()).getInitTime();
                long nextTimeObtainUsageWillBeCall = lastTimeObtainUsageWasCalled + (long)(this.currentUsageIntervalSleepSeconds * 1000);
                KmsNodeUsageInfo auxInfo = new KmsNodeUsageInfo((String)null, (String)null, (String)null, nodeUsage.getCores(), timeOfConnectionRecovery, (ServerInfo)null);
                auxInfo.setEndTime(nextTimeObtainUsageWillBeCall);
                long possibleNextQuantity = this.calculateQuantity(auxInfo);
                return possibleNextQuantity > lastStoredQuantity;
            }
        }
    }

    public void setCurrentUsageIntervalSleepSeconds(int seconds) {
        this.currentUsageIntervalSleepSeconds = seconds;
    }
}
