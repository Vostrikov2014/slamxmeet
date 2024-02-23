//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.monitoring.model;

public enum SessionProblem {
    MEDIA_FLOWING_READY_TOO_LATE("MEDIA_FLOWING_READY_TOO_LATE"),
    MEDIA_FLOWING_BACK_TOO_LATE("MEDIA_FLOWING_BACK_TOO_LATE"),
    MEDIA_FLOWING_NOT_FOUND("MEDIA_FLOWING_NOT_FOUND"),
    ICE_FAILED("ICE_FAILED");

    private String value;

    private SessionProblem(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public String getDescription() {
        switch (this.getValue()) {
            case "ICE_FAILED":
                return "Found <strong>" + ICE_FAILED.getValue() + "</strong> event in some participant";
            default:
                return "";
        }
    }

    public String getDescription(String mediaType, String time) {
        switch (this.getValue()) {
            case "MEDIA_FLOWING_BACK_TOO_LATE":
                return "<strong>" + mediaType + "</strong> had a <strong>" + time + "</strong> break";
            case "MEDIA_FLOWING_READY_TOO_LATE":
                return "<strong>" + mediaType + "</strong> was <strong>FLOWING after " + time + "</strong> from endpoint creation";
            case "MEDIA_FLOWING_NOT_FOUND":
                return "Stopped transmitting <strong>" + mediaType + "</strong> and connection was disconected <strong>" + time + "</strong> later";
            default:
                return "";
        }
    }
}
