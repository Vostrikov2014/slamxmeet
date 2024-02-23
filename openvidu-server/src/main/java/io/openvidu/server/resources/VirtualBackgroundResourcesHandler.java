//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.resources;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualBackgroundResourcesHandler implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    public VirtualBackgroundResourcesHandler() {
    }

    public void customize(ConfigurableServletWebServerFactory server) {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("wasm", "application/wasm");
        server.setMimeMappings(mappings);
    }
}
