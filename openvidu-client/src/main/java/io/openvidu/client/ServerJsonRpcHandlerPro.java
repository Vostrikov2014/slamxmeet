//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.IceCandidate;
import io.openvidu.client.internal.IceCandidateInfo;
import io.openvidu.client.internal.JsonRoomUtils;
import io.openvidu.client.internal.MediaErrorInfo;
import io.openvidu.client.internal.Notification;
import io.openvidu.client.internal.ParticipantEvictedInfo;
import io.openvidu.client.internal.ParticipantJoinedInfo;
import io.openvidu.client.internal.ParticipantLeftInfo;
import io.openvidu.client.internal.ParticipantPublishedInfo;
import io.openvidu.client.internal.ParticipantUnpublishedInfo;
import io.openvidu.client.internal.RoomClosedInfo;
import io.openvidu.client.internal.SendMessageInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerJsonRpcHandlerPro extends DefaultJsonRpcHandler<JsonObject> {
    private static final Logger log = LoggerFactory.getLogger(ServerJsonRpcHandler.class);
    private BlockingQueue<Notification> notifications = new ArrayBlockingQueue(100);

    public ServerJsonRpcHandlerPro() {
    }

    public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {
        Notification notif = null;

        try {
            switch (request.getMethod()) {
                case "iceCandidate":
                    notif = this.iceCandidate(transaction, request);
                    break;
                case "mediaError":
                    notif = this.mediaError(transaction, request);
                    break;
                case "participantJoined":
                    notif = this.participantJoined(transaction, request);
                    break;
                case "participantLeft":
                    notif = this.participantLeft(transaction, request);
                    break;
                case "participantEvicted":
                    notif = this.participantEvicted(transaction, request);
                    break;
                case "participantPublished":
                    notif = this.participantPublished(transaction, request);
                    break;
                case "participantUnpublished":
                    notif = this.participantUnpublished(transaction, request);
                    break;
                case "roomClosed":
                    notif = this.roomClosed(transaction, request);
                    break;
                case "sendMessage":
                    notif = this.participantSendMessage(transaction, request);
                    break;
                default:
                    throw new Exception("Unrecognized request " + request.getMethod());
            }
        } catch (Exception var7) {
            log.error("Exception processing request {}", request, var7);
            transaction.sendError(var7);
            return;
        }

        if (notif != null) {
            try {
                this.notifications.put(notif);
                log.debug("Enqueued notification {}", notif);
            } catch (InterruptedException var6) {
                log.warn("Interrupted when enqueuing notification {}", notif, var6);
            }
        }

    }

    private Notification participantSendMessage(Transaction transaction, Request<JsonObject> request) {
        String data = (String)JsonRoomUtils.getRequestParam(request, "data", String.class);
        String from = (String)JsonRoomUtils.getRequestParam(request, "from", String.class);
        String type = (String)JsonRoomUtils.getRequestParam(request, "type", String.class);
        SendMessageInfo eventInfo = new SendMessageInfo(data, from, type);
        log.debug("Recvd send message event {}", eventInfo);
        return eventInfo;
    }

    private Notification roomClosed(Transaction transaction, Request<JsonObject> request) {
        String room = (String)JsonRoomUtils.getRequestParam(request, "sessionId", String.class);
        RoomClosedInfo eventInfo = new RoomClosedInfo(room);
        log.debug("Recvd room closed event {}", eventInfo);
        return eventInfo;
    }

    private Notification participantUnpublished(Transaction transaction, Request<JsonObject> request) {
        String name = (String)JsonRoomUtils.getRequestParam(request, "connectionId", String.class);
        ParticipantUnpublishedInfo eventInfo = new ParticipantUnpublishedInfo(name);
        log.debug("Recvd participant unpublished event {}", eventInfo);
        return eventInfo;
    }

    private Notification participantPublished(Transaction transaction, Request<JsonObject> request) {
        String id = (String)JsonRoomUtils.getRequestParam(request, "id", String.class);
        JsonArray jsonStreams = (JsonArray)JsonRoomUtils.getRequestParam(request, "streams", JsonArray.class);
        Iterator<JsonElement> streamIt = jsonStreams.iterator();
        List<String> streams = new ArrayList();

        while(streamIt.hasNext()) {
            streams.add((String)JsonRoomUtils.getResponseProperty((JsonElement)streamIt.next(), "id", String.class));
        }

        ParticipantPublishedInfo eventInfo = new ParticipantPublishedInfo(id, streams);
        log.debug("Recvd published event {}", eventInfo);
        return eventInfo;
    }

    private Notification participantEvicted(Transaction transaction, Request<JsonObject> request) {
        ParticipantEvictedInfo eventInfo = new ParticipantEvictedInfo();
        log.debug("Recvd participant evicted event {}", eventInfo);
        return eventInfo;
    }

    private Notification participantLeft(Transaction transaction, Request<JsonObject> request) {
        String name = (String)JsonRoomUtils.getRequestParam(request, "connectionId", String.class);
        ParticipantLeftInfo eventInfo = new ParticipantLeftInfo(name);
        log.debug("Recvd participant left event {}", eventInfo);
        return eventInfo;
    }

    private Notification participantJoined(Transaction transaction, Request<JsonObject> request) {
        String id = (String)JsonRoomUtils.getRequestParam(request, "id", String.class);
        ParticipantJoinedInfo eventInfo = new ParticipantJoinedInfo(id);
        log.debug("Recvd participant joined event {}", eventInfo);
        return eventInfo;
    }

    private Notification mediaError(Transaction transaction, Request<JsonObject> request) {
        String description = (String)JsonRoomUtils.getRequestParam(request, "error", String.class);
        MediaErrorInfo eventInfo = new MediaErrorInfo(description);
        log.debug("Recvd media error event {}", eventInfo);
        return eventInfo;
    }

    private Notification iceCandidate(Transaction transaction, Request<JsonObject> request) {
        String candidate = (String)JsonRoomUtils.getRequestParam(request, "candidate", String.class);
        String sdpMid = (String)JsonRoomUtils.getRequestParam(request, "sdpMid", String.class);
        int sdpMLineIndex = (Integer)JsonRoomUtils.getRequestParam(request, "sdpMLineIndex", Integer.class);
        IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
        String endpoint = (String)JsonRoomUtils.getRequestParam(request, "endpointName", String.class);
        IceCandidateInfo eventInfo = new IceCandidateInfo(iceCandidate, endpoint);
        log.debug("Recvd ICE candidate event {}", eventInfo);
        return eventInfo;
    }

    public Notification getNotification() {
        try {
            Notification notif = (Notification)this.notifications.take();
            log.debug("Dequeued notification {}", notif);
            return notif;
        } catch (InterruptedException var2) {
            log.info("Interrupted while polling notifications' queue");
            return null;
        }
    }
}
