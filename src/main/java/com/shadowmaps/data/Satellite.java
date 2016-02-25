package com.shadowmaps.data;

/**
 * Created by Danny Iland on 2/17/15.
 */
public class Satellite {
    int prn;
    double snr;
    double el;
    double az;
    boolean eph;
    boolean alm;
    boolean used;

    public Satellite() {}

    public void setPrn(int prn) {
        this.prn = prn;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }

    public void setEl(double el) {
        this.el = el;
    }

    public void setAz(double az) {
        this.az = az;
    }

    public void setEph(boolean eph) {
        this.eph = eph;
    }

    public void setAlm(boolean alm) {
        this.alm = alm;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Satellite(int prn, double snr, double el, double az, boolean eph, boolean alm, boolean used) {
        this.prn = prn;
        this.snr = snr;
        this.el = el;
        this.az = az;
        this.eph = eph;
        this.alm = alm;
        this.used = used;
    }

    public Satellite(int prn, double snr, double el, double az) {
        this.prn = prn;
        this.snr = snr;
        this.el = el;
        this.az = az;
    }

    public int getPrn() {

        return prn;
    }

    public double getSnr() {
        return snr;
    }

    public double getEl() {
        return el;
    }

    public double getAz() {
        return az;
    }

    public boolean isEph() {
        return eph;
    }

    public boolean isAlm() {
        return alm;
    }

    public boolean isUsed() {
        return used;
    }

}
