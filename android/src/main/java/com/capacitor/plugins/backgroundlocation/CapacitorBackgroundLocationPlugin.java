package com.capacitor.plugins.backgroundlocation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CapacitorBackgroundLocation")
public class CapacitorBackgroundLocationPlugin extends Plugin {

    private final String SMTH_WENT_WRONG = "SOME_THING_WENT_WRONG";

    Context context;
    ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    public void load() {
        super.load();

        context = getContext().getApplicationContext();

        BackgroundLocationService.confCustomErrorListener((JSObject payload) -> {
            this.notifyListeners("ERROR", payload);
        });

        BackgroundLocationService.confCustomChangeEventListener((locationEventPayload) -> {
            notifyListeners("CHANGE", locationEventPayload, false);
        });

        locationPermissionRequest =
                getBridge().registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            locationPermissionRequest.launch(new String[]{
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            });
                        }
                );

        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    @Override
    @PluginMethod(returnType = PluginMethod.RETURN_NONE)
    public void removeAllListeners(PluginCall call) {
        super.removeAllListeners(call);
    }

    @Override
    protected void handleOnDestroy() {
        unsetAppListeners();
    }

    private void unsetAppListeners() {
        bridge.getApp().setStatusChangeListener(null);
        bridge.getApp().setAppRestoredListener(null);
    }

    private void stopService() {
        try {
            BackgroundLocationService.terminateProcess();

            Thread.sleep(3000);

            Intent serviceIntent = new Intent(context, BackgroundLocationService.class);
            context.stopService(serviceIntent);
        } catch (Exception ex) {
            System.out.println("");
        }
    }

    @PluginMethod
    public void setConfig(PluginCall call) {
        String title = call.getString("title");
        String description = call.getString("description");
        String URL = call.getString("url");
        JSObject headers = call.getObject("headers");
        JSObject body = call.getObject("body");

        BackgroundLocationService.setConfig(title, description, URL, headers, body);

        call.resolve();
    }

    @PluginMethod
    public void start(PluginCall call) {
        try {
            Integer interval = call.getInt("interval");
            Integer locationPriority = call.getInt("locationPriority");

            if (interval == null || interval < 5000) {
                call.reject("INCORRECT_INTERVAL_VALUE");
                return;
            }

            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (!LocationManagerCompat.isLocationEnabled(lm)) {
                this.notifyListeners("ERROR", new JSObject().put("error", "GPS_IS_NOT_ENABLE"));
                call.reject("GPS_IS_NOT_ENABLE");
                return;
            }

            stopService();

            if (BackgroundLocationService.isServiceRunning) {
                call.reject("SERVICE_IS_RUNNING_ALREADY");
                return;
            }

            Intent serviceIntent = new Intent(context, BackgroundLocationService.class);
            context.startService(context, serviceIntent);

            BackgroundLocationService.startProcess(
                    context,
                    interval,
                    locationPriority
            );

            call.resolve();
        } catch (Exception ex) {
            stopService();
            call.reject(SMTH_WENT_WRONG);
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            stopService();

            call.resolve();
        } catch (Exception ex) {
            call.reject(SMTH_WENT_WRONG);
        }
    }
}
