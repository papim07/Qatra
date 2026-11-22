package com.example.qatra;

import java.io.Serializable;
import java.util.ArrayList;

public class Forecast implements Serializable {
    public static final String FACTOR_SEPARATOR = "\n";

    private String bloodType;
    private long forecastDate;
    private String riskLevel = "Low";
    private int unitsNeeded;
    private boolean isThresholdBased;
    private String factors;
    private int daysUntilShort;

    public Forecast(String bloodType, long forecastDate, boolean isThresholdBased,
                    String factors, int daysUntilShort) {
        this.bloodType = bloodType;
        this.forecastDate = forecastDate;
        this.isThresholdBased = isThresholdBased;
        this.factors = factors;
        this.daysUntilShort = daysUntilShort;
    }

    public static Forecast findLatest(ArrayList<Forecast> forecasts, String bloodType) {
        Forecast latest = null;
        for (Forecast f : forecasts) {
            if (f.bloodType.equals(bloodType) && (latest == null || f.forecastDate >= latest.forecastDate)) latest = f;
        }
        return latest;
    }

    public String explainFactors() { return factors; }

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
