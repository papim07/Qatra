package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

/** <<entity>> Forecast: produced by ForecastingModule; may trigger an Appeal. */
public class Forecast implements Serializable {
    public static final String FACTOR_SEPARATOR = "\n";

    private String bloodType;
    private long forecastDate;
    private String riskLevel = "Low";    // Low / Medium / High (FR-9)
    private int unitsNeeded;             // FR-10
    private boolean isThresholdBased;    // FR-11
    private String factors;              // FR-13, one factor per line
    private int daysUntilShort;          // derived: first day below the safe level, -1 = none

    public Forecast(String bloodType, long forecastDate, boolean isThresholdBased,
                    String factors, int daysUntilShort) {
        this.bloodType = bloodType;
        this.forecastDate = forecastDate;
        this.isThresholdBased = isThresholdBased;
        this.factors = factors;
        this.daysUntilShort = daysUntilShort;
    }

    /** Newest forecast for a blood type, or null. */
    public static Forecast findLatest(ArrayList<Forecast> forecasts, String bloodType) {
        Forecast latest = null;
        for (Forecast f : forecasts) {
            if (f.bloodType.equals(bloodType) && (latest == null || f.forecastDate >= latest.forecastDate)) latest = f;
        }
        return latest;
    }

    /** FR-13: the factors behind the forecast, one per line. */
    public String explainFactors() { return factors; }

    /** For sorting High first (FR-12). */
    public int riskOrder() {
        if (riskLevel.equals("High")) return 0;
        if (riskLevel.equals("Medium")) return 1;
        return 2;
    }

    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setUnitsNeeded(int unitsNeeded) { this.unitsNeeded = unitsNeeded; }

    public String getBloodType() { return bloodType; }
    public long getForecastDate() { return forecastDate; }
    public String getRiskLevel() { return riskLevel; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public boolean isThresholdBased() { return isThresholdBased; }
    public int getDaysUntilShort() { return daysUntilShort; }
}
