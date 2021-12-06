package com.capacitor.plugins.backgroundlocation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundLocationWorker extends Worker {
    private final Context context;

    public BackgroundLocationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = getApplicationContext();
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
    }
}
