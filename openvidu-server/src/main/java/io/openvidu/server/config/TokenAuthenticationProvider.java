//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.config;

import io.openvidu.server.core.TokenRegister;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {
    private final String CREDENTIALS_SEPARATOR = "%/%";
    @Autowired
    private OpenviduConfigPro openviduConfigPro;
    @Autowired
    private TokenRegister tokenRegister;

    public TokenAuthenticationProvider() {
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (this.openviduConfigPro.getSendBrowserLogs() == BrowserLog.disabled) {
            throw new BadCredentialsException("Openvidu debug browser logs are disabled");
        } else if (!authentication.getName().contains("%/%")) {
            throw new BadCredentialsException("Credentials not sent correctly");
        } else {
            String finalUserId = authentication.getName().split("%/%")[0];
            String sessionId = authentication.getName().split("%/%")[1];
            String tokenString = authentication.getCredentials().toString();
            if (finalUserId != null && !finalUserId.isEmpty()) {
                if (sessionId != null && !sessionId.isEmpty()) {
                    if (tokenString != null && !tokenString.isEmpty()) {
                        if (this.tokenRegister.isTokenRegistered(tokenString, finalUserId, sessionId)) {
                            return new UsernamePasswordAuthenticationToken(authentication.getName(), authentication.getCredentials(), Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                        } else {
                            throw new BadCredentialsException("Token is not valid");
                        }
                    } else {
                        throw new BadCredentialsException("Not valid credentials. Token is null or empty");
                    }
                } else {
                    throw new BadCredentialsException("Not valid credentials. Session id is null or empty");
                }
            } else {
                throw new BadCredentialsException("Not valid credentials. Final User id is null or empty");
            }
        }
    }

    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }
}
