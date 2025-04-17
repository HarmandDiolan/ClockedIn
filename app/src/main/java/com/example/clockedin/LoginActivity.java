package com.example.clockedin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    Button btnLogin;
    TextView signUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        signUpText = findViewById(R.id.textView4); // Sign-up TextView

        btnLogin.setOnClickListener(v -> {
            // Add authentication logic here (e.g., Firebase Auth)
            //startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        });

        // Redirect to SignupActivity when the "Don't have an account? Sign up" TextView is clicked
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, Signup.class);
            startActivity(intent);
        });
    }
}
