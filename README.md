# Qatra – A Predictive Blood Donation Matching System

SWE 401 – Group 20 – Abu Dhabi University
Instructor: Dr. Murad Al Rajab

## Team Members

| Name | Student ID | Role |
|---|---|---|
| Rashed Mohammed Alhassani | 1091155 | Scrum Master |
| Mohammed Fahim Bhuiyan | 1097593 | Product Backlog Manager |

## Problem

Blood banks often find out that a blood type is running low only after the shortage has started. They then send messages to all donors, even those who have the wrong blood type or are not allowed to donate yet. This wastes time and makes donors ignore future messages.

Qatra solves this by predicting shortages early and contacting only the donors who can help.

## Key Features

1. **Eligibility check:** donors can see if they are allowed to donate and when they can donate next.
2. **Shortage prediction (AI):** the app predicts the stock of each blood type for the next 14 days and shows the risk level (High, Medium or Low).
3. **Donor matching:** when a shortage is expected, the app finds donors who are eligible, have a matching blood type and live nearby.
4. **Alerts and booking:** staff approve the appeal, donors get an alert and can book an appointment.
5. **Staff dashboard:** shows stock levels, forecasts, appeals and booked visits.
6. **Administrator tools:** change eligibility rules, view reports and add staff accounts.
7. **Arabic and English**, with a larger text option.

## Technologies

- **Java** – programming language
- **Android Studio** – development tool
- **Android SDK** – to build the mobile app
- **Gradle** – to build the project
- **Linear regression** (written in Java) – the AI model for predicting shortages
- **JUnit 4** – for unit testing

## Prerequisites

Before you start, you need:

- **Android Studio** (latest version)
- An **Android emulator** or an **Android phone** (Android 7.0 or newer)
- **Git** (optional, to download the project)

## Installation

1. Download the project:
   ```
   git clone https://github.com/papim07/Qatra.git
   ```
   Or click the green **Code** button on GitHub and choose **Download ZIP**, then unzip it.
2. Open **Android Studio** and choose **Open**, then select the `Qatra` folder.
3. Wait for Android Studio to finish loading the project (Gradle sync).

## Configuration

No extra setup is needed. The app works offline and stores its data on the device.

- Sample data is added automatically the first time the app runs.
- You can switch the language using the **ع** button and change the text size using the **A±** button.

## How to Run

1. In Android Studio, open **Device Manager** and create an emulator (for example, a Pixel phone), or connect your Android phone.
2. Choose the device at the top of the window.
3. Click the green **Run ▶** button.
4. Log in with one of the demo accounts:

| Role | Username | Password |
|---|---|---|
| Staff | `staff` | `staff123` |
| Administrator | `admin` | `admin123` |
| Donor | `ahmed` | `1234` |
| New donor | `fatima` | `1234` |

## How to Test

**Unit tests:**

1. In Android Studio, open `app/src/test/java/com/example/qatra/QatraTest.java`.
2. Right-click the file and choose **Run 'QatraTest'**.
3. All 19 tests should pass.

**Manual test:**

1. Log in as `staff`. The O- blood type should show **High** risk.
2. Open the appeal and click **Approve & send**.
3. Log out and log in as `ahmed`. You should see the donation request and be able to book an appointment.

## More Information

- Screenshots: [docs/screenshots](docs/screenshots)
- UML diagrams: [docs/diagrams](docs/diagrams)
- Project plan (Gantt chart): [docs/planning](docs/planning)
- Test results: [docs/testing](docs/testing)
