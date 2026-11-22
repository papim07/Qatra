package com.example.qatra;

import java.util.ArrayList;

public class EligibilityModule {
    public static final String ELIGIBLE = "Eligible";
    public static final String NOT_YET = "Not yet eligible";
    public static final String UNDER_AGE = "Under minimum age";
    public static final String ERROR = "Rules unavailable";
    public static final int MIN_AGE = 18;

    private final QatraDatabase db;

    public EligibilityModule(QatraDatabase db) {
        this.db = db;
    }

    @SuppressWarnings("unchecked")
    public String checkEligibility(String donorId) {
        Donor donor = db.findDonor(donorId);
        if (donor == null) return ERROR;

        Object[] lists = donor.listDonationsAndDeferrals();
        ArrayList<Donation> donations = (ArrayList<Donation>) lists[0];
        ArrayList<Deferral> deferrals = (ArrayList<Deferral>) lists[1];

        EligibilityRule rules = EligibilityRule.loadCurrentRules(db.getRules(), "Whole blood");
        if (rules == null) {
            db.log("system", "Eligibility rules could not be loaded");
            return ERROR;
        }
        long nextDate = calculateNextDate(donations, deferrals, rules);

        String status;
        long today = QatraDatabase.today();
        if (donor.getAge(today) < MIN_AGE) status = UNDER_AGE;
        else if (nextDate <= today) status = ELIGIBLE;
        else status = NOT_YET;

        donor.updateEligibility(status, nextDate);
        return status;
    }

    public long calculateNextDate(ArrayList<Donation> donations, ArrayList<Deferral> deferrals,
                                  EligibilityRule rules) {
        long next = 0;
        for (Donation d : donations) {
            next = Math.max(next, d.getDonationDate() + rules.getRecoveryIntervalDays());
        }
        for (Deferral d : deferrals) {
            next = Math.max(next, d.getEndDate());
        }
        return next;
    }

    public boolean recordHealthAnswers(String donorId, boolean[] answers) {
        Donor donor = db.findDonor(donorId);
        boolean ok = donor.answerHealthQuestions(answers);
        if (!ok) donor.addDeferral(new Deferral("Health questionnaire: please see staff", QatraDatabase.today() + 30));
        db.log(donorId, "Answered health questions (" + (ok ? "OK" : "deferred 30 days") + ")");
        checkEligibility(donorId);
        return ok;
    }

    public boolean isEligible(String donorId) {
        return checkEligibility(donorId).equals(ELIGIBLE);
    }
}
