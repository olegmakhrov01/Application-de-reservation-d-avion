package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Creercompte extends AppCompatActivity {


    private static final String SERVER_URL = "https://backend-g7pr.onrender.com/users";


    private EditText editNom, editPrenom, editEmail, editPassword;
    private Button btnRegister, btnConnexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creercompte);

        // Liens avec les nouveaux ID du XML
        editNom = findViewById(R.id.inputNom);
        editPrenom = findViewById(R.id.inputPrenom);
        editEmail = findViewById(R.id.inputEmail);
        editPassword = findViewById(R.id.inputPassword);
        btnRegister = findViewById(R.id.btnCreerCompte);
        btnConnexion = findViewById(R.id.lienConnexion);

        // Redirige vers l'accueil ou la connexion
        btnConnexion.setOnClickListener(view -> {
            Intent intent = new Intent(Creercompte.this, Login.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String nom = editNom.getText().toString();
        String prenom = editPrenom.getText().toString();
        String email = editEmail.getText().toString();
        String motDePasse = editPassword.getText().toString();



        EditText editConfirmPassword = findViewById(R.id.inputConfirmPassword);
        String confirmPassword = editConfirmPassword.getText().toString();
        if (!motDePasse.equals(confirmPassword)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }


        User newUser = new User(nom, prenom, email, motDePasse);
        String jsonData = newUser.getJson();

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(jsonData, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(Creercompte.this, "Compte créé!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Creercompte.this, Login.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    String errorMessage = "Erreur lors de l'inscription";
                    try {
                        String responseBody = response.body().string();
                        org.json.JSONObject errorObj = new org.json.JSONObject(responseBody);
                        if (errorObj.has("error")) {
                            errorMessage = errorObj.getString("error");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String finalErrorMessage = errorMessage;
                    runOnUiThread(() -> Toast.makeText(Creercompte.this, finalErrorMessage, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(Creercompte.this, "Erreur serveur", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
