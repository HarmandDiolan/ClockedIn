package com.example.clockedin.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.clockedin.model.User;
import com.example.clockedin.repository.AuthenticationRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthenticationRepository repository;
    private final MutableLiveData<User> userData;
    private final MutableLiveData<Boolean> loggedStatus;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthenticationRepository(application);
        userData = repository.getCurrentUserMutableLiveData();
        loggedStatus = repository.getUserLoggedMutableLiveData();
    }

    public MutableLiveData<User> getUserData() {
        return userData;
    }

    public MutableLiveData<Boolean> getLoggedStatus() {
        return loggedStatus;
    }

    public void register(String email, String pass, String username, String contact){
        repository.register(email, pass, username, contact);
    }

    public void signIn(String email, String pass){
        repository.login(email, pass);
    }

    public void signOut(){
        repository.signOut();
    }
}
