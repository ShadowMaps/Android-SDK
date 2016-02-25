package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 2/23/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cell {
    @JsonProperty()
    String cellid;
    @JsonProperty()
    long timestamp;
    @JsonProperty()
    int lac;
    @JsonProperty()
    int rssi;

    public Cell() {}

    public Cell(String cellid, int lac, int level, long timestamp) {
        this.cellid = cellid;
        this.timestamp = timestamp;
        lac = lac;
        this.rssi = level;
    }

    public String getBssid() {
        return cellid;
    }

    public void setBssid(String bssid) {
        this.cellid = bssid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFrequency() {
        return lac;
    }

    public void setFrequency(int frequency) {
        this.lac = frequency;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
