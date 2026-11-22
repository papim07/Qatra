package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

/** <<entity>> EligibilityRule: the recovery interval for a donation type (DR-1). */
public class EligibilityRule implements Serializable {
    private String donationType;
    private int recoveryIntervalDays;

    public EligibilityRule(String donationType, int recoveryIntervalDays) {
        this.donationType = donationType;
        this.recoveryIntervalDays = recoveryIntervalDays;
    }

    /** Finds the current rule for a donation type, or null if it cannot be loaded. */
    public static EligibilityRule loadCurrentRules(ArrayList<EligibilityRule> rules, String donationType) {
        for (EligibilityRule r : rules) {
            if (r.donationType.equals(donationType)) return r;
        }
        return null;
    }

    public String getDonationType() { return donationType; }
    public int getRecoveryIntervalDays() { return recoveryIntervalDays; }
    public void setRecoveryIntervalDays(int d) { recoveryIntervalDays = d; }
}
