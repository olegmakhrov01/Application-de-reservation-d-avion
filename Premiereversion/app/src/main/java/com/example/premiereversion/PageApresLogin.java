package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PageApresLogin extends AppCompatActivity {

    private ImageButton menuButton;
    private String userId; // Déclaration manquante dans ton code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_apres_login);

        menuButton = findViewById(R.id.menuButton);

        menuButton.setOnClickListener(this::showPopupMenu);

        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> logout());


        userId = getIntent().getStringExtra("id_utilisateurs");

        if (userId == null || userId.isEmpty()) {
            Log.e("PageApresLogin", "Erreur: ID utilisateur non reçu!");
            Toast.makeText(this, "Erreur: ID utilisateur introuvable", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Log.d("PageApresLogin", "User connecté avec ID: " + userId);
        }
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

    private void logout() {
        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);
        Toast.makeText(this, "Déconnexion...", Toast.LENGTH_SHORT).show();
    }


}
