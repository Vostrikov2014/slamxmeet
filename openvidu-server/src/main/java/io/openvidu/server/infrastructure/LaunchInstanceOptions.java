//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import io.openvidu.server.infrastructure.InstanceType;

public class LaunchInstanceOptions {
    private InstanceType instanceType;
    private String infrastructureInstanceType;
    private Integer volumeSize;

    public LaunchInstanceOptions(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public InstanceType getInstanceType() {
        return this.instanceType;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getInfrastructureInstanceType() {
        return this.infrastructureInstanceType;
    }

    public void setInfrastructureInstanceType(String infrastructureInstanceType) {
        this.infrastructureInstanceType = infrastructureInstanceType;
    }

    public Integer getVolumeSize() {
        return this.volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String toString() {
        return "LaunchInstanceOptions{instanceType=" + this.instanceType + ", infrastructureInstanceType='" + this.infrastructureInstanceType + "', volumeSize='" + this.volumeSize + "'}";
    }
}
