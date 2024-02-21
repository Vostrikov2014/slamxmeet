//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account.usage;

public class NodeUsageInfoFactory {
    public NodeUsageInfoFactory() {
    }

    public NodeUsageInfo copyNodeUsageInfo(NodeUsageInfo info) {
        NodeUsageInfo infoCopy = null;
        if (info instanceof KmsNodeUsageInfo) {
            infoCopy = new KmsNodeUsageInfo(info, info.getEndTime(), info.getQuantity(), ((KmsNodeUsageInfo)info).getServerInfo());
        } else if (info instanceof OpenviduNodeUsageInfo) {
            infoCopy = new OpenviduNodeUsageInfo(info, info.getEndTime(), info.getQuantity(), ((OpenviduNodeUsageInfo)info).getVersion());
        }

        return (NodeUsageInfo)infoCopy;
    }
}
