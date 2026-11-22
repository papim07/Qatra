package com.example.qatra;

/** <<entity>> BloodBankStaff (a User). Reviews 0..* Appeal. */
public class BloodBankStaff extends User {
    private String staffRole;   // e.g. "Blood bank officer", "Nurse"

    public BloodBankStaff(String userId, String name, String contact, String password, String staffRole) {
        super(userId, name, contact, password);
        this.staffRole = staffRole;
    }

    @Override
    public String getRole() { return "staff"; }

    /** Staff can only decide on appeals that are still pending (FR-15). */
    public boolean reviewAppeal(Appeal appeal) {
        return appeal.getStatus().equals(Appeal.PENDING);
    }

    public String getStaffRole() { return staffRole; }
}
