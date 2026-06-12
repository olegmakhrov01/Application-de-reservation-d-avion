package com.example.premiereversion;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Reservation extends AppCompatActivity {

    private int volId;
    private String userId;
    private String destinationVol;

    private TextView textDetails, textPlaces;
    private Spinner spinnerSiege, spinnerPaiement;
    private Button btnConfirmer, btnAnnuler;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private static final String SERVER_URL = "https://backend-g7pr.onrender.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        // Canal de notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "vols_channel_id",
                    "Notifications Vols",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Permission de notification refusée", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);

        volId = getIntent().getIntExtra("vol_id", -1);
        userId = getIntent().getStringExtra("id_utilisateurs");

        textDetails = findViewById(R.id.textReservationDetails);
        textPlaces = findViewById(R.id.textPlacesRestantes);
        spinnerSiege = findViewById(R.id.spinnerSiege);
        spinnerPaiement = findViewById(R.id.spinnerPaiement);
        btnConfirmer = findViewById(R.id.btnConfirmer);
        btnAnnuler = findViewById(R.id.btnAnnuler);

        if (volId == -1 || userId == null) {
            Toast.makeText(this, "Erreur de données", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadVolDetails();

        btnAnnuler.setOnClickListener(v -> finish());

        btnConfirmer.setOnClickListener(v -> {
            String siege = spinnerSiege.getSelectedItem().toString().replace("Siège ", "");
            String moyenPaiement = spinnerPaiement.getSelectedItem().toString();
            effectuerReservation(siege, moyenPaiement);
        });
    }

    private void loadVolDetails() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SERVER_URL + "/vols/" + volId).build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);

                    String rawDate = obj.getString("date");
                    String depart = obj.getString("depart");
                    String dest = obj.getString("destination");
                    double prix = obj.getDouble("prix");
                    int places = obj.getInt("places_restantes");
                    JSONArray array = obj.getJSONArray("places_disponibles");

                    destinationVol = dest;

                    String formattedDate = formatDate(rawDate);

                    runOnUiThread(() -> {
                        textDetails.setText("Date : " + formattedDate + "\nDépart : " + depart + "\nDestination : " + dest + "\nPrix : " + prix + " $");
                        textPlaces.setText("Places restantes : " + places);

                        ArrayList<String> sieges = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            try {
                                int numero = array.getInt(i);
                                sieges.add("Siège " + numero);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sieges);
                        spinnerSiege.setAdapter(adapter);
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur lors du chargement", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return rawDate;
        }
    }

    private void effectuerReservation(String siege, String paiement) {
        OkHttpClient client = new OkHttpClient();
        JSONObject body = new JSONObject();

        try {
            body.put("id_utilisateur", userId);
            body.put("id_vol", volId);
            body.put("siege", siege);
            body.put("paiement", paiement);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SERVER_URL + "/reserver")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Réservation réussie !", Toast.LENGTH_SHORT).show();

                        // ✅ Notification immédiate
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "vols_channel_id")
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("Réservation confirmée")
                                .setContentText("Votre siège " + siege + " a été réservé avec succès.")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED) {
                            notificationManager.notify(1, builder.build());
                        }

                        // ✅ Créer le message à enregistrer côté serveur
                        String messageConfirmation = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())
                                + " - Confirmation : siège " + siege + " réservé pour " + destinationVol;

                        // ✅ Envoyer confirmation au serveur
                        OkHttpClient clientNotif = new OkHttpClient();
                        JSONObject jsonNotif = new JSONObject();
                        try {
                            jsonNotif.put("id_utilisateur", Integer.parseInt(userId));

                            jsonNotif.put("notification", messageConfirmation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Request notifRequest = new Request.Builder()
                                .url(SERVER_URL + "/notifications")
                                .post(RequestBody.create(jsonNotif.toString(), MediaType.get("application/json")))
                                .build();

                        new Thread(() -> {
                            try (Response notifResponse = clientNotif.newCall(notifRequest).execute()) {
                                if (!notifResponse.isSuccessful()) {
                                    System.err.println("❌ Échec envoi notif confirmation");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                        // ✅ Notification planifiée (30 sec)
                        Intent notifIntent = new Intent(this, NotificationReceiver.class);
                        notifIntent.putExtra("destination", destinationVol);
                        notifIntent.putExtra("id_utilisateurs", userId);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        long triggerAtMillis = System.currentTimeMillis() + 30 * 1000;
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur de réservation", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}