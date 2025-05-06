package com.example.clockedin.repository;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.clockedin.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class AuthenticationRepository {
    private Application application;
    private MutableLiveData<User> currentUserMutableLiveData;
    private MutableLiveData<Boolean> userLoggedMutableLiveData;
    private DatabaseReference dbRef;

    public MutableLiveData<User> getCurrentUserMutableLiveData() {
        return currentUserMutableLiveData;
    }

    public MutableLiveData<Boolean> getUserLoggedMutableLiveData() {
        return userLoggedMutableLiveData;
    }

    public AuthenticationRepository(Application application) {
        this.application = application;
        currentUserMutableLiveData = new MutableLiveData<>();
        userLoggedMutableLiveData = new MutableLiveData<>();
        dbRef = FirebaseDatabase.getInstance().getReference("users");
    }

    public void register(String email, String pass, String username, String contactNumber) {
        // First check if email already exists
        Query emailQuery = dbRef.orderByChild("email").equalTo(email);
        emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(application, "Email already registered", Toast.LENGTH_SHORT).show();
                } else {
                    // Create new user
                    String uid = dbRef.push().getKey();
                    User newUser = new User(uid, email, username, contactNumber, pass);

                    dbRef.child(uid).setValue(newUser)
                            .addOnSuccessListener(aVoid -> {
                                currentUserMutableLiveData.postValue(newUser);
                                Toast.makeText(application, "Registration successful", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(application, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(application, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void login(String email, String pass) {
        Query emailQuery = dbRef.orderByChild("email").equalTo(email);
        emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean userFound = false;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null && user.password != null && user.password.equals(pass)) {
                            currentUserMutableLiveData.postValue(user);
                            Toast.makeText(application, "Login successful", Toast.LENGTH_SHORT).show();
                            userFound = true;
                            break;
                        }
                    }
                    if (!userFound) {
                        Toast.makeText(application, "Invalid password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(application, "Email not registered", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(application, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void signOut() {
        currentUserMutableLiveData.postValue(null);
        userLoggedMutableLiveData.postValue(true);
    }
}
