# Qatra: traceability (requirement → code)

| Req. | Implemented in (file → function) | Test |
|---|---|---|
| FR-1 Register Donor | `QatraDatabase.registerDonor`, `MainActivity.showRegister` | – |
| FR-2 Update Profile | `QatraDatabase.updateProfile`, `Donor.updateProfile`, `MainActivity.showProfile` | – |
| FR-3 Display History | `Donor.listDonationsNewestFirst`, `MainActivity.showHistory` | – |
| FR-4 Calculate Eligibility | `EligibilityModule.checkEligibility`, `calculateNextDate`; called from `MainActivity.showVisit` after a donation or deferral | `fr4_*` (5 tests) |
| FR-5 Display Eligibility | `EligibilityScreen.openScreen`, `displayStatus` | – |
| FR-6 Record Donation | `QatraDatabase.recordDonation`, `Donation.record` | – |
| FR-7 Store Stock Data | `QatraDatabase.enterStock` (validation), `InventorySystem.importStock`, `BloodBank.addRecord` | – |
| FR-8 Generate Forecast | `ForecastingModule.generateForecast`, `SeasonCalendar` | `fr9_fr10_*` |
| FR-9 Classify Risk | `ForecastingModule.classifyRisk` | `fr9_riskLevelsFromFirstDayBelowSafeLevel` |
| FR-10 Estimate Units | `ForecastingModule.estimateUnits` | `fr9_fr10_*` |
| FR-11 Apply Thresholds | `ForecastingModule.applyThreshold` | `fr11_shortHistoryIsThresholdBasedAndLabelled` |
| FR-12 Staff Dashboard | `ForecastingModule.getForecasts`, `StaffDashboard.openDashboard`, `displayForecasts` | `fr12_highRiskIsListedFirst` |
| FR-13 Forecast Factors | `Forecast.explainFactors`, `StaffDashboard.selectBloodType`, `displayDetails` | – |
| FR-14 Match Donors | `MatchingModule.matchDonors`, `isCompatible`, `MapsService.getDistance` | `fr14_*` (3 tests) |
| FR-15 Approve Appeal | `ForecastingModule.generateForecast` → `MatchingModule.prepareAppeal`; `AppealReviewScreen.approveAppeal`, `BloodBankStaff.reviewAppeal` | `fr15_highRiskForecastCreatesPendingAppeal` |
| FR-16 Donation Alerts | `MatchingModule.sendAppeal` → `NotificationService.sendAlerts` → `Alert` (bloodType, location, bookingLink, channel) | `uc7_mainFlow_*` |
| FR-17 Book Appointment | `MainActivity.showBooking`, `QatraDatabase.book`, `Alert.book` | – |
| FR-18 Send Reminder | `EligibilityScreen.showRequests` (reminder within 24 h, shown in-app) | – |
| FR-19 Response Rate | `QatraDatabase.appealResponse`, `AppealReviewScreen.displayConfirmation` | – |
| FR-20 Manage Rules | `Administrator.manageRules`, `QatraDatabase.applyRule`, `addStaff`, `log` | `fr4_ruleChangeAppliesImmediately` |
| FR-21 Widen Search | `MatchingModule.widenSearch`, `nextDistance`, `AppealReviewScreen.displaySummary` | `uc7_3a_*` |
| FR-22 Supply Report | `QatraDatabase.supplyReport(days)`, `MainActivity.showReport` | – |
| FR-23 Update Model | `ForecastingModule.updateModel` (train, test against baseline, keep the better model) | `fr23_*` |
| FR-24 Record Consent | `MainActivity.showRegister` (consent text + checkbox), `QatraDatabase.registerDonor` | – |
| FR-25 Confirm Eligibility | `MainActivity.showVisit` ("Provisional" until confirm or reject), `QatraDatabase.rejectAtVisit` | – |
| UC-2 2a / 3a | `EligibilityScreen.askHealthQuestions`, `EligibilityModule.recordHealthAnswers`; `displayError` | `uc2_3a_*` |
| UC-6 2a / 2b | `applyThreshold`; `getForecasts` + "Last forecast from" in `StaffDashboard.displayForecasts` | `fr11_*` |
| UC-7 4a / 5a | `MatchingModule.rejectAppeal`; `sendAppeal` → `Appeal.markPending` | `uc7_4a_*`, `uc7_5a_*` |
| DR-1 | `EligibilityRule`, `EligibilityModule.calculateNextDate` | `fr4_*` |
| DR-2 | `MatchingModule.COMPATIBLE`, `isCompatible` | `fr14_redCellCompatibility` |
| DR-3 | No points or rewards anywhere; consent text says voluntary and unpaid | – |
| NFR-7 | `ForecastingModule.overallImprovement`, `MainActivity.showModel` | `fr23_*` |
| NFR-8 (at rest) | `QatraDatabase.save` / `load` (AES-GCM, Android Keystore) | – (needs a device) |
| NFR-9 | `PasswordHasher` (SHA-256 + salt), `User.logIn` | – |
| NFR-10 | `MainActivity.showHome` (role-based screens) | – |
| NFR-11 | `QatraDatabase.log`, `MainActivity.showVisit` ("Viewed donor record") | – |
| NFR-14 / NFR-16 | `Lang`, `Ui` (RTL, large text, content descriptions) | – |
| NFR-15 | `MainActivity.showAdminHome` (rule editor) | – |
