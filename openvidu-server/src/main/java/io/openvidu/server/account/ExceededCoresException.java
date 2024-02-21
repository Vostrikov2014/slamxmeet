//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account;

import io.openvidu.server.kurento.kms.Kms;

public class ExceededCoresException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private Kms kms;
    private int maxCores;
    private int existingCores;
    private int newCores;

    public ExceededCoresException(Kms kms, int maxCores, int existingCores, int newCores) {
        this.kms = kms;
        this.maxCores = maxCores;
        this.existingCores = existingCores;
        this.newCores = newCores;
    }

    public Kms getKms() {
        return this.kms;
    }

    public int getMaxCores() {
        return this.maxCores;
    }

    public int getExistingCores() {
        return this.existingCores;
    }

    public int getNewCores() {
        return this.newCores;
    }
}
