package com.example.qatra;

import java.io.Serializable;

/** <<entity>> User: general class of Donor, BloodBankStaff and Administrator. */
public class User implements Serializable {
    private String userId;          // login name
    private String name;
    private String contact;
    private String salt;
    private String passwordHash;    // never the plain password (NFR-9)

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
        // nothing stored per session; MainActivity forgets the current user
    }

    /** "donor", "staff" or "admin" (NFR-10). Each subclass overrides this. */
    public String getRole() { return "user"; }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getFirstName() { return name.split(" ")[0]; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
