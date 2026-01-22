package com.example.projektzaliczeniowy;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView fullScreenImageView;
    private TextView userNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        fullScreenImageView = findViewById(R.id.fullScreenImageView);
        userNameTextView = findViewById(R.id.userNameTextView);

        // Ukrycie system bars dla pełnego ekranu
        hideSystemUI();

        // Pobranie danych z Intent
        String imageBase64 = getIntent().getStringExtra("imageBase64");
        String userName = getIntent().getStringExtra("userName");

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            if (ImageUtils.isValidBase64(imageBase64)) {
                ImageUtils.loadBase64IntoImageView(imageBase64, fullScreenImageView);
            } else {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd ładowania obrazu");
                finish();
            }
        } else {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Brak obrazu do wyświetlenia");
            finish();
        }

        if (userName != null) {
            userNameTextView.setText("Zdjęcie użytkownika: " + userName);
        }

        // Zamknięcie po kliknięciu w obraz
        fullScreenImageView.setOnClickListener(v -> finish());

        // Zamknięcie po kliknięciu w tło
        findViewById(R.id.fullScreenContainer).setOnClickListener(v -> finish());
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}