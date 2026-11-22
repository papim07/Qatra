# Qatra: gap report (code vs. report Phases 1–3 and Phase 3 UML)

The code is an Android app in Java (package `com.example.qatra`): 29 classes plus `QatraTest`.

## 1. Platform (not changed, needs a team decision)
| Spec | Current state | Decision |
|---|---|---|
| Flutter donor app, React staff dashboard, FastAPI, PostgreSQL, Prophet/scikit-learn, FCM, Docker | One native Android app (Java). Staff and admin screens are in the same app. Data is stored in an encrypted file on the device. The model is a hand-written linear regression. | **Kept as is.** A rewrite to that stack is out of scope for "refine the existing codebase". Describe this as the prototype implementation and list the full stack as the target architecture. |

## 2. Class diagram gaps
| Class / operation | Before | Change made |
|---|---|---|
| `EligibilityModule.checkEligibility(donorId)` | Took a `Donor` object | Now takes `donorId` |
| `EligibilityModule.recordHealthAnswers(donorId, answers)` | Took a `Donor` | Now takes `donorId`, then re-runs step 3 |
| `Donor.listDonationsAndDeferrals()` | Two separate methods | Added; returns `{donations, deferrals}` |
| `Donor.dateOfBirth` | Birth year (int) | Full date (FR-1) |
| `Administrator.manageRules(rule)` | Took (rule, days, age) | Takes a changed `EligibilityRule`; `QatraDatabase.applyRule` applies and logs it |
| `MatchingModule.matchDonors / widenSearch / sendAppeal (appealId)` | Took an `Appeal` object | Now take `appealId` |
| `ForecastingModule.classifyRisk(forecast, safeLevel)`, `estimateUnits(forecast)` | Took numbers or arrays | Take a `Forecast` |
| `Forecast` expected stock per day | Not produced | Calculated by `generateForecast()` as an output (not stored, so Forecast keeps simple types) |
| `ForecastingModule.getForecasts()` | Regenerated every time | Loops over the 8 types calling `Stock.currentLevel` and `Forecast.findLatest`, as in the sequence diagram |

Small differences kept on purpose (Java needs the list or donor passed in):
- `Forecast.findLatest(list, type)`
- `Stock.currentLevel(list, type)`
- `EligibilityRule.loadCurrentRules(list, type)`
- `Donation.record(donor)`

## 3. Requirement gaps fixed
| Req. | Before | Change made |
|---|---|---|
| FR-1 | Birth year only | Date of birth dd/mm/yyyy, exact 18+ check |
| FR-4 | Status computed only when shown | `checkEligibility` runs right after staff record a donation or deferral |
| FR-8 | Forecast rebuilt on every screen open | Generated once per day (and after new stock data), stored with its date |
| FR-15 | Staff had to tap "Prepare appeal" | A **High** risk forecast now creates a Pending appeal automatically |
| UC-6 2b | Missing | If today's forecast can't be made, the last one is shown with "Last forecast from <date>" |
| FR-13 | Didn't mention turnout | Factors now say "donor turnout" |
| FR-22 | Fixed 30 days | Period chooser: 7, 30 or 60 days |
| FR-23 | Model replaced on every retrain | Keeps the old model if the new one is worse |
| NFR-8 (at rest) | Plain file | AES-GCM encryption, key in Android Keystore |
| NFR-16 | No screen-reader labels | Content descriptions on icon-only elements |
| Tests | None | `QatraTest`: 19 JUnit tests, all passing |

## 4. Still simulated or not done
| Item | State |
|---|---|
| Notification Service (FCM/SMS), FR-16/18 | Simulated: alerts and reminders appear inside the app |
| Maps Service, FR-14 | Simulated: a distance table per area |
| Inventory System import, FR-7 | Simulated: generates realistic daily records |
| Prophet / scikit-learn | Replaced by an equivalent linear regression in Java (see ML_REPORT.md) |
| Encryption in transit (NFR-8) | Not applicable: no network in the prototype |
| NFR-1–6 performance/availability, NFR-13 iOS, NFR-18 bias review | Need the server version; not testable here |
| FR-20 changing a user's *role* | Admin can add staff users and change rules; moving an existing user between roles is not built |

## 5. Features not in the spec
| Feature | Decision |
|---|---|
| Demo-account buttons on the login screen | Kept (team decision: demo support) |
| "Reset demo data" on the admin screen | Kept (team decision: demo support) |
| "Prepare appeal" button for Medium risk | **Removed**: appeals are created only by High-risk forecasts (FR-15) |
| "Retrain model now" button | **Removed**: the model retrains weekly only (FR-23) |
| Large-text (A±) toggle | Kept: required by NFR-16 (large text support) |

## 6. Kept to the class diagram (no extra classes or rule fields)
- The AI model lives inside `ForecastingModule.updateModel()`. There is no separate model class.
- `EligibilityRule` has only `donationType` and `recoveryIntervalDays`. The 18+ check (FR-1) is a fixed constant in `EligibilityModule`.

Attributes added because the report's requirements need them (add them to the class diagram):
| Class | Attribute | Why |
|---|---|---|
| Forecast | `/daysUntilShort` (derived) | F2 output: "first day below safe level" |
| Stock | `unitsDonated`, `unitsExpiring` | FR-7: store turnout and expiry |
| Alert | `status`, `appointmentDate`, `appointmentTime` | FR-17: save the booked slot; FR-19: count bookings |
| Donor | `healthChecked` | UC-2 extension 2a: remember the health questions were answered |

## 7. Simple types only (course UML rules)
- `Forecast.factors` is a `String` (one factor per line). The expected stock per day is calculated by `generateForecast()` and is not stored.
- `BloodBank.safeStockLevel` is one `int`: the number of days of supply to keep (5). `safeLevelFor(bloodType)` turns it into units using that type's average daily usage.
