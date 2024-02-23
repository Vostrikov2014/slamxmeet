//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openvidu.server.config.CurrentUserHolder;
import io.openvidu.server.config.OpenviduConfigPro;
import javax.servlet.http.HttpSession;

import io.openvidu.server.config.OpenviduConfigPro;
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
@RequestMapping({"/openvidu/inspector-api"})
public class DashboardLoginController {
    private static final Logger log = LoggerFactory.getLogger(DashboardLoginController.class);
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private CurrentUserHolder currentUserHolder;

    public DashboardLoginController() {
    }

    @RequestMapping(
            value = {"/login"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<?> login(@RequestBody String userPass) {
        log.info("REST API: POST {}/login", "/openvidu/inspector-api");
        if (this.userAlreadyLogged()) {
            return new ResponseEntity(this.currentUserHolder.getUserPass(), HttpStatus.OK);
        } else {
            JsonObject userPassJson = JsonParser.parseString(userPass).getAsJsonObject();
            JsonElement user = userPassJson.get("user");
            JsonElement pass = userPassJson.get("pass");
            if (this.login(user, pass)) {
                this.currentUserHolder.setLoggedUser(true);
                this.currentUserHolder.setUserPass(userPass);
                return new ResponseEntity(userPass, HttpStatus.OK);
            } else {
                this.currentUserHolder.setLoggedUser(false);
                this.currentUserHolder.setUserPass((String)null);
                return new ResponseEntity(HttpStatus.FORBIDDEN);
            }
        }
    }

    @RequestMapping(
            value = {"/logout"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<Object> logout(HttpSession session) {
        log.info("REST API: POST {}/logout", "/openvidu/inspector-api");
        session.invalidate();
        this.currentUserHolder.setLoggedUser(false);
        this.currentUserHolder.setUserPass((String)null);
        return new ResponseEntity(HttpStatus.OK);
    }

    private boolean login(JsonElement user, JsonElement pass) {
        return user != null && pass != null && "OPENVIDUAPP".equals(user.getAsString()) && this.openviduConfigPro.getOpenViduSecret().equals(pass.getAsString());
    }

    private boolean userAlreadyLogged() {
        return this.currentUserHolder.isLoggedUser();
    }
}
