package com.example.qatra;

public class Administrator extends User {
    public Administrator(String userId, String name, String contact, String password) {
        super(userId, name, contact, password);
    }

    @Override
    public String getRole() { return "admin"; }

    public String manageRules(EligibilityRule rule) {
        if (rule.getRecoveryIntervalDays() < 1 || rule.getRecoveryIntervalDays() > 365)
            return "Recovery days must be 1 to 365";
        return null;
    }
}
