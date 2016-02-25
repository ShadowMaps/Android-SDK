package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
/**
 * Created by Danny Iland on 2/23/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorData {
    @JsonProperty()
    public float pressure;
    @JsonProperty()
    public float lux;
    @JsonProperty()
    public float temp;
    @JsonProperty
    public int stepC;
    @JsonProperty
    public int stepD;
    @JsonProperty
    public float battery;
    @JsonProperty("accel")
    List<float[]> accelerometer;
    @JsonProperty("orient")
    List<float[]> orientation;

    @JsonProperty()
    public List<float[]> getAccelerometer() {
        return accelerometer;
    }

    public void setAccelerometer(List<float[]> accelerometer) {
        this.accelerometer = accelerometer;
    }

    @JsonProperty()
    public List<float[]> getOrientation() {
        return orientation;
    }

    public void setOrientation(List<float[]> orientation) {
        this.orientation = orientation;
    }

    @JsonProperty()
    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }

    public SensorData() {
    }

    @JsonProperty()
    public int getStepC() {
        return stepC;
    }

    public void setStepC(int stepC) {
        this.stepC = stepC;
    }

    @JsonProperty()
    public int getStepD() {
        return stepD;
    }

    public void setStepD(int stepD) {
        this.stepD = stepD;
    }

    public SensorData(float pressure, float lux, float temp, float battery, List<float[]> accelerometer, List<float[]> orientation, int stepD, int stepC) {
        this.pressure = pressure;
        this.lux = lux;
        this.temp = temp;
        this.stepC = stepC;
        this.stepD = stepD;
        this.battery = battery;
        this.accelerometer = accelerometer;
        this.orientation = orientation;
    }

    @JsonProperty()
    public float getPressure() {

        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    @JsonProperty()
    public float getLux() {
        return lux;
    }

    public void setLux(float lux) {
        this.lux = lux;
    }

    @JsonProperty()
    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }
}
