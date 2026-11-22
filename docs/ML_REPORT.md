# Qatra: shortage prediction model report (F2, FR-23, NFR-7)

## Model
- **Type:** linear regression per blood type, written in Java inside `ForecastingModule.updateModel()` (no extra class). It stands in for the scikit-learn regression named in the Phase 1 report.
- **Formula:** `units used per day = w0 + w1 × trend + w2 × weekend`. A second model predicts donor turnout the same way.
- **Training:** least squares (normal equations, solved by Gaussian elimination) with a small ridge term.
- **Calendar factors:** the forecast multiplies turnout by a factor for Ramadan (0.6), public holidays (0.7), summer (0.75) and school breaks (0.85) (`SeasonCalendar`). Units expiring on their expiry date are removed from expected stock.
- **Baseline:** the average daily usage over the last 28 days.
- **Fallback (FR-11):** blood types with fewer than 14 days of history use the safe-stock threshold and are labelled "threshold-based".

## Data
Synthetic data, 60 days per blood type (7 days for AB-, to show the fallback). It has the patterns the report describes: usage rising over time (0.8× to 1.2×), lower usage at weekends, and higher donor turnout at weekends. Random noise is ±1 unit per day.

## Evaluation (NFR-7)
- **Method:** train on the first 46 days and test on the last 14. The metric is Mean Absolute Error (units per day).

| Blood type | AI model MAE | Baseline MAE | Improvement | Used for forecast |
|---|---|---|---|---|
| O+ | 0.93 | 4.34 | 79% | AI model |
| O- | 0.93 | 1.54 | 40% | AI model |
| A+ | 0.96 | 3.26 | 71% | AI model |
| A- | 0.66 | 0.86 | 23% | AI model |
| B+ | 1.07 | 2.59 | 59% | AI model |
| B- | 1.01 | 1.21 | 17% | AI model |
| AB+ | 0.61 | 1.05 | 42% | AI model |
| AB- | – | – | – | threshold-based (7 days of history) |
| **Average** | **0.88** | **2.12** | **≈59%** | |

**Result:** the MAE is 59% below the baseline, which meets the NFR-7 target of at least 15%. Every blood type with enough history beats the baseline. The test `fr23_modelBeatsBaselineByAtLeast15Percent` checks this automatically.

## Retraining (FR-23)
- `ForecastingModule.updateModel()` runs on its own when the model is more than 7 days old.
- It trains a new model and compares its test MAE with the current model's. It **keeps the old model if the new one is worse**.
- Each run is written to the audit log.

## Limitations
- The data is synthetic, so the 59% shows the method works, not how accurate it would be at a real blood bank.
- Seasonal effects are rules, not learned. The 60-day history contains no Ramadan or summer period to learn them from.
- Prophet and a comparison of several models are future work once real or public data is connected.
