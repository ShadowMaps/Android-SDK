package com.shadowmaps.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/*
 * Satellite info to receive (or transmit) over wire
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

public class SatelliteInfo {	
	
	// box the fields so we can be sure what was set/not set later
	@JsonProperty
	protected Integer prn;
	@JsonProperty("az")
	protected Float azimuth;
	@JsonProperty("el")
	protected Float elevation;
	@JsonProperty
	protected Float snr;
	@JsonProperty
	protected Float filt_snr;
	@JsonProperty
	protected Float ref_snr;						// reference SNR levelevation (dB)
	@JsonProperty
	protected Boolean used;
	@JsonProperty("eph")
	protected Boolean ephemeris;
	@JsonProperty("alm")
	protected Boolean almanac;
	@JsonProperty
	protected Float flos_over_fnlos;
	@JsonProperty
	protected Boolean usable;  // if usable for shadow matching (e.g., not belevationow threshold elevation)
	@JsonProperty
	protected Float pct_calibrated; // percent calibrated, used in the SNR model
	@JsonProperty
	protected Float los_prob; // the probability (percent) that this satellite is line of sight given the map and its SNR
	@JsonProperty("llr")
	protected Float log10_flos_over_fnlos;	// included for legacy purposes: TODO: tell brian to just use log_10 ( flos_over_fnlos )
	
	@JsonIgnore
	protected SatelliteInfo parent;
	@JsonIgnore
	protected Long utc;							// time of this satellite observation

	public SatelliteInfo() {}

	public SatelliteInfo(int prn, double snr, double el, double az, boolean eph, boolean alm, boolean use) {
		this.prn = prn;
		this.snr = (float)snr;
		this.elevation = (float)el;
		this.azimuth = (float)az;
		this.ephemeris = eph;
		this.almanac = alm;
		this.used = use;
	}

	public SatelliteInfo(SatelliteInfo other) {
		this.prn = other.prn;
		this.azimuth = other.azimuth;
		this.elevation = other.elevation;
		this.snr = other.snr;
		this.used = other.used;
		this.ephemeris = other.ephemeris;
		this.almanac = other.almanac;
		this.flos_over_fnlos = other.flos_over_fnlos;
		this.usable = other.usable;
		this.ref_snr = other.ref_snr;
		this.filt_snr = other.filt_snr;
		this.utc = other.utc;
		this.pct_calibrated = other.pct_calibrated;
		this.los_prob = other.los_prob;
	}
	
	public SatelliteInfo copy() { 
		return new SatelliteInfo(this);
	}
	
	@JsonIgnore
	public Integer getPRN() { return prn; }
	@JsonIgnore
	public Float getAzimuth() { return azimuth; }
	@JsonIgnore
	public Float getElevation() { return elevation; }
	@JsonIgnore
	public Float getSNR() { return snr; }
	@JsonIgnore
	public Float getRefSNR() { return ref_snr; }
	@JsonIgnore
	public Float getFiltSNR() { return filt_snr; }
	@JsonIgnore
	public Boolean hasAlmanac() { return almanac; }
	@JsonIgnore
	public Boolean hasEphemeris() { return ephemeris; }
	@JsonIgnore
	public Boolean isUsed() { return used; }
	@JsonIgnore
	public boolean isUsedNotNull() { return used==null ? false : used; }
	@JsonIgnore
	public Float getFlosOverFnlos() { return flos_over_fnlos; }
	@JsonIgnore
	public Boolean isUsable() { return usable; }
	@JsonIgnore
	public boolean isUsableNotNull() { return usable==null ? false : usable; }
	@JsonIgnore
	public Long getUTC() { return utc; }
	@JsonIgnore
	public boolean isObserved() {
		return filt_snr==null ? false : filt_snr!=0;
	}
	@JsonIgnore
	public Float getPctCalibrated() { return pct_calibrated; }
	@JsonIgnore
	public Float getLosProb() { return los_prob; }
	@JsonIgnore
	public SatelliteInfo getParent() { return parent; }
	@JsonIgnore
	public boolean hasParent() { return parent!=null; }
	@JsonIgnore
	public double getSNRNonNull() { return snr==null ? 0 : snr; }
	@JsonIgnore
	public double getRefSNRNonNull() { return ref_snr==null ? 0 : ref_snr; }

	public SatelliteInfo withPRN(Integer prn) { this.prn = prn; return this; }
	public SatelliteInfo withSNR(Float snr) { this.snr = snr; return this; }
	public SatelliteInfo withFiltSNR(Float filt_snr) { this.filt_snr = filt_snr; return this; }
	public SatelliteInfo withAzimuth(Float azimuth) { this.azimuth = azimuth; return this; }
	public SatelliteInfo withElevation(Float elevation) { this.elevation = elevation; return this; }
	public SatelliteInfo withEphemeris(Boolean ephemeris) { this.ephemeris = ephemeris; return this; }
	public SatelliteInfo withAlmanac(Boolean almanac) { this.almanac = almanac; return this; }
	public SatelliteInfo withUsed(Boolean used) { this.used = used; return this; }
	public SatelliteInfo withFlosOverFnlos(Float flos_over_fnlos) { 
		this.flos_over_fnlos = flos_over_fnlos; 
		this.log10_flos_over_fnlos = (float) Math.log10( flos_over_fnlos );
		return this;
	}
	public SatelliteInfo withUsable(Boolean usable) { this.usable = usable; return this; }
	public SatelliteInfo withUTC(long utc) { this.utc = utc; return this; }
	public SatelliteInfo withRefSNR(float ref_snr) { this.ref_snr = ref_snr; return this; }
	public SatelliteInfo withPctCalibrated(float pct_calibrated) { this.pct_calibrated = pct_calibrated; return this; }
	public SatelliteInfo withLosProb(float los_prob) { this.los_prob = los_prob; return this; }
	public SatelliteInfo withParentAndClearGrandparent(SatelliteInfo parent) { 
		this.parent = parent; 
		parent.parent = null; // so we don't build up an endless history of descendants
		return this;
	}
	public void assertOlderThanParent() {
		if (parent!=null && utc<parent.getUTC()) {
			throw new IllegalStateException(
					String.format("SatelliteInfo (%d) is %d millis younger than its parent (%d)", 
							utc, utc - parent.getUTC(), parent.getUTC()));
		}
	}
	
	@JsonIgnore @Override
	public String toString() {
		return "SatelliteInfo ["
				+ (prn != null ? "prn=" + prn + ", " : "")
				+ (azimuth != null ? "az=" + azimuth + ", " : "")
				+ (elevation != null ? "el=" + elevation + ", " : "")
				+ (snr != null ? "snr=" + snr + ", " : "")
				+ (ref_snr != null ? "snr_ref=" + ref_snr + ", " : "")
				+ (filt_snr != null ? "snr_filt=" + filt_snr + ", " : "")
				+ (used != null ? "used=" + used + ", " : "")
				+ (pct_calibrated != null ? "pct_calibrated=" + pct_calibrated + ", " : "")
				+ (flos_over_fnlos != null ? "flos_over_fnlos=" + flos_over_fnlos + ", " : "")
				+ (usable != null ? "usable=" + usable : "") + "]";
	}
	
	@JsonIgnore
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((almanac == null) ? 0 : almanac.hashCode());
		result = prime * result + ((azimuth == null) ? 0 : azimuth.hashCode());
		result = prime * result + ((elevation == null) ? 0 : elevation.hashCode());
		result = prime * result + ((ephemeris == null) ? 0 : ephemeris.hashCode());
		result = prime * result + ((filt_snr == null) ? 0 : filt_snr.hashCode());
		result = prime * result + ((flos_over_fnlos == null) ? 0 : flos_over_fnlos.hashCode());
		result = prime * result + ((log10_flos_over_fnlos == null) ? 0 : log10_flos_over_fnlos.hashCode());
		result = prime * result + ((los_prob == null) ? 0 : los_prob.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((pct_calibrated == null) ? 0 : pct_calibrated.hashCode());
		result = prime * result + ((prn == null) ? 0 : prn.hashCode());
		result = prime * result + ((ref_snr == null) ? 0 : ref_snr.hashCode());
		result = prime * result + ((snr == null) ? 0 : snr.hashCode());
		result = prime * result + ((usable == null) ? 0 : usable.hashCode());
		result = prime * result + ((used == null) ? 0 : used.hashCode());
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
		SatelliteInfo other = (SatelliteInfo) obj;
		if (almanac == null) {
			if (other.almanac != null)
				return false;
		} else if (!almanac.equals(other.almanac))
			return false;
		if (azimuth == null) {
			if (other.azimuth != null)
				return false;
		} else if (!azimuth.equals(other.azimuth))
			return false;
		if (elevation == null) {
			if (other.elevation != null)
				return false;
		} else if (!elevation.equals(other.elevation))
			return false;
		if (ephemeris == null) {
			if (other.ephemeris != null)
				return false;
		} else if (!ephemeris.equals(other.ephemeris))
			return false;
		if (filt_snr == null) {
			if (other.filt_snr != null)
				return false;
		} else if (!filt_snr.equals(other.filt_snr))
			return false;
		if (flos_over_fnlos == null) {
			if (other.flos_over_fnlos != null)
				return false;
		} else if (!flos_over_fnlos.equals(other.flos_over_fnlos))
			return false;
		if (log10_flos_over_fnlos == null) {
			if (other.log10_flos_over_fnlos != null)
				return false;
		} else if (!log10_flos_over_fnlos.equals(other.log10_flos_over_fnlos))
			return false;
		if (los_prob == null) {
			if (other.los_prob != null)
				return false;
		} else if (!los_prob.equals(other.los_prob))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (pct_calibrated == null) {
			if (other.pct_calibrated != null)
				return false;
		} else if (!pct_calibrated.equals(other.pct_calibrated))
			return false;
		if (prn == null) {
			if (other.prn != null)
				return false;
		} else if (!prn.equals(other.prn))
			return false;
		if (ref_snr == null) {
			if (other.ref_snr != null)
				return false;
		} else if (!ref_snr.equals(other.ref_snr))
			return false;
		if (snr == null) {
			if (other.snr != null)
				return false;
		} else if (!snr.equals(other.snr))
			return false;
		if (usable == null) {
			if (other.usable != null)
				return false;
		} else if (!usable.equals(other.usable))
			return false;
		if (used == null) {
			if (other.used != null)
				return false;
		} else if (!used.equals(other.used))
			return false;
		if (utc == null) {
			if (other.utc != null)
				return false;
		} else if (!utc.equals(other.utc))
			return false;
		return true;
	}

}