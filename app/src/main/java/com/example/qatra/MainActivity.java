package com.example.qatra;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Starts the app, creates the control modules, and handles navigation.
 * The main boundary classes from the class diagram are EligibilityScreen,
 * StaffDashboard and AppealReviewScreen; the smaller screens are below.
 */
public class MainActivity extends AppCompatActivity {

    // shared with the screen classes
    QatraDatabase db;
    Ui ui;
    User me;                              // logged-in user, null = logged out
    EligibilityModule eligibility;
    MatchingModule matching;
    ForecastingModule forecasting;
    InventorySystem inventory;
    EligibilityScreen eligibilityScreen;
    StaffDashboard dashboard;
    AppealReviewScreen appealScreen;

    private Runnable current;             // redraws the current screen
    private boolean atHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new Ui(this);
        setContentView(ui.root());
        setUp(QatraDatabase.get(this));
        showLogin();
    }

    /** Creates the modules (control classes) and screens (boundary classes). */
    private void setUp(QatraDatabase database) {
        db = database;
        eligibility = new EligibilityModule(db);
        matching = new MatchingModule(db, eligibility, new MapsService(), new NotificationService(db));
        forecasting = new ForecastingModule(db);
        forecasting.setMatching(matching);      // Forecast triggers Appeal (FR-15)
        inventory = new InventorySystem(db);
        eligibilityScreen = new EligibilityScreen(this);
        dashboard = new StaffDashboard(this);
        appealScreen = new AppealReviewScreen(this);
    }

    /** Save only when the app goes to the background: keeps every tap fast. */
    @Override
    protected void onPause() {
        super.onPause();
        db.save();
    }

    @Override
    public void onBackPressed() {
        if (atHome) super.onBackPressed();
        else if (me == null) showLogin();
        else showHome();
    }

    // ================= navigation helpers =================

    void startScreen(Runnable redraw, boolean home) {
        current = redraw;
        atHome = home;
        ui.clear();
    }

    void header(String title, String subtitle, boolean back) {
        ui.header(title, subtitle,
                back ? (Runnable) this::showHome : null,
                me != null ? (Runnable) this::logout : null,
                () -> current.run());
    }

    String who() { return me.getUserId(); }

    void showHome() {
        if (me instanceof BloodBankStaff) dashboard.openDashboard();
        else if (me instanceof Administrator) showAdminHome();
        else if (me instanceof Donor) eligibilityScreen.openScreen();
        else showLogin();
    }

    private void logout() {
        db.log(who(), "Logged out");
        me.logOut();
        me = null;
        showLogin();
    }

    // ================= login & register (FR-1, FR-24) =================

    void showLogin() {
        startScreen(this::showLogin, true);
        ui.hero(() -> current.run());

        LinearLayout card = ui.floatingCard();
        card.addView(ui.text("Welcome back", 22, true, Ui.BLACK));
        card.addView(ui.text("Log in to continue", 14, false, Ui.GRAY));
        EditText user = ui.textInput(card, "Username", "");
        EditText pass = ui.passwordInput(card, "Password");
        card.addView(ui.button("Log in", Ui.RED, v -> {
            User u = db.login(user.getText().toString(), pass.getText().toString());
            if (u == null) {
                ui.toast("Wrong username or password");
            } else {
                me = u;
                showHome();
            }
        }));
        card.addView(ui.link("New here?  Create donor account", v -> showRegister()));

        LinearLayout demo = ui.card();
        demo.addView(ui.text("Try a demo account", 15, true, Ui.BLACK));
        demo.addView(ui.text("Tap one to fill in the login", 12, false, Ui.GRAY));
        String[][] accounts = {
            {"🧑‍⚕️ Staff", "staff", "staff123"},
            {"🛡 Admin", "admin", "admin123"},
            {"🩸 Ahmed (O-)", "ahmed", "1234"},
            {"🆕 Fatima", "fatima", "1234"}
        };
        LinearLayout row1 = new LinearLayout(this);
        LinearLayout row2 = new LinearLayout(this);
        for (int i = 0; i < accounts.length; i++) {
            String[] acc = accounts[i];
            (i < 2 ? row1 : row2).addView(ui.chip(acc[0], v -> {
                user.setText(acc[1]);
                pass.setText(acc[2]);
            }));
        }
        demo.addView(row1);
        demo.addView(row2);
        ui.spacer();
    }

    private void showRegister() {
        startScreen(this::showRegister, false);
        header("Create account", "Join Qatra as a blood donor", false);

        LinearLayout card = ui.card();
        EditText name = ui.textInput(card, "Full name", "");
        EditText phone = ui.phoneInput(card, "Phone", "");
        EditText year = ui.input(card, "Date of birth (dd/mm/yyyy)", "", android.text.InputType.TYPE_CLASS_DATETIME);
        Spinner type = ui.spinner(card, "Blood type", QatraDatabase.BLOOD_TYPES, "O+");
        Spinner place = ui.spinner(card, "Location", MapsService.locations(), "Abu Dhabi - Al Reem");
        Spinner notify = ui.spinner(card, "Notify me by", new String[]{"Push", "SMS"}, "Push");
        EditText user = ui.textInput(card, "Username", "");
        EditText pass = ui.passwordInput(card, "Password");

        LinearLayout consentCard = ui.card();
        consentCard.addView(ui.text("How we use your data", 15, true, Ui.BLACK));
        consentCard.addView(ui.text("We store only your name, phone, birth year, blood type, location and donation history. "
                + "They are used to check when you can donate and to contact you when your blood type is needed. "
                + "Your password is stored hashed. Donating is always voluntary and unpaid.", 13, false, Ui.GRAY));
        CheckBox consent = ui.checkBox(consentCard, "I agree");

        consentCard.addView(ui.button("Register", Ui.RED, v -> {
            String error = db.registerDonor(name.getText().toString(), phone.getText().toString(),
                    year.getText().toString(), type.getSelectedItem().toString(),
                    place.getSelectedItem().toString(), notify.getSelectedItem().toString(),
                    user.getText().toString(), pass.getText().toString(), consent.isChecked());
            if (error != null) ui.toast(error);
            else { ui.toast("Account created, please log in"); showLogin(); }
        }));
        consentCard.addView(ui.link("Back to login", v -> showLogin()));
        ui.spacer();
    }

    // ================= donor screens (UC-1, UC-3) =================

    /** FR-17: pick a slot, then see date, time and place. */
    void showBooking(Alert a) {
        startScreen(() -> showBooking(a), false);
        header("Book appointment", a.getBloodType() + " appeal", true);

        long today = QatraDatabase.today();
        String[] times = {"09:00", "12:00", "16:00"};
        String[] labels = new String[9];
        long[] days = new long[9];
        int k = 0;
        for (int d = 1; d <= 3; d++) {
            for (String t : times) {
                days[k] = today + d;
                labels[k] = QatraDatabase.formatDay(today + d) + "  " + t;
                k++;
            }
        }
        LinearLayout card = ui.card();
        card.addView(ui.text("📍 " + a.getLocation(), 16, true, Ui.BLACK));
        Spinner slot = ui.spinner(card, "Choose a time", labels, labels[0]);
        card.addView(ui.button("Confirm booking", Ui.RED, v -> {
            int i = slot.getSelectedItemPosition();
            db.book(a, days[i], times[i % 3]);
            ui.toast("Booked! See you soon");
            showHome();
        }));
    }

    /** FR-2. */
    void showProfile() {
        startScreen(this::showProfile, false);
        Donor d = (Donor) me;
        header("Edit profile", d.getName() + "  •  " + d.getBloodType(), true);

        LinearLayout card = ui.card();
        EditText phone = ui.phoneInput(card, "Phone", d.getContact());
        Spinner place = ui.spinner(card, "Location", MapsService.locations(), d.getLocation());
        Spinner notify = ui.spinner(card, "Notify me by", new String[]{"Push", "SMS"}, d.getContactPreference());
        card.addView(ui.button("Save", Ui.RED, v -> {
            String error = db.updateProfile(d, phone.getText().toString(),
                    place.getSelectedItem().toString(), notify.getSelectedItem().toString());
            if (error != null) ui.toast(error);
            else { ui.toast("Profile saved"); showHome(); }
        }));
    }

    /** FR-3: newest first. */
    void showHistory() {
        startScreen(this::showHistory, false);
        Donor d = (Donor) me;
        header("My donations", d.listDonations().size() + " donation(s)", true);
        if (d.listDonations().isEmpty()) ui.emptyText("No donations yet");
        for (Donation x : d.listDonationsNewestFirst()) {
            LinearLayout c = ui.card();
            c.addView(ui.row(ui.column(
                    ui.text(QatraDatabase.formatDay(x.getDonationDate()), 16, true, Ui.BLACK),
                    ui.text(x.getCentre() + "  •  " + x.getDonationType(), 13, false, Ui.GRAY)),
                    ui.bloodDrop(x.getBloodType())));
        }
        ui.spacer();
    }

    // ================= staff screens (UC-4, UC-5) =================

    /** UC-4 / FR-7: enter or import stock data. */
    void showStockForm() {
        startScreen(this::showStockForm, false);
        header("Update stock", "Enter today's numbers or import them", true);

        LinearLayout imp = ui.card();
        imp.addView(ui.text("Blood-bank Inventory System", 15, true, Ui.BLACK));
        imp.addView(ui.softButton("Import today's stock", Ui.BLUE, v -> {
            int n = inventory.importStock(QatraDatabase.today());
            db.log(who(), "Imported " + n + " stock records from Inventory System");
            forecasting.refresh();
            ui.toast("Imported " + n + " records");
            showStockForm();
        }));

        LinearLayout card = ui.card();
        card.addView(ui.text("Manual entry", 15, true, Ui.BLACK));
        Spinner type = ui.spinner(card, "Blood type", QatraDatabase.BLOOD_TYPES, "O+");
        EditText units = ui.numberInput(card, "Units in stock now", "");
        EditText used = ui.numberInput(card, "Units used today", "");
        EditText donated = ui.numberInput(card, "Units donated today", "");
        EditText expiring = ui.numberInput(card, "Units expiring within 5 days", "");
        card.addView(ui.button("Save", Ui.RED, v -> {
            String error = db.enterStock(type.getSelectedItem().toString(), units.getText().toString(),
                    used.getText().toString(), donated.getText().toString(), expiring.getText().toString(), who());
            if (error != null) ui.toast(error);
            else { forecasting.refresh(); ui.toast("Stock saved"); showHome(); }
        }));

        ui.sectionTitle("Current stock");
        BloodBank bank = db.getBank();
        for (String t : QatraDatabase.BLOOD_TYPES) {
            Stock s = bank.currentLevel(t);
            LinearLayout c = ui.card();
            c.addView(ui.row(ui.text(t, 18, true, Ui.RED),
                    ui.text(s.getUnitsInStock() + " units  •  " + s.getUnitsExpiring() + " expiring", 14, false, Ui.BLACK)));
        }
        ui.spacer();
    }

    /** UC-5 / FR-6 / FR-25: provisional eligibility, then staff confirm or reject. */
    void showVisit(Alert a) {
        startScreen(() -> showVisit(a), false);
        Donor d = a.getDonor();
        db.log(who(), "Viewed donor record " + d.getUserId());   // NFR-11
        header("Donor visit", d.getName(), true);

        LinearLayout card = ui.card();
        card.addView(ui.row(ui.column(
                ui.text(d.getName(), 18, true, Ui.BLACK),
                ui.text("Age " + d.getAge(QatraDatabase.today()) + "  •  " + d.getContact(), 13, false, Ui.GRAY),
                ui.text("Donations: " + d.listDonations().size(), 13, false, Ui.GRAY)),
                ui.bloodDrop(d.getBloodType())));
        boolean ok = eligibility.isEligible(d.getUserId());
        card.addView(ui.pill("Provisional: " + (ok ? "eligible" : "NOT eligible"), ok ? Ui.GREEN : Ui.RED));
        card.addView(ui.text("Medical staff must confirm before the donation.", 13, false, Ui.GRAY));

        LinearLayout decide = ui.card();
        decide.addView(ui.button("Confirm & record donation", Ui.GREEN, v -> {
            db.recordDonation(a, who());
            eligibility.checkEligibility(d.getUserId());            // FR-4
            ui.toast("Donation recorded, stock +1");
            showHome();
        }));
        EditText reason = ui.textInput(decide, "Reason (if not accepted)", "");
        EditText days = ui.numberInput(decide, "Defer for days", "30");
        decide.addView(ui.softButton("Not accepted", Ui.GRAY, v -> {
            String r = reason.getText().toString().trim();
            int n;
            try { n = Integer.parseInt(days.getText().toString().trim()); }
            catch (NumberFormatException e) { n = -1; }
            if (r.isEmpty() || n < 1) { ui.toast("Enter a reason and number of days"); return; }
            db.rejectAtVisit(a, r, n, who());
            eligibility.checkEligibility(d.getUserId());            // FR-4
            ui.toast("Decision recorded");
            showHome();
        }));
        ui.spacer();
    }

    // ================= admin screens (UC-9) =================

    private void showAdminHome() {
        startScreen(this::showAdminHome, true);
        Administrator admin = (Administrator) me;
        header("Administrator", "Users, rules and reports", false);

        ui.sectionTitle("Eligibility rules");
        EligibilityRule rule = EligibilityRule.loadCurrentRules(db.getRules(), "Whole blood");
        LinearLayout rules = ui.card();
        EditText recovery = ui.numberInput(rules, "Days between donations", "" + rule.getRecoveryIntervalDays());
        rules.addView(ui.button("Save rules", Ui.RED, v -> {
            int r;
            try {
                r = Integer.parseInt(recovery.getText().toString().trim());
            } catch (NumberFormatException e) { ui.toast("Value must be a whole number"); return; }
            EligibilityRule changed = new EligibilityRule(rule.getDonationType(), r);
            String error = admin.manageRules(changed);
            if (error != null) { ui.toast(error); return; }
            db.applyRule(changed, who());
            ui.toast("Rules applied");
        }));

        ui.sectionTitle("Reports");
        LinearLayout rep = ui.card();
        rep.addView(ui.softButton("Supply report", Ui.BLUE, v -> showReport(30)));
        rep.addView(ui.softButton("AI forecast model", Ui.GREEN, v -> showModel()));
        rep.addView(ui.softButton("Audit log", Ui.GRAY, v -> showAudit()));
        rep.addView(ui.softButton("Reset demo data", Ui.ORANGE, v -> {
            setUp(QatraDatabase.reset(this));
            me = null;
            ui.toast("Demo data reset");
            showLogin();
        }));

        ui.sectionTitle("Add staff user");
        LinearLayout add = ui.card();
        EditText sid = ui.textInput(add, "Username", "");
        EditText sname = ui.textInput(add, "Full name", "");
        EditText spass = ui.passwordInput(add, "Password");
        add.addView(ui.button("Add staff", Ui.RED, v -> {
            String error = db.addStaff(sid.getText().toString(), sname.getText().toString(),
                    spass.getText().toString(), who());
            ui.toast(error != null ? error : "Staff user added");
            if (error == null) showAdminHome();
        }));

        ui.sectionTitle("Users");
        for (User u : db.getUsers()) {
            LinearLayout c = ui.card();
            int color = u instanceof Donor ? Ui.BLUE : u instanceof Administrator ? Ui.RED : Ui.GREEN;
            c.addView(ui.row(ui.column(ui.text(u.getName(), 15, true, Ui.BLACK),
                    ui.text(u.getUserId(), 12, false, Ui.GRAY)), ui.badge(u.getRole(), color)));
        }
        ui.spacer();
    }

    /** FR-22: anonymised statistics only. */
    private void showReport(int days) {
        startScreen(() -> showReport(days), false);
        header("Supply report", "Last " + days + " days  •  anonymised", true);

        LinearLayout period = ui.card();
        String[] options = {"Last 7 days", "Last 30 days", "Last 60 days"};
        int[] values = {7, 30, 60};
        Spinner choice = ui.spinner(period, "Period", options, "Last " + days + " days");
        period.addView(ui.softButton("Show report", Ui.BLUE,
                v -> showReport(values[choice.getSelectedItemPosition()])));

        LinearLayout c = ui.card();
        for (String line : db.supplyReport(days)) c.addView(ui.text(line, 14, false, Ui.BLACK));
        ui.spacer();
    }

    /** FR-23 / NFR-7: AI model results, MAE vs baseline, and retraining. */
    void showModel() {
        startScreen(this::showModel, false);
        if (db.getModels().isEmpty()) forecasting.updateModel();
        header("AI forecast model", "Linear regression vs average baseline", true);

        double improvement = forecasting.overallImprovement();
        LinearLayout sum = ui.card();
        sum.addView(ui.text("Overall: " + String.format("%.0f%%", improvement) + " lower error than the baseline", 17, true, Ui.BLACK));
        sum.addView(ui.pill(improvement >= 15 ? "✅  Meets NFR-7 (at least 15% better)" : "⚠  Below the NFR-7 target of 15%",
                improvement >= 15 ? Ui.GREEN : Ui.ORANGE));
        sum.addView(ui.text("Trained on past daily usage with trend and weekend features. "
                + "Tested on the last 14 days. Last trained: "
                + QatraDatabase.formatDay(db.getModelTrainedDay()), 13, false, Ui.GRAY));

        ui.sectionTitle("Mean Absolute Error per blood type");
        for (String type : QatraDatabase.BLOOD_TYPES) {
            LinearLayout c = ui.card();
            if (!forecasting.hasModel(type)) {
                c.addView(ui.row(ui.text(type, 18, true, Ui.RED), ui.badge("threshold-based", Ui.GRAY)));
                continue;
            }
            boolean used = forecasting.isAIUsed(type);
            double ai = forecasting.getMaeModel(type), base = forecasting.getMaeBaseline(type);
            c.addView(ui.row(ui.text(type, 18, true, Ui.RED),
                    ui.badge(used ? "AI model used" : "baseline kept", used ? Ui.GREEN : Ui.GRAY)));
            c.addView(ui.text(String.format("AI MAE %.2f   •   Baseline MAE %.2f   •   %.0f%% better",
                    ai, base, (1 - ai / base) * 100), 14, false, Ui.BLACK));
        }
        ui.spacer();
    }

    private void showAudit() {
        startScreen(this::showAudit, false);
        ArrayList<String> log = db.getAuditLog();
        header("Audit log", log.size() + " entries", true);
        LinearLayout c = ui.card();
        for (int i = log.size() - 1; i >= 0 && i >= log.size() - 150; i--) {
            c.addView(ui.text(log.get(i), 12, false, Ui.BLACK));
        }
        ui.spacer();
    }
}
