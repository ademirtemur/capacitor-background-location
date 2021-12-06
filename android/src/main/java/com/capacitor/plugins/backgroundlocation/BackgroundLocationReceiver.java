package com.capacitor.plugins.backgroundlocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BackgroundLocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManager.getInstance(context);

        OneTimeWorkRequest startServiceRequest = new OneTimeWorkRequest.Builder(BackgroundLocationWorker.class).build();

        workManager.enqueue(startServiceRequest);
    }
}