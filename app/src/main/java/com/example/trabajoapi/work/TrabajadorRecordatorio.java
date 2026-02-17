package com.example.trabajoapi.work;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.trabajoapi.MainActivity;
import com.example.trabajoapi.data.RecordatorioResponse;
import com.example.trabajoapi.data.RetrofitClient;
import com.example.trabajoapi.data.SessionManager;

import retrofit2.Response;

public class TrabajadorRecordatorio extends Worker {

    private static final String CHANNEL_ID = "canal_recordatorio_fichaje_bg_v1";

    public TrabajadorRecordatorio(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // Consulta el backend y lanza una notificación si hay un recordatorio pendiente.
    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();

            // Si el sistema exige permiso y no existe, se omite el aviso sin fallar el worker.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    return Result.success();
                }
            }

            SessionManager sm = new SessionManager(ctx);
            String auth = sm.getAuthToken();
            if (auth == null) return Result.success();

            Response<RecordatorioResponse> resp =
                    RetrofitClient.getInstance()
                            .getMyApi()
                            .getRecordatorioFichaje("Bearer " + auth)
                            .execute();

            if (resp.code() == 204) return Result.success();
            if (!resp.isSuccessful() || resp.body() == null) return Result.success();

            RecordatorioResponse r = resp.body();
            String titulo = safe(r.getTitulo(), "Aviso");
            String mensaje = safe(r.getMensaje(), "");

            if (mensaje.trim().isEmpty()) return Result.success();

            mostrarNotificacion(ctx, titulo, mensaje);
            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        }
    }

    // Construye la notificación con canal propio y navegación de vuelta a la app.
    private void mostrarNotificacion(Context ctx, String titulo, String cuerpo) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorio de fichaje (BG)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify(501, b.build());
    }

    // Normaliza strings para evitar nulos y valores vacíos en la notificación.
    private String safe(String v, String fallback) {
        if (v == null) return fallback;
        String s = v.trim();
        return s.isEmpty() ? fallback : s;
    }
}
