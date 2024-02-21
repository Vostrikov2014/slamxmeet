//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.account;

public class LicenseAndUsageInterval {
    private int licensePetitions;
    private int licenseSleepSeconds;
    private int allowedLicenseConsecutiveFailures;
    private int usageSleepSeconds;
    private int allowedUsageFailures;

    public LicenseAndUsageInterval(int licensePetitions, int licenseSleepSeconds, int allowedLicenseConsecutiveFailures, int usageSleepSeconds, int allowedUsageFailures) {
        this.licensePetitions = licensePetitions;
        this.licenseSleepSeconds = licenseSleepSeconds;
        this.allowedLicenseConsecutiveFailures = allowedLicenseConsecutiveFailures;
        this.usageSleepSeconds = usageSleepSeconds;
        this.allowedUsageFailures = allowedUsageFailures;
    }

    public int getLicensePetitions() {
        return this.licensePetitions;
    }

    public int getLicenseSleepTimeInSeconds() {
        return this.licenseSleepSeconds;
    }

    public int getLicenseAllowedConsecutiveFailures() {
        return this.allowedLicenseConsecutiveFailures;
    }

    public int getUsageSleepTimeInSeconds() {
        return this.usageSleepSeconds;
    }

    public int getUsageAllowedFailures() {
        return this.allowedUsageFailures;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof LicenseAndUsageInterval)) {
            return false;
        } else {
            LicenseAndUsageInterval interval = (LicenseAndUsageInterval)o;
            return interval.allowedLicenseConsecutiveFailures == this.allowedLicenseConsecutiveFailures && interval.allowedUsageFailures == this.allowedUsageFailures && interval.licensePetitions == this.licensePetitions && interval.licenseSleepSeconds == this.licenseSleepSeconds && interval.usageSleepSeconds == this.usageSleepSeconds;
        }
    }

    public String toString() {
        return "[" + this.licensePetitions + "," + this.licenseSleepSeconds + "," + this.allowedLicenseConsecutiveFailures + "," + this.usageSleepSeconds + "," + this.allowedUsageFailures + "]";
    }
}
