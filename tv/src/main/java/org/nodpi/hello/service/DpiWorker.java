package org.nodpi.hello.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DpiWorker extends Worker {

    private final Context context;

    public DpiWorker(Context context, WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        ContextCompat.startForegroundService(context, new Intent(context, DpiService.class));
        return Result.success();
    }
}