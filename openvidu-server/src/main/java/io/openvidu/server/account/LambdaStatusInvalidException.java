//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account;

public class LambdaStatusInvalidException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private int status;

    public LambdaStatusInvalidException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }
}
