//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.grpc.StatusRuntimeException;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.infrastructure.Instance;
import io.openvidu.server.stt.SpeechToTextType;
import io.openvidu.server.stt.grpc.SpeechToTextGrpcClient;
import io.openvidu.server.rest.SessionRestController;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/api"})
public class SpeechToTextRestController {
    private static final Logger log = LoggerFactory.getLogger(SpeechToTextRestController.class);
    @Autowired(
            required = false
    )
    private SpeechToTextGrpcClient speechToTextGrpcClient;
    @Autowired
    private InfrastructureManager infrastructureManager;
    @Autowired
    private OpenviduConfigPro openviduConfigPro;

    public SpeechToTextRestController() {
    }

    @RequestMapping(
            value = {"/speech-to-text/load"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> loadVoskModel(@RequestBody Map<String, ?> params) {
        if (params == null) {
            return SessionRestController.generateErrorResponse("Error in body parameters. Cannot be empty", "/speech-to-text/load", HttpStatus.BAD_REQUEST);
        } else {
            log.info("REST API: POST {}/speech-to-text/load {}", "/openvidu/api", params.toString());
            if (!SpeechToTextType.vosk.equals(this.openviduConfigPro.getSpeechToText())) {
                return SessionRestController.generateErrorResponse("Speech to Text module is disabled or configured with an engine that does not support language model management", "/speech-to-text/load", HttpStatus.NOT_IMPLEMENTED);
            } else {
                String lang;
                String mediaNodeId;
                try {
                    label44: {
                        if (params.get("lang") != null) {
                            lang = (String)params.get("lang");
                            mediaNodeId = SessionProperties.getMediaNodeProperty(params);
                            if (mediaNodeId != null) {
                                break label44;
                            }

                            return SessionRestController.generateErrorResponse("\"mediaNode\" parameter is mandatory", "/speech-to-text/load", HttpStatus.BAD_REQUEST);
                        }

                        return SessionRestController.generateErrorResponse("\"lang\" parameter is mandatory", "/speech-to-text/load", HttpStatus.BAD_REQUEST);
                    }
                } catch (IllegalArgumentException | ClassCastException var9) {
                    return SessionRestController.generateErrorResponse(var9.getMessage(), "/speech-to-text/load", HttpStatus.BAD_REQUEST);
                }

                String mediaNodeIp = null;

                try {
                    mediaNodeIp = this.getMediaNodeIpFromMediaNodeId(mediaNodeId);
                } catch (Exception var8) {
                    return SessionRestController.generateErrorResponse(var8.getMessage(), "/speech-to-text/load", HttpStatus.BAD_REQUEST);
                }

                try {
                    this.speechToTextGrpcClient.loadVoskModel(mediaNodeIp, lang);
                } catch (ExecutionException var6) {
                    return this.manageKnwonGrpcException(var6, "/speech-to-text/load");
                } catch (TimeoutException | InterruptedException var7) {
                    return SessionRestController.generateErrorResponse(var7.getMessage(), "/speech-to-text/load", HttpStatus.BAD_REQUEST);
                }

                return new ResponseEntity(HttpStatus.OK);
            }
        }
    }

    @RequestMapping(
            value = {"/speech-to-text/unload"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> unloadVoskModel(@RequestBody Map<String, ?> params) {
        if (params == null) {
            return SessionRestController.generateErrorResponse("Error in body parameters. Cannot be empty", "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
        } else {
            log.info("REST API: POST {}/speech-to-text/unload {}", "/openvidu/api", params.toString());
            if (!SpeechToTextType.vosk.equals(this.openviduConfigPro.getSpeechToText())) {
                return SessionRestController.generateErrorResponse("Speech to Text module is disabled or configured with an engine that does not support language model management", "/speech-to-text/unload", HttpStatus.NOT_IMPLEMENTED);
            } else {
                String lang;
                String mediaNodeId;
                try {
                    label44: {
                        if (params.get("lang") != null) {
                            lang = (String)params.get("lang");
                            mediaNodeId = SessionProperties.getMediaNodeProperty(params);
                            if (mediaNodeId != null) {
                                break label44;
                            }

                            return SessionRestController.generateErrorResponse("\"mediaNode\" parameter is mandatory", "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
                        }

                        return SessionRestController.generateErrorResponse("\"lang\" parameter is mandatory", "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
                    }
                } catch (IllegalArgumentException | ClassCastException var9) {
                    return SessionRestController.generateErrorResponse(var9.getMessage(), "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
                }

                String mediaNodeIp = null;

                try {
                    mediaNodeIp = this.getMediaNodeIpFromMediaNodeId(mediaNodeId);
                } catch (Exception var8) {
                    return SessionRestController.generateErrorResponse(var8.getMessage(), "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
                }

                try {
                    this.speechToTextGrpcClient.unloadVoskModel(mediaNodeIp, lang);
                } catch (ExecutionException var6) {
                    return this.manageKnwonGrpcException(var6, "/speech-to-text/unload");
                } catch (TimeoutException | InterruptedException var7) {
                    return SessionRestController.generateErrorResponse(var7.getMessage(), "/speech-to-text/unload", HttpStatus.BAD_REQUEST);
                }

                return new ResponseEntity(HttpStatus.OK);
            }
        }
    }

    private String getMediaNodeIpFromMediaNodeId(String mediaNodeId) throws Exception {
        String mediaNodeIp = null;
        if (mediaNodeId != null) {
            Instance instance = this.infrastructureManager.getInstance(mediaNodeId);
            if (instance == null) {
                throw new Exception("Media Node identified with " + mediaNodeId + " does not exist");
            }

            mediaNodeIp = instance.getIp();
        }

        return mediaNodeIp;
    }

    private ResponseEntity<String> manageKnwonGrpcException(ExecutionException e, String path) {
        Throwable cause = e.getCause();
        if (cause instanceof StatusRuntimeException) {
            StatusRuntimeException grpcException = (StatusRuntimeException)cause;
            String description = grpcException.getStatus().getDescription();
            JsonObject json = JsonParser.parseString(description).getAsJsonObject();
            String message = json.get("message").getAsString();
            int status = json.get("status").getAsInt();
            return SessionRestController.generateErrorResponse(message, path, HttpStatus.resolve(status));
        } else {
            return SessionRestController.generateErrorResponse(e.getMessage(), path, HttpStatus.BAD_REQUEST);
        }
    }
}
