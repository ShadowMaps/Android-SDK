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

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.shadowmaps.R;
import com.shadowmaps.sdk.Mode;
import com.shadowmaps.util.api.protobufs.BatchUpdate;
import com.shadowmaps.util.api.protobufs.LocationEstimate;
import com.shadowmaps.util.api.protobufs.LocationImprovement;
import com.shadowmaps.util.api.protobufs.LocationUpdate;
import com.shadowmaps.util.api.protobufs.SatInfo;
import com.shadowmaps.util.api.protobufs.UserInformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ShadowMaps minimal stand-alone service implements listeners for GPS Data only.
 */
public class MinimalShadowMapsService extends IntentService {
    private int UPDATE_INTERVAL_MS = 1000;

    // Internal state
    private static String current_mode = Mode.STOPPED;

    // API Key, Device ID, Device Model.
    UserInformation user_info = getUserInformation();

    private AtomicInteger locationEstimateCount = new AtomicInteger(0);
    private AtomicInteger satInfoCount = new AtomicInteger(0);

    private List<LocationUpdate> recentUpdates = new ArrayList<>();
    private int numberUpdatesInBatch = 0;
    private LocationUpdate next_update = null;

    // LocationManager used to request GPS Information
    private LocationManager locMgr;
    // Listener for GPS locations and GPS Satellite data (i.e. Signal to Noise ratios)
    ShadowStatusListener gpsListener;

    public final String URL_TO_POST = "https://api.shadowmaps.com/v1/compact/";
    private OkHttpClient client = new OkHttpClient();

    private String TAG = "ShadowMaps";

    public MinimalShadowMapsService() {
        super("ShadowMaps");
        Log.v(TAG, "Starting ShadowMaps");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Creating ShadowMaps Service.");
        registerReceiver(stopServiceReceiver, new IntentFilter("shadowmaps"));
    }

    public UserInformation getUserInformation() {
        UserInformation user_info = new UserInformation();
        // ID can be persistent or ephemeral.
        // If you want a persistent identifier, use:
        // user_info.id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        // For increased anonymity, use an ephemeral identifier, changing each session:
        user_info.id = UUID.randomUUID().toString();
        // Get API key from key shadowmaps_api_key in strings.xml
        user_info.apiKey = getApplicationContext().getString(R.string.shadowmaps_api_key);
        // Or you can modify this file to hardcode an API key
        // user_info.apiKey = "ShadowMapsExampleAPIKey";
        // Get model information to allow for device-specific configuration.
        user_info.model = Build.MODEL;
        return user_info;
    }

    private static boolean isShadowServiceRunning() {
        if(!current_mode.equals(Mode.STOPPED)) {
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

    // Static method creates, starts, or stops instance of this service.
    public static boolean start(Context c, String mode) {
        Log.v("ShadowMapsService", "Starting ShadowMapsService");
        if (mode.equals(current_mode)) {
            return false;
        } else {
            current_mode = mode;
            if (isShadowServiceRunning()) {
                Log.v("ShadowMaps", "Service is already running");
            } else {
                final String STARTUP_EXTRA = "com.shadowmaps.start";
                Intent i = new Intent(c, MinimalShadowMapsService.class);
                i.putExtra(STARTUP_EXTRA, true);
                c.startService(i);
                Log.v("SERVICE", "Starting ShadowService in activity onCreate");
            }
            return true;
        }
    }

    // Called when Service is destroyed. Unregister all receivers.
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "OnDestroy");
        locMgr.removeGpsStatusListener(gpsListener);
        unregisterReceiver(stopServiceReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        try {
            UPDATE_INTERVAL_MS = PreferenceManager.getDefaultSharedPreferences(this).getInt("shadowmaps_update_interval_ms", 1000);
            gpsListener = new ShadowStatusListener();
            locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if(current_mode.equals(Mode.REALTIME)) {
                requestPeriodicLocationUpdates(UPDATE_INTERVAL_MS);
            } else if(current_mode.equals(Mode.PASSIVE)) {
                requestPassiveLocationUpdates();
            }
        } catch (Exception e) {
            // We may not have permission to use GPS, resulting in failure to start GPS Listener.
            Log.v(TAG, "Error starting ShadowMaps GPS Listener: " + e.getMessage());
        }
        return START_STICKY;
    }

    // Request periodic GPS updates
    private void requestPeriodicLocationUpdates(int update_interval_ms) {
        Log.v(TAG, String.format("Starting ShadowMaps with Periodic Updates with interval of %sms", update_interval_ms));
        // Request GPS location updates every update_interval_ms milliseconds (typically 1000);
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, update_interval_ms, 0, gpsListener);
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
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "Intent handled");
    }

    private boolean isConnected() {
        ConnectivityManager cm =(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected;
    }

    void provideImprovedLocation(LocationImprovement response) {
        Intent intent = new Intent();
        double lon = response.lon;
        double lat = response.lat;
        double acc = response.acc;

        intent.setAction("shadowmaps.location.update");
        intent.putExtra("lat", lat);
        intent.putExtra("lon", lon);
        intent.putExtra("radius", acc);

        if(response.geocoded != null) {
            intent.putExtra("location", response.geocoded);
        }
        sendBroadcast(intent);

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

    public class ShadowStatusListener implements GpsStatus.Listener, LocationListener {
        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
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
        try {
            byte[] new_update_as_bytes = new byte[next_update.getSerializedSize()];
            next_update.writeTo(CodedOutputByteBufferNano.newInstance(new_update_as_bytes));
            //getPhoneState();
            recentUpdates.add(next_update);
            numberUpdatesInBatch++;
            if (current_mode.equals(Mode.REALTIME) && isConnected()) {
                uploadShadowMapsData(new_update_as_bytes);
            }
            if (numberUpdatesInBatch > 120) {
                boolean stored = createBatchFile(recentUpdates);
                if (stored) {
                    recentUpdates.clear();
                    numberUpdatesInBatch = 0;
                }
            }
            next_update = newProtoLocationUpdate();
            System.out.println("Now have" + numberUpdatesInBatch + " in batch");
            return false;
        } catch(Exception e) {
            return false;
        }
    }

    private boolean createBatchFile(List<LocationUpdate> recentUpdates) {
        BatchUpdate bu = new BatchUpdate();
        bu.apiKey = user_info.apiKey;
        bu.batchId = user_info.id + "_" + System.currentTimeMillis() + ".smpb1";
        FileOutputStream fos = null;
        try {
            bu.startTime = recentUpdates.get(0).estimates[0].utc;
            bu.endTime = recentUpdates.get(recentUpdates.size()).estimates[0].utc;
            LocationUpdate[] locs = new LocationUpdate[numberUpdatesInBatch];
            bu.updates = recentUpdates.toArray(locs);
            byte[] batch_bytes = new byte[bu.getSerializedSize()];
            bu.writeTo(CodedOutputByteBufferNano.newInstance(batch_bytes));
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String dirName = "ShadowMapsData";
            String folderPath = baseDir + File.separator + dirName;
            boolean success = true;
            File folder = new File(folderPath);
            String filePath;
            if (!folder.exists()) {
                success = folder.mkdir();
            }
            if (success) {
                filePath = baseDir + File.separator + dirName + File.separator + bu.batchId;
            } else {
                filePath = baseDir + File.separator + bu.batchId;
            }
            Log.v(TAG, "Logging to " + filePath);
            File file = new File(filePath);
            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            fos.write(batch_bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                Log.v(TAG, "IOException writing ShadowMaps data to file: " + ioe);
            }

        }
    }

    private LocationUpdate newProtoLocationUpdate() {
        LocationUpdate new_update = new LocationUpdate();
        new_update.estimates = new LocationEstimate[1];
        new_update.satelliteInfos = new SatInfo[30];
        new_update.userInfo = user_info;
        locationEstimateCount.set(0);
        satInfoCount.set(0);
        return new_update;
    }

    private void uploadShadowMapsData(final byte[] update) {
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
                    if (response.code() == 200) {
                        byte[] response_stream = response.body().bytes();
                        LocationImprovement updated_location = LocationImprovement.parseFrom(response_stream);
                        provideImprovedLocation(updated_location);
                    } else {
                        Log.v(TAG, "Unexpected response from ShadowMaps:" + response.code() + ":" + response.message());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
}