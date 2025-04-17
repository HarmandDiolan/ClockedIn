package com.example.clockedin;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Signup extends AppCompatActivity {

    EditText email, password, confirmPassword;
    Button btnSignup;
    TextView textViewLogin;  // To navigate to the login activity

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // Ensure the XML layout matches

        mAuth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        textViewLogin = findViewById(R.id.textViewLogin); // This is the "Login" link

        // Handle signup logic when the button is clicked
        btnSignup.setOnClickListener(v -> {
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();
            String userConfirmPassword = confirmPassword.getText().toString().trim();

            // Validate user input
            if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword) || TextUtils.isEmpty(userConfirmPassword)) {
                Toast.makeText(Signup.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!userPassword.equals(userConfirmPassword)) {
                Toast.makeText(Signup.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPassword.length() < 6) {
                Toast.makeText(Signup.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create the user using Firebase Authentication
            mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Signup.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Signup.this, LoginActivity.class));
                            finish();
                        } else {
                            Exception e = task.getException();
                            if (e != null) {
                                Toast.makeText(Signup.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(Signup.this, "Signup failed: Unknown error", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Handle redirect to Login activity when user clicks "Already have an account? Login"
        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, LoginActivity.class)); // Navigate to login
            finish(); // Close the signup activity
        });
    }
}
