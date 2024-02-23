//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.rest;

import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.rest.ApiRestPathRewriteFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

public class ApiRestPathRewriteFilterPro extends ApiRestPathRewriteFilter {
    public ApiRestPathRewriteFilterPro() {
        this.PATH_REDIRECTIONS_MAP.put("/pro/", "/openvidu/api/");
        this.PATH_REDIRECTIONS_MAP.put("/elasticsearch/", "/openvidu/elk/");
        this.PATH_REDIRECTIONS_MAP.put("/api-login/", "/openvidu/inspector-api/");
        this.PATH_REDIRECTIONS_ARRAY = (String[])this.PATH_REDIRECTIONS_MAP.keySet().toArray(new String[this.PATH_REDIRECTIONS_MAP.size()]);
    }

    public static void protectOldPathsPro(ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry conf, OpenviduConfig openviduConf) throws Exception {
        ((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)conf.antMatchers(new String[]{"/elasticsearch/**"})).hasRole("ADMIN").antMatchers(new String[]{"/pro/**"})).hasRole("ADMIN").antMatchers(HttpMethod.POST, new String[]{"/api-login/**"})).permitAll().antMatchers(HttpMethod.OPTIONS, new String[]{"/api-login/**"})).permitAll();
    }
}
