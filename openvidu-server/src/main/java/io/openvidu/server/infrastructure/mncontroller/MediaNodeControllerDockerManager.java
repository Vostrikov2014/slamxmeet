//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.OpenViduException;
import io.openvidu.server.config.OpenviduBuildInfo;
import io.openvidu.server.config.DockerRegistryConfig;
import io.openvidu.server.config.PublicIpAutodiscovery;
import io.openvidu.server.infrastructure.mncontroller.model.DockerContainerCreateOptions;
import io.openvidu.server.infrastructure.mncontroller.model.DockerDeviceRequests;
import io.openvidu.server.infrastructure.mncontroller.model.DockerHostConfig;
import io.openvidu.server.infrastructure.mncontroller.model.DockerUlimit;
import io.openvidu.server.infrastructure.mncontroller.model.LogConfig;
import io.openvidu.server.infrastructure.mncontroller.model.RestartPolicy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaNodeControllerDockerManager {
    private static final Logger log = LoggerFactory.getLogger(MediaNodeControllerDockerManager.class);
    private final String MEDIA_NODE_CONTROLLER_PATH = "media-node/";
    private final int DOCKER_PULL_TIMEOUT = 600000;
    private int DOCKER_TIMEOUT;
    private String mediaNodeControllerUrl;
    protected String mediaNodeIp;
    protected final String basicAuthPassword;

    public MediaNodeControllerDockerManager(String nodeIp, String basicAuthPassword, int dockerTimeout) {
        this.mediaNodeIp = nodeIp;
        this.mediaNodeControllerUrl = "http://" + nodeIp + ":3000/";
        this.basicAuthPassword = basicAuthPassword;
        this.DOCKER_TIMEOUT = dockerTimeout * 1000;
    }

    public void initMediaNodeController() throws OpenViduException {
        this.initMediaNodeController((OpenviduBuildInfo)null, (List)null);
    }

    public void initMediaNodeController(OpenviduBuildInfo openviduBuildInfo, List<DockerRegistryConfig> dockerRegistryConfigList) throws OpenViduException {
        boolean reachable = this.checkUrlExists(this.mediaNodeControllerUrl);
        if (!reachable) {
            throw new OpenViduException(Code.GENERIC_ERROR_CODE, "Media Node controller " + this.mediaNodeControllerUrl + " is not reachable");
        } else {
            log.info("Media Node Controller is reachable through: {}", this.mediaNodeControllerUrl);

            try {
                if (openviduBuildInfo != null) {
                    String openviduProVersion = openviduBuildInfo.getVersion();
                    Boolean checkVersion = this.checkVersion(openviduProVersion);
                    log.info("Media Node Controller version is correct");
                    if (checkVersion == null || !checkVersion) {
                        throw new OpenViduException(Code.GENERIC_ERROR_CODE, "Media Node controller deployed version is not compatible with OpenVidu: " + openviduProVersion + ". If you have upgraded, check your media nodes are updated.");
                    }
                }
            } catch (IOException var8) {
                throw new OpenViduException(Code.GENERIC_ERROR_CODE, var8.getMessage());
            }

            if (dockerRegistryConfigList != null) {
                Iterator var9 = dockerRegistryConfigList.iterator();

                while(var9.hasNext()) {
                    DockerRegistryConfig dockerRegistryConfig = (DockerRegistryConfig)var9.next();

                    try {
                        this.authToDockerRegistry(dockerRegistryConfig);
                    } catch (IOException var7) {
                        OpenViduException.Code var10002 = Code.GENERIC_ERROR_CODE;
                        String var10003 = dockerRegistryConfig.getServerAddress();
                        throw new OpenViduException(var10002, "Error logging in to private registry : '" + var10003 + "' :" + var7.getMessage());
                    }
                }
            }

        }
    }

    public Boolean checkVersion(String version) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        Boolean var6;
        label48: {
            Object var7;
            try {
                String PATH = "checkVersion/" + version;
                HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/" + PATH);
                httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
                httpget.setHeader("Authorization", this.getBasicAuth());
                log.info("Checking media-node-controller version: {}", version);
                log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
                ResponseHandler<String> responseHandler = this.getStringResponseHandler();

                try {
                    var6 = Boolean.parseBoolean((String)httpclient.execute(httpget, responseHandler));
                    break label48;
                } catch (HttpResponseException var9) {
                    if (var9.getStatusCode() != 404) {
                        throw var9;
                    }
                }

                var7 = null;
            } catch (Throwable var10) {
                if (httpclient != null) {
                    try {
                        httpclient.close();
                    } catch (Throwable var8) {
                        var10.addSuppressed(var8);
                    }
                }

                throw var10;
            }

            if (httpclient != null) {
                httpclient.close();
            }

            return (Boolean)var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public List<String> getRunningContainers(String fullImageName) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        List var10;
        try {
            String PATH = "getRunningContainers/";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getRunningContainers/");
            httpget.setHeader("Authorization", this.getBasicAuth());
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            log.info("Getting running containers in {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            String jsonResponse = (String)httpclient.execute(httpget, responseHandler);
            Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
            DockerContainerCreateOptions[] containers = (DockerContainerCreateOptions[])gson.fromJson(jsonResponse, DockerContainerCreateOptions[].class);
            List<String> containerIds = (List)Arrays.asList(containers).stream().filter((container) -> {
                return container.getImage().equals(fullImageName);
            }).map((container) -> {
                return container.getId();
            }).collect(Collectors.toList());
            var10 = containerIds;
        } catch (Throwable var12) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }
            }

            throw var12;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var10;
    }

    public String getContainerIdByName(String containerName) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var6;
        label48: {
            Object var7;
            try {
                String PATH = "getContainerIdByName/" + containerName;
                HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/" + PATH);
                httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
                httpget.setHeader("Authorization", this.getBasicAuth());
                log.info("Getting container Id of container with name: {}", containerName);
                log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
                ResponseHandler<String> responseHandler = this.getStringResponseHandler();

                try {
                    var6 = (String)httpclient.execute(httpget, responseHandler);
                    break label48;
                } catch (HttpResponseException var9) {
                    if (var9.getStatusCode() != 404) {
                        throw var9;
                    }
                }

                var7 = null;
            } catch (Throwable var10) {
                if (httpclient != null) {
                    try {
                        httpclient.close();
                    } catch (Throwable var8) {
                        var10.addSuppressed(var8);
                    }
                }

                throw var10;
            }

            if (httpclient != null) {
                httpclient.close();
            }

            return (String)var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public Map<String, String> getLabelsByName(String containerName) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        Map var9;
        label50: {
            HashMap var7;
            try {
                String PATH = "getLabelsFromContainerNameOrId/" + containerName;
                HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/" + PATH);
                httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
                httpget.setHeader("Authorization", this.getBasicAuth());
                log.info("Getting container labels of container with name: {}", containerName);
                log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
                ResponseHandler<String> responseHandler = this.getStringResponseHandler();

                try {
                    String result = (String)httpclient.execute(httpget, responseHandler);
                    Gson gson = (new GsonBuilder()).create();
                    Map<String, String> labels = (Map)gson.fromJson(result, Map.class);
                    var9 = labels;
                    break label50;
                } catch (HttpResponseException var11) {
                    if (var11.getStatusCode() != 404) {
                        throw var11;
                    }
                } catch (Exception var12) {
                    throw var12;
                }

                log.warn("Container {} did not existed when getting its labels: {}", containerName, var11.getMessage());
                var7 = new HashMap();
            } catch (Throwable var13) {
                if (httpclient != null) {
                    try {
                        httpclient.close();
                    } catch (Throwable var10) {
                        var13.addSuppressed(var10);
                    }
                }

                throw var13;
            }

            if (httpclient != null) {
                httpclient.close();
            }

            return var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var9;
    }

    public String getContainerIp(String containerId) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var6;
        try {
            String PATH = "getContainerIp/";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getContainerIp/" + containerId);
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting container ip of '{}' in {}", containerId, this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var6 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var8) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public void checkDockerEnabled() throws OpenViduException {
        try {
            this.dockerImageExists("hello-world");
            log.info("Docker is installed and enabled");
        } catch (IOException var2) {
            throw new OpenViduException(Code.DOCKER_NOT_FOUND, "Error while checking Docker. Please be sure to run media-node-controller");
        }
    }

    public boolean dockerImageExists(String image) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        boolean var10;
        try {
            DockerContainerCreateOptions options = new DockerContainerCreateOptions(image);
            Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
            String PATH = "checkImageExists/";
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/checkImageExists/");
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(600000).setSocketTimeout(600000).build());
            String json = gson.toJson(options);
            StringEntity stringEntity = new StringEntity(json);
            httpPost.setEntity(stringEntity);
            log.info("Checking if Image '{}' exists in {}", image, this.mediaNodeIp);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var10 = Boolean.parseBoolean((String)httpclient.execute(httpPost, responseHandler));
        } catch (Throwable var12) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var11) {
                    var12.addSuppressed(var11);
                }
            }

            throw var12;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var10;
    }

    public void downloadDockerImage(String image) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            DockerContainerCreateOptions options = new DockerContainerCreateOptions(image);
            String PATH = "pullDockerImage/";
            Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/pullDockerImage/");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(600000).setSocketTimeout(600000).build());
            String json = gson.toJson(options);
            StringEntity stringEntity = new StringEntity(json);
            httpPost.setEntity(stringEntity);
            log.info("Pulling Image '{}' in {}", image, this.mediaNodeIp);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var11) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var10) {
                    var11.addSuppressed(var10);
                }
            }

            throw var11;
        }

        if (httpclient != null) {
            httpclient.close();
        }

    }

    public String runContainer(String image, String containerName, String user, List<Volume> volumes, List<Bind> binds, String networkMode, List<String> envs, List<String> command, Long shmSize, boolean privileged, Map<String, String> labels, boolean autoremove, boolean enableGPU) throws Exception {
        List<String> volumesString = (List)binds.stream().map((bind) -> {
            String var10000 = bind.getPath();
            return var10000 + ":" + bind.getVolume();
        }).collect(Collectors.toList());
        return this.runContainerAux(image, containerName, user, volumesString, networkMode, envs, command, labels, (LogConfig)null, autoremove, new RestartPolicy("no"), enableGPU, (List)null);
    }

    public String runContainerAux(String image, String containerName, String user, List<String> volumes, String networkMode, List<String> envs, List<String> command, Map<String, String> labels, LogConfig logConfig, boolean autoremove, RestartPolicy restartPolicy, boolean enableGPU, List<Pair<String, String>> portBindingList) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var23;
        try {
            List<DockerUlimit> uLimit = Collections.singletonList(new DockerUlimit("core", -1, -1));
            DockerHostConfig dockerHostConfig;
            if (enableGPU) {
                List<DockerDeviceRequests> deviceRequests = ImmutableList.of(new DockerDeviceRequests(-1, ImmutableList.of(ImmutableList.of("gpu"))));
                dockerHostConfig = new DockerHostConfig(autoremove, networkMode, volumes, uLimit, logConfig, restartPolicy, deviceRequests);
            } else {
                dockerHostConfig = new DockerHostConfig(autoremove, networkMode, volumes, uLimit, logConfig, restartPolicy);
            }

            if (portBindingList != null) {
                Iterator var26 = portBindingList.iterator();

                while(var26.hasNext()) {
                    Pair<String, String> portBinding = (Pair)var26.next();
                    dockerHostConfig.addPortBinding(portBinding);
                }
            }

            DockerContainerCreateOptions containerOptions = new DockerContainerCreateOptions(image, containerName, envs, user, dockerHostConfig, command, labels);
            if (portBindingList != null) {
                Iterator var28 = portBindingList.iterator();

                while(var28.hasNext()) {
                    Pair<String, String> portBinding = (Pair)var28.next();
                    containerOptions.addExposedPorts((String)portBinding.getRight());
                }
            }

            log.info("Running container '{}' in {}", image, this.mediaNodeIp);
            Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/");
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            String json = gson.toJson(containerOptions);
            StringEntity stringEntity = new StringEntity(json);
            httpPost.setEntity(stringEntity);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var23 = (String)httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var25) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var24) {
                    var25.addSuppressed(var24);
                }
            }

            throw var25;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var23;
    }

    public void removeContainer(String containerNameOrId, boolean force) {
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();

            try {
                String PATH = "removeContainer/";
                URIBuilder builder = new URIBuilder(this.mediaNodeControllerUrl);
                builder.setPath("media-node/removeContainer/" + containerNameOrId);
                builder.setParameter("throwError", "false");
                HttpDelete httpDelete = new HttpDelete(builder.build());
                httpDelete.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
                httpDelete.setHeader("Authorization", this.getBasicAuth());
                log.info("Removing container '{}' in {}", containerNameOrId, this.mediaNodeIp);
                log.info("Media Node Controller HTTP-DELETE request: " + httpDelete.getRequestLine());
                ResponseHandler<String> responseHandler = this.getStringResponseHandler();
                httpclient.execute(httpDelete, responseHandler);
            } catch (Throwable var9) {
                if (httpclient != null) {
                    try {
                        httpclient.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }
                }

                throw var9;
            }

            if (httpclient != null) {
                httpclient.close();
            }
        } catch (IOException var10) {
            log.error("Error sending request to Media Node Controller: " + var10.getMessage());
        } catch (URISyntaxException var11) {
            var11.printStackTrace();
        }

    }

    public void waitForContainerStopped(String containerNameOrId, int secondsOfWait) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            String PATH = "waitContainer/";
            log.info("Waiting for container stopped '{}' in {}", containerNameOrId, this.mediaNodeIp);
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/waitContainer/" + containerNameOrId);
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Authorization", this.getBasicAuth());
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var8) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (httpclient != null) {
            httpclient.close();
        }

    }

    public String getPublicIp(PublicIpAutodiscovery autodiscoverIpMode) throws IOException {
        if (PublicIpAutodiscovery.AUTO_IPV4.equals(autodiscoverIpMode)) {
            return this.getPublicIpv4();
        } else {
            return PublicIpAutodiscovery.AUTO_IPV4.equals(autodiscoverIpMode) ? this.getPublicIpv6() : null;
        }
    }

    public String getPublicIpv4() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "getPublicIpv4";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getPublicIpv4");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting Public Ipv4 from {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    public String getPublicIpv6() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "getPublicIpv6";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getPublicIpv6");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting Public Ipv6 from {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    public String getDockerGatewayIp() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "dockerGatewayIp";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/dockerGatewayIp");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting Docker Gateway Ip");
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    public boolean isContainerRunning(String containerNameOrId) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        boolean var6;
        try {
            String PATH = "isContainerRunning/" + containerNameOrId;
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/" + PATH);
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Checking if container '{}' is running in {}", containerNameOrId, this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var6 = Boolean.parseBoolean((String)httpclient.execute(httpget, responseHandler));
        } catch (Throwable var8) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public String getEnvVariable(String envVariableName) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var6;
        try {
            String PATH = "getEnvVariable/" + envVariableName;
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/" + PATH);
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting env variable from media-node-controller with ip {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var6 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var8) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public String getCpuLoad(int avgFromLastSeconds) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var6;
        try {
            String PATH = "cpu";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/cpu");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.debug("Getting cpu load from media-node-controller with ip {}", this.mediaNodeIp);
            log.debug("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var6 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var8) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var6;
    }

    public String isServiceAvailable() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "status/";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/status/");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Checking if media-node-controller is available for Http Requests {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    public String getOpenViduIpForMediaNode() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "getRequestIp/";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getRequestIp/");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting OpenVidu IP from Media Node Request {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    public void checkImages(List<String> images) throws IOException {
        Iterator var2 = images.iterator();

        while(var2.hasNext()) {
            String image = (String)var2.next();

            try {
                if (!this.dockerImageExists(image)) {
                    this.downloadDockerImage(image);
                }
            } catch (Exception var5) {
                log.error("Error downloading docker image {}", image);
                throw var5;
            }
        }

    }

    private ResponseHandler<String> getStringResponseHandler() throws HttpResponseException {
        return (response) -> {
            String message = null;

            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    message = EntityUtils.toString(entity, "UTF-8");
                }

                if (message == null || message.isBlank()) {
                    message = response.getStatusLine().getReasonPhrase();
                }
            } catch (Throwable var3) {
                message = response.getStatusLine().getReasonPhrase();
            }

            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return message;
            } else {
                throw new HttpResponseException(status, message);
            }
        };
    }

    private String getBasicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(("OPENVIDUAPP:" + this.basicAuthPassword).getBytes());
    }

    private boolean checkUrlExists(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection huc = (HttpURLConnection)url.openConnection();
            huc.setConnectTimeout(2500);
            huc.setReadTimeout(2500);
            huc.setRequestMethod("HEAD");
            int responseCode = huc.getResponseCode();
            return 404 == responseCode;
        } catch (Exception var5) {
            return false;
        }
    }

    public void runCommandInContainerAsync(String containerNameOrId, String command) throws IOException {
        this.runCommandInContainerCommon(containerNameOrId, command, (Integer)null);
    }

    public void runCommandInContainerSync(String containerNameOrId, String command, int secondsOfWait) throws IOException {
        this.runCommandInContainerCommon(containerNameOrId, command, secondsOfWait);
    }

    private void runCommandInContainerCommon(String containerNameOrId, String command, Integer secondsOfWait) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            String PATH = "runCommandInContainer/";
            log.info("Running command in container '{}' in {}", containerNameOrId, this.mediaNodeIp);
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/runCommandInContainer/" + containerNameOrId);
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            JsonObject json = new JsonObject();
            json.addProperty("command", command);
            if (secondsOfWait != null) {
                json.addProperty("secondsTimeout", secondsOfWait);
            }

            StringEntity stringEntity = null;

            try {
                stringEntity = new StringEntity(json.toString());
            } catch (UnsupportedEncodingException var11) {
            }

            httpPost.setEntity(stringEntity);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var12) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var10) {
                    var12.addSuppressed(var10);
                }
            }

            throw var12;
        }

        if (httpclient != null) {
            httpclient.close();
        }

    }

    public void waitForFileNotEmpty(String absolutePathToFile, int secondsOfWait) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            String PATH = "waitForFile/";
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/waitForFile/");
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(secondsOfWait * 1000).setConnectionRequestTimeout(secondsOfWait * 1000).setSocketTimeout(secondsOfWait * 1000).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            JsonObject json = new JsonObject();
            json.addProperty("absolutePath", absolutePathToFile);
            json.addProperty("secondsTimeout", secondsOfWait);
            StringEntity stringEntity = null;

            try {
                stringEntity = new StringEntity(json.toString());
            } catch (UnsupportedEncodingException var10) {
            }

            httpPost.setEntity(stringEntity);
            log.info("Waiting for file {} to exist and not be empty at Media Node {}", absolutePathToFile, this.mediaNodeIp);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var11) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var9) {
                    var11.addSuppressed(var9);
                }
            }

            throw var11;
        }

        if (httpclient != null) {
            httpclient.close();
        }

    }

    public String waitForContainerLog(String containerNameOrId, String expectedLog, boolean expectedLogIsRegex, String forbiddenLog, boolean forbiddenLogIsRegex, int secondsTimeout) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var13;
        try {
            String PATH = "waitForContainerLog/";
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/waitForContainerLog/" + containerNameOrId);
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setConnectionRequestTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            JsonObject json = new JsonObject();
            json.addProperty("expectedLog", expectedLog);
            json.addProperty("expectedLogIsRegex", expectedLogIsRegex);
            if (forbiddenLog != null) {
                json.addProperty("forbiddenLog", forbiddenLog);
                json.addProperty("forbiddenLogIsRegex", forbiddenLogIsRegex);
            }

            json.addProperty("secondsTimeout", secondsTimeout);
            StringEntity stringEntity = null;

            try {
                stringEntity = new StringEntity(json.toString());
            } catch (UnsupportedEncodingException var15) {
            }

            httpPost.setEntity(stringEntity);
            log.info("Waiting for log \"{}\" in container {} at Media Node {}", new Object[]{expectedLog, containerNameOrId, this.mediaNodeIp});
            log.info("Media Node Controller HTTP-POST request: {}", httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var13 = (String)httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var16) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var14) {
                    var16.addSuppressed(var14);
                }
            }

            throw var16;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var13;
    }

    public String getRecordingsVolumePath() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String var5;
        try {
            String PATH = "getRecordingsPath";
            HttpGet httpget = new HttpGet(this.mediaNodeControllerUrl + "media-node/getRecordingsPath");
            httpget.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpget.setHeader("Authorization", this.getBasicAuth());
            log.info("Getting recordings volume path for media-node-controller {}", this.mediaNodeIp);
            log.info("Media Node Controller HTTP-GET request: " + httpget.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            var5 = (String)httpclient.execute(httpget, responseHandler);
        } catch (Throwable var7) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (httpclient != null) {
            httpclient.close();
        }

        return var5;
    }

    private void authToDockerRegistry(DockerRegistryConfig dockerRegistryConfig) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            String PATH = "authDockerRegistry";
            log.info("Authorizing access to docker registry: '{}'", dockerRegistryConfig.getServerAddress());
            HttpPost httpPost = new HttpPost(this.mediaNodeControllerUrl + "media-node/authDockerRegistry");
            httpPost.setConfig(RequestConfig.custom().setConnectTimeout(this.DOCKER_TIMEOUT).setSocketTimeout(this.DOCKER_TIMEOUT).build());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", this.getBasicAuth());
            JsonObject json = dockerRegistryConfig.toJson();
            StringEntity stringEntity = null;

            try {
                stringEntity = new StringEntity(json.toString());
            } catch (UnsupportedEncodingException var9) {
            }

            httpPost.setEntity(stringEntity);
            log.info("Media Node Controller HTTP-POST request: " + httpPost.getRequestLine());
            ResponseHandler<String> responseHandler = this.getStringResponseHandler();
            httpclient.execute(httpPost, responseHandler);
        } catch (Throwable var10) {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Throwable var8) {
                    var10.addSuppressed(var8);
                }
            }

            throw var10;
        }

        if (httpclient != null) {
            httpclient.close();
        }

    }
}
