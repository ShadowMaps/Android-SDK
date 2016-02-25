package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * POJO for road clamping info to be outputted in IncomingInfo if road matching is enabled
 */
public class RouteInfo {
	
	private Long utc;
	@JsonProperty("street")
	private String street_name;
	@JsonProperty("lat")
	private Double latitude;
	@JsonProperty("lon")
	private Double longitude;
	@JsonProperty("acc")
	private Float accuracy_horiz;
	@JsonProperty("conf")
	private Short confidence;
	
	public RouteInfo() { }

	@Override
	@JsonIgnore()
	public String toString() {
		return "RouteInfo ["
				+ (utc != null ? "utc=" + utc + ", " : "")
				+ (street_name != null ? "street_name=" + street_name + ", " : "")
				+ (latitude != null ? "latitude=" + latitude + ", " : "")
				+ (longitude != null ? "longitude=" + longitude + ", " : "")
				+ (accuracy_horiz != null ? "accuracy_horiz=" + accuracy_horiz + ", " : "")
				+ (confidence != null ? "confidence=" + confidence : "") + "]";
	}

	@JsonIgnore() 
	public String getStreetName() {
		return street_name;
	}
	public void setStreetName(String street_name) {
		this.street_name = street_name;
	}
	@JsonIgnore() 
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	@JsonIgnore() 
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	@JsonIgnore() 
	public Float getAccuracyHoriz() {
		return accuracy_horiz;
	}
	public void setAccuracyHoriz(Float accuracy_horiz) {
		this.accuracy_horiz = accuracy_horiz;
	}
	@JsonIgnore() 
	public Short getConfidence() {
		return confidence;
	}
	public void setConfidence(Short confidence) {
		this.confidence = confidence;
	}
    @JsonIgnore()
    public Long getUTC() {
		return utc;
	}
	public void setUTC(Long utc) {
		this.utc = utc;
	}
	
}