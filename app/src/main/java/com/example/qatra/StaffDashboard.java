package com.example.qatra;

import android.widget.LinearLayout;

import java.util.ArrayList;

public class StaffDashboard {
    private final MainActivity app;
    private String selectedBloodType;

    public StaffDashboard(MainActivity app) {
        this.app = app;
    }

    public void openDashboard() {
        app.startScreen(this::openDashboard, true);
        Ui ui = app.ui;
        app.header("Staff Dashboard", app.db.getBank().getName(), false);

        int pending = 0;
        for (Appeal a : app.db.getAppeals()) if (a.getStatus().equals(Appeal.PENDING)) pending++;
        ui.statRow(new String[]{"Pending appeals", "Booked", "Response"},
                new String[]{"" + pending, "" + app.db.bookedAlerts().size(), app.db.responseRate() + "%"});

        LinearLayout actions = ui.card();
        actions.addView(ui.softButton("Update stock", Ui.BLUE, v -> app.showStockForm()));
        actions.addView(ui.softButton("AI forecast model", Ui.GREEN, v -> app.showModel()));

        ArrayList<Forecast> forecasts = app.forecasting.getForecasts();
        actions.addView(ui.text(String.format("🤖 AI model: %.0f%% lower error than the baseline",
                app.forecasting.overallImprovement()), 13, false, Ui.GRAY));
        displayForecasts(forecasts);
        showAppeals();
        showVisits();
        ui.spacer();
    }

    public void displayForecasts(ArrayList<Forecast> forecasts) {
        Ui ui = app.ui;
        BloodBank bank = app.db.getBank();
        ui.sectionTitle("Stock & 14-day forecast");
        for (Forecast f : forecasts) {
            int units = bank.currentLevel(f.getBloodType()).getUnitsInStock();
            int safe = bank.safeLevelFor(f.getBloodType());
            LinearLayout card = ui.card();
            card.addView(ui.row(ui.text(f.getBloodType(), 24, true, Ui.RED), ui.riskBadge(f.getRiskLevel())));
            card.addView(ui.text(units + " units   (safe level " + safe + ")", 15, false, Ui.BLACK));
            card.addView(ui.stockBar(units, safe));
            card.addView(ui.text(whenText(f), 14, true, f.getRiskLevel().equals("Low") ? Ui.GREEN : Ui.RED));
            if (f.isThresholdBased()) card.addView(ui.badge("threshold-based", Ui.GRAY));
            if (f.getUnitsNeeded() > 0) card.addView(ui.text("Units needed: " + f.getUnitsNeeded(), 14, false, Ui.BLACK));
            if (f.getForecastDate() < QatraDatabase.today()) {
                card.addView(ui.pill("Last forecast from " + QatraDatabase.formatDay(f.getForecastDate()), Ui.ORANGE));
            }
            card.addView(ui.text("Tap for details ›", 12, false, Ui.GRAY));
            ui.onTap(card, v -> selectBloodType(f.getBloodType()));
        }
    }

    public void selectBloodType(String bloodType) {
        selectedBloodType = bloodType;
        app.startScreen(() -> selectBloodType(bloodType), false);
        app.header(bloodType + " forecast", "Next 14 days", true);
        Forecast f = app.forecasting.getForecastDetails(bloodType);
        displayDetails(f.getUnitsNeeded(), f.explainFactors());
    }

    public void displayDetails(int unitsNeeded, String factors) {
        Ui ui = app.ui;
        BloodBank bank = app.db.getBank();
        Forecast f = app.forecasting.getForecastDetails(selectedBloodType);
        int units = bank.currentLevel(selectedBloodType).getUnitsInStock();
        int safe = bank.safeLevelFor(selectedBloodType);

        LinearLayout card = ui.card();
        card.addView(ui.row(ui.text(units + " units in stock", 18, true, Ui.BLACK), ui.riskBadge(f.getRiskLevel())));
        card.addView(ui.stockBar(units, safe));
        card.addView(ui.text("Safe level: " + safe + "   •   Forecast date: " + QatraDatabase.formatDay(f.getForecastDate()), 13, false, Ui.GRAY));
        card.addView(ui.text(whenText(f), 15, true, f.getRiskLevel().equals("Low") ? Ui.GREEN : Ui.RED));
        card.addView(ui.text("Units needed to stay safe: " + unitsNeeded, 15, true, Ui.BLACK));
        if (f.isThresholdBased()) card.addView(ui.pill("Threshold-based: not enough history to forecast", Ui.GRAY));

        ui.sectionTitle("What affected this forecast");
        LinearLayout fc = ui.card();
        for (String factor : factors.split(Forecast.FACTOR_SEPARATOR)) fc.addView(ui.text("•  " + factor, 14, false, Ui.BLACK));

        ui.spacer();
    }

    private void showAppeals() {
        Ui ui = app.ui;
        ui.sectionTitle("Appeals");
        ArrayList<Appeal> appeals = app.db.getAppeals();
        if (appeals.isEmpty()) ui.emptyText("No appeals yet");
        for (int i = appeals.size() - 1; i >= 0; i--) {
            Appeal a = appeals.get(i);
            LinearLayout c = ui.card();
            int color = a.getStatus().equals(Appeal.APPROVED) ? Ui.GREEN
                    : a.getStatus().equals(Appeal.REJECTED) ? Ui.GRAY : Ui.ORANGE;
            c.addView(ui.row(ui.text("#" + a.getAppealId() + "  " + a.getBloodType() + "  •  " + a.getUrgency(), 16, true, Ui.BLACK),
                    ui.badge(a.getStatus(), color)));
            ui.onTap(c, v -> app.appealScreen.selectAppeal(a.getAppealId()));
        }
    }

    private void showVisits() {
        Ui ui = app.ui;
        ui.sectionTitle("Today's visits");
        ArrayList<Alert> booked = app.db.bookedAlerts();
        if (booked.isEmpty()) ui.emptyText("No booked appointments");
        for (Alert a : booked) {
            LinearLayout c = ui.card();
            c.addView(ui.row(ui.column(
                    ui.text(a.getDonor().getName(), 16, true, Ui.BLACK),
                    ui.text(QatraDatabase.formatDay(a.getAppointmentDate()) + "  " + a.getAppointmentTime(), 13, false, Ui.GRAY)),
                    ui.bloodDrop(a.getDonor().getBloodType())));
            c.addView(ui.softButton("Open visit", Ui.GREEN, v -> app.showVisit(a)));
        }
    }

    private String whenText(Forecast f) {
        if (f.getDaysUntilShort() == -1) return "✔ Safe for the next 14 days";
        if (f.getDaysUntilShort() == 0) return "⚠ Already below safe level!";
        return "⚠ Below safe level in " + f.getDaysUntilShort() + " day(s)";
    }
}
