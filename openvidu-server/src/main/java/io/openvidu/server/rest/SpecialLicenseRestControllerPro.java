//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import com.google.gson.JsonObject;
import io.openvidu.server.config.SpecialLicenseConfig;
import io.openvidu.server.utils.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping({"/openvidu/api"})
@ConditionalOnProperty(
        name = {"OPENVIDU_PRO_LICENSE_OFFLINE"},
        havingValue = "true"
)
public class SpecialLicenseRestControllerPro {
    @Autowired
    private SpecialLicenseConfig specialLicenseConfig;

    public SpecialLicenseRestControllerPro() {
    }

    @RequestMapping(
            value = {"/offline-license"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<String> offlineLicense() {
        try {
            JsonObject json = this.specialLicenseConfig.getLicenseDetails();
            return new ResponseEntity(json.toString(), RestUtils.getResponseHeaders(), HttpStatus.OK);
        } catch (Exception var2) {
            return RestUtils.getErrorResponse("Error checking license details: " + var2.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
