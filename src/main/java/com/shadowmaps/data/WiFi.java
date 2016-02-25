package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 2/23/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WiFi {
    @JsonProperty()
    String bssid;
    @JsonProperty()
    long timestamp;
    @JsonProperty()
    int frequency;
    @JsonProperty()
    int rssi;

    public WiFi() {}

    public WiFi(String bssid, int frequency, int level, long timestamp) {
        this.bssid = bssid;
        this.timestamp = timestamp;
        this.frequency = frequency;
        this.rssi = level;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
