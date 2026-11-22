package com.example.qatra;

import java.util.ArrayList;

/**
 * <<control>> ForecastingModule (F2, the AI component): forecasts each blood type 14 days ahead (UC-6).
 *
 * AI model: a linear regression per blood type,
 *     units per day = w0 + w1 * trend + w2 * weekend
 * one for usage and one for donor turnout, trained by least squares in updateModel().
 * It is used only when it beats the average-usage baseline on test data (NFR-7).
 */
public class ForecastingModule {
    private static final int MIN_HISTORY = 14;   // less than this -> threshold-based (FR-11)
    private static final int MIN_TRAINING = 28;
    private static final int TEST_DAYS = 14;
    private static final int WINDOW = 28;        // baseline = average of the last 28 days

    // layout of the model array stored per blood type in QatraDatabase
    private static final int USAGE_W = 0;        // 3 usage weights at index 0..2
    private static final int TURNOUT_W = 3;      // 3 turnout weights at index 3..5
    private static final int MAE_MODEL = 6;
    private static final int MAE_BASELINE = 7;

    private int horizonDays = 14;
    private final QatraDatabase db;
    private MatchingModule matching;             // Forecast triggers Appeal (FR-15)

    public ForecastingModule(QatraDatabase db) {
        this.db = db;
    }

    public void setMatching(MatchingModule matching) { this.matching = matching; }

    // ================= AI model (FR-23, NFR-7) =================

    /**
     * Weekly: trains a new model per blood type on all but the last 14 days, tests it on those
     * 14 days against the baseline (MAE), and keeps the old model if the new one is worse.
     */
    public String updateModel() {
        BloodBank bank = db.getBank();
        double sumModel = 0, sumBaseline = 0;
        int trained = 0, used = 0, keptOld = 0;

        for (String type : QatraDatabase.BLOOD_TYPES) {
            ArrayList<Stock> history = bank.history(type);
            int n = history.size();
            if (n < MIN_TRAINING + TEST_DAYS) continue;

            int split = n - TEST_DAYS;
            double[] usageW = train(history, 0, split, true);

            double baseline = 0;
            for (int i = split - WINDOW; i < split; i++) baseline += history.get(i).getUnitsUsed();
            baseline /= WINDOW;

            double errModel = 0, errBaseline = 0;
            for (int i = split; i < n; i++) {
                Stock s = history.get(i);
                errModel += Math.abs(predict(usageW, 0, s.getRecordDate(), i) - s.getUnitsUsed());
                errBaseline += Math.abs(baseline - s.getUnitsUsed());
            }
            double maeModel = errModel / TEST_DAYS;
            double maeBaseline = errBaseline / TEST_DAYS;

            double[] old = db.getModels().get(type);
            if (old != null && old[MAE_MODEL] <= maeModel) {
                keptOld++;                                    // FR-23: new one is worse -> keep old
            } else {
                double[] u = train(history, 0, n, true);       // retrain on all data
                double[] t = train(history, 0, n, false);
                db.getModels().put(type, new double[]{u[0], u[1], u[2], t[0], t[1], t[2], maeModel, maeBaseline});
            }
            double[] current = db.getModels().get(type);
            trained++;
            if (isAIUsed(type)) used++;
            sumModel += current[MAE_MODEL];
            sumBaseline += current[MAE_BASELINE];
        }

        db.setModelTrainedDay(QatraDatabase.today());
        if (trained == 0) return "Not enough history to train the model.";
        String summary = String.format("Model updated for %d blood types (%d kept the old model, AI used for %d). "
                        + "Average MAE %.2f vs baseline %.2f: %.0f%% better.",
                trained, keptOld, used, sumModel / trained, sumBaseline / trained, (1 - sumModel / sumBaseline) * 100);
        db.log("system", summary);
        return summary;
    }

    /** Overall improvement over the baseline in percent (NFR-7 target: at least 15%). */
    public double overallImprovement() {
        double m = 0, b = 0;
        for (double[] model : db.getModels().values()) { m += model[MAE_MODEL]; b += model[MAE_BASELINE]; }
        return b == 0 ? 0 : (1 - m / b) * 100;
    }

    public boolean hasModel(String type) { return db.getModels().containsKey(type); }
    public double getMaeModel(String type) { return db.getModels().get(type)[MAE_MODEL]; }
    public double getMaeBaseline(String type) { return db.getModels().get(type)[MAE_BASELINE]; }

    /** FR-23: the AI is used only if it beat the baseline. */
    public boolean isAIUsed(String type) {
        double[] m = db.getModels().get(type);
        return m != null && m[MAE_MODEL] < m[MAE_BASELINE];
    }

    /** UAE weekend is Saturday and Sunday. Day 0 (1 Jan 1970) was a Thursday. */
    public static boolean isWeekend(long day) {
        int dayOfWeek = (int) ((day + 4) % 7);   // 0 = Sunday ... 6 = Saturday
        return dayOfWeek == 0 || dayOfWeek == 6;
    }

    /** Model inputs for one day: [1, trend, weekend]. index = position in the history. */
    private static double[] features(long day, int index) {
        return new double[]{1.0, index / 30.0, isWeekend(day) ? 1.0 : 0.0};
    }

    /** Least squares on history[from..to-1]: solves (X^T X) w = X^T y. */
    private static double[] train(ArrayList<Stock> history, int from, int to, boolean usage) {
        double[][] xtx = new double[3][3];
        double[] xty = new double[3];
        for (int i = from; i < to; i++) {
            Stock s = history.get(i);
            double[] x = features(s.getRecordDate(), i);
            double y = usage ? s.getUnitsUsed() : s.getUnitsDonated();
            for (int a = 0; a < 3; a++) {
                xty[a] += x[a] * y;
                for (int b = 0; b < 3; b++) xtx[a][b] += x[a] * x[b];
            }
        }
        for (int a = 0; a < 3; a++) xtx[a][a] += 0.001;   // small ridge term for stability
        return solve(xtx, xty);
    }

    /** Gaussian elimination with pivoting: solves A w = b. */
    private static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) pivot = r;
            double[] tmpRow = a[col]; a[col] = a[pivot]; a[pivot] = tmpRow;
            double tmp = b[col]; b[col] = b[pivot]; b[pivot] = tmp;
            for (int r = col + 1; r < n; r++) {
                double factor = a[r][col] / a[col][col];
                b[r] -= factor * b[col];
                for (int c = col; c < n; c++) a[r][c] -= factor * a[col][c];
            }
        }
        double[] w = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double sum = b[r];
            for (int c = r + 1; c < n; c++) sum -= a[r][c] * w[c];
            w[r] = sum / a[r][r];
        }
        return w;
    }

    /** Prediction from 3 weights starting at 'offset' in the array. */
    private static double predict(double[] w, int offset, long day, int index) {
        double[] x = features(day, index);
        double y = w[offset] * x[0] + w[offset + 1] * x[1] + w[offset + 2] * x[2];
        return Math.max(0, y);
    }

    // ================= forecasting =================

    /** FR-8: daily run. Makes one Forecast per blood type and keeps it with its date. */
    public void generateForecast() {
        long today = QatraDatabase.today();
        if (db.getModelTrainedDay() < today - 7) updateModel();      // weekly retraining

        BloodBank bank = db.getBank();
        for (String type : QatraDatabase.BLOOD_TYPES) {
            Stock current = bank.currentLevel(type);
            if (current == null) continue;
            Forecast f;
            if (bank.history(type).size() < MIN_HISTORY) {
                f = applyThreshold(type);                              // FR-11
            } else {
                f = forecastOne(type, current, today);
            }
            db.getForecasts().add(f);
            // FR-15: a high risk triggers a prepared appeal, held for staff approval
            if (f.getRiskLevel().equals("High") && matching != null) matching.prepareAppeal(f);
        }
        // keep the last 30 days of forecasts
        ArrayList<Forecast> list = db.getForecasts();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getForecastDate() < today - 30) list.remove(i);
        }
    }

    /**
     * Expected stock for each day of the horizon (an output of generateForecast, not stored).
     * Uses the AI model when it beat the baseline, otherwise the 28-day averages.
     */
    private double[] expectedStock(String type, long fromDay) {
        BloodBank bank = db.getBank();
        ArrayList<Stock> history = bank.history(type);
        Stock current = bank.currentLevel(type);
        double[] model = db.getModels().get(type);
        boolean useAI = isAIUsed(type);
        double avgUse = average(history, WINDOW, true);
        double avgTurnout = average(history, WINDOW, false);

        double[] expected = new double[horizonDays];
        double stock = current.getUnitsInStock();
        int last = history.size() - 1;
        for (int d = 1; d <= horizonDays; d++) {
            long day = fromDay + d;
            double use = useAI ? predict(model, USAGE_W, day, last + d) : avgUse;
            double turnout = (useAI ? predict(model, TURNOUT_W, day, last + d) : avgTurnout)
                    * SeasonCalendar.donationFactor(day);
            stock = stock + turnout - use;
            if (day == current.getExpiryDate()) stock -= current.getUnitsExpiring();
            expected[d - 1] = stock;
        }
        return expected;
    }

    private Forecast forecastOne(String type, Stock current, long today) {
        BloodBank bank = db.getBank();
        ArrayList<Stock> history = bank.history(type);
        int safe = bank.safeLevelFor(type);
        double[] model = db.getModels().get(type);
        boolean useAI = isAIUsed(type);
        double avgUse = average(history, WINDOW, true);
        double recentUse = average(history, 7, true);

        double[] expected = expectedStock(type, today);
        int daysUntilShort = current.getUnitsInStock() < safe ? 0 : -1;
        for (int d = 0; d < horizonDays && daysUntilShort == -1; d++) {
            if (expected[d] < safe) daysUntilShort = d + 1;
        }

        // FR-13: the factors behind the forecast, one per line
        StringBuilder factors = new StringBuilder();
        if (useAI) {
            factors.append(String.format("AI model (linear regression): MAE %.2f vs baseline %.2f",
                    model[MAE_MODEL], model[MAE_BASELINE]));
            if (model[USAGE_W + 1] > 0.3) factors.append(Forecast.FACTOR_SEPARATOR).append("Usage trend is rising");
        } else {
            factors.append("Baseline model (average of the last 28 days)");
        }
        for (int d = 1; d <= horizonDays; d++) {
            String season = SeasonCalendar.seasonName(today + d);
            if (season != null && factors.indexOf(season + ":") < 0) {
                factors.append(Forecast.FACTOR_SEPARATOR).append(season).append(": lower donor turnout");
            }
        }
        if (recentUse > avgUse * 1.1) {
            factors.append(Forecast.FACTOR_SEPARATOR).append(String.format("High recent usage: %.1f/day this week", recentUse));
        }
        if (current.getUnitsExpiring() > 0) {
            factors.append(Forecast.FACTOR_SEPARATOR).append(current.getUnitsExpiring())
                    .append(" units expire on ").append(QatraDatabase.formatDay(current.getExpiryDate()));
        }
        factors.append(Forecast.FACTOR_SEPARATOR).append(String.format(
                "Expected stock in %d days: %.0f units (safe level %d)", horizonDays, expected[horizonDays - 1], safe));

        Forecast f = new Forecast(type, today, false, factors.toString(), daysUntilShort);
        f.setRiskLevel(classifyRisk(f, safe));
        f.setUnitsNeeded(estimateUnits(f));
        return f;
    }

    /** FR-9: High = below the safe level within 3 days, Medium = within 7, else Low. */
    public String classifyRisk(Forecast forecast, int safeLevel) {
        int days = forecast.getDaysUntilShort();
        if (days == -1 || days > 7) return "Low";
        if (days <= 3) return "High";
        return "Medium";
    }

    /** FR-10: units needed to be back at the safe level at the end of the horizon. */
    public int estimateUnits(Forecast forecast) {
        BloodBank bank = db.getBank();
        String type = forecast.getBloodType();
        int safe = bank.safeLevelFor(type);
        if (forecast.isThresholdBased()) {
            return Math.max(0, safe - bank.currentLevel(type).getUnitsInStock());
        }
        double[] expected = expectedStock(type, forecast.getForecastDate());
        return (int) Math.max(0, Math.ceil(safe - expected[horizonDays - 1]));
    }

    /** FR-11: not enough history, compare stock with the safe-stock threshold only. */
    public Forecast applyThreshold(String bloodType) {
        BloodBank bank = db.getBank();
        int units = bank.currentLevel(bloodType).getUnitsInStock();
        int safe = bank.safeLevelFor(bloodType);
        String factors = "Only " + bank.history(bloodType).size() + " days of history: compared with the safe level";
        Forecast f = new Forecast(bloodType, QatraDatabase.today(), true, factors, units < safe ? 0 : -1);
        f.setRiskLevel(units < safe ? "High" : units < safe * 1.2 ? "Medium" : "Low");
        f.setUnitsNeeded(estimateUnits(f));
        return f;
    }

    /**
     * Sequence diagram "Monitor Shortages": loop over the 8 blood types calling
     * Stock.currentLevel and Forecast.findLatest. High risk first (FR-12).
     * If today's forecast cannot be made, the last one is shown with its date (extension 2b).
     */
    public ArrayList<Forecast> getForecasts() {
        long today = QatraDatabase.today();
        Forecast any = Forecast.findLatest(db.getForecasts(), QatraDatabase.BLOOD_TYPES[0]);
        if (any == null || any.getForecastDate() < today) {
            try {
                generateForecast();
            } catch (RuntimeException e) {
                db.log("system", "Today's forecast failed, showing the last forecast");
            }
        }

        ArrayList<Forecast> result = new ArrayList<>();
        for (String type : QatraDatabase.BLOOD_TYPES) {
            Stock stock = db.getBank().currentLevel(type);
            Forecast latest = Forecast.findLatest(db.getForecasts(), type);
            if (stock != null && latest != null) result.add(latest);
        }
        for (int i = 0; i < result.size(); i++) {               // simple bubble sort, High first
            for (int j = 0; j < result.size() - 1 - i; j++) {
                if (result.get(j).riskOrder() > result.get(j + 1).riskOrder()) {
                    Forecast tmp = result.get(j);
                    result.set(j, result.get(j + 1));
                    result.set(j + 1, tmp);
                }
            }
        }
        return result;
    }

    /** Forces a fresh forecast today (after new stock data). */
    public void refresh() {
        long today = QatraDatabase.today();
        ArrayList<Forecast> list = db.getForecasts();
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i).getForecastDate() == today) list.remove(i);
        generateForecast();
    }

    public Forecast getForecastDetails(String bloodType) {
        return Forecast.findLatest(db.getForecasts(), bloodType);
    }

    private double average(ArrayList<Stock> history, int lastN, boolean usage) {
        int start = Math.max(0, history.size() - lastN);
        if (history.size() == start) return 0;
        double sum = 0;
        for (int i = start; i < history.size(); i++) {
            sum += usage ? history.get(i).getUnitsUsed() : history.get(i).getUnitsDonated();
        }
        return sum / (history.size() - start);
    }

    public int getHorizonDays() { return horizonDays; }
}
