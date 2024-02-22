//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import io.openvidu.server.config.SecurityConfig;
import io.openvidu.server.rest.ApiRestPathRewriteFilterPro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

@Configuration("securityConfigPro")
@Order(Integer.MIN_VALUE)
public class SecurityConfigPro extends SecurityConfig {
    @Autowired
    TokenAuthenticationProvider tokenAuthenticationProvider;

    public SecurityConfigPro() {
    }

    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.headers().xssProtection().disable().frameOptions().sameOrigin();
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry conf = ((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)http.authorizeRequests().antMatchers(new String[]{"/openvidu/elk/openvidu-browser-logs"})).hasRole("USER").antMatchers(new String[]{"/openvidu/elk/**"})).hasRole("ADMIN").antMatchers(new String[]{"/openvidu/multi-master/**"})).hasRole("ADMIN").antMatchers(HttpMethod.POST, new String[]{"/openvidu/inspector-api/**"})).permitAll().antMatchers(HttpMethod.OPTIONS, new String[]{"/openvidu/inspector-api/**"})).permitAll().antMatchers(new String[]{"/"})).permitAll();
        if (Boolean.valueOf(this.environment.getProperty("SUPPORT_DEPRECATED_API"))) {
            ApiRestPathRewriteFilterPro.protectOldPathsPro(conf, this.openviduConf);
        }

    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.tokenAuthenticationProvider);
        super.configureGlobal(auth);
    }
}
