package com.shadowmaps.service;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.shadowmaps.util.api.protobufs.LocationImprovement;

import org.chromium.base.Log;
import org.chromium.net.CronetEngine;
import org.chromium.net.UploadDataProvider;
import org.chromium.net.UploadDataSink;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlRequestException;
import org.chromium.net.UrlResponseInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by user on 1/10/16.
 */


public class NetworkProcessor {

    private static final String TAG = "ShadowMapsCronet";
    private CronetEngine mCronetEngine = null;
    private static final String appUrl = "https://and.shadowmaps.com:8080";
    private Context context;

    public NetworkProcessor(Context c){
        CronetEngine.Builder myBuilder = new CronetEngine.Builder(c);
        context = c;
        try {
            myBuilder.setStoragePath(c.getCacheDir().getCanonicalPath())
                    .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 100 * 1024)
                    .enableQUIC(true)
                    .addQuicHint("and.shadowmaps.com", 8080, 8080);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCronetEngine = myBuilder.build();
    }

    class SimpleUrlRequestCallback extends UrlRequest.Callback {
        private ByteArrayOutputStream mBytesReceived = new ByteArrayOutputStream();
        private WritableByteChannel mReceiveChannel = Channels.newChannel(mBytesReceived);

        @Override
        public void onRedirectReceived(
                UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
            Log.i(TAG, "****** onRedirectReceived ******");
            request.followRedirect();
        }

        @Override
        public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
            Log.i(TAG, "****** Response Started ******");
            Log.i(TAG, "*** Headers Are *** " + info.getAllHeaders());

            request.readNew(ByteBuffer.allocateDirect(32 * 1024));
        }

        @Override
        public void onReadCompleted(
                UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
            Log.i(TAG, "****** onReadCompleted ******" + byteBuffer);
            byteBuffer.flip();
            try {
                if(info.getHttpStatusCode() == 200 || info.getHttpStatusCode() == 202) {
                    mReceiveChannel.write(byteBuffer);
                    LocationImprovement updated_location = LocationImprovement.parseFrom(byteBuffer.array());
                    // Broadcast Intent
                    long request_end = System.currentTimeMillis();
                    shareLocationImprovement(updated_location);
                }
            } catch (IOException e) {
                Log.i(TAG, "IOException during ByteBuffer read. Details: ", e);
            }
            byteBuffer.clear();
            request.readNew(byteBuffer);
        }

        void shareLocationImprovement(LocationImprovement response) {
            Intent intent = new Intent();
            double lon = response.lon;
            double lat = response.lat;
            double acc = response.acc;
            long utc = response.utc;
            android.util.Log.v("ShadowMaps", "ShadowMaps update received, " + (System.currentTimeMillis() - utc) + "ms delay.");
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
            context.sendBroadcast(intent);

        }

        @Override
        public void onSucceeded(UrlRequest request, final UrlResponseInfo info) {
            Log.i(TAG, "****** Request Completed, status code is " + info.getHttpStatusCode()
                    + ", total received bytes is " + info.getReceivedBytesCount());

            final String receivedData = mBytesReceived.toString();
            final String url = info.getUrl();
            final String text = "Completed " + url + " (" + info.getHttpStatusCode() + ")";
            Log.v(TAG, "Info received: " + info.toString());
            Log.v(TAG, "Info received: " + receivedData);
        }

        @Override
        public void onFailed(UrlRequest request, UrlResponseInfo info, UrlRequestException error) {
            Log.i(TAG, "****** onFailed, error is: " + error.getMessage());
        }
    }

    class SimpleUploadDataProvider extends UploadDataProvider {
        private final byte[] mUploadData;
        private int mOffset;

        SimpleUploadDataProvider(byte[] uploadData) {
            mUploadData = uploadData;
            mOffset = 0;
        }

        @Override
        public long getLength() {
            return mUploadData.length;
        }

        @Override
        public void read(final UploadDataSink uploadDataSink, final ByteBuffer byteBuffer)
                throws IOException {
            Log.v("TAG", "Offset is " + mOffset);
            int position = byteBuffer.position();
            byteBuffer.put(mUploadData, mOffset, Math.min(mUploadData.length - mOffset, byteBuffer.remaining()));
            mOffset += byteBuffer.position() - position;
            Log.v("TAG", "Offset is now " + mOffset);
            uploadDataSink.onReadSucceeded(false);
        }

        @Override
        public void rewind(final UploadDataSink uploadDataSink) throws IOException {
            mOffset = 0;
            uploadDataSink.onRewindSucceeded();
        }
    }




    private void applyPostDataToUrlRequestBuilder(
            UrlRequest.Builder builder, Executor executor, byte[] postData) {
        if (postData != null && postData.length > 0) {
            UploadDataProvider uploadDataProvider = new SimpleUploadDataProvider(postData);
            builder.setHttpMethod("POST");
            builder.addHeader("Content-Type", "application/octet-stream");
            builder.setUploadDataProvider(uploadDataProvider, executor);
        }
    }

    public void sendUpdate(byte[] postData) {

        Log.i(TAG, "Cronet started: " + appUrl);
        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Callback callback = new SimpleUrlRequestCallback();
        UrlRequest.Builder builder = new UrlRequest.Builder(appUrl, callback, executor, mCronetEngine);
        applyPostDataToUrlRequestBuilder(builder, executor, postData);
        builder.build().start();
    }

    private void startNetLog() {
        mCronetEngine.startNetLogToFile(
                Environment.getExternalStorageDirectory().getPath() + "/cronet_sample_netlog.json",
                false);
    }

    private void stopNetLog() {
        mCronetEngine.stopNetLog();
    }
}
