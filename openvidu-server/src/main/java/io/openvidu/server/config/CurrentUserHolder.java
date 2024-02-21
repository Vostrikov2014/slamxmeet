//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.pro.config;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(
        value = "session",
        proxyMode = ScopedProxyMode.TARGET_CLASS
)
public class CurrentUserHolder {
    private boolean logged = false;
    private String userPass;

    public CurrentUserHolder() {
    }

    public void setLoggedUser(boolean logged) {
        this.logged = logged;
    }

    public boolean isLoggedUser() {
        return this.logged;
    }

    public void setUserPass(String userPass) {
        this.userPass = userPass;
    }

    public String getUserPass() {
        return this.userPass;
    }
}
