package com.example.clockedin.model;

public class User {
    public String uid;
    public String email;
    public String username;
    public String contactNumber;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String uid, String email, String username, String contactNumber) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.contactNumber = contactNumber;
    }
}
