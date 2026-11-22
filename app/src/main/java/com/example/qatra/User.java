package com.example.qatra;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String name;
    private String contact;
    private String salt;
    private String passwordHash;

    public User(String userId, String name, String contact, String password) {
        this.userId = userId;
        this.name = name;
        this.contact = contact;
        this.salt = PasswordHasher.newSalt();
        this.passwordHash = PasswordHasher.hash(salt, password);
    }

    public boolean logIn(String userId, String password) {
        return this.userId.equalsIgnoreCase(userId.trim())
                && PasswordHasher.hash(salt, password).equals(passwordHash);
    }

    public void logOut() {
    }

    public String getRole() { return "user"; }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getFirstName() { return name.split(" ")[0]; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
