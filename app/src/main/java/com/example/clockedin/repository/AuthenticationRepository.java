package com.example.clockedin.repository;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.clockedin.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthenticationRepository {
    private Application application;
    private MutableLiveData<FirebaseUser> firebaseUserMutableLiveData;
    private MutableLiveData<Boolean> userLoggedMutableLiveData;
    private FirebaseAuth auth;

    public MutableLiveData<FirebaseUser> getFirebaseUserMutableLiveData() {
        return firebaseUserMutableLiveData;
    }

    public MutableLiveData<Boolean> getUserLoggedMutableLiveData() {
        return userLoggedMutableLiveData;
    }

    public AuthenticationRepository(Application application){
        this.application = application;
        firebaseUserMutableLiveData = new MutableLiveData<>();
        userLoggedMutableLiveData = new MutableLiveData<>();
        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() != null){
            firebaseUserMutableLiveData.postValue(auth.getCurrentUser());
        }
    }

    public void register(String email, String pass, String username, String contactNumber){
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        FirebaseUser user = auth.getCurrentUser();
                        firebaseUserMutableLiveData.postValue(user);

                        String uid = user.getUid();
                        User newUser = new User(uid, email, username, contactNumber);

                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");

                        dbRef.child(uid).setValue(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(application, "User saved successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(application, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        Toast.makeText(application, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void login(String email, String pass){
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                firebaseUserMutableLiveData.postValue(auth.getCurrentUser());
            }else{
                Toast.makeText(application, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void signOut(){
        auth.signOut();
        userLoggedMutableLiveData.postValue(true);
    }
}
