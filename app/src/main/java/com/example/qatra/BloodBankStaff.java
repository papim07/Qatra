package com.example.qatra;

public class BloodBankStaff extends User {
    private String staffRole;

    public BloodBankStaff(String userId, String name, String contact, String password, String staffRole) {
        super(userId, name, contact, password);
        this.staffRole = staffRole;
    }

    @Override
    public String getRole() { return "staff"; }

    public boolean reviewAppeal(Appeal appeal) {
        return appeal.getStatus().equals(Appeal.PENDING);
    }

    public String getStaffRole() { return staffRole; }
}
