package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class MesVols extends AppCompatActivity {

    private String userId;
    private ListView listViewVols;
    private ArrayList<JSONObject> volsList;
    private ReservationAdapter adapter;
    private ImageButton menuButton;

    private static final String SERVER_URL = "https://backend-g7pr.onrender.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mes_vols);

        menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(this::showPopupMenu);

        userId = getIntent().getStringExtra("id_utilisateurs");
        listViewVols = findViewById(R.id.listViewVols);
        volsList = new ArrayList<>();
        adapter = new ReservationAdapter();
        listViewVols.setAdapter(adapter);

        if (userId == null) {
            Toast.makeText(this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadUserReservations();
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

    private void loadUserReservations() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SERVER_URL + "/reservations/" + userId)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JSONArray jsonArray = new JSONArray(json);

                    runOnUiThread(() -> {
                        volsList.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                volsList.add(jsonArray.getJSONObject(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("MesVols", "Loaded reservations: " + volsList);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erreur de récupération des réservations", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ✅ Now takes reservationId + volId
    private void cancelReservation(String reservationId, String volId) {
        OkHttpClient client = new OkHttpClient();
        JSONObject body = new JSONObject();

        try {
            body.put("id_reservation", reservationId);
            body.put("id_vol", volId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SERVER_URL + "/cancel_reservation")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Réservation annulée", Toast.LENGTH_SHORT).show();
                        loadUserReservations();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erreur lors de l'annulation", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private class ReservationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return volsList.size();
        }

        @Override
        public JSONObject getItem(int position) {
            return volsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            try {
                return volsList.get(position).getInt("id");
            } catch (JSONException e) {
                return position;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.list_item_reservation, null);

            TextView textDate = view.findViewById(R.id.textDate);
            TextView textDepart = view.findViewById(R.id.textDepart);
            TextView textDestination = view.findViewById(R.id.textDestination);
            TextView textPrix = view.findViewById(R.id.textPrix);
            Button btnAnnuler = view.findViewById(R.id.btnAnnuler);

            JSONObject reservation = getItem(position);
            try {
                String reservationId = reservation.getString("id_reservation");
                String volId = reservation.getString("id_vol");
                String date = reservation.getString("date");
                String depart = reservation.getString("depart");
                String destination = reservation.getString("destination");
                String prix = reservation.getString("prix");

                // Format propre de la date (enlever le Z)
                String dateFormattee = date.split("T")[0];

                textDate.setText("Date : " + dateFormattee);
                textDepart.setText("Départ : " + depart);
                textDestination.setText("Destination : " + destination);
                textPrix.setText("Prix : " + prix + " $");

                btnAnnuler.setOnClickListener(v -> {
                    new AlertDialog.Builder(MesVols.this)
                            .setTitle("Confirmation")
                            .setMessage("Annuler la réservation de ce vol ?")
                            .setPositiveButton("Oui", (dialog, which) -> cancelReservation(reservationId, volId))
                            .setNegativeButton("Non", null)
                            .show();
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return view;
        }
    }
}
