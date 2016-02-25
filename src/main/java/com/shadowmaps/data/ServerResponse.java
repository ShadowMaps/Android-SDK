package com.shadowmaps.data;

/*
 * Main output of this entire project. Class could just as well be called IncomingInfo
 * {"s3":{"flushed":false,"bucket_name":"sm-kml-logs","file_path":"model_SPH-L710\/id_79c126d9a08e7b07\/session_a8z1m2\/dataset_e5b3w2\/out\/git_2a26db3\/mapconf_f8i9c5\/pfconf_o4o5b9\/"},"route":{"acc":11.0835,"utc":1429233555374,"lon":-122.4212579437,"street":"CALIFORNIA ST","conf":99,"lat":37.7905057491},"sats":null,"dev":{"acc":3.0,"utc":1429233555374,"lon":-122.4212436641,"acc3d":3.0035574,"turn":-1.1850463,"alt":56.539825,"brng":275.33597,"lat":37.790435479,"model":"SPH-L710","accv":0.21533659,"spd":0.40514624,"id":"79c126d9a08e7b07"}}

 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerResponse {
	
	// all data that could possibly be sent back to Mode control in a packet
	@JsonProperty("dev")
	protected DeviceInfo device_info; // device data (utc, lat, lon, acc, etc..)
	@JsonProperty("route")
	protected RouteInfo route_info;	// road clamping info

	public ServerResponse() { }
	
	@JsonIgnore()
	public DeviceInfo getDeviceInfo() {
		return device_info;
	}
	public void setDeviceInfo(DeviceInfo device_info) {
		this.device_info = device_info;
	}
	@JsonIgnore()
	public RouteInfo getRouteInfo() {
		return route_info;
	}
	public void setRouteInfo(RouteInfo route_info) {
		this.route_info = route_info;
	}

	@Override
	@JsonIgnore()
	public String toString() {
		return "ServerResponse ["
				+ (device_info != null ? "device_info=" + device_info + ", " : "")
				+ (route_info != null ? "route_info=" + route_info + "' " : "")
				+ "]";
	}

}
