package com.example.trabajoapi.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class TrabajadorRecordatorioScheduler {

    private static final String UNIQUE_NAME = "recordatorio_fichaje_bg";

    private TrabajadorRecordatorioScheduler() { }

    // Programa el worker periódico con red disponible y nombre único para evitar duplicados.
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(TrabajadorRecordatorio.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    // Cancela el worker periódico asociado al nombre único.
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME);
    }
}
