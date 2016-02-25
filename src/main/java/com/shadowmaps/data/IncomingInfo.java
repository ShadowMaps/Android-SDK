package com.shadowmaps.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Danny Iland on 2/17/15.
 */

@JsonInclude(Include.NON_EMPTY)
public class IncomingInfo {
    @JsonProperty()
    DeviceInfo dev;
    @JsonProperty()
    List<com.shadowmaps.data.SatelliteInfo> sats;
    @JsonProperty()
    com.shadowmaps.data.SensorData sensors;
    @JsonProperty()
    List<com.shadowmaps.data.WiFi> wifi;
    @JsonProperty()
    List<Cell> cells;
//    @JsonProperty()
//    InertialInfo inertial;


    public IncomingInfo() {}

    public IncomingInfo(DeviceInfo dev, List<com.shadowmaps.data.SatelliteInfo> sats, com.shadowmaps.data.SensorData sensors, List<com.shadowmaps.data.WiFi> wifi, List<Cell> cells) {
        this.dev = dev;
        this.sats = sats;
        this.sensors = sensors;
        this.wifi = wifi;
        this.cells = cells;

    }

    public void setDev(DeviceInfo dev) {
        this.dev = dev;
    }

    public void setSats(List<com.shadowmaps.data.SatelliteInfo> sats) {
        this.sats = sats;
    }

    public void setSensors(com.shadowmaps.data.SensorData sensors) {
        this.sensors = sensors;
    }

    public void setWifi(List<com.shadowmaps.data.WiFi> wifi) {
        this.wifi = wifi;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    @JsonProperty()
    public com.shadowmaps.data.SensorData getSensors() { return sensors; }

    @JsonProperty()
    public List<com.shadowmaps.data.WiFi> getWifi() {
        return wifi;
    }

    @JsonProperty()
    public List<Cell> getCells() {
        return cells;
    }

    @JsonProperty()
    public DeviceInfo getDev() {
        return dev;
    }

    @JsonProperty()
    public List<com.shadowmaps.data.SatelliteInfo> getSats() {
        return sats;
    }

}
