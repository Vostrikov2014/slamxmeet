//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.MediaMode;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.broadcast.BroadcastManager;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionEventsHandler;
import io.openvidu.server.core.Token;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.broadcast.BroadcastManagerPro;
import io.openvidu.server.infrastructure.InfrastructureManager;
import io.openvidu.server.kurento.core.KurentoSessionEventsHandlerPro;
import io.openvidu.server.rest.SessionRestController;
import io.openvidu.server.utils.RestUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController("sessionRestControllerPro")
@CrossOrigin
public class SessionRestControllerPro extends SessionRestController {
    @Autowired
    private SessionEventsHandler sessionEventsHandler;
    @Autowired
    private InfrastructureManager infrastructureManager;
    @Autowired
    private BroadcastManager broadcastManager;
    private static final Logger log = LoggerFactory.getLogger(SessionRestControllerPro.class);

    public SessionRestControllerPro() {
    }

    @RequestMapping(
            value = {"/sessions/{sessionId}/connection/{connectionId}"},
            method = {RequestMethod.PATCH}
    )
    public ResponseEntity<?> updateParticipant(@PathVariable("sessionId") String sessionId, @PathVariable("connectionId") String connectionId, @RequestBody(required = true) Map<String, ?> params) {
        log.info("REST API: PATCH {}/sessions/{}/connection/{}", new Object[]{"/openvidu/api", sessionId, connectionId});
        Session session = this.sessionManager.getSessionWithNotActive(sessionId);
        if (session == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            OpenViduRole updatedRole = null;
            Boolean updatedRecord = null;

            try {
                if (params.get("role") != null) {
                    updatedRole = OpenViduRole.valueOf((String)params.get("role"));
                }

                if (params.get("record") != null) {
                    updatedRecord = (Boolean)params.get("record");
                }
            } catch (IllegalArgumentException | ClassCastException var13) {
                return SessionRestController.generateErrorResponse("Type error in some parameter", "/sessions/" + sessionId + "/connection/" + connectionId, HttpStatus.BAD_REQUEST);
            }

            Participant participant = session.getParticipantByPublicId(connectionId);
            if (participant != null) {
                ConnectionProperties.Builder builder = new ConnectionProperties.Builder();
                builder.role(updatedRole != null ? updatedRole : participant.getToken().getRole());
                builder.record(updatedRecord != null ? updatedRecord : participant.getToken().record());
                ConnectionProperties connectionProperties = builder.build();
                boolean oldRecord = participant.getToken().record();
                OpenViduRole oldRole = participant.getToken().getRole();
                if (this.updateConnectionProperties(participant, connectionProperties)) {
                    if (updatedRecord != null && oldRecord != updatedRecord && this.recordingManager.sessionIsBeingRecorded(sessionId)) {
                        if (updatedRecord) {
                            this.recordingManager.startOneIndividualStreamRecording(session, participant);
                        } else {
                            this.recordingManager.stopOneIndividualStreamRecording(session, participant.getPublisherStreamId(), (Long)null);
                        }
                    }

                    if (updatedRole != null && OpenViduRole.SUBSCRIBER.equals(oldRole) && !OpenViduRole.SUBSCRIBER.equals(updatedRole)) {
                        KurentoParticipant kParticipant = (KurentoParticipant)participant;
                        if (!kParticipant.isPublisherEndpointDefined()) {
                            kParticipant.initPublisherEndpoint();
                        }
                    }

                    return new ResponseEntity(participant.toJson().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                } else {
                    return new ResponseEntity(participant.toJson().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                }
            } else {
                Token token = this.getTokenFromConnectionId(connectionId, session.getTokenIterator());
                if (token != null) {
                    ConnectionProperties.Builder builder = new ConnectionProperties.Builder();
                    builder.role(updatedRole != null ? updatedRole : token.getRole());
                    builder.record(updatedRecord != null ? updatedRecord : token.record());
                    ConnectionProperties connectionProperties = builder.build();
                    return this.modifyPendingConnectionFromConnectionId(connectionId, connectionProperties, session.getTokenIterator()) ? new ResponseEntity(token.toJsonAsParticipant().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK) : new ResponseEntity(token.toJsonAsParticipant().toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
                } else {
                    return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            }
        }
    }

    @RequestMapping(
            value = {"/broadcast/start"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<?> startBroadcast(@RequestBody Map<String, ?> params) {
        if (params == null) {
            return SessionRestController.generateErrorResponse("Error in body parameters. Cannot be empty", "/broadcast/start", HttpStatus.BAD_REQUEST);
        } else {
            log.info("REST API: POST {}/broadcast/start {}", "/openvidu/api", params.toString());

            String sessionId;
            String broadcastUrl;
            try {
                sessionId = (String)params.get("session");
                broadcastUrl = (String)params.get("broadcastUrl");
            } catch (Exception var10) {
                return SessionRestController.generateErrorResponse("Type error in some parameter", "/broadcast/start", HttpStatus.BAD_REQUEST);
            }

            if (sessionId == null) {
                return SessionRestController.generateErrorResponse("\"session\" parameter is mandatory", "/broadcast/start", HttpStatus.BAD_REQUEST);
            } else if (broadcastUrl == null) {
                return SessionRestController.generateErrorResponse("\"broadcastUrl\" parameter is mandatory", "/broadcast/start", HttpStatus.BAD_REQUEST);
            } else {
                try {
                    URI uri = new URI(broadcastUrl);
                    if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
                        return SessionRestController.generateErrorResponse("\"broadcastUrl\" parameter \"" + broadcastUrl + "\" is not a valid broadcast URI: scheme is empty", "/broadcast/start", HttpStatus.BAD_REQUEST);
                    }
                } catch (URISyntaxException var11) {
                    return SessionRestController.generateErrorResponse("\"broadcastUrl\" parameter \"" + broadcastUrl + "\" is not a valid broadcast URI: " + var11.getMessage(), "/broadcast/start", HttpStatus.BAD_REQUEST);
                }

                Session session = this.sessionManager.getSession(sessionId);
                if (session == null) {
                    session = this.sessionManager.getSessionNotActive(sessionId);
                    if (session != null) {
                        return MediaMode.ROUTED.equals(session.getSessionProperties().mediaMode()) && !this.broadcastManager.sessionIsBeingBroadcasted(session.getSessionId()) ? new ResponseEntity(HttpStatus.NOT_ACCEPTABLE) : new ResponseEntity(HttpStatus.CONFLICT);
                    } else {
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                    }
                } else if (MediaMode.ROUTED.equals(session.getSessionProperties().mediaMode()) && !this.broadcastManager.sessionIsBeingBroadcasted(session.getSessionId())) {
                    params = RecordingProperties.removeNonBroadcastProperties(params);

                    RecordingProperties recordingProperties;
                    try {
                        recordingProperties = this.getRecordingPropertiesFromParams(params, session).build();
                    } catch (IllegalStateException var8) {
                        return SessionRestController.generateErrorResponse(var8.getMessage(), "/sessions", HttpStatus.UNPROCESSABLE_ENTITY);
                    } catch (RuntimeException var9) {
                        return SessionRestController.generateErrorResponse(var9.getMessage(), "/sessions", HttpStatus.BAD_REQUEST);
                    }

                    try {
                        ((BroadcastManagerPro)this.broadcastManager).startBroadcast(session, recordingProperties, broadcastUrl);
                        return new ResponseEntity(RestUtils.getResponseHeaders(), HttpStatus.OK);
                    } catch (Exception var7) {
                        return new ResponseEntity("Error starting broadcast: " + var7.getMessage(), RestUtils.getResponseHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    return new ResponseEntity(HttpStatus.CONFLICT);
                }
            }
        }
    }

    @RequestMapping(
            value = {"/broadcast/stop"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<?> stopBroadcast(@RequestBody Map<String, ?> params) {
        if (params == null) {
            return SessionRestController.generateErrorResponse("Error in body parameters. Cannot be empty", "/broadcast/stop", HttpStatus.BAD_REQUEST);
        } else {
            log.info("REST API: POST {}/broadcast/stop {}", "/openvidu/api", params.toString());

            String sessionId;
            try {
                sessionId = (String)params.get("session");
            } catch (Exception var6) {
                return SessionRestController.generateErrorResponse("Type error in some parameter", "/broadcast/stop", HttpStatus.BAD_REQUEST);
            }

            if (sessionId == null) {
                return SessionRestController.generateErrorResponse("\"session\" parameter is mandatory", "/broadcast/stop", HttpStatus.BAD_REQUEST);
            } else {
                Session session = this.sessionManager.getSession(sessionId);
                if (session == null) {
                    return new ResponseEntity(HttpStatus.NOT_FOUND);
                } else if (!this.broadcastManager.sessionIsBeingBroadcasted(session.getSessionId())) {
                    return new ResponseEntity(HttpStatus.CONFLICT);
                } else {
                    try {
                        ((BroadcastManagerPro)this.broadcastManager).stopBroadcast(session, (RecordingProperties)null, EndReason.broadcastStoppedByServer);
                        return new ResponseEntity(RestUtils.getResponseHeaders(), HttpStatus.OK);
                    } catch (OpenViduException var5) {
                        return new ResponseEntity("Error stopping broadcast: " + var5.getMessage(), RestUtils.getResponseHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }
    }

    protected SessionProperties.Builder getSessionPropertiesFromParams(Map<String, ?> params) throws Exception {
        SessionProperties.Builder builder = super.getSessionPropertiesFromParams(params);
        String mediaNode = SessionProperties.getMediaNodeProperty(params);
        if (mediaNode != null && !mediaNode.isEmpty()) {
            if (!this.infrastructureManager.isMediaNodeAvailableForSession(mediaNode)) {
                throw new Exception("Media Node " + mediaNode + " does not allow initializing new Sessions");
            }

            builder.mediaNode(mediaNode);
        }

        return builder;
    }

    protected RecordingProperties.Builder getRecordingPropertiesFromParams(Map<String, ?> params, Session session) throws RuntimeException {
        String mediaNodeDefault = session.getSessionProperties().defaultRecordingProperties().mediaNode();
        String mediaNodeParam = SessionProperties.getMediaNodeProperty(params);
        String mediaNodeFinal;
        if (mediaNodeParam != null && !mediaNodeParam.isEmpty()) {
            mediaNodeFinal = mediaNodeParam;
        } else if (mediaNodeDefault != null && !mediaNodeDefault.isEmpty()) {
            mediaNodeFinal = mediaNodeDefault;
        } else {
            mediaNodeFinal = session.getMediaNodeId();
        }

        if (!this.infrastructureManager.isMediaNodeAvailableForRecordingOrBroadcast(mediaNodeFinal)) {
            throw new RuntimeException("Media Node " + mediaNodeFinal + " is not available for starting new recordings or broadcasts");
        } else {
            RecordingProperties.Builder builder = super.getRecordingPropertiesFromParams(params, session);
            builder.mediaNode(mediaNodeFinal);
            return builder;
        }
    }

    private boolean updateConnectionProperties(Participant participant, ConnectionProperties connectionProperties) {
        boolean modified;
        if (participant.getToken().getRole().equals(connectionProperties.getRole()) && participant.getToken().record() == connectionProperties.record()) {
            modified = false;
        } else {
            if (connectionProperties.getRole() != null && !connectionProperties.getRole().equals(participant.getToken().getRole())) {
                participant.getToken().setRole(connectionProperties.getRole());
                ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onConnectionPropertyChanged(participant, "role", connectionProperties.getRole().name());
            }

            if (connectionProperties.record() != null && !connectionProperties.record().equals(participant.getToken().record())) {
                participant.getToken().setRecord(connectionProperties.record());
                ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onConnectionPropertyChanged(participant, "record", connectionProperties.record());
            }

            modified = true;
        }

        return modified;
    }

    private boolean modifyPendingConnectionFromConnectionId(String connectionId, ConnectionProperties connectionProperties, Iterator<Map.Entry<String, Token>> iterator) {
        boolean modified = false;

        while(iterator.hasNext() && !modified) {
            Map.Entry<String, Token> entry = (Map.Entry)iterator.next();
            if (connectionId.equals(((Token)entry.getValue()).getConnectionId())) {
                Token t = (Token)entry.getValue();
                if (t.getRole().equals(connectionProperties.getRole()) && t.record() == connectionProperties.record()) {
                    modified = false;
                } else {
                    t.setRole(connectionProperties.getRole());
                    t.setRecord(connectionProperties.record());
                    modified = true;
                }
            }
        }

        return modified;
    }
}
