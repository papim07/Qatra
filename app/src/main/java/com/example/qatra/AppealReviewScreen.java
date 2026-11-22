package com.example.qatra;

import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * <<boundary>> AppealReviewScreen (UC-7 Send Appeal, UC-8 Widen Search Area).
 * Follows the "Send Appeal" sequence and communication diagrams.
 */
public class AppealReviewScreen {
    private final MainActivity app;
    private int appealId = -1;

    public AppealReviewScreen(MainActivity app) {
        this.app = app;
    }

    /** 1: selectAppeal(appealId) -> 2: matchDonors(appealId) -> 7: displaySummary(...) */
    public void selectAppeal(int appealId) {
        if (appealId != this.appealId) app.matching.resetSearch();   // new appeal starts at 25 km
        this.appealId = appealId;
        Appeal a = app.db.findAppeal(appealId);
        app.startScreen(() -> selectAppeal(appealId), false);
        app.header("Appeal #" + a.getAppealId(), a.getBloodType() + "  •  " + a.getUrgency() + "  •  " + a.getStatus(), true);

        if (a.getStatus().equals(Appeal.PENDING)) {
            ArrayList<Donor> donors = app.matching.matchDonors(appealId);
            displaySummary(a.getUnitsNeeded(), a.getDonorCount());
            showDonors(donors);
            showDecision();
        } else if (a.getStatus().equals(Appeal.REJECTED)) {
            LinearLayout c = app.ui.card();
            c.addView(app.ui.pill("Rejected: " + a.getRejectReason(), Ui.GRAY));
        } else {
            displayConfirmation();
        }
    }

    public void displaySummary(int unitsNeeded, int donorCount) {
        Ui ui = app.ui;
        Appeal a = app.db.findAppeal(appealId);
        LinearLayout card = ui.card();
        card.addView(ui.row(ui.text(a.getBloodType() + " needed", 20, true, Ui.RED), ui.riskBadge(a.getUrgency())));
        card.addView(ui.text("Units needed: " + unitsNeeded, 15, false, Ui.BLACK));
        card.addView(ui.text("Search area: " + (int) app.matching.getSearchDistanceKm() + " km", 15, false, Ui.BLACK));
        card.addView(ui.pill(donorCount + " eligible, compatible donors found",
                donorCount >= unitsNeeded ? Ui.GREEN : Ui.ORANGE));

        double next = app.matching.nextDistance();
        if (donorCount < unitsNeeded && next != -1) {                // UC-8 extends UC-7
            card.addView(ui.text("Too few donors for the units needed.", 13, false, Ui.GRAY));
            card.addView(ui.softButton("Widen search to " + (int) next + " km", Ui.ORANGE, v -> {
                app.matching.widenSearch(appealId, next);
                selectAppeal(appealId);
            }));
        }
    }

    private void showDonors(ArrayList<Donor> donors) {
        Ui ui = app.ui;
        ui.sectionTitle("Donors to be alerted");
        if (donors.isEmpty()) ui.emptyText("No matching donors in this area");
        MapsService maps = new MapsService();
        for (Donor d : donors) {
            LinearLayout c = ui.card();
            int km = maps.getDistance(d.getLocation(), app.db.getBank().getLocation());
            c.addView(ui.row(ui.column(ui.text(d.getName(), 15, true, Ui.BLACK),
                    ui.text(km + " km  •  via " + d.getContactPreference(), 13, false, Ui.GRAY)),
                    ui.bloodDrop(d.getBloodType())));
        }
    }

    private void showDecision() {
        Ui ui = app.ui;
        LinearLayout decide = ui.card();
        decide.addView(ui.text("Staff decide before anything is sent.", 13, false, Ui.GRAY));
        decide.addView(ui.button("Approve & send", Ui.GREEN, v -> approveAppeal()));
        EditText reason = ui.textInput(decide, "Reason (if rejecting)", "");
        decide.addView(ui.softButton("Reject", Ui.GRAY, v -> rejectAppeal(reason.getText().toString())));
        ui.spacer();
    }

    /** 8: approveAppeal() -> 9: sendAppeal(appealId) -> 13: displayConfirmation() */
    public void approveAppeal() {
        Appeal a = app.db.findAppeal(appealId);
        if (!((BloodBankStaff) app.me).reviewAppeal(a)) return;
        int n = app.matching.sendAppeal(appealId, app.who());
        if (n < 0) app.ui.toast("Notification Service unavailable, appeal kept pending");   // 5a
        else app.ui.toast("Alerts sent to " + n + " donor(s)");
        selectAppeal(appealId);
    }

    public void rejectAppeal(String reason) {
        if (reason.trim().isEmpty()) { app.ui.toast("Please enter a reason"); return; }
        app.matching.rejectAppeal(appealId, reason.trim(), app.who());
        selectAppeal(appealId);
    }

    /** Shows that the appeal was sent, with the response so far (FR-19). */
    public void displayConfirmation() {
        Ui ui = app.ui;
        Appeal a = app.db.findAppeal(appealId);
        LinearLayout card = ui.card();
        card.addView(ui.pill("✅  Appeal sent to " + a.getDonorCount() + " donor(s)", Ui.GREEN));

        int[] r = app.db.appealResponse(a);
        ui.statRow(new String[]{"Contacted", "Booked", "Donated", "Still needed"},
                new String[]{"" + r[0], "" + r[1], "" + r[2], "" + r[3]});
        ui.sectionTitle("Alerts");
        for (Alert al : app.db.alertsFor(a)) {
            LinearLayout c = ui.card();
            c.addView(ui.row(ui.text(al.getDonor().getName() + "  (" + al.getDonor().getBloodType() + ")", 15, false, Ui.BLACK),
                    ui.badge(al.getStatus(), al.getStatus().equals(Alert.SENT) ? Ui.GRAY : Ui.GREEN)));
        }
        ui.spacer();
    }
}
