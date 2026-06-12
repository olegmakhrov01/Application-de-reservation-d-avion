package com.example.premiereversion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String destination = intent.getStringExtra("destination");
        String userId = intent.getStringExtra("id_utilisateurs"); // on le passe depuis l'AlarmManager

        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String message = timestamp + " - Rappel de vol vers " + destination;

        // ✅ Notification locale
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "vols_channel_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Rappel de vol")
                .setContentText("Votre vol vers " + destination + " est prévu demain.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify(2, builder.build());
        }

        // ✅ Enregistrer dans SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("notif_data", Context.MODE_PRIVATE);
        prefs.edit().putString("notif_" + System.currentTimeMillis(), message).apply();

        // ✅ Envoi vers API REST
        if (userId != null) {
            OkHttpClient client = new OkHttpClient();

            JSONObject json = new JSONObject();
            try {
                json.put("id_utilisateur", userId);
                json.put("notification", message);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Request request = new Request.Builder()
                    .url("https://backend-g7pr.onrender.com/notifications")
                    .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("❌ Notification non enregistrée : " + response.code());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}