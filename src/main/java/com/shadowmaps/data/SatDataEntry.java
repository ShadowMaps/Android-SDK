package com.shadowmaps.data;

/**
 * Created by Danny Iland on 2/16/15.
 */
public class SatDataEntry {

    public double time;
    public double lat;
    public double lon;
    public double alt;
    public double acc;
    public float speed;
    public float bearing;
    public String provider;
    public String ongoing;
    public SatDataArray satDataArray;


        public double getTime() {
            return time;
        }

    public SatDataEntry() {}

    public SatDataEntry(double time, double lat, double lon, double alt, double acc,
                        float speed, float bearing, String provider, String ongoing,
                        int[] mPrns, float[] mSnrs, float[] mSvElevations, float[] mSvAzimuths,
                        boolean[] ephemeris, boolean[] almanac, boolean[] used) {
        this.time = time;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.acc = acc;
        this.speed = speed;
        this.bearing = bearing;
        this.provider = provider;
        this.ongoing = ongoing;
        this.satDataArray = new SatDataArray(
                mPrns, mSnrs, mSvElevations, mSvAzimuths,
                ephemeris, almanac, used);

    }

    public SatDataEntry(double time, double lat, double lon, double alt, double acc,
                        float speed, float bearing, String provider, String ongoing,
                        String jsonArray) {
        this.time = time;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.acc = acc;
        this.speed = speed;
        this.bearing = bearing;
        this.provider = provider;
        this.ongoing = ongoing;
        this.satDataArray = new SatDataArray(jsonArray);

    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAlt() {
        return alt;
    }


    public double getAcc() {
        return acc;
    }


    public float getSpeed() {
        return speed;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOngoing() {
        return ongoing;
    }

    public void setOngoing(String ongoing) {
        this.ongoing = ongoing;
    }

    public SatDataArray getSatDataArray() {
        return satDataArray;
    }

    public void setSatDataArray(SatDataArray satDataArray) {
        this.satDataArray = satDataArray;
    }
}
