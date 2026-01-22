package com.example.projektzaliczeniowy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginBtn, goToRegisterBtn;
    private FirebaseAuth mAuth;

    private Button forgotPasswordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.login_email);
        passwordField = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_button);
        forgotPasswordBtn = findViewById(R.id.forgot_password_button);
        goToRegisterBtn = findViewById(R.id.go_to_register);

        loginBtn.setOnClickListener(v -> loginUser());

        forgotPasswordBtn.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class)));
        goToRegisterBtn.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Uzupełnij wszystkie pola");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        SnackbarUtils.showTopSnackbar(findViewById(android.R.id.content), "Uzupełnij wszystkie pola"));
    }

}
