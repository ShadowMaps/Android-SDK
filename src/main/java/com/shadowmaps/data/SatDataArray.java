package com.shadowmaps.data;

/**
 * Created by Danny Iland on 2/16/15.
 */
public class SatDataArray {
        int[] mPrns;
        float[] mSnrs;
        float[] mSvElevations;
        float[] mSvAzimuths;
        boolean[] ephemeris;
        boolean[] almanac;
        boolean[] used;

    public SatDataArray() {}

    public SatDataArray(int[] mPrns, float[] mSnrs, float[] mSvElevations, float[] mSvAzimuths,
                        boolean[] ephemeris, boolean[] almanac, boolean[] used) {
        this.mPrns = mPrns;
        this.mSnrs = mSnrs;
        this.mSvElevations = mSvElevations;
        this.mSvAzimuths = mSvAzimuths;
        this.ephemeris = ephemeris;
        this.almanac = almanac;
        this.used = used;
    }

    public SatDataArray(String json) {
        //Extract out from JSON
    }

    public int[] getmPrns() {
        return mPrns;
    }

    public void setmPrns(int[] mPrns) {
        this.mPrns = mPrns;
    }

    public float[] getmSnrs() {
        return mSnrs;
    }

    public void setmSnrs(float[] mSnrs) {
        this.mSnrs = mSnrs;
    }

    public float[] getmSvElevations() {
        return mSvElevations;
    }

    public void setmSvElevations(float[] mSvElevations) {
        this.mSvElevations = mSvElevations;
    }

    public float[] getmSvAzimuths() {
        return mSvAzimuths;
    }

    public void setmSvAzimuths(float[] mSvAzimuths) {
        this.mSvAzimuths = mSvAzimuths;
    }

    public boolean[] getEphemeris() {
        return ephemeris;
    }

    public void setEphemeris(boolean[] ephemeris) {
        this.ephemeris = ephemeris;
    }

    public boolean[] getAlmanac() {
        return almanac;
    }

    public void setAlmanac(boolean[] almanac) {
        this.almanac = almanac;
    }

    public boolean[] getUsed() {
        return used;
    }

    public void setUsed(boolean[] used) {
        this.used = used;
    }
}
