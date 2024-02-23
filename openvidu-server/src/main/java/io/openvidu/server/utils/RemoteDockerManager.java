//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.utils;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeControllerDockerManager;
import io.openvidu.server.infrastructure.mncontroller.MediaNodeProvisioner;
import io.openvidu.server.utils.DockerManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteDockerManager implements DockerManager {
    private static final Logger log = LoggerFactory.getLogger(RemoteDockerManager.class);
    private MediaNodeProvisioner provisioner;
    private InfrastructureManager infrastructureManager;
    private OpenviduConfigPro openviduConfigPro;

    public RemoteDockerManager(OpenviduConfigPro openviduConfigPro, InfrastructureManager infrastructureManager) {
        this.infrastructureManager = infrastructureManager;
        this.openviduConfigPro = openviduConfigPro;
        this.provisioner = new MediaNodeProvisioner(openviduConfigPro);
    }

    public DockerManager init() {
        return this;
    }

    public String runContainer(String mediaNodeId, String image, String containerName, String user, List<Volume> volumes, List<Bind> binds, String networkMode, List<String> envs, List<String> command, Long shmSize, boolean privileged, Map<String, String> labels, boolean autoremove, boolean enableGPU) throws Exception {
        String var10001 = this.openviduConfigPro.getOpenviduRecordingImageRepo();
        if (image.equals(var10001 + ":" + this.openviduConfigPro.getOpenViduRecordingVersion()) && OpenViduClusterEnvironment.docker.equals(this.openviduConfigPro.getClusterEnvironment())) {
            envs = this.replaceLayoutUrlForDinD(envs);
        }

        return this.getMncDockerManager(mediaNodeId).runContainer(image, containerName, user, volumes, binds, networkMode, envs, command, shmSize, privileged, labels, autoremove, enableGPU);
    }

    public String runContainer(String mediaNodeId, String image, String containerName, String user, List<Volume> volumes, List<Bind> binds, String networkMode, List<String> envs, List<String> command, Long shmSize, boolean privileged, Map<String, String> labels, boolean enableGPU) throws Exception {
        return this.runContainer(mediaNodeId, image, containerName, user, volumes, binds, networkMode, envs, command, shmSize, privileged, labels, true, enableGPU);
    }

    public void removeContainer(String mediaNodeId, String containerId, boolean force) {
        this.getMncDockerManager(mediaNodeId).removeContainer(containerId, force);
    }

    public void runCommandInContainerSync(String mediaNodeId, String containerId, String command, int secondsOfWait) throws IOException {
        this.getMncDockerManager(mediaNodeId).runCommandInContainerSync(containerId, command, secondsOfWait);
    }

    public void runCommandInContainerAsync(String mediaNodeId, String containerId, String command) throws IOException {
        this.getMncDockerManager(mediaNodeId).runCommandInContainerAsync(containerId, command);
    }

    public void waitForContainerStopped(String mediaNodeId, String containerId, int secondsOfWait) throws Exception {
        this.getMncDockerManager(mediaNodeId).waitForContainerStopped(containerId, secondsOfWait);
    }

    public void waitForFileToExistAndNotEmpty(String mediaNodeId, String absolutePath) throws IOException {
        this.getMncDockerManager(mediaNodeId).waitForFileNotEmpty(absolutePath, this.maxSecondsWaitForFile());
    }

    public String waitForContainerLog(String mediaNodeId, String containerId, String expectedLog, boolean expectedLogIsRegex, String forbiddenLog, boolean forbiddenLogIsRegex, int secondsTimeout) throws IOException {
        String logLine = this.getMncDockerManager(mediaNodeId).waitForContainerLog(containerId, expectedLog, expectedLogIsRegex, forbiddenLog, forbiddenLogIsRegex, secondsTimeout);
        return logLine;
    }

    private MediaNodeControllerDockerManager getMncDockerManager(String mediaNodeId) {
        Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
        if (instance != null) {
            return this.provisioner.getMediaNodeControllerDockerManager(instance.getIp());
        } else {
            throw new OpenViduException(Code.MEDIA_NODE_NOT_FOUND, "Media Node " + mediaNodeId + " does not exist");
        }
    }

    private List<String> replaceLayoutUrlForDinD(List<String> envs) {
        String ENV_NAME = "URL=";
        String DOCKER_IP = "172.17.0.1";
        String initialUrlEnv = (String)envs.stream().filter((env) -> {
            return env.startsWith("URL=");
        }).findFirst().orElse((Object)null);
        String modifiedUrlEnv = initialUrlEnv.replaceFirst("URL=", "");
        String newUrlEnv = null;

        try {
            URIBuilder builder = new URIBuilder(modifiedUrlEnv);
            if (builder.getHost().startsWith("172.")) {
                log.info("Running in DinD environment. The URL {} is already a Docker IP. Skipping IP replacement", modifiedUrlEnv);
            } else {
                builder.setHost("172.17.0.1");
                newUrlEnv = "URL=" + builder.build().toString();
                envs.remove(initialUrlEnv);
                envs.add(newUrlEnv);
                log.info("Running in DinD environment. Replacing IP in layout URL to use Docker IP: {}", newUrlEnv);
            }
        } catch (URISyntaxException var8) {
            var8.printStackTrace();
        }

        return envs;
    }

    public int maxSecondsWaitForFile() {
        return OpenViduClusterEnvironment.docker.equals(this.openviduConfigPro.getClusterEnvironment()) ? 100 : 30;
    }
}
