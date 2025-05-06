package com.example.clockedin.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.clockedin.model.User;
import com.example.clockedin.utils.PasswordUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class AuthenticationRepository {
    private static final String TAG = "AuthRepo";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_CONTACT = "user_contact";

    private Application application;
    private MutableLiveData<User> currentUserMutableLiveData;
    private MutableLiveData<Boolean> userLoggedMutableLiveData;
    private DatabaseReference dbRef;
    private SharedPreferences sharedPreferences;

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
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Check if we have a saved user and restore it
        checkForSavedUser();
    }
    
    private void checkForSavedUser() {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        if (userId != null) {
            String email = sharedPreferences.getString(KEY_USER_EMAIL, "");
            String username = sharedPreferences.getString(KEY_USER_NAME, "");
            String contact = sharedPreferences.getString(KEY_USER_CONTACT, "");
            
            // Create user without password (we don't save passwords)
            User savedUser = new User(userId, email, username, contact, null);
            currentUserMutableLiveData.postValue(savedUser);
            Log.d(TAG, "Restored saved user: " + username);
        }
    }
    
    private void saveUserToPrefs(User user) {
        if (user != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_USER_ID, user.uid);
            editor.putString(KEY_USER_EMAIL, user.email);
            editor.putString(KEY_USER_NAME, user.username);
            editor.putString(KEY_USER_CONTACT, user.contactNumber);
            editor.apply();
            Log.d(TAG, "Saved user to preferences: " + user.username);
        }
    }
    
    private void clearUserFromPrefs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Cleared user from preferences");
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
                    // Encrypt the password before storing
                    String encryptedPassword = PasswordUtils.encryptPassword(pass);
                    
                    // Create new user with encrypted password
                    String uid = dbRef.push().getKey();
                    User newUser = new User(uid, email, username, contactNumber, encryptedPassword);

                    dbRef.child(uid).setValue(newUser)
                            .addOnSuccessListener(aVoid -> {
                                // Create a user object without the encrypted password for local use
                                User userForLocal = new User(uid, email, username, contactNumber, null);
                                currentUserMutableLiveData.postValue(userForLocal);
                                saveUserToPrefs(userForLocal);
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
                        if (user != null && user.password != null) {
                            // Verify password against the encrypted one from database
                            if (PasswordUtils.verifyPassword(pass, user.password)) {
                                Log.d(TAG, "Login successful for: " + user.username);
                                
                                // Create a user object without the encrypted password for local use
                                User userForLocal = new User(user.uid, user.email, user.username, user.contactNumber, null);
                                currentUserMutableLiveData.postValue(userForLocal);
                                saveUserToPrefs(userForLocal);
                                Toast.makeText(application, "Login successful", Toast.LENGTH_SHORT).show();
                                userFound = true;
                                break;
                            }
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
        clearUserFromPrefs();
        currentUserMutableLiveData.postValue(null);
        userLoggedMutableLiveData.postValue(true);
    }
}
