//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.kurento.core;

public enum NetworkQualityLevel {
    EXCELLENT("excellent", 5),
    GOOD("good", 4),
    NON_OPTIMAL("non_optimal", 3),
    POOR("poor", 2),
    BAD("bad", 1),
    BROKEN("broken", 0);

    private String label;
    private int value;

    private NetworkQualityLevel(String label, int value) {
        this.label = label;
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }

    public String toString() {
        return this.getLabel();
    }
}
