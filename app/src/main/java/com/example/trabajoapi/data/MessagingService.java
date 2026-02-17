package com.example.trabajoapi.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

import com.example.trabajoapi.MainActivity;
import com.example.trabajoapi.R;

public class MessagingService extends FirebaseMessagingService {

    // Recibe push y extrae título/cuerpo tanto de notification como de data (según cómo llegue).
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = null;
        String cuerpo = null;

        // Primero prueba el bloque estándar de notificación.
        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            cuerpo = remoteMessage.getNotification().getBody();
        }

        // Si viene vacío, cae al payload de datos para no perder avisos.
        if (titulo == null && remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            if (data.containsKey("titulo")) titulo = data.get("titulo");
            if (data.containsKey("mensaje")) cuerpo = data.get("mensaje");
            if (data.containsKey("title")) titulo = data.get("title");
            if (data.containsKey("body")) cuerpo = data.get("body");
        }

        // Con texto listo, levanta una notificación local.
        if (titulo != null && cuerpo != null) {
            mostrarNotificacion(titulo, cuerpo);
        }
    }

    // Crea el canal si hace falta y muestra el aviso apuntando a MainActivity.
    private void mostrarNotificacion(String titulo, String cuerpo) {
        String channelId = "canal_fichajes";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Avisos de Fichaje",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(0, builder.build());
    }
}
