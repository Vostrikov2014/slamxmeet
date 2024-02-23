//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

public enum InstanceStatus {
    launching {
        public String toString() {
            return "launching";
        }
    },
    canceled {
        public String toString() {
            return "canceled";
        }
    },
    failed {
        public String toString() {
            return "failed";
        }
    },
    running {
        public String toString() {
            return "running";
        }
    },
    waitingIdleToTerminate {
        public String toString() {
            return "waiting-idle-to-terminate";
        }
    },
    terminating {
        public String toString() {
            return "terminating";
        }
    },
    terminated {
        public String toString() {
            return "terminated";
        }
    };

    private InstanceStatus() {
    }

    public static InstanceStatus stringToStatus(String str) throws IllegalArgumentException {
        switch (str) {
            case "launching":
                return launching;
            case "canceled":
                return canceled;
            case "failed":
                return failed;
            case "running":
                return running;
            case "waiting-idle-to-terminate":
                return waitingIdleToTerminate;
            case "terminating":
                return terminating;
            case "terminated":
                return terminated;
            default:
                throw new IllegalArgumentException();
        }
    }
}
