# Qatra – A Predictive Blood Donation Matching System

**Course:** SWE 401 – Group 20 – Abu Dhabi University
**Instructor:** Dr. Murad Al Rajab

## Team members

| Name | Student ID |
|---|---|
| Rashed Mohammed Alhassani | 1091155 |
| Mohammed Fahim Bhuiyan | 1097593 |

## Problem

Blood banks usually find out they are short of a blood type only once the shortage has already happened. Appeals are then sent to every donor on the list, even though many of them have the wrong blood type or are not yet eligible to donate. This causes late appeals, donor fatigue and delays to treatment. Blood cannot be manufactured, and red cells last only 42 days, so supply has to be planned ahead. In the UAE, shortages happen often in summer, during holidays and in Ramadan.

Qatra predicts shortages before they happen and alerts only the right donors: those who are eligible, have a compatible blood type and live nearby.

## Key features

- **F1 – Eligibility tracking:** works out who can donate now from the last donation date, deferral reasons and the recovery interval. It also shows each donor's next eligible date.
- **F2 – Shortage prediction (AI):** a linear regression model forecasts the stock of each blood type for the next 14 days. It gives a risk level (High, Medium or Low), the units needed and the factors behind the forecast (trend, weekends, Ramadan, holidays, summer, school breaks, expiry). Blood types with fewer than 14 days of history use a safe-stock threshold instead.
- **F3 – Targeted donor matching and alerts:** a High-risk forecast creates an appeal. When staff approve it, alerts go only to eligible, compatible and nearby donors, with an urgency level and one-tap appointment booking. If there are too few donors, the search area can be widened.
- **F4 – Staff dashboard:** shows forecasts, stock risk, appeals, booked visits and the appeal response rate.
- **Administrator tools:** manage eligibility rules, view supply reports (7, 30 or 60 days), view the AI model's accuracy, view the audit log and add staff accounts.
- **Accessibility and security:** English and Arabic (RTL), large text, encrypted storage, hashed passwords and role-based screens.

## Technologies

| Area | Technology |
|---|---|
| Language | Java |
| Platform | Android (minSdk 24, targetSdk 37), AppCompat |
| IDE / build | Android Studio, Gradle (Kotlin DSL) |
| AI model | Linear regression in Java (least squares), compared with a 28-day average baseline using MAE |
| Storage | Serialized Java objects in an AES-GCM encrypted file (key kept in the Android Keystore) |
| Security | SHA-256 salted password hashing, role-based screens, audit log |
| Testing | JUnit 4 |

The Maps, Notification and Inventory services are simulated inside the app (`MapsService`, `NotificationService`, `InventorySystem`).

## Prerequisites

- **Android Studio** (latest version). It includes the JDK and the Android SDK.
- An **Android emulator** or an **Android phone** with Android 7.0 (API 24) or newer.
- **Git**, to clone the repository (optional: you can also download the ZIP).

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/papim07/Qatra.git
   ```
   Or, in Android Studio: **File → New → Project from Version Control** and paste `https://github.com/papim07/Qatra.git`.
2. Open the `Qatra` folder in Android Studio.
3. Wait for **Gradle sync** to finish. It downloads the needed libraries automatically.

## Configuration

No API keys, server or database setup are needed. Everything runs on the device.

- **Android SDK path:** Android Studio creates `local.properties` with your SDK path automatically on first open. It is not committed to the repository.
- **Demo data:** loaded automatically the first time the app runs. The administrator can restore it with **Reset demo data**.
- **Eligibility rules:** the administrator can change the recovery interval (in days) for each donation type from the admin screen.
- **Language and text size:** switch between English and Arabic, and change text size with the **A±** buttons.

## How to run

1. Create an emulator if you don't have one: **Device Manager → + → Create Virtual Device** (for example, a Pixel with a recent Android image). Or connect a phone with USB debugging turned on.
2. Select the **app** configuration and your device at the top of Android Studio.
3. Click **Run ▶** (Shift + F10).

To build an installable APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

**Demo accounts:**

| Role | Username | Password |
|---|---|---|
| Blood bank staff | `staff` | `staff123` |
| Administrator | `admin` | `admin123` |
| Donor (eligible, O-) | `ahmed` | `1234` |
| Donor (new) | `fatima` | `1234` |

## How to test

**Unit tests (JUnit 4):** 19 tests in `app/src/test/java/com/example/qatra/QatraTest.java`. They cover eligibility (FR-4), risk classification (FR-9 to FR-12), AI model accuracy (FR-23), donor matching (FR-14) and the Send Appeal extensions (widen search, reject, notification down).

- In Android Studio: right-click `QatraTest.java` and choose **Run 'QatraTest'**.
- From a terminal in the project folder:
  ```
  gradlew test
  ```
  On Windows PowerShell, use `.\gradlew test`.

**Manual test:**
1. Log in as `staff`. O- should show **High** risk, and a Pending appeal should exist.
2. Open the appeal and approve it. Alerts are sent to matched donors.
3. Log in as `ahmed`. The alert appears, and you can book a slot.
4. Log back in as `staff` and open the visit to record the donation. Ahmed's next eligible date is updated.

## Project structure

```
app/src/main/java/com/example/qatra/
  MainActivity.java          app entry point, login and navigation
  EligibilityScreen.java     donor screens            (boundary)
  StaffDashboard.java        forecasts dashboard      (boundary)
  AppealReviewScreen.java    review / send appeals    (boundary)
  EligibilityModule.java     eligibility logic        (control)
  ForecastingModule.java     AI forecasting           (control)
  MatchingModule.java        donor matching, appeals  (control)
  User, Donor, BloodBankStaff, Administrator, Donation, Deferral,
  EligibilityRule, Stock, BloodBank, Forecast, Appeal, Alert   (entities)
  QatraDatabase, Ui, Lang, SeasonCalendar, PasswordHasher      (supporting)
app/src/test/java/com/example/qatra/QatraTest.java   unit tests
docs/   GAP_REPORT.md, ML_REPORT.md, TRACEABILITY.md
```
