package com.shadowmaps.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Arrays;

/*
 * Device info to receive or transmit over the wire
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)

public class DeviceInfo {

    // box the fields so we can be sure what was set/not set later
    @JsonProperty
    protected String id = "unknown";        // unique ID of the device
    @JsonProperty
    protected String model = "unknown";    // device model
    @JsonProperty
    protected String provider = "unknown";    // device model
    @JsonProperty
    protected String key = "unknown";    // device model
    @JsonProperty
    protected Long utc;            // UTC timestamp
    @JsonProperty("lat")
    protected Double latitude;        // latitude (WGS84)
    @JsonProperty("lon")
    protected Double longitude;        // Longitude (WGS84
    @JsonProperty("alt")
    protected Float altitude;        // altitude (WGS84)

    public Float getAccuracy_horiz() {
        return accuracy_horiz;
    }

    public void setAccuracy_horiz(Float accuracy_horiz) {
        this.accuracy_horiz = accuracy_horiz;
    }

    @JsonProperty("acc")
    protected Float accuracy_horiz;    // horizontal position accuracy (68% prob in circle with this radius)
    @JsonProperty("brng")
    protected Float bearing_horiz;    // horizontal bearing
    @JsonProperty("spd")
    protected Float speed_horiz;        // horizontal speed
    @JsonProperty("accv")
    protected Float accuracy_vert;    // vertical position accuracy (68% prob within alt +- accv)
    @JsonProperty("acc3d")
    protected Float accuracy_3d;    // 3d position accuracy (68% prob in sphere with this radius)
    @JsonProperty
    protected Float prob_indoors;
    @JsonProperty
    protected double[][] cov_horiz;
    @JsonIgnore
    private boolean virtual; // virtual infos are not provided would be e.g. ones at non-reported time stamps

    public String getSkyview_png() {
        return skyview_png;
    }

    public void setSkyview_png(String skyview_png) {
        this.skyview_png = skyview_png;
    }

    @JsonProperty()
    public String skyview_png;

    public DeviceInfo() {
    }

    public DeviceInfo(long utc, double lat, double lon, double alt, double acc, float spd, float brng, String provider, String id, String model, String key, String skyview_png) {
        this.utc = utc;
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = (float)alt;
        this.accuracy_horiz = (float)acc;
        this.speed_horiz = spd;
        this.bearing_horiz = brng;
        this.provider = provider;
        this.id = id;
        this.model = model;
        this.key = key;
    }

    public DeviceInfo(DeviceInfo other) {
        this.id = other.id;
        this.model = other.model;
        this.utc = other.utc;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.altitude = other.altitude;
        this.accuracy_horiz = other.accuracy_horiz;
        this.bearing_horiz = other.bearing_horiz;
        this.speed_horiz = other.speed_horiz;
        this.accuracy_vert = other.accuracy_vert;
        this.accuracy_3d = other.accuracy_3d;
        this.prob_indoors = other.prob_indoors;
        this.cov_horiz = other.cov_horiz;
    }

    public DeviceInfo copy() {
        return new DeviceInfo(this);
    }

    // so the we get something that is human readable inside, e.g., logs
    @JsonIgnore
    @Override
    public String toString() {
        return "DeviceInfo ["
                + (id != null ? "id=" + id + ", " : "")
                + (model != null ? "model=" + model + ", " : "")
                + (utc != null ? "utc=" + utc + ", " : "")
                + (latitude != null ? "latitude=" + latitude + ", " : "")
                + (longitude != null ? "longitude=" + longitude + ", " : "")
                + (altitude != null ? "altitude=" + altitude + ", " : "")
                + (accuracy_horiz != null ? "accuracy_horiz=" + accuracy_horiz + ", " : "")
                + (bearing_horiz != null ? "bearing_horiz=" + bearing_horiz + ", " : "")
                + (speed_horiz != null ? "speed_horiz=" + speed_horiz + ", " : "")
                + (accuracy_vert != null ? "accuracy_vert=" + accuracy_vert + ", " : "")
                + (accuracy_3d != null ? "accuracy_3d=" + accuracy_3d + ", " : "") + "]";
    }

    @JsonIgnore
    public Long getUTC() {
        return utc;
    }

    @JsonIgnore
    public Double getLatitude() {
        return latitude;
    }

    @JsonIgnore
    public Double getLongitude() {
        return longitude;
    }

    @JsonIgnore
    public Float getAltitude() {
        return altitude;
    }

    @JsonIgnore
    public Float getAccuracyVert() {
        return accuracy_vert;
    }

    @JsonIgnore
    public Float getAccuracy3D() {
        return accuracy_3d;
    }

    @JsonIgnore
    public Float getBearingHoriz() {
        return bearing_horiz;
    }

    @JsonIgnore
    public Float getSpeedHoriz() {
        return speed_horiz;
    }

    @JsonIgnore
    public String getID() {
        return id;
    }

    @JsonIgnore
    public String getModel() {
        return model;
    }

    @JsonIgnore
    public Float getProbIndoors() {
        return prob_indoors;
    }

    @JsonIgnore
    public boolean isVirtual() {
        return virtual;
    }


    public DeviceInfo withID(String id) {
        this.id = id;
        return this;
    }

    public DeviceInfo withModel(String model) {
        this.model = model;
        return this;
    }

    public DeviceInfo withLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public DeviceInfo withLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public DeviceInfo withAltitude(float altitude) {
        this.altitude = altitude;
        return this;
    }

    public DeviceInfo withAccuracyVert(float accuracy_vert) {
        this.accuracy_vert = accuracy_vert;
        return this;
    }

    public DeviceInfo withAccuracy3D(float accuracy_3d) {
        this.accuracy_3d = accuracy_3d;
        return this;
    }

    public DeviceInfo withBearingHoriz(float bearing_horiz) {
        this.bearing_horiz = bearing_horiz;
        return this;
    }

    public DeviceInfo withSpeedHoriz(float speed_horiz) {
        this.speed_horiz = speed_horiz;
        return this;
    }

    public DeviceInfo withProbIndoors(float inside_prob) {
        this.prob_indoors = inside_prob;
        return this;
    }

    public DeviceInfo withVirtual(boolean virtual) {
        this.virtual = virtual;
        return this;
    }


    // check that this meets the minimum required spec to be a valid DeviceState object
    @JsonIgnore
    public boolean isValid() {
        if (latitude == null || longitude == null || utc == null || accuracy_horiz == null)
            return false;
        if (accuracy_horiz < 0 || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            return false;
        return true;
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accuracy_3d == null) ? 0 : accuracy_3d.hashCode());
        result = prime * result + ((accuracy_horiz == null) ? 0 : accuracy_horiz.hashCode());
        result = prime * result + ((accuracy_vert == null) ? 0 : accuracy_vert.hashCode());
        result = prime * result + ((altitude == null) ? 0 : altitude.hashCode());
        result = prime * result + ((bearing_horiz == null) ? 0 : bearing_horiz.hashCode());
        result = prime * result + Arrays.deepHashCode(cov_horiz);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
        result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((prob_indoors == null) ? 0 : prob_indoors.hashCode());
        result = prime * result + ((speed_horiz == null) ? 0 : speed_horiz.hashCode());
        result = prime * result + ((utc == null) ? 0 : utc.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeviceInfo other = (DeviceInfo) obj;
        if (accuracy_3d == null) {
            if (other.accuracy_3d != null)
                return false;
        } else if (!accuracy_3d.equals(other.accuracy_3d))
            return false;
        if (accuracy_horiz == null) {
            if (other.accuracy_horiz != null)
                return false;
        } else if (!accuracy_horiz.equals(other.accuracy_horiz))
            return false;
        if (accuracy_vert == null) {
            if (other.accuracy_vert != null)
                return false;
        } else if (!accuracy_vert.equals(other.accuracy_vert))
            return false;
        if (altitude == null) {
            if (other.altitude != null)
                return false;
        } else if (!altitude.equals(other.altitude))
            return false;
        if (bearing_horiz == null) {
            if (other.bearing_horiz != null)
                return false;
        } else if (!bearing_horiz.equals(other.bearing_horiz))
            return false;
        if (!Arrays.deepEquals(cov_horiz, other.cov_horiz))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (latitude == null) {
            if (other.latitude != null)
                return false;
        } else if (!latitude.equals(other.latitude))
            return false;
        if (longitude == null) {
            if (other.longitude != null)
                return false;
        } else if (!longitude.equals(other.longitude))
            return false;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (utc == null) {
            if (other.utc != null)
                return false;
        } else if (!utc.equals(other.utc))
            return false;
        return true;
    }
}

