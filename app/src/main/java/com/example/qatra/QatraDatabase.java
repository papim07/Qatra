package com.example.qatra;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Storage for all entities (stands in for the PostgreSQL database).
 * Saved to an encrypted file when the app goes to the background.
 */
public class QatraDatabase implements Serializable {
    public static final String[] BLOOD_TYPES = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"};
    private static final String FILE_NAME = "qatra_v6.dat";

    private static QatraDatabase instance;
    private transient File file;

    private BloodBank bank = new BloodBank("Abu Dhabi Blood Bank", "Abu Dhabi - Al Reem", 5);   // keep 5 days of supply
    private ArrayList<User> users = new ArrayList<>();
    private ArrayList<EligibilityRule> rules = new ArrayList<>();
    private ArrayList<Appeal> appeals = new ArrayList<>();
    private ArrayList<Alert> alerts = new ArrayList<>();
    private ArrayList<Forecast> forecasts = new ArrayList<>();
    private ArrayList<String> auditLog = new ArrayList<>();
    private HashMap<String, double[]> models = new HashMap<>();   // AI model weights + MAE per blood type
    private long modelTrainedDay = -1;
    private int nextAppealId = 1;

    // ================= loading & saving =================

    public static QatraDatabase get(Context c) {
        if (instance == null) {
            File f = new File(c.getFilesDir(), FILE_NAME);
            instance = load(f);
            if (instance == null) {
                instance = new QatraDatabase();
                instance.loadSampleData();
            }
            instance.file = f;
        }
        return instance;
    }

    /** Database with sample data and no file: used by the unit tests. */
    static QatraDatabase createForTesting() {
        QatraDatabase db = new QatraDatabase();
        db.loadSampleData();
        return db;
    }

    // NFR-8: data at rest is encrypted with AES-GCM; the key is kept in the Android Keystore.
    private static final String KEY_ALIAS = "qatra_data_key";

    private static SecretKey getKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) return (SecretKey) ks.getKey(KEY_ALIAS, null);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return gen.generateKey();
    }

    private static QatraDatabase load(File f) {
        if (!f.exists()) return null;
        try (DataInputStream raw = new DataInputStream(new FileInputStream(f))) {
            byte[] iv = new byte[raw.readInt()];
            raw.readFully(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            ObjectInputStream in = new ObjectInputStream(new CipherInputStream(raw, cipher));
            return (QatraDatabase) in.readObject();
        } catch (Exception e) {
            return null;   // missing, old or unreadable file -> start with sample data
        }
    }

    /** Called from MainActivity.onPause. */
    public void save() {
        if (file == null) return;
        try (DataOutputStream raw = new DataOutputStream(new FileOutputStream(file))) {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] iv = cipher.getIV();
            raw.writeInt(iv.length);
            raw.write(iv);
            ObjectOutputStream out = new ObjectOutputStream(new CipherOutputStream(raw, cipher));
            out.writeObject(this);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static QatraDatabase reset(Context c) {
        new File(c.getFilesDir(), FILE_NAME).delete();
        instance = null;
        return get(c);
    }

    // ================= helpers =================

    public static long today() {
        return System.currentTimeMillis() / 86400000L;
    }

    public static String formatDay(long day) {
        return new SimpleDateFormat("EEE dd MMM yyyy", Locale.US).format(new Date(day * 86400000L));
    }

    public static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /** FR-20, NFR-11: who did what and when. */
    public void log(String who, String action) {
        String time = new SimpleDateFormat("dd MMM HH:mm", Locale.US).format(new Date());
        auditLog.add(time + "  •  " + who + "  •  " + action);
    }

    public int nextAppealId() { return nextAppealId++; }

    // ================= users =================

    public User login(String userId, String password) {
        for (User u : users) {
            if (u.logIn(userId, password)) {
                log(u.getUserId(), "Logged in");
                return u;
            }
        }
        return null;
    }

    public User findUser(String userId) {
        for (User u : users) if (u.getUserId().equalsIgnoreCase(userId.trim())) return u;
        return null;
    }

    public Donor findDonor(String userId) {
        User u = findUser(userId);
        return u instanceof Donor ? (Donor) u : null;
    }

    /** Parses dd/MM/yyyy into a day number, or -1 if not a valid date. */
    public static long parseDate(String text) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            f.setLenient(false);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(text.trim()).getTime() / 86400000L;
        } catch (Exception e) {
            return -1;
        }
    }

    /** FR-20: applies a changed rule straight away and logs who did it and when. */
    public void applyRule(EligibilityRule changed, String by) {
        EligibilityRule current = EligibilityRule.loadCurrentRules(rules, changed.getDonationType());
        current.setRecoveryIntervalDays(changed.getRecoveryIntervalDays());
        log(by, "Changed rule '" + changed.getDonationType() + "': " + changed.getRecoveryIntervalDays()
                + " days between donations");
    }

    public ArrayList<Donor> getDonors() {
        ArrayList<Donor> list = new ArrayList<>();
        for (User u : users) if (u instanceof Donor) list.add((Donor) u);
        return list;
    }

    /** FR-1 + FR-24. Returns an error message, or null if the account was created. */
    public String registerDonor(String name, String contact, String dobText, String bloodType,
                                String location, String preference, String userId, String password,
                                boolean consent) {
        if (name.trim().isEmpty() || contact.trim().isEmpty() || dobText.trim().isEmpty()
                || userId.trim().isEmpty() || password.isEmpty()) return "Please fill in all fields";
        if (!contact.trim().matches("[0-9+ ]{7,15}")) return "Phone number is not valid";
        long dob = parseDate(dobText);
        if (dob == -1) return "Date of birth must be like 25/03/2001";
        int age = Donor.yearsBetween(dob, today());
        if (age < EligibilityModule.MIN_AGE || age > 100) return "You must be 18 or older to donate";
        if (password.length() < 4) return "Password must be at least 4 characters";
        if (findUser(userId) != null) return "Username already taken";
        if (!consent) return "Please accept how your data is used";

        users.add(new Donor(userId.trim(), name.trim(), contact.trim(), password, dob,
                bloodType, location, preference));
        log(userId.trim(), "Registered as donor (consent given)");
        return null;
    }

    /** UC-9: administrator adds a staff account. */
    public String addStaff(String userId, String name, String password, String by) {
        if (userId.trim().isEmpty() || name.trim().isEmpty() || password.length() < 4)
            return "Fill in all fields (password 4+ characters)";
        if (findUser(userId) != null) return "Username already taken";
        users.add(new BloodBankStaff(userId.trim(), name.trim(), "-", password, "Blood bank officer"));
        log(by, "Added staff user " + userId.trim());
        return null;
    }

    public String updateProfile(Donor d, String contact, String location, String preference) {
        if (!contact.trim().matches("[0-9+ ]{7,15}")) return "Phone number is not valid";
        d.updateProfile(contact.trim(), location, preference);
        log(d.getUserId(), "Updated profile");
        return null;
    }

    // ================= stock (UC-4, FR-7) =================

    public String enterStock(String type, String unitsText, String usedText, String donatedText,
                             String expiringText, String by) {
        int units, used, donated, expiring;
        try {
            units = Integer.parseInt(unitsText.trim());
            used = Integer.parseInt(usedText.trim());
            donated = Integer.parseInt(donatedText.trim());
            expiring = Integer.parseInt(expiringText.trim());
        } catch (NumberFormatException e) {
            return "All values must be whole numbers";
        }
        if (units < 0 || used < 0 || donated < 0 || expiring < 0) return "Values cannot be negative";
        if (expiring > units) return "Expiring units cannot be more than units in stock";
        if (units > 5000 || used > 1000 || donated > 1000) return "Value is too large, please check it";
        bank.addRecord(new Stock(type, today(), units, used, donated, expiring, today() + 5));
        log(by, "Entered " + type + " stock: " + units + " units");
        return null;
    }

    // ================= alerts, booking, visits =================

    public boolean hasOpenAlert(Donor d) {
        for (Alert a : alerts) if (a.getDonor() == d && a.isOpen()) return true;
        return false;
    }

    public ArrayList<Alert> alertsFor(Donor d) {
        ArrayList<Alert> list = new ArrayList<>();
        for (Alert a : alerts) if (a.getDonor() == d) list.add(a);
        return list;
    }

    public ArrayList<Alert> alertsFor(Appeal ap) {
        ArrayList<Alert> list = new ArrayList<>();
        for (Alert a : alerts) if (a.getAppeal() == ap) list.add(a);
        return list;
    }

    public ArrayList<Alert> bookedAlerts() {
        ArrayList<Alert> list = new ArrayList<>();
        for (Alert a : alerts) if (a.getStatus().equals(Alert.BOOKED)) list.add(a);
        return list;
    }

    public void book(Alert a, long date, String time) {
        a.book(date, time);
        log(a.getDonor().getUserId(), "Booked " + formatDay(date) + " " + time);
    }

    /** UC-5 / FR-6: staff confirm and record the donation. */
    public void recordDonation(Alert a, String by) {
        Donor d = a.getDonor();
        new Donation(today(), bank.getName(), "Whole blood", d.getBloodType()).record(d);
        Stock s = bank.currentLevel(d.getBloodType());
        if (s != null) s.addUnit();
        a.setStatus(Alert.DONATED);
        log(by, "Confirmed and recorded donation for " + d.getUserId());
    }

    /** FR-25: medical staff do not accept the donor. */
    public void rejectAtVisit(Alert a, String reason, int days, String by) {
        a.getDonor().addDeferral(new Deferral(reason, today() + days));
        a.setStatus(Alert.NOT_ACCEPTED);
        log(by, "Did not accept " + a.getDonor().getUserId() + ": " + reason);
    }

    /** FR-19: {contacted, booked, donated, still needed}. */
    public int[] appealResponse(Appeal ap) {
        int contacted = 0, booked = 0, donated = 0;
        for (Alert a : alertsFor(ap)) {
            contacted++;
            if (a.getStatus().equals(Alert.BOOKED)) booked++;
            if (a.getStatus().equals(Alert.DONATED)) { booked++; donated++; }
        }
        return new int[]{contacted, booked, donated, Math.max(0, ap.getUnitsNeeded() - donated)};
    }

    public int responseRate() {
        if (alerts.isEmpty()) return 0;
        int responded = 0;
        for (Alert a : alerts) if (!a.getStatus().equals(Alert.SENT)) responded++;
        return responded * 100 / alerts.size();
    }

    public Appeal findAppeal(int id) {
        for (Appeal a : appeals) if (a.getAppealId() == id) return a;
        return null;
    }

    // ================= FR-22 anonymised report =================

    public ArrayList<String> supplyReport(int days) {
        ArrayList<String> lines = new ArrayList<>();
        long from = today() - days;
        for (String type : BLOOD_TYPES) {
            int used = 0, donated = 0;
            for (Stock s : bank.history(type)) {
                if (s.getRecordDate() >= from) { used += s.getUnitsUsed(); donated += s.getUnitsDonated(); }
            }
            Stock now = bank.currentLevel(type);
            lines.add(type + ":  used " + used + ",  donated " + donated + ",  now "
                    + (now == null ? 0 : now.getUnitsInStock()) + " (safe " + bank.safeLevelFor(type) + ")");
        }
        int recorded = 0;
        for (Donor d : getDonors()) for (Donation x : d.listDonations()) if (x.getDonationDate() >= from) recorded++;
        int prepared = 0, approved = 0, rejected = 0;
        for (Appeal a : appeals) {
            if (a.getCreatedDate() < from) continue;
            prepared++;
            if (a.getStatus().equals(Appeal.APPROVED)) approved++;
            if (a.getStatus().equals(Appeal.REJECTED)) rejected++;
        }
        lines.add("Donations recorded: " + recorded);
        lines.add("Appeals: " + prepared + " prepared, " + approved + " approved, " + rejected + " rejected");
        lines.add("Alert response rate: " + responseRate() + "%");
        lines.add("Registered donors: " + getDonors().size());
        return lines;
    }

    // ================= getters =================

    public BloodBank getBank() { return bank; }
    public ArrayList<User> getUsers() { return users; }
    public ArrayList<EligibilityRule> getRules() { return rules; }
    public ArrayList<Appeal> getAppeals() { return appeals; }
    public ArrayList<Alert> getAlerts() { return alerts; }
    public ArrayList<Forecast> getForecasts() { return forecasts; }
    public ArrayList<String> getAuditLog() { return auditLog; }
    public HashMap<String, double[]> getModels() { return models; }
    public long getModelTrainedDay() { return modelTrainedDay; }
    public void setModelTrainedDay(long day) { modelTrainedDay = day; }

    // ================= synthetic sample data =================

    private void loadSampleData() {
        Random r = new Random(42);
        long now = today();
        rules.add(new EligibilityRule("Whole blood", 56));

        users.add(new BloodBankStaff("staff", "Sara Staff", "0500000001", "staff123", "Blood bank officer"));
        users.add(new Administrator("admin", "Admin", "0500000002", "admin123"));

        String[] names = {
            "Ahmed Ali", "Fatima Hassan", "Omar Khalid", "Mariam Saeed", "Yousef Nasser",
            "Aisha Rashid", "Khalid Salem", "Noura Ahmed", "Hamad Saif", "Sara Ibrahim",
            "Ali Mansour", "Huda Obaid", "Saeed Juma", "Latifa Hamdan", "Rashed Majid",
            "Maryam Faisal", "Sultan Zayed", "Reem Abdulla", "Majid Hamad", "Shamma Saleh",
            "Abdulla Omar", "Hessa Khalfan", "Faisal Tariq", "Moza Sultan", "Tariq Hilal"
        };
        String[] types = {"O+", "O+", "O-", "O-", "A+", "A+", "A-", "B+", "B-", "AB+", "AB-"};
        String[] places = MapsService.locations();

        for (int i = 0; i < names.length; i++) {
            String type = types[r.nextInt(types.length)];
            String place = places[r.nextInt(places.length)];
            long last = (r.nextInt(6) == 0) ? -1 : now - (10 + r.nextInt(190));

            // fixed donors so the demo always works
            if (i == 0) { type = "O-"; place = "Abu Dhabi - Al Reem"; last = now - 100; }   // eligible
            if (i == 1) { type = "O-"; place = "Abu Dhabi - Khalifa City"; last = -1; }    // new donor
            if (i == 2) { type = "A-"; place = "Abu Dhabi - Al Reem"; last = now - 20; }    // must wait
            if (i == 3) { type = "O-"; place = "Abu Dhabi - Al Shamkha"; last = now - 90; } // needs wider search

            Donor d = new Donor(names[i].split(" ")[0].toLowerCase(), names[i],
                    "05" + (10000000 + r.nextInt(89999999)), "1234", now - 365L * (20 + r.nextInt(35)) - r.nextInt(365), type, place,
                    r.nextBoolean() ? "Push" : "SMS");
            if (last != -1) {
                new Donation(last - 120, bank.getName(), "Whole blood", type).record(d);
                new Donation(last, bank.getName(), "Whole blood", type).record(d);
            }
            if (i > 3 && r.nextInt(8) == 0) d.addDeferral(new Deferral("Recent travel", now + 5 + r.nextInt(25)));
            users.add(d);
        }

        // units now, safe level, used/day, donated/day, expiring soon, days of history
        int[][] cfg = {
            {120, 80, 14, 13, 6, 60},   // O+
            {28, 25, 5, 3, 4, 60},      // O-  -> High
            {90, 60, 10, 10, 5, 60},    // A+
            {30, 20, 4, 3, 3, 60},      // A-  -> Medium
            {70, 45, 7, 7, 2, 60},      // B+
            {18, 12, 2, 2, 1, 60},      // B-
            {35, 20, 3, 3, 1, 60},      // AB+
            {9, 8, 1, 1, 0, 7}          // AB- -> threshold-based
        };
        for (int i = 0; i < BLOOD_TYPES.length; i++) {
            int[] c = cfg[i];
            int n = c[5];
            for (int day = n - 1; day >= 0; day--) {
                long date = now - day;
                int k = n - 1 - day;                                   // 0 = oldest day
                boolean weekend = ForecastingModule.isWeekend(date);
                // realistic patterns for the AI to learn: usage rises over the period
                // (0.8x -> 1.2x) and is lower at weekends; donations are higher at weekends
                double trend = n > 1 ? 0.8 + 0.4 * k / (n - 1) : 1.0;
                int used = (int) Math.max(0, Math.round(c[2] * trend * (weekend ? 0.6 : 1.1) + r.nextInt(3) - 1));
                int donated = (int) Math.max(0, Math.round(c[3] * (weekend ? 1.3 : 0.9) + r.nextInt(3) - 1));
                boolean latest = (day == 0);
                int units = latest ? c[0] : c[0] + r.nextInt(11) - 5;
                bank.addRecord(new Stock(BLOOD_TYPES[i], date, units, used, donated,
                        latest ? c[4] : 0, date + 4));
            }
        }
        log("system", "Loaded sample data");
    }
}
