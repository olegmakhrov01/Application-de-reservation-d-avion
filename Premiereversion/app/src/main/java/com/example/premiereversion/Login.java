package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Login extends AppCompatActivity {

    private static final String SERVER_URL = "https://backend-g7pr.onrender.com/users";

    private EditText editEmail, editPassword;
    private Button btnLogin, btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 🆕 IDs from updated XML
        editEmail = findViewById(R.id.loginEmail);
        editPassword = findViewById(R.id.loginPassword); // changed from loginMotDePasse
        btnLogin = findViewById(R.id.btnLogin);          // changed from login
        btnCreateAccount = findViewById(R.id.registerLink); // changed from goCreerUnCompte

        btnLogin.setOnClickListener(view -> loginUser());

        btnCreateAccount.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Creercompte.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    Log.d("Login", "Server Response: " + jsonResponse);

                    JSONArray usersArray = new JSONArray(jsonResponse);
                    JSONObject foundUser = null;

                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject user = usersArray.getJSONObject(i);
                        Log.d("Login", "Checking user: " + user.toString());

                        if (user.has("email") && user.getString("email").equals(email) &&
                                user.has("mot_de_passe") && user.getString("mot_de_passe").equals(password)) {
                            foundUser = user;
                            break;
                        }
                    }

                    if (foundUser != null) {
                        if (!foundUser.has("id")) {
                            Log.e("Login", "Erreur: Clé 'id' manquante!");
                            runOnUiThread(() -> Toast.makeText(Login.this, "Erreur: Clé 'id' manquante", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        String userId = foundUser.optString("id", "").trim();
                        String role = foundUser.optString("role", "user").trim(); // 👈 récupère le rôle

                        if (userId.isEmpty()) {
                            Log.e("Login", "Erreur: ID utilisateur vide!");
                            runOnUiThread(() -> Toast.makeText(Login.this, "Erreur: ID utilisateur manquant", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        Log.d("Login", "User found! ID: " + userId + " | Role: " + role);

                        Intent intent;
                        if (role.equals("admin")) {
                            intent = new Intent(Login.this, AdminPage.class); // 👈 Crée cette activité pour les admins
                        } else {
                            intent = new Intent(Login.this, PageApresLogin.class);
                        }

                        intent.putExtra("id_utilisateurs", userId);
                        Log.d("Login", "Intent Extra: id_utilisateurs = " + userId);
                        startActivity(intent);
                        finish();
                    } else {
                        runOnUiThread(() -> Toast.makeText(Login.this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Login.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Login.this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
