package com.example.projektzaliczeniowy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailField, passwordField, repeatPasswordField, nameField;
    private Button registerButton;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        nameField = findViewById(R.id.register_name);
        emailField = findViewById(R.id.register_email);
        passwordField = findViewById(R.id.register_password);
        repeatPasswordField = findViewById(R.id.register_repeat_password);
        registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        String repeatPassword = repeatPasswordField.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(repeatPassword)) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Wypełnij wszystkie pola");
            return;
        }

        if (!password.equals(repeatPassword)) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Hasła się nie zgadzają");
            return;
        }

        if (password.length() < 6) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Hasło musi mieć co najmniej 6 znaków");
            return;
        }

        String passwordError = checkPasswordStrength(password);
        if (passwordError != null) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), passwordError);
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        auth.getCurrentUser().updateProfile(
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build()
                        ).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                auth.signOut();
                                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Rejestracja zakończona. Zaloguj się.");
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd aktualizacji profilu: " + updateTask.getException().getMessage());
                            }
                        });
                    } else {
                        SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Błąd rejestracji: " + task.getException().getMessage());
                    }
                });
    }
    private String checkPasswordStrength(String password) {
        if (password.length() < 6) {
            return "Hasło musi mieć co najmniej 6 znaków";
        }

        if (!password.matches(".*[A-Z].*")) {
            return "Hasło musi zawierać co najmniej jedną wielką literę";
        }

        if (!password.matches(".*[a-z].*")) {
            return "Hasło musi zawierać co najmniej jedną małą literę";
        }

        if (!password.matches(".*\\d.*")) {
            return "Hasło musi zawierać co najmniej jedną cyfrę";
        }

        if (!password.matches(".*[!@#$%^&*()_+=<>?{}\\[\\]~-].*")) {
            return "Hasło musi zawierać co najmniej jeden znak specjalny";
        }

        return null; // null oznacza, że hasło jest poprawne
    }
}
