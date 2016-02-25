package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 2/23/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InertialInfo {
    @JsonProperty()
    public float pressure;
    @JsonProperty()
    public float lux;
    @JsonProperty()
    public float temp;

    public InertialInfo() {
    }


    public InertialInfo(float pressure, float lux, float temp) {
        this.pressure = pressure;
        this.lux = lux;
        this.temp = temp;
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
