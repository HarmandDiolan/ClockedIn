package com.example.clockedin.model;

public class User {
    public String uid;
    public String email;
    public String username;
    public String contactNumber;
    public String password;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String uid, String email, String username, String contactNumber, String password) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.contactNumber = contactNumber;
        this.password = password;
    }
}
