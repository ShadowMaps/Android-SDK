package com.shadowmaps.sdk;

/**
 * Created by user on 1/19/16.
 */
public class Mode {
    public static final String PEDESTRIAN = "pedestrian";
    public static final String VEHICULAR = "vehicular";
    public static final String UNKNOWN = "unknown";

    public static final String BATCH = "batch";
    public static final String REALTIME = "realtime";
    public static final String STOPPED = "stopped";

    public static final String PASSIVE = "passive";
    public static final String PERIODIC = "periodic";

    private static int interval_seconds;

    public void setupdateInterval(int seconds) {
        this.interval_seconds = seconds;
    }

}
