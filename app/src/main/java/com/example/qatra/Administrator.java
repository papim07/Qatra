package com.example.qatra;

/** <<entity>> Administrator (a User). Manages users and eligibility rules (UC-9). */
public class Administrator extends User {

    public Administrator(String userId, String name, String contact, String password) {
        super(userId, name, contact, password);
    }

    @Override
    public String getRole() { return "admin"; }

    /**
     * FR-20, NFR-15: checks a changed rule. Returns an error message, or null if it is valid.
     * QatraDatabase.applyRule() then applies it straight away and logs who and when.
     */
    public String manageRules(EligibilityRule rule) {
        if (rule.getRecoveryIntervalDays() < 1 || rule.getRecoveryIntervalDays() > 365)
            return "Recovery days must be 1 to 365";
        return null;
    }
}
