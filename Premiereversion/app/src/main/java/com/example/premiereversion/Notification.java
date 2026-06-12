package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Notification extends AppCompatActivity {

    private String userId;
    private static final String SERVER_URL = "https://backend-g7pr.onrender.com";
    private ListView listHistorique;
    private ArrayList<NotificationItem> historique;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(this::showPopupMenu);

        userId = getIntent().getStringExtra("id_utilisateurs");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listHistorique = findViewById(R.id.listHistorique);
        Button btnEffacer = findViewById(R.id.btnEffacerHistorique);

        btnEffacer.setOnClickListener(v -> deleteAllNotifications());

        // Charger les notifications
        fetchNotifications();
    }

    private void fetchNotifications() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SERVER_URL + "/notifications/" + userId)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);

                    historique = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject notif = jsonArray.getJSONObject(i);
                        int id = notif.getInt("id");
                        String contenu = notif.getString("contenu");
                        historique.add(new NotificationItem(id, contenu));
                    }

                    runOnUiThread(() -> {
                        NotificationAdapter adapter = new NotificationAdapter(this, historique);
                        listHistorique.setAdapter(adapter);
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur chargement notifications", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur parsing", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void deleteAllNotifications() {
        OkHttpClient deleteClient = new OkHttpClient();
        Request deleteRequest = new Request.Builder()
                .url(SERVER_URL + "/notifications/" + userId)
                .delete()
                .build();

        new Thread(() -> {
            try (Response response = deleteClient.newCall(deleteRequest).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Historique supprimé ✅", Toast.LENGTH_SHORT).show();
                        listHistorique.setAdapter(new NotificationAdapter(this, new ArrayList<>()));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            Intent intent = null;

            if (id == R.id.menu_accueil) {
                intent = new Intent(this, PageApresLogin.class);
            } else if (id == R.id.menu_recherche) {
                intent = new Intent(this, Recherche.class);
            } else if (id == R.id.menu_notifications) {
                intent = new Intent(this, Notification.class);
            } else if (id == R.id.menu_compte) {
                intent = new Intent(this, DetailCompte.class);
            } else if (id == R.id.menu_mes_vols) {
                intent = new Intent(this, MesVols.class);
            }

            if (intent != null) {
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            }
            return true;
        });

        popup.show();
    }
}
