package com.example.projektzaliczeniowy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText emailField;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.reset_email);
        Button resetPasswordBtn = findViewById(R.id.reset_button);

        resetPasswordBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            if (email.isEmpty()) {
                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Wprowadź email");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Wysłano email resetujący hasło"))
                    .addOnFailureListener(e ->
                            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd: " + e.getMessage()));
        });
    }
}
