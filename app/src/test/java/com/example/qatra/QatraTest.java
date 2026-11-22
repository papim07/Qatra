package com.example.qatra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Unit tests for FR-4, FR-9, FR-11, FR-14 and the UC-7 extensions 3a, 4a, 5a.
 * Run in Android Studio: right-click this file -> Run 'QatraTest'.
 */
public class QatraTest {
    private QatraDatabase db;
    private EligibilityModule eligibility;
    private NotificationService notification;
    private MatchingModule matching;
    private ForecastingModule forecasting;
    private long today;

    @Before
    public void setUp() {
        db = QatraDatabase.createForTesting();
        eligibility = new EligibilityModule(db);
        notification = new NotificationService(db);
        matching = new MatchingModule(db, eligibility, new MapsService(), notification);
        forecasting = new ForecastingModule(db);
        today = QatraDatabase.today();
    }

    private Appeal newAppeal(String bloodType, int unitsNeeded) {
        Appeal a = new Appeal(db.nextAppealId(), bloodType, "High", unitsNeeded, today);
        db.getAppeals().add(a);
        return a;
    }

    // ================= FR-4 Calculate Eligibility =================

    @Test
    public void fr4_donorWithOldDonationIsEligible() {
        assertEquals(EligibilityModule.ELIGIBLE, eligibility.checkEligibility("ahmed"));
    }

    @Test
    public void fr4_recordingDonationSetsNextDateFromInterval() {
        Donor d = db.findDonor("ahmed");
        new Donation(today, "Test centre", "Whole blood", d.getBloodType()).record(d);
        assertEquals(EligibilityModule.NOT_YET, eligibility.checkEligibility("ahmed"));
        assertEquals(today + 56, d.getNextEligibleDate());
        assertEquals(EligibilityModule.NOT_YET, d.getEligibilityStatus());
    }

    @Test
    public void fr4_deferralLongerThanIntervalWins() {
        Donor d = db.findDonor("ahmed");
        new Donation(today, "Test centre", "Whole blood", d.getBloodType()).record(d);
        d.addDeferral(new Deferral("Travel", today + 80));
        eligibility.checkEligibility("ahmed");
        assertEquals(today + 80, d.getNextEligibleDate());
    }

    @Test
    public void fr4_ruleChangeAppliesImmediately() {
        Donor d = db.findDonor("ahmed");
        new Donation(today, "Test centre", "Whole blood", d.getBloodType()).record(d);
        db.applyRule(new EligibilityRule("Whole blood", 90), "admin");
        eligibility.checkEligibility("ahmed");
        assertEquals(today + 90, d.getNextEligibleDate());
    }

    @Test
    public void fr4_underMinimumAgeIsNotEligible() {
        Donor teen = new Donor("teen", "Teen Donor", "0501234567", "1234", today - 365L * 17,
                "O+", "Abu Dhabi - Al Reem", "SMS");
        db.getUsers().add(teen);
        assertEquals(EligibilityModule.UNDER_AGE, eligibility.checkEligibility("teen"));
    }

    @Test
    public void uc2_3a_missingRulesGivesErrorAndLog() {
        db.getRules().clear();
        assertEquals(EligibilityModule.ERROR, eligibility.checkEligibility("ahmed"));
        assertTrue(lastLog().contains("could not be loaded"));
    }

    // ================= FR-9 Classify Risk / FR-11 Thresholds =================

    @Test
    public void fr9_riskLevelsFromFirstDayBelowSafeLevel() {
        assertEquals("High", forecasting.classifyRisk(forecastShortIn(0), 10));
        assertEquals("High", forecasting.classifyRisk(forecastShortIn(3), 10));
        assertEquals("Medium", forecasting.classifyRisk(forecastShortIn(5), 10));
        assertEquals("Medium", forecasting.classifyRisk(forecastShortIn(7), 10));
        assertEquals("Low", forecasting.classifyRisk(forecastShortIn(10), 10));
        assertEquals("Low", forecasting.classifyRisk(forecastShortIn(-1), 10));
    }

    private Forecast forecastShortIn(int days) {
        return new Forecast("O+", today, false, "", days);
    }

    @Test
    public void fr9_fr10_sampleDataGivesHighRiskForOnegWithUnitsNeeded() {
        forecasting.getForecasts();
        Forecast f = forecasting.getForecastDetails("O-");
        assertEquals("High", f.getRiskLevel());
        assertTrue(f.getUnitsNeeded() > 0);
        assertFalse(f.explainFactors().isEmpty());
    }

    @Test
    public void fr11_shortHistoryIsThresholdBasedAndLabelled() {
        forecasting.getForecasts();
        Forecast f = forecasting.getForecastDetails("AB-");    // only 7 days of history
        assertTrue(f.isThresholdBased());
        assertTrue(f.explainFactors().contains("days of history"));
    }

    @Test
    public void fr12_highRiskIsListedFirst() {
        ArrayList<Forecast> list = forecasting.getForecasts();
        assertEquals(8, list.size());
        assertEquals("High", list.get(0).getRiskLevel());
    }

    @Test
    public void fr23_modelBeatsBaselineByAtLeast15Percent() {
        forecasting.updateModel();
        assertTrue(forecasting.overallImprovement() >= 15);      // NFR-7
    }

    // ================= FR-14 Matching: compatibility and distance =================

    @Test
    public void fr14_redCellCompatibility() {
        String[] all = QatraDatabase.BLOOD_TYPES;
        for (String patient : all) {
            assertTrue("O- gives to " + patient, matching.isCompatible("O-", patient));
            assertTrue("AB+ receives from " + patient, matching.isCompatible(patient, "AB+"));
        }
        assertFalse(matching.isCompatible("A+", "O-"));
        assertFalse(matching.isCompatible("AB+", "A+"));
        assertFalse(matching.isCompatible("B+", "A+"));
        assertTrue(matching.isCompatible("B-", "AB-"));
        assertTrue(matching.isCompatible("O+", "B+"));
    }

    @Test
    public void fr14_mapsServiceDistance() {
        MapsService maps = new MapsService();
        assertEquals(40, maps.getDistance("Abu Dhabi - Al Shamkha", "Abu Dhabi - Al Reem"));
        assertEquals(0, maps.getDistance("Abu Dhabi - Al Reem", "Abu Dhabi - Al Reem"));
    }

    @Test
    public void fr14_onlyEligibleCompatibleAndNearbyDonors() {
        Appeal a = newAppeal("O-", 10);
        ArrayList<Donor> matched = matching.matchDonors(a.getAppealId());
        MapsService maps = new MapsService();
        for (Donor d : matched) {
            assertEquals("O-", d.getBloodType());
            assertTrue(eligibility.isEligible(d.getUserId()));
            assertTrue(maps.getDistance(d.getLocation(), db.getBank().getLocation()) <= 25);
        }
        assertTrue(matched.contains(db.findDonor("ahmed")));       // O-, 5 km, eligible
        assertFalse(matched.contains(db.findDonor("mariam")));     // O-, 40 km away
        assertEquals(matched.size(), a.getDonorCount());           // assignRecipients
    }

    // ================= UC-7 Send Appeal extensions =================

    @Test
    public void uc7_3a_widenSearchFindsMoreDonors() {
        Appeal a = newAppeal("O-", 10);
        int before = matching.matchDonors(a.getAppealId()).size();
        assertTrue(before < a.getUnitsNeeded());                    // too few donors
        double next = matching.nextDistance();
        assertEquals(50.0, next, 0.001);
        ArrayList<Donor> after = matching.widenSearch(a.getAppealId(), next);
        assertTrue(after.size() > before);
        assertTrue(after.contains(db.findDonor("mariam")));
    }

    @Test
    public void uc7_4a_rejectLogsReasonAndSendsNothing() {
        Appeal a = newAppeal("O-", 5);
        matching.rejectAppeal(a.getAppealId(), "Stock arriving from Dubai", "staff");
        assertEquals(Appeal.REJECTED, a.getStatus());
        assertEquals(0, db.alertsFor(a).size());
        assertTrue(lastLog().contains("Stock arriving from Dubai"));
    }

    @Test
    public void uc7_5a_notificationDownKeepsAppealPending() {
        Appeal a = newAppeal("O-", 5);
        notification.setAvailable(false);
        assertEquals(-1, matching.sendAppeal(a.getAppealId(), "staff"));
        assertEquals(Appeal.PENDING, a.getStatus());
        assertEquals(0, db.alertsFor(a).size());
        assertTrue(lastLog().contains("unavailable"));
    }

    @Test
    public void uc7_mainFlow_approveSendsAlertsWithBookingLink() {
        Appeal a = newAppeal("O-", 5);
        int sent = matching.sendAppeal(a.getAppealId(), "staff");
        assertTrue(sent > 0);
        assertEquals(Appeal.APPROVED, a.getStatus());
        assertEquals(sent, db.alertsFor(a).size());
        Alert first = db.alertsFor(a).get(0);
        assertNotNull(first.getBookingLink());
        assertEquals(first.getDonor().getContactPreference(), first.getChannel());   // FR-16
    }

    @Test
    public void fr15_highRiskForecastCreatesPendingAppeal() {
        forecasting.setMatching(matching);
        forecasting.getForecasts();
        boolean found = false;
        for (Appeal a : db.getAppeals()) {
            if (a.getBloodType().equals("O-") && a.getStatus().equals(Appeal.PENDING)) found = true;
        }
        assertTrue(found);
    }

    private String lastLog() {
        ArrayList<String> log = db.getAuditLog();
        return log.get(log.size() - 1);
    }
}
