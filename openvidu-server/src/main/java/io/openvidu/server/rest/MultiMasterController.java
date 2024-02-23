//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import io.openvidu.server.core.SessionEventsHandler;
import io.openvidu.server.cdr.CDREventNodeCrashed;
import io.openvidu.server.cdr.NodeRole;
import io.openvidu.server.config.OpenviduConfigPro;
import io.openvidu.server.kurento.core.KurentoSessionEventsHandlerPro;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
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
@RequestMapping({"/openvidu/multi-master"})
public class MultiMasterController {
    private static final Logger log = LoggerFactory.getLogger(MultiMasterController.class);
    @Autowired
    private ApplicationContext context;
    @Autowired
    private SessionEventsHandler sessionEventsHandler;
    @Autowired
    protected OpenviduConfigPro openviduConfigPro;

    public MultiMasterController() {
    }

    @RequestMapping(
            value = {"/masternode-crashed"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> generateMasterNodeCrashedEvent(@RequestBody(required = true) Map<String, ?> params) {
        log.info("REST API: POST {}/masternode-crashed {}", "/openvidu/multi-master", params.toString());
        CDREventNodeCrashed event = this.getMasterNodeCrashedEventFromParams(params);
        ((KurentoSessionEventsHandlerPro)this.sessionEventsHandler).onMasterNodeCrashed(event);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(
            value = {"/masternode-shutdown"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<String> shutdownOpenViduServer(@RequestParam(required = false) boolean waitForever) {
        log.info("REST API: POST {}/masternode-shutdown", "/openvidu/multi-master");
        Thread shutdownThread = new Thread(() -> {
            log.warn("Calling System.exit...");
            SpringApplication.exit(this.context, new ExitCodeGenerator[0]);

            try {
                if (waitForever) {
                    (new Semaphore(0)).acquire();
                }

                System.exit(0);
            } catch (InterruptedException var3) {
                log.error("Error shutting down");
                var3.printStackTrace();
            }

        });
        shutdownThread.setContextClassLoader(this.getClass().getClassLoader());
        shutdownThread.start();
        return new ResponseEntity(HttpStatus.OK);
    }

    public CDREventNodeCrashed getMasterNodeCrashedEventFromParams(Map<?, ?> params) {
        Long timestamp = (Long)params.get("timestamp");
        String id = (String)params.get("id");
        String environmentId = (String)params.get("environmentId");
        String ip = (String)params.get("ip");
        String uri = (String)params.get("uri");
        List<String> sessionIds = (List)params.get("sessionIds");
        List<String> recordingIds = new ArrayList();
        List<String> broadcasts = new ArrayList();
        CDREventNodeCrashed e = new CDREventNodeCrashed(timestamp, id, environmentId, ip, uri, NodeRole.masternode, timestamp, this.openviduConfigPro.getClusterId(), sessionIds, recordingIds, broadcasts);
        return e;
    }
}
