package com.capacitor.plugins.backgroundlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
// import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class BackgroundLocationService extends Service {
    public static boolean isServiceRunning;
    private final String CHANNEL_ID = "CAP_BG_LOC_NOTIFICATION_CHANNEL";
    private static String title = "location service is running";
    private static String description = "Listening for screen off/on events";
    private static String URL;
    private static JSObject headers;
    private static JSObject body;

    private static FusedLocationProviderClient fusedLocationClient;
    private static LocationCallback locationCallback;

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static OnCustomErrorListener onCustomErrorListener;
    private static OnCustomEventListener onCustomChangeEventListener;

    public BackgroundLocationService() {
        isServiceRunning = false;
    }

    public static void confCustomErrorListener(OnCustomErrorListener onCustomErrorListener) {
        BackgroundLocationService.onCustomErrorListener = onCustomErrorListener;
    }

    public static void confCustomChangeEventListener(OnCustomEventListener onCustomChangeEventListener) {
        BackgroundLocationService.onCustomChangeEventListener = onCustomChangeEventListener;
    }

    public static void setConfig(
            String title,
            String description,
            String URL,
            JSObject headers,
            JSObject body
    ) {
        if (title != null) {
            BackgroundLocationService.title = title;
        }
        if (description != null) {
            BackgroundLocationService.description = description;
        }

        BackgroundLocationService.URL = URL;
        BackgroundLocationService.headers = headers != null ? headers : new JSObject();
        BackgroundLocationService.body = body != null ? body : new JSObject();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        this.createNotificationChannel();

        BackgroundLocationService.client.addHeader("Content-Type", "application/json");
        BackgroundLocationService.client.addHeader("Accept", "application/json");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    appName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    private static void doLocationUpdateProcess(Context context, Location location) {
        double _lat = (float) location.getLatitude();
        double _lng = (float) location.getLongitude();
        float _accuracy = (float) location.getAccuracy();
        float _altitude = (float) location.getAltitude();
        float _bearing = (float) location.getBearing();
        float _angle = (float) location.getBearing();
        float _speed = (float) location.getSpeed();

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date(location.getTime()));

        JSObject locationEventPayload = new JSObject();
        locationEventPayload.put("latitude", _lat);
        locationEventPayload.put("longitude", _lng);
        locationEventPayload.put("accuracy", _accuracy);
        locationEventPayload.put("altitude", _altitude);
        locationEventPayload.put("bearing", _bearing);
        locationEventPayload.put("angle", _angle);
        locationEventPayload.put("speed", _speed);
        locationEventPayload.put("time", nowAsISO);
        locationEventPayload.put("lastUpdate", nowAsISO);

        BackgroundLocationService.onCustomChangeEventListener.onEvent(locationEventPayload);

        String url = BackgroundLocationService.URL;


        if (url != null && url != "") {
            try {
                Iterator<String> iterator = BackgroundLocationService.headers.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    BackgroundLocationService.client.addHeader(key, BackgroundLocationService.headers.getString(key));
                }

                JSObject _body = new JSObject();

                try {
                    moveJSFromTo(BackgroundLocationService.body, _body);
                    moveJSFromTo(locationEventPayload, _body);
                } catch (Exception ex) {

                }
                StringEntity entity = new StringEntity(_body.toString());
                BackgroundLocationService.client.post(context, url, entity, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        // Log.d("snow", responseBody.toString());
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        // Log.d("snow", "onErrorResponse: ", error);
                        BackgroundLocationService.onCustomErrorListener.onError(
                                new JSObject().put("error", error)
                        );
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getAppResourceIdentifier(String name, String defType) {
        return this.getResources().getIdentifier(
                name,
                defType,
                this.getPackageName()
        );
    }

    private String getAppString(String name, String fallback) {
        int id = getAppResourceIdentifier(name, "string");
        return id == 0 ? fallback : this.getString(id);
    }

    private int getIcon() {
        Integer icon = R.drawable.ic_transparent;

        try {
            String name = getAppString(
                    "capacitor_background_geolocation_notification_icon",
                    "mipmap/cap_bg_loc"
            );
            String[] parts = name.split("/");
            icon = getAppResourceIdentifier(parts[1], parts[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return icon;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BackgroundLocationService.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent notificationIntent = new Intent(this, getApplication().getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(pendingIntent)
                .setSmallIcon(getIcon())
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        stopForeground(true);

        Intent broadcastIntent = new Intent(getApplicationContext(), BackgroundLocationReceiver.class);
        sendBroadcast(broadcastIntent);

        super.onDestroy();
    }

    private static void moveJSFromTo(
            @NonNull JSObject from,
            @NonNull JSObject target
    ) throws JSONException {
        Iterator<String> iterator = from.keys();

        while (iterator.hasNext()) {
            String key = iterator.next();
            target.put(key, from.get(key));
        }
    }

    public static void startProcess(
            Context context,
            Integer interval,
            Integer locationPriority
    ) throws Exception {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(interval);
        locationRequest.setPriority(locationPriority);

        BackgroundLocationService.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        BackgroundLocationService.doLocationUpdateProcess(context, location);
                    }
                };
                r.run();
            }
        };

        Boolean isGrantedAFL = (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        );
        Boolean isGrantedACL = (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        );

        if (isGrantedAFL && isGrantedACL) {
            Thread.sleep(5000);

            Task<Location> task = BackgroundLocationService.fusedLocationClient.getLastLocation();

            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    BackgroundLocationService.doLocationUpdateProcess(context, location);
                }
            });

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("");
                }
            });

            BackgroundLocationService.fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    BackgroundLocationService.locationCallback,
                    Looper.getMainLooper()
            );
        } else {
            BackgroundLocationService.onCustomErrorListener.onError(
                    new JSObject().put("error", "UNPERMITTED")
            );
            throw new Exception("UNPERMITTED");
        }
    }

    public static void terminateProcess() {
        if (BackgroundLocationService.locationCallback != null) {
            BackgroundLocationService.fusedLocationClient.removeLocationUpdates(
                    BackgroundLocationService.locationCallback
            );
        }
    }
}
