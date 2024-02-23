//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.autoscaling;

import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.pro.cdr.CallDetailRecordPro;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.InstanceStatus;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.utils.UpdatableTimerTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoscalingApplier {
    private static final Logger log = LoggerFactory.getLogger(AutoscalingApplier.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private InfrastructureManager infrastructureManager;
    @Autowired
    private CallDetailRecord CDR;
    private AutoscalingManager autoscalingManager = new AutoscalingManager();
    private UpdatableTimerTask autoscalingTimer;
    private ExecutorService autoscalingLaunchInstancesExecutor = Executors.newCachedThreadPool();

    public AutoscalingApplier() {
    }

    @PostConstruct
    private void initAutoscaling() {
        if (this.openviduConfigPro.isAutoscaling()) {
            this.autoscalingTimer = new UpdatableTimerTask(() -> {
                log.debug("Running autoscaling task...");

                try {
                    if (KmsManager.selectAndRemoveKmsLock.tryLock(15L, TimeUnit.SECONDS)) {
                        try {
                            List<Node> nodes = new ArrayList();
                            List<Instance> unmodifiableInstances = Collections.unmodifiableList(new ArrayList(this.infrastructureManager.getInstances()));
                            unmodifiableInstances.forEach((i) -> {
                                nodes.add(this.instanceToNode(i));
                            });
                            SystemStatus status = new SystemStatus(nodes);
                            AutoscalingConfig config = new AutoscalingConfig(this.openviduConfigPro.getAutoscalingMaxNodes(), this.openviduConfigPro.getAutoscalingMinNodes(), (double)this.openviduConfigPro.getAutoscalingMaxAvgLoad(), (double)this.openviduConfigPro.getAutoscalingMinAvgLoad());
                            AutoscalingResult result = this.autoscalingManager.evalAutoscaling(status, config);
                            if (result.isDoNothing()) {
                                log.debug(config.toString());
                                log.debug(status.toString());
                                log.debug("Autoscaling doing nothing");
                            } else {
                                log.info("Autoscaling modifiyng cluster");
                                log.info(config.toString());
                                log.info(status.toString());
                                log.info(result.toString());
                            }

                            if (!result.isDoNothing()) {
                                this.applyAutoscalingResult(result);
                            }
                        } catch (RuntimeException var10) {
                            log.error("Exception on autsocaling task: {}", var10.getMessage());
                        } finally {
                            KmsManager.selectAndRemoveKmsLock.unlock();
                        }
                    } else {
                        log.error("Autoscaling task wasn't able to acquire selectAndRemoveKmsLock in {} seconds. Next autoscaling itaration will try again", 15);
                    }
                } catch (InterruptedException var12) {
                    log.error("InterruptedException waiting to acquire selectAndRemoveKmsLock when running autoscaling task. Next autoscaling iteration will try again: {}", var12.getMessage());
                }

            }, () -> {
                return (long)(this.openviduConfigPro.getAutoscalingInterval() * 1000);
            });
            this.autoscalingTimer.updateTimer();
            log.info("Autoscaling loop initialized. Current period set to {} seconds", this.openviduConfigPro.getAutoscalingInterval());
        }

    }

    private Node instanceToNode(Instance instance) {
        return new Node(instance.getCpuLoad(), InstanceStatus.valueOf(instance.getStatus().name()), instance.getLaunchingTime(), instance.getId(), instance.getEnvironmentId(), instance.getIp());
    }

    private void applyAutoscalingResult(AutoscalingResult result) {
        ((CallDetailRecordPro)this.CDR).recordAutoscalingEvent(result, this.openviduConfigPro.getClusterId());
        result.getRelaunchWaitingIdleToTerminateNodes().forEach((node) -> {
            Instance instance = this.infrastructureManager.getInstance(node.getInstanceId());
            this.infrastructureManager.modifyMediaNode(InstanceStatus.running, instance, false);
        });
        result.getRelaunchCanceledNodes().forEach((node) -> {
            Instance instance = this.infrastructureManager.getInstance(node.getInstanceId());
            this.infrastructureManager.modifyMediaNode(InstanceStatus.launching, instance, false);
        });
        result.getTerminateLaunchingNodes().forEach((node) -> {
            Instance instance = this.infrastructureManager.getInstance(node.getInstanceId());
            this.infrastructureManager.modifyMediaNode(InstanceStatus.canceled, instance, false);
        });
        result.getNodesToTerminate().forEach((node) -> {
            this.infrastructureManager.removeMediaNode(node.getInstanceId(), "when-no-sessions", false, false, true);
        });
        if (OpenViduClusterMode.auto.equals(this.openviduConfigPro.getClusterMode())) {
            List<Callable<Instance>> launchInstanceThreads = new ArrayList();

            for(int i = 0; i < result.getNumNodesToLaunch(); ++i) {
                launchInstanceThreads.add(() -> {
                    return this.infrastructureManager.asyncNewMediaNode();
                });
            }

            try {
                this.autoscalingLaunchInstancesExecutor.invokeAll(launchInstanceThreads);
            } catch (InterruptedException var4) {
                log.error("Some new media node thread was interrupted while waiting to be executed in the thread pool: {}", var4.getMessage());
            }
        }

    }

    @PreDestroy
    public void close() {
        if (this.autoscalingTimer != null) {
            this.autoscalingTimer.cancelTimer();
        }

        if (this.autoscalingLaunchInstancesExecutor != null) {
            this.autoscalingLaunchInstancesExecutor.shutdown();
        }

    }
}
