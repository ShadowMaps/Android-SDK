/*
* Copyright (C) 2008-2013 The Android Open Source Project,
* Sean J. Barbeau
* Daniel P. Iland, ShadowMaps Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.shadowmaps.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.shadowmaps.sdk.Mode;
import com.shadowmaps.util.api.protobufs.BatchUpdate;
import com.shadowmaps.util.api.protobufs.CellInfo;
import com.shadowmaps.util.api.protobufs.LocationEstimate;
import com.shadowmaps.util.api.protobufs.LocationImprovement;
import com.shadowmaps.util.api.protobufs.LocationUpdate;
import com.shadowmaps.util.api.protobufs.NMEAInfo;
import com.shadowmaps.util.api.protobufs.SatInfo;
import com.shadowmaps.util.api.protobufs.SensorInfo;
import com.shadowmaps.util.api.protobufs.UserInformation;
import com.shadowmaps.util.api.protobufs.WiFiInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import au.com.bytecode.opencsv.CSVWriter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ShadowMaps stand-alone service implements listeners for GPS and Sensor data.
 * Also requires data model classes: DeviceInfo, Message, IncomingInfo, Satellite
 */
public class ShadowMaps extends IntentService implements ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener, SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     *  Settings!!
     *  Periodic mode requests GPS updates every X ms (default [1000])
     *  Passive mode does not request GPS updates.
     *  Passive mode receives GPS updates ONLY when other apps are using GPS
     *  Change gps_mode and timeBetweenPeriodicUpdates to your liking.
     *  Defaults to 1000 ms = 1 second interval
     */

    // You can enter your API Key here for testing, or
    // if you are sending data directly from your app to ShadowMaps.
    // In production, this should be added to the JSON or HTTP Headers
    // by your servers instead.
    private String API_KEY = "YOUR_API_KEY";

    // Configuration settings and constants
    private static String id;

    private static String current_mode = Mode.STOPPED;

    private int UPDATE_INTERVAL_MS = 1000;

    /**
     * We can broadcast our locations through the mock location provider,
     * so developers can try out ShadowMaps with almost 0 new code
     * (Just allow Mock Locations!)
     */
    private boolean mock = false;
    /**
     * If you want to evaluate the resulting lat, lon pairs, here is one way to do so.
     */
    boolean log_to_csv = true;


    // API Key, Device ID, Device Model.
    UserInformation user_info = null;

    /**
     * Provides the entry point to Google Play services FusedLocationProvider
     */
    protected GoogleApiClient mGoogleApiClient;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;


    /**
     * Sensor managers and receivers
     */
    private SensorManager mSensorManager;
    private Sensor mPressure;
    private Sensor mLux;
    private Sensor mTemp;


    /**
     * Last known sensor readings
     * Do not resend if the new readings are
     * the same as the old readings.
     */

    //
    private float lastPressure;
    private float lastLux;
    private float lastTemp;


    // Array indexes for next_update
    private AtomicInteger sensorCount = new AtomicInteger(0);
    private AtomicInteger cellInfoCount = new AtomicInteger(0);
    private AtomicInteger wifiInfoCount = new AtomicInteger(0);
    private AtomicInteger locationEstimateCount = new AtomicInteger(0);
    private AtomicInteger nmeaCount = new AtomicInteger(0);
    private AtomicInteger satInfoCount = new AtomicInteger(0);


    // Inertial Sensors
    private Sensor mLinear;
    private Sensor mAccel;
    private Sensor mMagnetometer;
    // Inertial data
    private List<float[]> accelerometer = new ArrayList<>(20);
    private List<float[]> rotation = new ArrayList<>(20);
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float[] orientation;

    // Step Detection sensors
    private Sensor mStepCounterSensor;
    private Sensor mStepDetectorSensor;

    // Step counters values
    private int stepsCounted = 0;
    private int lastStepsCounted = 0;
    private int stepsDetected = 0;

    // Receivers for Wi-Fi and cellular info
    private TelephonyManager telephonyManager;
    private BroadcastReceiver wifiReceiver;
    private WifiManager wifiManager;
    private PhoneStateListener phoneStateListener;
    private ConnectivityManager cm;

    // Data regarding cellular transmitters
    private int lastCellLocation;
    private int lastCellArea;
    private int lastSignalStrength;
    private long lastSignalStrengthTime;

    private String HTTP_TAG = "HTTP";


    private Deque<LocationUpdate> recentUpdates = new ArrayDeque<LocationUpdate>();
    int recentUpdateSize = 0;
    private LocationUpdate next_update = null;


    // Location data
    private LocationManager locMgr;

    // The Key GPS Status Listener that receives GPS Signal to Noise ratios
    ShadowStatusListener gpsListener;;


    private int lastSatCount = 0;
    // Required API fields. id can be a persistent per-user ID or a unique ID per session.

    // Local Logging
    private CSVWriter writer;

    // HTTP + JSON
    // ShadowMaps Public API Endpoint
    // public final String URL_TO_POST = "http://staging.shadowmaps.com:4567/v1/update/";
    // public final String URL_TO_POST = "https://api.shadowmaps.com/v1/update/";
    // public final String BATCH_URL = "https://api.shadowmaps.com/v1/batch/";
    public final String URL_TO_POST = "https://api.shadowmaps.com/v1/update/";
    //public final String BATCH_URL = "http://192.168.0.106:8080/v1/batch/";
    //public final String URL_TO_POST = "http://192.168.8.215:8080/v1/update/";

    private OkHttpClient client = new OkHttpClient();

    private NetworkProcessor cronetSender;
    // In Logcat, filter by ShadowMaps to see logs from this service
    private final String TAG = "ShadowMaps";

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v(TAG, "GPS Status Changed from " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.v(TAG, "GPS enabled from " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.v(TAG, "GPS disabled from " + provider);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "OnCreate");
        next_update = newProtoLocationUpdate();
        registerReceiver(stopServiceReceiver, new IntentFilter("shadowmaps"));
        cronetSender = new NetworkProcessor(getApplicationContext());

    }

    public UserInformation getUserInformation() {
        if(user_info == null) {
            user_info = new UserInformation();
            // The ShadowMaps Server requires a unique ID per device, user, or session.
            id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            // Get from manifest or Shared Preferences?
            API_KEY = PreferenceManager.getDefaultSharedPreferences(this).getString("shadowmaps_api_key", "NO_INVITE");
            user_info.apiKey = API_KEY;
            user_info.id = id;
            user_info.model = Build.MODEL;
        }
        return user_info;
    }
    private static boolean isShadowServiceRunning() {
        if(current_mode.equals(Mode.STOPPED)) {
            return true;
        } else {
            return false;
        }
    }

    // Call to terminate the service.
    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    public static boolean start(Context c, String mode) {
        Log.v("ShadowMapsService", "Start called!");
        if (mode.equals(current_mode)) {
            return false;
            // Do nothing
        } else {
            current_mode = mode;
            if (isShadowServiceRunning()) {
                Log.v("ShadowMaps", "Service is already running");
                switchModes(mode);
            } else {
                final String STARTUP_EXTRA = "com.shadowmaps.start";
                Intent i = new Intent(c, ShadowMaps.class);
                i.putExtra(STARTUP_EXTRA, true);
                c.startService(i);
                Log.v("SERVICE", "Starting ShadowService in activity onCreate");
            }
            return true;
        }
    }



    public static boolean switchModes(String mode) {
        return true;
    }

    // Called when Service is destroyed. Unregister all receivers.
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "OnDestroy");
         this.unregisterReceiver(wifiReceiver);
        // The GPS Status listener is what provides Satellite SNR and Az/El information!
        locMgr.removeGpsStatusListener(gpsListener);
        locMgr.removeUpdates(this);
        locMgr.removeNmeaListener(gpsListener);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mSensorManager.unregisterListener(this);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(stopServiceReceiver);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(this, mStepCounterSensor);
        mSensorManager.unregisterListener(this, mStepDetectorSensor);
        try {
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Construct IntentService with name
    public ShadowMaps() {
        super("ShadowMaps");
        Log.v(TAG, "Starting ShadowMaps");
    }

    public ShadowMaps(String name) {
        super(name);
        Log.v(TAG, "Starting: " + name);
    }

    protected LocationRequest createLocationRequest(int update_interval) {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(update_interval);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v("ShadowMaps", "Connected");
        mLocationRequest = createLocationRequest(UPDATE_INTERVAL_MS);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        //     java.lang.SecurityException: In order to use mock mode functionality app com.shadowmaps.example must have the ACCESS_MOCK_LOCATION permission and the Settings.Secure.ALLOW_MOCK_LOCATION system setting must be enabled.
        if(isMockSettingsON(getApplicationContext())) {
            LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true);
            mock = true;
        } else {
            Log.v("ShadowMaps", "Mock Locations Disabled, not setting mock mode");
            mock = false;
        }
    }

    public static boolean isMockSettingsON(Context context) {
        // returns true if mock location enabled, false if not enabled.
        if (Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION).equals("0"))
            return false;
        else
            return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());


    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("LOC", "Updating Location from " + location.getProvider() + " at " + location.getTime());
        next_update = withLocationInfo(next_update, location);
        if(location.getTime() > next_update.satInfoTime) {
            finalizeProtobuf();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
            if(key.equals("update_interval_ms")) {
                try {
                    Log.v("ShadowMapsPreferences", "Key change: " + key);
                    Log.v("ShadowMapsPreferences", "Sticking with 1 second updates");
//  String interval_secs_sting = sharedPreferences.getString(key, "1");
//                    double seconds = Double.parseDouble(interval_secs_sting);
//                    UPDATE_INTERVAL_MS = (int) Math.round(seconds * 1000);
//                    Log.v("ShadowMapsPreferences", "UPDATE_INTERVAL_MS: " + UPDATE_INTERVAL_MS);
                } catch (Exception e) {
                    e.printStackTrace();
                    UPDATE_INTERVAL_MS = 1000;
                    sharedPreferences.edit().putString(key, "1").commit();
                }
                //updateInterval(UPDATE_INTERVAL_MS);
            }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        try {
            // The ShadowMaps Server requires a unique ID per device, user, or session.
            id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            // Get from manifest or Shared Preferences?
            API_KEY = PreferenceManager.getDefaultSharedPreferences(this).getString("shadowmaps_api_key", "NO_INVITE");
            // Determine how frequently to update
            UPDATE_INTERVAL_MS = PreferenceManager.getDefaultSharedPreferences(this).getInt("shadowmaps_update_interval_ms", 1000);
            gpsListener = new ShadowStatusListener();

            locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            requestPeriodicLocationUpdates(UPDATE_INTERVAL_MS);

            // Request NMEA listener
            locMgr.addNmeaListener(gpsListener);

            // Start logging to CSV
            setupCSV();
            //BluetoothLEListener bluetoothLEListener = new BluetoothLEListener(getApplicationContext());
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

            // Start listening for GPS Status updates!

            // Initialize listeners for sensors.
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mLux = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            boolean hasStepDetector = this.getApplicationContext().getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
            boolean hasStepCounter = this.getApplicationContext().getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_SENSOR_STEP_COUNTER);
            if (hasStepDetector) {
                mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
                mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                Log.v("Steps", "Step detector not available!");
            }

            if (hasStepCounter) {
                mStepCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                mSensorManager.registerListener(this, mStepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                Log.v("Steps", "Step counter not available!");
            }
            mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mLux , SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mTemp , SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mLinear , SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

            // Initialize Location Listener
            buildGoogleApiClient();
            mGoogleApiClient.connect();

            // Request cellular location and signal strength data from Telephony Service
            telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            phoneStateListener = new ShadowPhoneStateListener();
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            // Request the results of any Wi-Fi scans be delivered to our wifiReceiver
            wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context c, Intent intent) {
                    withWiFiInfo(next_update, processWifiScan());
                }
            };
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            // ConnectivityManager enables checks for internet connectivity before using network.
            cm =(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    // Request periodic GPS updates
    private void requestPeriodicLocationUpdates(int update_interval_ms) {
        Log.v(TAG, String.format("Starting ShadowMaps with Periodic Updates with interval of %sms", update_interval_ms));
        String locationProvider = LocationManager.GPS_PROVIDER;

        // Request GPS location updates
        locMgr.requestLocationUpdates(locationProvider, update_interval_ms, 0, gpsListener);
        // Essential component here: A GPS Status Listener to provide satellite SNRs!
        locMgr.addGpsStatusListener(gpsListener);
    }

    // Receive all location updates requested by other apps/services
    public void requestPassiveLocationUpdates() {
        Log.v(TAG, "Starting ShadowMaps with passive (only when otherwise in use) GPS updates");
        String locationProvider = LocationManager.GPS_PROVIDER;
        // minimum 1 second spacing, no distance limits
        locMgr.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, UPDATE_INTERVAL_MS, 0, this);
        locMgr.addGpsStatusListener(gpsListener);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We don't care, but must override
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public WiFiInfo[] processWifiScan() {
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            final int size = results.size();
            if (size != 0) {
                WiFiInfo[] wifi_aps = new WiFiInfo[size];
                for (int i = 0; i < size; i++)  {
                    ScanResult result = results.get(i);
                    WiFiInfo new_ap = new WiFiInfo();
                    // Signal strength
                    new_ap.rssi = result.level;
                    // Note that we use BSSIDs (AP MAC Address), not human readable SSIDs.
                    new_ap.bssid = result.BSSID;
                    new_ap.frequency = result.frequency;
                    // We would like to know exactly when this scan took place
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        new_ap.timestamp = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (result.timestamp / 1000);
                    } else {
                        new_ap.timestamp = System.currentTimeMillis();
                    }
                    wifi_aps[i] = new_ap;
                }
                return wifi_aps;
            }
        }
        return new WiFiInfo[1];
    }

    public LocationUpdate withSensorEvent(LocationUpdate update, SensorEvent event) {
        SensorInfo info;
        try {
             info = update.sensorInfos[update.sensorInfos.length-1];
             if (info != null) {
                int current_size = update.sensorInfos.length;
                SensorInfo[] temp = new SensorInfo[current_size + 5];
                for (int i = 0; i < current_size; i++) {
                    temp[i] = update.sensorInfos[i];
                }
                update.sensorInfos = temp;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            update.sensorInfos = new SensorInfo[1];
        }

        info = new SensorInfo();
        info.timestamp = event.timestamp;

        int sensor = event.sensor.getType();
        // This is very chatty, Sensors 5/6 update frequently.
        //Log.v(TAG, "Sensor reading from " + sensor);
        if (sensor == Sensor.TYPE_PRESSURE) {
            // float pressure = event.values[0];
            if(event.values[0] != lastPressure) {
                info.pressure = event.values[0];
                lastPressure = event.values[0];
            }
        } else if (sensor == Sensor.TYPE_LIGHT) {
            lastLux = event.values[0];
            info.light = event.values[0];

            // Depricated TYPE_TEMPERATURE returns battery temp on some models, useful to us.
        } else if (sensor == Sensor.TYPE_TEMPERATURE) {
            lastTemp = event.values[0];
        } else if (sensor == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            //Log.v("Accelerometer", "Accelerometer values: " + x + "," + y + "," + z);
            float[] acc = {x, y, z};
            accelerometer.add(acc);
        } else if (sensor == Sensor.TYPE_STEP_COUNTER) {
            float[] values = event.values;
            int value = -1;
            if (values.length > 0) {
                value = (int) values[0];
                Log.v("Steps", "Step Counter Incremented : " + value);
                stepsCounted = value;
            }
        } else if (sensor == Sensor.TYPE_STEP_DETECTOR) {
            float[] values = event.values;
            int value = -1;
            if (values.length > 0) {
                value = (int) values[0];
                Log.v("Steps", "Step Detected : " + value);
                stepsDetected = stepsDetected + 1;
            }
        } else if (sensor == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        } else if (sensor == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float time = System.currentTimeMillis();
                orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                mGravity = null;
                mGeomagnetic = null;
                rotation.add(orientation);

            }
        }
        return update;
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        next_update = withSensorEvent(next_update, event);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("Intent", "Intent handled");
    }

    private boolean isConnected() {
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected;
    }



    /**
     * Returns current battery level in range [0,1]
     * @return float batteryPct
     */
    private float getBatteryLevel() {
        float batteryPct = -1;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float)scale;
        } catch (Exception e) {
            Log.v("Battery", "Error getting battery info.");
        }
        return batteryPct;
    }


    void shareLocationImprovement(LocationImprovement response) {
        Intent intent = new Intent();
        double lon = response.lon;
        double lat = response.lat;
        double acc = response.acc;
        long utc = response.utc;
        if(log_to_csv) {
            writeToCSV(utc, lat, lon, acc);
        }
        if(mock) {
            Location newLocation = new Location("Fused");
            newLocation.setLatitude(response.lat); // ShadowMaps lat
            newLocation.setLongitude(response.lon); // ShadowMaps lon
            newLocation.setTime(response.utc); // utc associated with ShadowMaps update
            newLocation.setAccuracy(response.acc); // ShadowMaps reported accuracy as float (from double)
            newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()); // Current system time
            LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, newLocation);
        }

        Log.v("ShadowMaps", "ShadowMaps update received, " + (System.currentTimeMillis() - utc) + "ms delay.");
        intent.setAction("shadowmaps.location.update");
        intent.putExtra("lat", lat);
        intent.putExtra("lon", lon);
        intent.putExtra("radius", acc);

        if(response.geocoded != null) {
            intent.putExtra("street", response.geocoded);
        }
        if(response.skyview != null) {
            intent.putExtra("skyview_png", response.skyview);
        }
        sendBroadcast(intent);

    }


    // CSV Logging of our results
    public void setupCSV() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmm");
        Date dt = new Date();
        String fileName = "ShadowMapsOutput_" + sdf.format(dt); // formats to 09/23/2009 13:53:28.238

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String dirName = "ShadowMaps";
        String folderPath = baseDir + File.separator + dirName;
        boolean success = true;
        File folder = new File(folderPath);
        String filePath = baseDir + File.separator + dirName + File.separator +  fileName;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            filePath = baseDir + File.separator + dirName + File.separator +  fileName;
        } else {
            filePath = baseDir + File.separator + fileName;
        }
        Log.v("Output Logging", "Logging to " + filePath);
        File f = new File(filePath );
        FileWriter mFileWriter = null;
        char separator = CSVWriter.DEFAULT_SEPARATOR;
        char quoted = CSVWriter.NO_QUOTE_CHARACTER;

        try {
            if (f.exists() && !f.isDirectory()) {
                // Append to existing file
                mFileWriter = new FileWriter(filePath, true);
                writer = new CSVWriter(mFileWriter, separator, quoted);
            } else {
                // Write to new file
                writer = new CSVWriter(new FileWriter(filePath), separator, quoted);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.v("ShadowMaps", "Error starting csv");
        }
    }

    public void writeToCSV(long utc, double lat, double lon, double acc) {
            // Null checking for if someone hits back button.
            String[] data = {String.valueOf(utc), String.valueOf(lat), String.valueOf(lon), String.valueOf(acc)};
            Log.v("CSV", "writing to csv: " + utc);
            writer.writeNext(data);
        try {
            writer.flush();
        } catch (Exception e) {
            Log.v("ShadowMaps", "Error flushing");
            e.printStackTrace();
        }
    }

    // Protobuf builder methods
    public LocationUpdate withSatInfos(LocationUpdate lu, GpsStatus status) {
        // Get/create iterator of visible satellites
        Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

        // Try to intelligently size our ArrayList based on previous observation
        Log.v("withSatInfos", "Max satellites: " + status.getMaxSatellites());
        lu.satelliteInfos = new SatInfo[status.getMaxSatellites()];
        int satCount = 0;
        while (satellites.hasNext()) {

            SatInfo newSat = new SatInfo();
            GpsSatellite satellite = satellites.next();
            newSat.prn = satellite.getPrn();
            newSat.snr = satellite.getSnr();
            newSat.elevation = satellite.getElevation();
            newSat.azimuth = satellite.getAzimuth();
            if (satellite.hasEphemeris()) {
                newSat.ephemeris = true;
            } else {
                newSat.ephemeris = true;
            }
            if (satellite.hasAlmanac()) {
                newSat.almanac = true;
            } else {
                newSat.almanac = true;
            }
            if (satellite.usedInFix()) {
                newSat.used = true;
            } else {
                newSat.used = true;
            }
            lu.satelliteInfos[satCount] = newSat;
            satCount++;
        }
        satInfoCount.incrementAndGet();
        return lu;
    }

    public LocationUpdate withLocationInfo(LocationUpdate lu, Location location) {
        if(lu.estimates == null | lu.estimates.length == 0) {
            lu.estimates = new LocationEstimate[1];
        } else if(locationEstimateCount.get() >= lu.estimates.length){
            LocationEstimate[] all_estimates = new LocationEstimate[locationEstimateCount.get() + 1];
            for(int i = 0 ; i < locationEstimateCount.get(); i++) {
                all_estimates[i] = lu.estimates[i];
            }
            lu.estimates = all_estimates;
        }
        LocationEstimate le = new LocationEstimate();
        le.lat = location.getLatitude();
        le.lon = location.getLongitude();
        le.alt = (float)location.getAltitude();
        le.acc = location.getAccuracy();
        le.speed = location.getSpeed();
        le.bearing = location.getBearing();
        le.provider = location.getProvider();
        le.utc = location.getTime();
        lu.estimates[locationEstimateCount.getAndIncrement()] = le;
        return lu;
    }

    public LocationUpdate withWiFiInfo(LocationUpdate lu, WiFiInfo[] wifi_info) {
        if(lu.wifiNetworks == null || lu.wifiNetworks.length == 0) {
            lu.wifiNetworks = wifi_info;
        } else {
            int new_len = lu.wifiNetworks.length + wifi_info.length;
            WiFiInfo[] combined = new WiFiInfo[new_len];
            System.arraycopy(lu.wifiNetworks, 0, combined, 0, lu.wifiNetworks.length);
            System.arraycopy(wifi_info, 0, combined, lu.wifiNetworks.length, wifi_info.length);
            lu.wifiNetworks = combined;
        }
        return lu;
    }

    private LocationUpdate withInertialInfo(LocationUpdate lu) {
        int stepsThisTime = stepsCounted - lastStepsCounted;
        lastStepsCounted = stepsCounted;
        // Create Steps sensor object
        SensorInfo info = new SensorInfo();
        info.timestamp = System.currentTimeMillis();
        info.stepsCounted = stepsThisTime;
        info.stepsDetected = stepsDetected;
        // Reset step counters
        stepsDetected = 0;
        if(lu.sensorInfos == null || lu.sensorInfos.length == 0) {
            lu.sensorInfos = new SensorInfo[1];
        } else if(sensorCount.get() >= lu.sensorInfos.length) {
            Log.v("sensorInfos", "Have array of: " + lu.sensorInfos.length);
            Log.v("sensorInfos", "Growing by one element to: " + lu.sensorInfos.length + 1);
            SensorInfo[] all_sensors = new SensorInfo[sensorCount.get() + 1];
            for (int i = 0; i < sensorCount.get(); i++) {
                all_sensors[i] = lu.sensorInfos[i];
            }
            lu.sensorInfos = all_sensors;
            Log.v("sensorInfos", "Now Have array of: " + lu.sensorInfos.length);
        }
        lu.sensorInfos[sensorCount.getAndIncrement()] = info;
        return lu;
    }

    private LocationUpdate withSensorInfo(LocationUpdate lu) {
        SensorInfo info = new SensorInfo();
        info.battery = getBatteryLevel();
        info.timestamp = System.currentTimeMillis();
        if(lu.sensorInfos == null || lu.sensorInfos.length == 0) {
            lu.sensorInfos = new SensorInfo[1];
        } else if(sensorCount.get() >= lu.sensorInfos.length) {
            SensorInfo[] all_sensors = new SensorInfo[lu.sensorInfos.length+1];
            Log.v("sensorInfos", "Have array of: " + lu.sensorInfos.length);
            Log.v("sensorInfos", "Growing by one element to: " + lu.sensorInfos.length + 1);
            for (int i = 0; i < sensorCount.get(); i++) {
                all_sensors[i] = lu.sensorInfos[i];
            }
            lu.sensorInfos = all_sensors;
        }
        lu.sensorInfos[sensorCount.getAndIncrement()] = info;
        Log.v("sensorInfos", "Now Have array of: " + lu.sensorInfos.length);
        return lu;
    }

    public LocationUpdate withNMEA(LocationUpdate lu, long timestamp, String nmea) {
            if(lu.nmeaInfos == null || lu.nmeaInfos.length == 0) {
                lu.nmeaInfos = new NMEAInfo[10];
            } else if(nmeaCount.get() >= lu.nmeaInfos.length) {
                Log.v("nmea", "Have array of: " + lu.nmeaInfos.length);
                Log.v("nmea", "Growing by one element to: " + lu.nmeaInfos.length + 1);
                NMEAInfo[] all_nmea = new NMEAInfo[nmeaCount.get() + 1];
                for (int i = 0; i < nmeaCount.get(); i++) {
                    all_nmea[i] = lu.nmeaInfos[i];
                }
                lu.nmeaInfos = all_nmea;
            }
        NMEAInfo newNmea = new NMEAInfo();
        newNmea.timestamp = timestamp;
        newNmea.sentence = nmea;
        lu.nmeaInfos[nmeaCount.getAndIncrement()] = newNmea;
        return lu;

    }

    public class ShadowStatusListener implements GpsStatus.Listener, GpsStatus.NmeaListener, LocationListener{

        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                long fix_time = System.currentTimeMillis();
                Log.v("SATS", "GPS Satellite info at " + System.currentTimeMillis());
                try {
                    GpsStatus status = locMgr.getGpsStatus(null);
                    next_update.satInfoTime = System.currentTimeMillis();
                    next_update = withSatInfos(next_update, status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.v("LOC", "Updating ShadowStatus Location from " + location.getProvider() + " at " + location.getTime());
            next_update = withLocationInfo(next_update, location);
        }

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            Log.v("NMEA", "" + timestamp + "," + nmea);
            next_update = withNMEA(next_update, timestamp, nmea);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(TAG, "GPS Status Changed from " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.v(TAG, "GPS enabled from " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.v(TAG, "GPS disabled from " + provider);

        }
    }


    private boolean finalizeProtobuf() {
        next_update = withSensorInfo(next_update);
        next_update = withInertialInfo(next_update);
        try {
            byte[] new_update_as_bytes = new byte[next_update.getSerializedSize()];
            next_update.writeTo(CodedOutputByteBufferNano.newInstance(new_update_as_bytes));
            //getPhoneState();
            recentUpdates.addFirst(next_update);
            recentUpdateSize++;
            if (current_mode.equals(Mode.REALTIME) && isConnected()) {
                uploadProtobufRealtimeTCP(new_update_as_bytes);
                //cronetSender.sendUpdate(new_update_as_bytes);
            }
            if (recentUpdateSize > 120) {
                boolean stored = createBatchFile(recentUpdates);
                if (stored) {
                    recentUpdates.clear();
                    recentUpdateSize = 0;
                }
            }
            next_update = newProtoLocationUpdate();
            System.out.println("Now have" + recentUpdateSize + " in batch");
            return false;
        } catch(Exception e) {
            return false;
        }
    }

    private boolean createBatchFile(Deque<LocationUpdate> recentUpdates) {
        BatchUpdate bu = new BatchUpdate();
        bu.apiKey = API_KEY;
        bu.batchId = "SM" + id + "_" + System.currentTimeMillis() + ".smpb1";
        FileOutputStream fos = null;
        try {

            //bu.startTime = recentUpdates.getLast().estimates[0].utc;
            //bu.endTime = recentUpdates.getFirst().estimates[0].utc;
            LocationUpdate[] locs = new LocationUpdate[recentUpdateSize];
            bu.updates = recentUpdates.toArray(locs);
            byte[] batch_bytes = new byte[bu.getSerializedSize()];
            bu.writeTo(CodedOutputByteBufferNano.newInstance(batch_bytes));
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String dirName = "ShadowMapsData";
            String folderPath = baseDir + File.separator + dirName;
            boolean success = true;
            File folder = new File(folderPath);
            String filePath = folderPath + File.separator + bu.batchId;
            if (!folder.exists()) {
                success = folder.mkdir();
            }
            if (success) {
                filePath = baseDir + File.separator + dirName + File.separator + bu.batchId;
            } else {
                filePath = baseDir + File.separator + bu.batchId;
            }
            Log.v("Batch storage", "Logging to " + filePath);
            File file = new File(filePath);
            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            fos.write(batch_bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // close the streams using close method
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                Log.v("ERROR", "Error while closing stream: " + ioe);
            }

        }
    }

    private LocationUpdate newProtoLocationUpdate() {
        LocationUpdate new_update = new LocationUpdate();
        new_update.estimates = new LocationEstimate[1];
        new_update.satelliteInfos = new SatInfo[30];
        new_update.cellNetworks = new CellInfo[1];
        new_update.wifiNetworks = new WiFiInfo[1];
        new_update.userInfo = getUserInformation();
        new_update.nmeaInfos = new NMEAInfo[3];
        locationEstimateCount.set(0);
        cellInfoCount.set(0);
        sensorCount.set(0);
        wifiInfoCount.set(0);
        nmeaCount.set(0);
        satInfoCount.set(0);
        return new_update;
    }

    private void uploadProtobufRealtimeUDP(final byte[] update) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    long request_start = System.currentTimeMillis();
                    RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), update);

                    Request request = new Request.Builder()
                            .url(URL_TO_POST)
                            .post(body)
                            .build();
                    request.header("Accept: application/octet-stream");
                    Response response = client.newCall(request).execute();
                    if (response.code() == 200) {
                        Log.v("Realtime", "200!");
                        byte[] response_stream = response.body().bytes();
                        LocationImprovement updated_location = LocationImprovement.parseFrom(response_stream);
                        // Broadcast Intent
                        long request_end = System.currentTimeMillis();
                        Log.v(TAG, "Total RTT:" + (request_end - request_start) + " ms");

                        shareLocationImprovement(updated_location);
                        Log.v(TAG, String.format("Received ShadowMapsUpdate: %s", updated_location));
                    } else {
                        Log.v("HTTP", "" + response.code());
                        Log.v("Realtime", response.message());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }


        private void uploadProtobufRealtimeTCP(final byte[] update) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), update);

                    Request request = new Request.Builder()
                            .url(URL_TO_POST)
                            .post(body)
                            .build();
                    request.header("Accept: application/octet-stream");
                    Response response = client.newCall(request).execute();
                    long request_start = System.currentTimeMillis();
                    if (response.code() == 200) {
                        Log.v("Realtime", "200!");
                        byte[] response_stream = response.body().bytes();
                        LocationImprovement updated_location = LocationImprovement.parseFrom(response_stream);
                        // Broadcast Intent
                        long request_end = System.currentTimeMillis();
                        Log.v(TAG, "Total RTT:" + (request_end - request_start) + " ms");

                        shareLocationImprovement(updated_location);
                        Log.v(TAG, String.format("Received ShadowMapsUpdate: %s", updated_location));
                    } else {
                        Log.v("HTTP", "" + response.code());
                        Log.v("Realtime", response.message());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public class ShadowPhoneStateListener extends PhoneStateListener {

        public ShadowPhoneStateListener() {}

        @SuppressLint("NewApi")
        @Override
        public void onCellLocationChanged(CellLocation location) {
            long ts = System.currentTimeMillis();
            if (location instanceof GsmCellLocation) {
                GsmCellLocation gcLoc = (GsmCellLocation) location;
                lastCellLocation = gcLoc.getCid() & 0xffff;
                lastCellArea = gcLoc.getLac();

            } else if (location instanceof CdmaCellLocation) {
                CdmaCellLocation ccLoc = (CdmaCellLocation) location;
                lastCellLocation = ccLoc.getBaseStationId();
                lastCellArea = ccLoc.getSystemId();
                Log.d(TAG, "Cell ID: " + lastCellLocation);
            }
            CellInfo cell = new CellInfo();
            cell.cellid = lastCellLocation;
            cell.timestamp = ts;
            cell.frequency =
            Log.v(TAG, "Adding CellID: " + lastCellLocation + " LAC: " + lastCellArea + "ts:" + ts);
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType)
        {
            Log.d(TAG, "Network Type: "+networkType);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            Log.v("RSSI", signalStrength.toString());
            String ssignal = signalStrength.toString();
            String[] parts = ssignal.split(" ");
            lastSignalStrengthTime = System.currentTimeMillis();
            if ( telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE){
                lastSignalStrength = Integer.parseInt(parts[8])*2-113;
            } else if (signalStrength.getGsmSignalStrength() != 99) {
                lastSignalStrength = -113 + 2
                            * signalStrength.getGsmSignalStrength();
            }
            if(lastSignalStrength == 85) {
                lastSignalStrength = signalStrength.getCdmaDbm();
            }

            Log.v(TAG, "Adding Cellular RSSI: " + lastSignalStrength + " CellID: " + lastCellLocation + " LAC: " + lastCellArea  + "ts:" + lastSignalStrengthTime);
        }
    }
}