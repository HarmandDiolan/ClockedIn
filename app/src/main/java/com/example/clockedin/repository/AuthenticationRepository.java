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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.example.clockedin.utils.EmailSender;

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
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

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
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1075414918615-52uukjel395a7j504ua4spmfk79q31ku.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(application, gso);
        
        // Sign out from Google to ensure account picker is shown
        googleSignInClient.signOut();
        
        // Check if we have a saved user and restore it
        checkForSavedUser();
    }

    public GoogleSignInClient getGoogleSignInClient() {
        return googleSignInClient;
    }

    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            Toast.makeText(application, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        com.google.firebase.auth.FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Check if user exists in database
                            Query emailQuery = dbRef.orderByChild("email").equalTo(firebaseUser.getEmail());
                            emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        // User exists, get their data
                                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                            User user = userSnapshot.getValue(User.class);
                                            if (user != null) {
                                                currentUserMutableLiveData.postValue(user);
                                                saveUserToPrefs(user);
                                                break;
                                            }
                                        }
                                    } else {
                                        // Create new user
                                        String uid = firebaseUser.getUid();
                                        String email = firebaseUser.getEmail();
                                        String displayName = firebaseUser.getDisplayName();
                                        String photoUrl = firebaseUser.getPhotoUrl() != null ? 
                                            firebaseUser.getPhotoUrl().toString() : "";

                                        User newUser = new User(uid, email, displayName, "", null);
                                        dbRef.child(uid).setValue(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                currentUserMutableLiveData.postValue(newUser);
                                                saveUserToPrefs(newUser);
                                                Toast.makeText(application, "Google sign in successful", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(application, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(application, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        // Sign in failed
                        Toast.makeText(application, "Authentication failed: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
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
                    // Create user with Firebase Authentication
                    firebaseAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Get the Firebase user
                                com.google.firebase.auth.FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    String uid = firebaseUser.getUid();
                                    
                                    // Create new user in Realtime Database
                                    User newUser = new User(uid, email, username, contactNumber, null);
                                    dbRef.child(uid).setValue(newUser)
                                        .addOnSuccessListener(aVoid -> {
                                            // Create a user object without the encrypted password for local use
                                            User userForLocal = new User(uid, email, username, contactNumber, null);
                                            currentUserMutableLiveData.postValue(userForLocal);
                                            saveUserToPrefs(userForLocal);
                                            Toast.makeText(application, "Registration successful", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(application, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                }
                            } else {
                                // Registration failed
                                Toast.makeText(application, "Registration failed: " + task.getException().getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            }
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
        // Use Firebase Authentication for login
        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Get the Firebase user
                    com.google.firebase.auth.FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        
                        // Get user data from Realtime Database
                        dbRef.child(uid).get()
                            .addOnSuccessListener(dataSnapshot -> {
                                User user = dataSnapshot.getValue(User.class);
                                if (user != null) {
                                    currentUserMutableLiveData.postValue(user);
                                    saveUserToPrefs(user);
                                    Toast.makeText(application, "Login successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(application, "User data not found", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(application, "Failed to get user data: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            });
                    }
                } else {
                    // Login failed
                    Toast.makeText(application, "Login failed: " + task.getException().getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void signOut() {
        clearUserFromPrefs();
        currentUserMutableLiveData.postValue(null);
        userLoggedMutableLiveData.postValue(true);
    }

    public void resetPassword(String email) {
        // Use the correct database reference path
        dbRef.orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String temporaryPassword = generateTemporaryPassword();
                            String encryptedPassword = PasswordUtils.encryptPassword(temporaryPassword);
                            
                            // Update password in database
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                userSnapshot.getRef().child("password").setValue(encryptedPassword);
                            }

                            // Send email with temporary password
                            String subject = "Your Temporary Password - ClockedIn";
                            String message = "Your temporary password is: " + temporaryPassword + "\n\n" +
                                    "Please login with this password and change it immediately for security reasons.";
                            
                            EmailSender.sendEmail(email, subject, message, new EmailSender.EmailCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(application, "Temporary password sent to your email", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(application, "Failed to send email: " + error, Toast.LENGTH_LONG).show();
                                }
                            });
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

    private String generateTemporaryPassword() {
        // Generate a random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
}
