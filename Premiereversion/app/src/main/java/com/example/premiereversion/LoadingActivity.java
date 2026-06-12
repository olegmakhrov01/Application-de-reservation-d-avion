package com.example.premiereversion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading); // tu vas le créer ci-dessous

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class); // Redirige vers page de login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, 1500); // 1.5 sec de loading (ajustable)
    }
}
