//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.core.TokenRegister;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@CrossOrigin
@RequestMapping({"/openvidu/virtual-background"})
public class VirtualBackgroundRestController {
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private TokenRegister tokenRegister;

    public VirtualBackgroundRestController() {
    }

    @GetMapping({"**"})
    public ResponseEntity<Resource> getVirtualBackgroundResource(HttpServletRequest request, @RequestParam(required = false) String token) throws Exception {
        String path = request.getServletPath().replaceFirst("^/openvidu/virtual-background", "virtual-background");
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            if (this.isProtected(resource)) {
                try {
                    this.checkToken(token);
                } catch (Exception var8) {
                    return new ResponseEntity(HttpStatus.UNAUTHORIZED);
                }
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            String mediaType = this.getMediaType(resource);
            responseHeaders.add("Content-Type", mediaType);
            ContentDisposition disposition = ContentDisposition.builder("inline").filename(resource.getFilename()).build();
            responseHeaders.setContentDisposition(disposition);
            return new ResponseEntity(resource, responseHeaders, HttpStatus.OK);
        }
    }

    private void checkToken(String token) throws Exception {
        if (token == null) {
            throw new Exception();
        } else {
            String decodedToken = new String(Base64.getDecoder().decode(token));
            if (!this.tokenRegister.isTokenRegistered(decodedToken)) {
                String sessionId;
                try {
                    sessionId = this.getParamValue(decodedToken, "sessionId");
                    if (sessionId == null) {
                        throw new Exception();
                    }
                } catch (Exception var5) {
                    throw new Exception();
                }

                Session session = this.sessionManager.getSessionWithNotActive(sessionId);
                if (session == null || !session.hasToken(decodedToken)) {
                    throw new Exception();
                }
            }

        }
    }

    private String getParamValue(String link, String paramName) throws URISyntaxException {
        List<NameValuePair> queryParams = (new URIBuilder(link)).getQueryParams();
        return (String)queryParams.stream().filter((param) -> {
            return param.getName().equalsIgnoreCase(paramName);
        }).map(NameValuePair::getValue).findFirst().orElse((Object)null);
    }

    private boolean isProtected(Resource resource) {
        return !resource.getFilename().equals("tflite.wasm") && !resource.getFilename().equals("tflite-simd.wasm");
    }

    private String getMediaType(Resource resource) {
        return resource.getFilename().endsWith(".wasm") ? "application/wasm" : ((MediaType)MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM)).toString();
    }
}
