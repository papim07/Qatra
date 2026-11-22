package com.example.qatra;

import android.widget.CheckBox;
import android.widget.LinearLayout;

public class EligibilityScreen {
    private final MainActivity app;
    private String donorId;

    public EligibilityScreen(MainActivity app) {
        this.app = app;
    }

    public void openScreen() {
        Donor donor = (Donor) app.me;
        donorId = donor.getUserId();
        app.startScreen(this::openScreen, true);
        app.header("Hello, " + donor.getFirstName() + " 👋", "Thank you for saving lives", false);

        String status = app.eligibility.checkEligibility(donor.getUserId());
        if (status.equals(EligibilityModule.ERROR)) {
            displayError("Eligibility rules could not be loaded. Please try later.");
        } else {
            displayStatus(status, donor.getNextEligibleDate());
        }
        if (donor.listDonations().isEmpty() && !donor.isHealthChecked()) askHealthQuestions();

        showRequests(donor);
        showAccount();
    }

    public void displayStatus(String status, long nextDate) {
        Ui ui = app.ui;
        Donor d = (Donor) app.me;
        LinearLayout card = ui.card();
        String last = d.lastDonationDate() == -1 ? "Never" : QatraDatabase.formatDay(d.lastDonationDate());
        card.addView(ui.row(
                ui.column(ui.text("My eligibility", 17, true, Ui.BLACK),
                        ui.text("📍 " + d.getLocation(), 13, false, Ui.GRAY),
                        ui.text("Last donation: " + last, 13, false, Ui.GRAY)),
                ui.bloodDrop(d.getBloodType())));

        if (status.equals(EligibilityModule.ELIGIBLE)) {
            card.addView(ui.pill("✅  " + Lang.t("You can donate now"), Ui.GREEN));
        } else if (status.equals(EligibilityModule.UNDER_AGE)) {
            card.addView(ui.pill("You are under the minimum age to donate", Ui.ORANGE));
        } else {
            long wait = nextDate - QatraDatabase.today();
            card.addView(ui.pill("⏳  Next eligible date: " + QatraDatabase.formatDay(nextDate)
                    + "  (" + wait + " days)", Ui.ORANGE));
            Deferral def = d.latestDeferral();
            if (def != null && def.getEndDate() > QatraDatabase.today()) {
                card.addView(ui.text("Reason: " + def.getReason(), 13, false, Ui.GRAY));
            }
        }
        card.addView(ui.text("Final check is done by medical staff when you arrive.", 12, false, Ui.GRAY));
    }

    public void askHealthQuestions() {
        Ui ui = app.ui;
        LinearLayout q = ui.card();
        q.addView(ui.text("Health questions", 17, true, Ui.BLACK));
        q.addView(ui.text("You have no donation history yet. Please answer honestly:", 13, false, Ui.GRAY));
        CheckBox q1 = ui.checkBox(q, "I feel healthy today");
        CheckBox q2 = ui.checkBox(q, "I weigh 50 kg or more");
        CheckBox q3 = ui.checkBox(q, "No tattoo, piercing or malaria-area travel in the last 6 months");
        q.addView(ui.button("Submit", Ui.RED, v -> {
            boolean[] answers = {q1.isChecked(), q2.isChecked(), q3.isChecked()};
            boolean ok = app.eligibility.recordHealthAnswers(app.me.getUserId(), answers);
            ui.toast(ok ? "Thank you!" : "We will ask you to wait 30 days");
            openScreen();
        }));
    }

    public void displayError(String message) {
        LinearLayout card = app.ui.card();
        card.addView(app.ui.pill("⚠  " + message, Ui.RED));
    }

    private void showRequests(Donor d) {
        Ui ui = app.ui;
        long today = QatraDatabase.today();
        ui.sectionTitle("Donation requests");
        boolean any = false;
        for (Alert a : app.db.alertsFor(d)) {
            if (!a.isOpen()) continue;
            any = true;
            LinearLayout req = ui.card();
            req.addView(ui.row(ui.text("🩸 " + a.getBloodType() + " blood needed", 17, true, Ui.RED),
                    ui.riskBadge(a.getAppeal().getUrgency())));
            req.addView(ui.text(a.getLocation() + " needs your help  •  via " + a.getChannel(), 14, false, Ui.BLACK));

            if (a.getStatus().equals(Alert.SENT)) {
                req.addView(ui.button("Book appointment", Ui.RED, v -> app.showBooking(a)));
            } else {
                req.addView(ui.pill("📅  " + QatraDatabase.formatDay(a.getAppointmentDate()) + "  "
                        + a.getAppointmentTime() + "\n📍  " + a.getLocation(), Ui.BLUE));
                long daysLeft = a.getAppointmentDate() - today;
                if (daysLeft >= 0 && daysLeft <= 1) {
                    req.addView(ui.pill("🔔  Reminder: your appointment is "
                            + (daysLeft == 0 ? "today" : "tomorrow") + " at " + a.getAppointmentTime(), Ui.ORANGE));
                }
            }
        }
        if (!any) ui.emptyText("No requests right now. We only contact you when your blood type is needed.");
    }

    private void showAccount() {
        Ui ui = app.ui;
        ui.sectionTitle("My account");
        LinearLayout acc = ui.card();
        acc.addView(ui.softButton("My donations", Ui.RED, v -> app.showHistory()));
        acc.addView(ui.softButton("Edit profile", Ui.GRAY, v -> app.showProfile()));
        ui.spacer();
    }

    public String getDonorId() { return donorId; }
}
