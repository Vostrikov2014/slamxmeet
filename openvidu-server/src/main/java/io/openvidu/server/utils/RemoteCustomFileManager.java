//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.utils;

import io.openvidu.server.utils.CustomFileManager;

public class RemoteCustomFileManager extends CustomFileManager {
    private RemoteDockerManager dockerManager;

    public RemoteCustomFileManager(RemoteDockerManager dockerManager) {
        this.dockerManager = dockerManager;
    }

    public void waitForFileToExistAndNotEmpty(String mediaNodeId, String absolutePathToFile) throws Exception {
        this.dockerManager.waitForFileToExistAndNotEmpty(mediaNodeId, absolutePathToFile);
    }

    public int maxSecondsWaitForFile() {
        return this.dockerManager.maxSecondsWaitForFile();
    }
}
