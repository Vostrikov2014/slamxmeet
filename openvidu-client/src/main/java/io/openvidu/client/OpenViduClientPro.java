//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.JsonRoomUtils;
import io.openvidu.client.internal.Notification;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcWSConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenViduClientPro {
    private static final Logger log = LoggerFactory.getLogger(OpenViduClientPro.class);
    private JsonRpcClient client;
    private ServerJsonRpcHandler handler;

    public OpenViduClientPro(String wsUri) {
        this((JsonRpcClient)(new JsonRpcClientWebSocket(wsUri, new JsonRpcWSConnectionListener() {
            public void reconnected(boolean sameServer) {
            }

            public void disconnected() {
                OpenViduClientPro.log.warn("JsonRpcWebsocket connection: Disconnected");
            }

            public void connectionFailed() {
                OpenViduClientPro.log.warn("JsonRpcWebsocket connection: Connection failed");
            }

            public void connected() {
            }

            public void reconnecting() {
                OpenViduClientPro.log.warn("JsonRpcWebsocket connection: is reconnecting");
            }
        }, new SslContextFactory(true))));
    }

    public OpenViduClientPro(JsonRpcClient client) {
        this.client = client;
        this.handler = new ServerJsonRpcHandler();
        this.client.setServerRequestHandler(this.handler);
    }

    public OpenViduClientPro(JsonRpcClient client, ServerJsonRpcHandler handler) {
        this.client = client;
        this.handler = handler;
        this.client.setServerRequestHandler(this.handler);
    }

    public void close() throws IOException {
        this.client.close();
    }

    public Map<String, List<String>> joinRoom(String roomName, String userName) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("session", roomName);
        params.addProperty("user", userName);
        JsonElement result = this.client.sendRequest("joinRoom", params);
        Map<String, List<String>> peers = new HashMap();
        JsonArray jsonPeers = (JsonArray)JsonRoomUtils.getResponseProperty(result, "value", JsonArray.class);
        String peerId;
        ArrayList streams;
        if (jsonPeers.size() > 0) {
            for(Iterator<JsonElement> peerIt = jsonPeers.iterator(); peerIt.hasNext(); peers.put(peerId, streams)) {
                JsonElement peer = (JsonElement)peerIt.next();
                peerId = (String)JsonRoomUtils.getResponseProperty(peer, "id", String.class);
                streams = new ArrayList();
                JsonArray jsonStreams = (JsonArray)JsonRoomUtils.getResponseProperty(peer, "streams", JsonArray.class, true);
                if (jsonStreams != null) {
                    Iterator<JsonElement> streamIt = jsonStreams.iterator();

                    while(streamIt.hasNext()) {
                        streams.add((String)JsonRoomUtils.getResponseProperty((JsonElement)streamIt.next(), "id", String.class));
                    }
                }
            }
        }

        return peers;
    }

    public void leaveRoom() throws IOException {
        this.client.sendRequest("leaveRoom", new JsonObject());
    }

    public String publishVideo(String sdpOffer, boolean doLoopback) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("sdpOffer", sdpOffer);
        params.addProperty("doLoopback", doLoopback);
        JsonElement result = this.client.sendRequest("publishVideo", params);
        return (String)JsonRoomUtils.getResponseProperty(result, "sdpAnswer", String.class);
    }

    public void unpublishVideo() throws IOException {
        this.client.sendRequest("unpublishVideo", new JsonObject());
    }

    public String receiveVideoFrom(String sender, String sdpOffer) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("sender", sender);
        params.addProperty("sdpOffer", sdpOffer);
        JsonElement result = this.client.sendRequest("receiveVideoFrom", params);
        return (String)JsonRoomUtils.getResponseProperty(result, "sdpAnswer", String.class);
    }

    public void unsubscribeFromVideo(String sender) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("sender", sender);
        this.client.sendRequest("unsubscribeFromVideo", params);
    }

    public void onIceCandidate(String endpointName, String candidate, String sdpMid, int sdpMLineIndex) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("endpointName", endpointName);
        params.addProperty("candidate", candidate);
        params.addProperty("sdpMid", sdpMid);
        params.addProperty("sdpMLineIndex", sdpMLineIndex);
        this.client.sendRequest("onIceCandidate", params);
    }

    public void sendMessage(String userName, String roomName, String message) throws IOException {
        JsonObject params = new JsonObject();
        params.addProperty("message", message);
        this.client.sendRequest("sendMessage", params);
    }

    public JsonElement customRequest(JsonObject customReqParams) throws IOException {
        return this.client.sendRequest("customRequest", customReqParams);
    }

    public Notification getServerNotification() {
        return this.handler.getNotification();
    }
}
