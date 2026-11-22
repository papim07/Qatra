package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

public class EligibilityRule implements Serializable {
    private String donationType;
    private int recoveryIntervalDays;

    public EligibilityRule(String donationType, int recoveryIntervalDays) {
        this.donationType = donationType;
        this.recoveryIntervalDays = recoveryIntervalDays;
    }

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
