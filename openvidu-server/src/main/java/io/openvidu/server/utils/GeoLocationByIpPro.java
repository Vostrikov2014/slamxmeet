//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.utils;

import com.maxmind.db.Reader.FileMode;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import io.openvidu.server.utils.GeoLocation;
import io.openvidu.server.utils.GeoLocationByIp;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class GeoLocationByIpPro implements GeoLocationByIp {
    private static final Logger log = LoggerFactory.getLogger(GeoLocationByIpPro.class);
    private static DatabaseReader reader = null;
    @Autowired
    private ResourceLoader resourceLoader;

    public GeoLocationByIpPro() {
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Trying to load user location database...");
            Resource resource = this.resourceLoader.getResource("classpath:GeoLite2-City.mmdb");
            InputStream dbAsStream = resource.getInputStream();
            reader = (new DatabaseReader.Builder(dbAsStream)).fileMode(FileMode.MEMORY).build();
            log.info("Database was loaded successfully");
        } catch (NullPointerException | IOException var3) {
            log.error("Database reader cound not be initialized", var3);
        }

    }

    @PreDestroy
    public void preDestroy() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException var2) {
                log.error("Failed to close the GeoLocation reader");
            }
        }

    }

    public GeoLocation getLocationByIp(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        CityResponse response = reader.city(ipAddress);
        return new GeoLocation(ipAddress.getHostAddress(), (String)response.getCountry().getNames().get("en"), (String)response.getCity().getNames().get("en"), response.getLocation().getTimeZone(), response.getLocation().getLatitude(), response.getLocation().getLongitude());
    }
}
