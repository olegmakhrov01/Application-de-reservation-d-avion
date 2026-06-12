package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class DetailCompte extends AppCompatActivity {

    private static final String SERVER_URL = "https://backend-g7pr.onrender.com";

    private ImageButton menuButton;
    private EditText editNom, editPrenom;
    private EditText editEmail, editPassword;
    private Button btnSave;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // ✅ D'abord
        setContentView(R.layout.activity_detail_compte); // ✅ Ensuite charger la vue

        // ✅ Ensuite seulement tu peux récupérer les vues
        Button btnDelete = findViewById(R.id.btnDeleteAccount);
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(this::showPopupMenu);

        editNom = findViewById(R.id.editNom);
        editPrenom = findViewById(R.id.editPrenom);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnSave = findViewById(R.id.btnSave);

        userId = getIntent().getStringExtra("id_utilisateurs");

        if (userId == null) {
            Toast.makeText(this, "Utilisateur non identifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserInfo();

        btnSave.setOnClickListener(v -> updateUserInfo());
    }


    private void showPopupMenu(View anchor) {
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

    private void loadUserInfo() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/users/" + userId)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JSONObject user = new JSONObject(json);

                    runOnUiThread(() -> {
                        try {
                            editNom.setText(user.getString("nom"));
                            editPrenom.setText(user.getString("prenom"));

                            editEmail.setText(user.getString("email"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Impossible de charger les infos", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateUserInfo() {
        String newNom = editNom.getText().toString().trim();
        String newPrenom = editPrenom.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();
        String newPassword = editPassword.getText().toString().trim();

        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Email obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        JSONObject body = new JSONObject();
        try {
            body.put("nom", newNom);
            body.put("prenom", newPrenom);
            body.put("email", newEmail);
            if (!newPassword.isEmpty()) {
                body.put("mot_de_passe", newPassword);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(SERVER_URL + "/users/" + userId)
                .put(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Compte mis à jour", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.")
                .setPositiveButton("Oui", (dialog, which) -> deleteAccount())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteAccount() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/users/" + userId)
                .delete()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class); // Redirige à l'écran de login
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
