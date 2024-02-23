//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.config.Dotenv;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.OpenViduServerPro;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.config.Status;
import io.openvidu.server.health.HealthCheck;
import io.openvidu.server.health.HealthCheckManager;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.OpenViduClusterEnvironment;
import io.openvidu.server.infrastructure.OpenViduClusterMode;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.utils.LocalDockerManager;
import io.openvidu.server.utils.RestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/api"})
public class AdministrationRestControllerPro {
    private static final Logger log = LoggerFactory.getLogger(AdministrationRestControllerPro.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private HealthCheckManager healthCheckManager;
    @Autowired
    private OpenViduServerPro openviduServerPro;
    @Autowired
    private InfrastructureManager infrastructureManager;
    @Autowired
    private RecordingManager recordingManager;

    public AdministrationRestControllerPro() {
    }

    @RequestMapping(
            value = {"/status"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<String> getOpenViduProSatus() {
        log.info("REST API: GET {}/status", "/openvidu/api");
        return new ResponseEntity(Status.toJson().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/health"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<String> healthCheck() {
        log.info("REST API: GET {}/health", "/openvidu/api");
        if (!this.openviduConfigPro.isCluster()) {
            return RestUtils.getErrorResponse("OpenVidu Pro cluster mode is disabled. Set 'OPENVIDU_PRO_CLUSTER' to true", HttpStatus.NOT_IMPLEMENTED);
        } else {
            HealthCheck health = this.healthCheckManager.health();
            if (health.getStatus() == io.openvidu.server.pro.health.HealthCheck.Status.DOWN) {
                return RestUtils.getErrorResponse(health.toJson().toString(), HttpStatus.SERVICE_UNAVAILABLE);
            } else {
                return health.getStatus() == io.openvidu.server.pro.health.HealthCheck.Status.UNSTABLE ? RestUtils.getErrorResponse(health.toJson().toString(), HttpStatus.CONFLICT) : new ResponseEntity(health.toJson().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
            }
        }
    }

    @RequestMapping(
            value = {"/restart"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> restart(@RequestBody(required = false) Map<String, ?> requestBody, @RequestParam(required = false,name = "dry-run") boolean dryRun) {
        log.info("REST API: POST {}/restart {}", "/openvidu/api", requestBody.toString());

        try {
            Map<String, String> properties = this.convertToStringMap(requestBody);
            if (!this.openviduConfigPro.isMultiMasterMonoNode()) {
                this.cleanUpdatedProperties(properties);
            }

            this.checkUpdatedProperties(properties);
            log.warn("RESTARTING OPENVIDU SERVER. Dry run: {}. Properties: {}", dryRun, properties);
            if (!dryRun) {
                this.saveNewProperties(properties);
                this.openviduServerPro.restart(properties);
            }

            return new ResponseEntity(HttpStatus.OK);
        } catch (IllegalArgumentException var4) {
            return RestUtils.getErrorResponse("Error restarting OpenVidu Server Pro: " + var4.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, String> convertToStringMap(Map<String, ?> requestBody) {
        Map<String, String> properties = new HashMap();

        Map.Entry e;
        String strValue;
        for(Iterator var3 = requestBody.entrySet().iterator(); var3.hasNext(); properties.put((String)e.getKey(), strValue)) {
            e = (Map.Entry)var3.next();
            Object value = e.getValue();
            if (value instanceof Iterable) {
                strValue = JsonUtils.toJson(e.getValue());
            } else {
                strValue = value.toString();
            }
        }

        return properties;
    }

    private void saveNewProperties(Map<String, String> properties) {
        File envFile = this.openviduConfigPro.getDotenvFile();
        if (envFile == null) {
            log.warn(".env file doesn't exist in folder {} (configured in property 'DOTENV_PATH'). Configuration properties changes won't be persisted", Paths.get(this.openviduConfigPro.getDotenvPath()));
        } else if (!envFile.canWrite()) {
            log.warn("OpenVidu does not have write permissions over .env file {}. Configuration properties changes won't be persisted", envFile.getAbsolutePath());
        } else {
            Path dotenvPath = envFile.toPath();
            Dotenv dotenv = new Dotenv();

            Logger var10000;
            Class var10001;
            try {
                dotenv.read(dotenvPath);
                Iterator var5 = properties.entrySet().iterator();

                while(var5.hasNext()) {
                    Map.Entry<String, String> e = (Map.Entry)var5.next();
                    String propAsEnvVariable = ((String)e.getKey()).toUpperCase().replace('.', '_').replace('-', '_');
                    String propValue = (String)e.getValue();
                    String deValue = dotenv.get(propAsEnvVariable);
                    if (!Objects.equal(propValue, deValue)) {
                        log.info("Updating .env file. Updating " + propAsEnvVariable + " from '" + deValue + "' to '" + propValue + "'");
                        dotenv.set(propAsEnvVariable, (String)e.getValue());
                    }
                }

                dotenv.write(dotenvPath);
            } catch (IOException var10) {
                var10000 = log;
                var10001 = var10.getClass();
                var10000.warn("Exception reading .env file. " + var10001 + ":" + var10.getMessage() + ". Configuration properties changes won't be persisted");
            } catch (Dotenv.DotenvFormatException var11) {
                var10000 = log;
                var10001 = var11.getClass();
                var10000.warn("Format error in .env file. " + var10001 + ":" + var11.getMessage() + ". Configuration properties changes won't be persisted");
            }

        }
    }

    private void cleanUpdatedProperties(Map<String, String> params) {
        List<String> NON_MODIFIABLE_PROPS = this.openviduConfigPro.getNonModifiablePropertiesOnRestart();
        params.entrySet().removeIf((entry) -> {
            return NON_MODIFIABLE_PROPS.contains(entry.getKey());
        });
    }

    private void checkUpdatedProperties(Map<String, String> params) throws IllegalArgumentException {
        boolean clusterEnabled = params.get("OPENVIDU_PRO_CLUSTER") != null ? Boolean.parseBoolean((String)params.get("OPENVIDU_PRO_CLUSTER")) : this.openviduConfigPro.isCluster();
        OpenViduClusterMode clusterMode;
        OpenViduClusterEnvironment clusterEnvironment;
        if (this.openviduConfigPro.isMultiMasterEnvironment() && !this.openviduConfigPro.isMultimasterOnPremises()) {
            clusterEnvironment = OpenViduClusterEnvironment.on_premise;
            clusterMode = OpenViduClusterMode.manual;
            params.put("OPENVIDU_PRO_CLUSTER_ENVIRONMENT", OpenViduClusterEnvironment.aws.name());
        } else {
            clusterEnvironment = params.get("OPENVIDU_PRO_CLUSTER_ENVIRONMENT") != null ? OpenViduClusterEnvironment.valueOf((String)params.get("OPENVIDU_PRO_CLUSTER_ENVIRONMENT")) : this.openviduConfigPro.getClusterEnvironment();
            clusterMode = OpenViduClusterEnvironment.on_premise.equals(clusterEnvironment) ? OpenViduClusterMode.manual : OpenViduClusterMode.auto;
        }

        String var10002;
        try {
            if (clusterEnabled && OpenViduClusterMode.auto.equals(clusterMode)) {
                params.remove("KMS_URIS");
            }

            OpenviduConfig config = this.openviduConfigPro.deriveWithAdditionalPropertiesSource(params);
            config.checkConfiguration(false);
            if (!config.getConfigErrors().isEmpty()) {
                StringBuilder errorMsg = new StringBuilder();
                config.getConfigErrors().forEach((e) -> {
                    errorMsg.append(e).append('\n');
                });
                throw new IllegalArgumentException(errorMsg.toString());
            }
        } catch (Exception var23) {
            var10002 = var23.getClass().getName();
            throw new IllegalArgumentException(var10002 + ":" + var23.getMessage());
        }

        String newRecordingPath = params.get("OPENVIDU_RECORDING_PATH") != null ? (String)params.get("OPENVIDU_RECORDING_PATH") : this.openviduConfigPro.getOpenViduRecordingPath();
        String newRecordingCustomLayout = params.get("OPENVIDU_RECORDING_CUSTOM_LAYOUT") != null ? (String)params.get("OPENVIDU_RECORDING_CUSTOM_LAYOUT") : this.openviduConfigPro.getOpenviduRecordingCustomLayout();

        try {
            boolean recordingEnabled = this.openviduConfigPro.isRecordingModuleEnabled();
            if (params.get("OPENVIDU_RECORDING") != null) {
                recordingEnabled = Boolean.parseBoolean((String)params.get("OPENVIDU_RECORDING"));
            }

            if (recordingEnabled) {
                this.recordingManager.checkRecordingRequirements(newRecordingPath, newRecordingCustomLayout);
            }
        } catch (OpenViduException var22) {
            var10002 = var22.getClass().getName();
            throw new IllegalArgumentException(var10002 + ":" + var22.getMessage());
        }

        String newKmsUris = (String)params.get("KMS_URIS");
        List finalKmsUris;
        if (newKmsUris != null && !newKmsUris.isEmpty()) {
            try {
                finalKmsUris = JsonUtils.toStringList((JsonArray)(new Gson()).fromJson(newKmsUris, JsonArray.class));
            } catch (Exception var21) {
                throw new IllegalArgumentException("'KMS_URIS' has not a valid Json array format");
            }

            this.checkConnectionToKmsUris(finalKmsUris);
        } else {
            finalKmsUris = this.openviduConfigPro.getKmsUris();
        }

        if (clusterEnabled) {
            if (OpenViduClusterEnvironment.docker.equals(clusterEnvironment)) {
                LocalDockerManager dockerManager = null;

                try {
                    dockerManager = new LocalDockerManager(true);
                    dockerManager.checkDockerEnabled();
                } catch (OpenViduException var19) {
                    throw new IllegalArgumentException("Invalid parameter \"OPENVIDU_PRO_CLUSTER_ENVIRONMENT\". Cannot be \"docker\" if Docker is not installed in the host");
                } finally {
                    dockerManager.close();
                }
            }

            if (OpenViduClusterMode.auto.equals(clusterMode) && this.infrastructureManager.isEnvironmentWithScripts(clusterEnvironment)) {
                String newClusterPath = params.get("OPENVIDU_PRO_CLUSTER_PATH") != null ? (String)params.get("OPENVIDU_PRO_CLUSTER_PATH") : this.openviduConfigPro.getClusterPath();
                newClusterPath = newClusterPath.endsWith("/") ? newClusterPath : newClusterPath + "/";

                try {
                    this.infrastructureManager.checkClusterScripts(newClusterPath);
                } catch (Exception var18) {
                    var10002 = var18.getClass().getName();
                    throw new IllegalArgumentException(var10002 + ":" + var18.getMessage());
                }
            }
        } else if (finalKmsUris.size() == 0) {
            throw new IllegalArgumentException("Invalid parameter \"KMS_URIS\". Cannot be empty if parameter \"OPENVIDU_PRO_CLUSTER\" is false");
        }

    }

    private void checkConnectionToKmsUris(List<String> kmsUris) throws IllegalArgumentException {
        Iterator var2 = kmsUris.iterator();

        while(var2.hasNext()) {
            String kmsUri = (String)var2.next();

            try {
                KurentoClient.create(kmsUri).getServerManager();
            } catch (Exception var5) {
                throw new IllegalArgumentException("Invalid parameter \"KMS_URIS\". Kms with URI '" + kmsUri + "' is not reachable");
            }
        }

    }
}
