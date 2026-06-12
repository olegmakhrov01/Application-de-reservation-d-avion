package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Recherche extends AppCompatActivity {

    private static final String SERVER_URL = "https://backend-g7pr.onrender.com/vols";

    private ListView listViewVols;
    private VolAdapter volAdapter;
    private ArrayList<Vol> volList = new ArrayList<>();
    private String userId;

    private ImageButton menuButton;
    private Button filterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recherche);


        userId = getIntent().getStringExtra("id_utilisateurs");
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listViewVols = findViewById(R.id.listViewVols);
        volAdapter = new VolAdapter(this, volList, userId);
        listViewVols.setAdapter(volAdapter);

        menuButton = findViewById(R.id.menuButton);
        filterButton = findViewById(R.id.btnFiltres);


        menuButton.setOnClickListener(this::showMenu);
        filterButton.setOnClickListener(this::showFilterPopup);

        fetchVolsFromServer();
    }

    private void fetchVolsFromServer() {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(SERVER_URL).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    JSONArray array = new JSONArray(jsonData);
                    volList.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Vol vol = new Vol(
                                obj.getInt("id"),
                                obj.getString("date"),
                                obj.getString("depart"),
                                obj.getString("destination"),
                                obj.getDouble("prix")
                        );
                        volList.add(vol);
                    }

                    runOnUiThread(() -> volAdapter.notifyDataSetChanged());
                } else {
                    showError("Erreur serveur");
                }
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
                showError("Erreur réseau");
            }
        }).start();
    }

    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_accueil) {
                Intent intent = new Intent(this, PageApresLogin.class);
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            } else if (id == R.id.menu_recherche) {
                Intent intent = new Intent(this, Recherche.class);
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            } else if (id == R.id.menu_notifications) {
                Intent intent = new Intent(this, Notification.class);
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            } else if (id == R.id.menu_compte) {
                Intent intent = new Intent(this, DetailCompte.class);
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            } else if (id == R.id.menu_mes_vols) {
                Intent intent = new Intent(this, MesVols.class);
                intent.putExtra("id_utilisateurs", userId);
                startActivity(intent);
            }
            return true;
        });

        popup.show();
    }

    private void showFilterPopup(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtres, null);
        EditText inputDest = dialogView.findViewById(R.id.inputDestination);
        EditText inputDate = dialogView.findViewById(R.id.inputDate);
        EditText inputPrix = dialogView.findViewById(R.id.inputPrixMax);

        new AlertDialog.Builder(this)
                .setTitle("Filtres de recherche")
                .setView(dialogView)
                .setPositiveButton("Rechercher", (dialog, which) -> {
                    String dest = inputDest.getText().toString().trim();
                    String date = inputDate.getText().toString().trim();
                    String prix = inputPrix.getText().toString().trim();
                    fetchVolsWithFilters(dest, date, prix);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void fetchVolsWithFilters(String dest, String date, String prix) {
        String url = SERVER_URL + "?";
        if (!dest.isEmpty()) url += "destination=" + dest + "&";
        if (!date.isEmpty()) url += "date=" + date + "&";
        if (!prix.isEmpty()) url += "prix=" + prix;

        String finalUrl = url;

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(finalUrl).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    JSONArray array = new JSONArray(jsonData);
                    volList.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Vol vol = new Vol(
                                obj.getInt("id"),
                                obj.getString("date"),
                                obj.getString("depart"),
                                obj.getString("destination"),
                                obj.getDouble("prix")
                        );
                        volList.add(vol);
                    }

                    runOnUiThread(() -> volAdapter.notifyDataSetChanged());
                } else {
                    showError("Erreur serveur");
                }
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
                showError("Erreur réseau");
            }
        }).start();
    }
}



