//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.metrics;

import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeControllerDockerManager;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeProvisioner;
import io.openvidu.server.utils.UpdatableTimerTask;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MediaNodesCpuLoadCollector {
    private static final Logger log = LoggerFactory.getLogger(MediaNodesCpuLoadCollector.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private InfrastructureManager infrastructureManager;
    private MediaNodeProvisioner dockerProvisioner;
    private Map<String, MediaNodeControllerDockerManager> dockerClients = new HashMap();
    private Map<String, Integer> errorCounter = new HashMap();
    private Map<String, UpdatableTimerTask> cpuCollectors = new HashMap();
    private final int ERROR_LIMIT = 4;

    public MediaNodesCpuLoadCollector() {
    }

    public void startCpuCollector(String mediaNodeIp, String mediaNodeId) {
        this.dockerClients.putIfAbsent(mediaNodeId, this.dockerProvisioner.getMediaNodeControllerDockerManager(mediaNodeIp));
        this.errorCounter.putIfAbsent(mediaNodeId, 0);
        this.cpuCollectors.computeIfAbsent(mediaNodeId, (key) -> {
            UpdatableTimerTask timer = new UpdatableTimerTask(new Runnable() {
                public void run() {
                    MediaNodesCpuLoadCollector.this.collectCpuLoad(mediaNodeId);
                }
            }, () -> {
                return (long)(this.openviduConfigPro.getMediaNodeLoadInterval() * 1000);
            });
            timer.updateTimer();
            log.info("CPU load gathering started for Media Node {} at {}. Performing every {} seconds", new Object[]{mediaNodeId, mediaNodeIp, this.openviduConfigPro.getMediaNodeLoadInterval()});
            return timer;
        });
    }

    public void cancelCpuCollector(String mediaNodeId) {
        this.dockerClients.remove(mediaNodeId);
        this.errorCounter.remove(mediaNodeId);
        UpdatableTimerTask timer = (UpdatableTimerTask)this.cpuCollectors.remove(mediaNodeId);
        if (timer != null) {
            timer.cancelTimer();
            log.info("CPU load gathering stopped for Media Node {}", mediaNodeId);
        }

    }

    private void collectCpuLoad(String mediaNodeId) {
        int numErrors = (Integer)this.errorCounter.get(mediaNodeId);
        if (numErrors > 4) {
            log.warn("CPU load gathering process for Media Node {} reached the maximum number of errors in a row. Stopping it", mediaNodeId);
            this.cancelCpuCollector(mediaNodeId);
        } else {
            try {
                MediaNodeControllerDockerManager dockerClient = (MediaNodeControllerDockerManager)this.dockerClients.get(mediaNodeId);
                if (dockerClient == null) {
                    throw new Exception("Docker client not found for instance " + mediaNodeId + " when collecting CPU load");
                }

                Double load = Double.parseDouble(dockerClient.getCpuLoad(9));
                Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
                if (instance == null) {
                    throw new Exception("Instance " + mediaNodeId + " does not exist when collecting CPU load");
                }

                instance.addCpuLoad(load);
                this.errorCounter.put(mediaNodeId, 0);
            } catch (Exception var6) {
                log.error("Exception collecting CPU load of Media Node {}: {}", mediaNodeId, var6.getMessage());
                this.errorCounter.put(mediaNodeId, numErrors + 1);
            }
        }

    }

    @PostConstruct
    public void init() {
        this.dockerProvisioner = new MediaNodeProvisioner(this.openviduConfigPro);
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        this.cpuCollectors.values().forEach((timer) -> {
            if (timer != null) {
                timer.cancelTimer();
            }

        });
    }
}
