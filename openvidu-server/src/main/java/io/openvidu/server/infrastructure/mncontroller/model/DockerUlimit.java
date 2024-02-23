//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure.mncontroller.model;

public class DockerUlimit {
    private String Name;
    private int Soft;
    private int Hard;

    public DockerUlimit(String name, int soft, int hard) {
        this.Name = name;
        this.Soft = soft;
        this.Hard = hard;
    }
}
