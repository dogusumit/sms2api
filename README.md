# SMS2API - Automated SMS Forwarder for Android

**SMS2API** is a production-grade, background-resilient Android application that automatically captures incoming SMS messages, filters them based on customizable rules, and forwards them to a target REST API endpoint over secure HTTPS with at-least-once delivery guarantees.

---

## 🚀 Key Features

* **Real-Time SMS Interception:** Listens for `android.provider.Telephony.SMS_RECEIVED` broadcasts with high priority and automatic multipart message concatenation.
* **Deterministic Deduplication:** Generates unique `message_id` using SHA-256 (`sms_${timestamp}_${sha256_first_8_chars}`) from raw attributes (`${sender}|${body}|${timestamp}`).
* **Local Persistence & Queue:** Backed by Android Jetpack Room (`sms_records` table) guaranteeing zero message loss across network downtime, process death, or device restarts.
* **Historical Bulk Transfer:** One-tap import of all past inbox SMS with `INSERT OR IGNORE` to prevent re-forwarding delivered messages.
* **Security & Encryption:**
  * Credentials and tokens stored in `EncryptedSharedPreferences` backed by Android Keystore (AES-256 GCM).
  * Enforced TLS 1.2+ via Android Network Security Configuration.
  * Authentication options: None, HTTP Basic Auth, Bearer Token, and Custom Key-Value Headers (e.g. `X-API-Key`).
* **OS Survival & 24/7 Reliability:**
  * Foreground Service (`dataSync` type) with live status notification.
  * Auto-start on device reboot via `BootReceiver`.
  * Fallback periodic sync via `WorkManager` (every 15 minutes).
  * One-tap Battery Optimization exclusion prompt.
* **Modern UI:** Built with **Jetpack Compose** and **Material 3**, featuring dynamic metrics, interactive message logs, search, and instant retry actions.

---

## 📐 Architecture Overview

```
┌────────────────┐     ┌─────────────────────┐     ┌──────────────────────┐     ┌───────────────┐
│  Android OS    │     │  SmsReceiver /      │     │  Room Database       │     │ Target REST   │
│  (Incoming SMS)│────>│  Historical Scanner │────>│  (Status: PENDING)   │────>│ API Endpoint  │
└────────────────┘     └─────────────────────┘     └──────────────────────┘     └───────────────┘
                                                              │                         ▲
                                                              │ Retry & Sync            │ HTTP 200 OK
                                                              ▼                         │
                                                   ┌──────────────────────┐             │
                                                   │ WorkManager /        │─────────────┘
                                                   │ Foreground Service   │
                                                   └──────────────────────┘
```

---

## 📦 Payload Specification

```json
{
  "message_id": "sms_1711234567890_a1f4b8c9",
  "sender": "VODAFONE",
  "body": "tuketim borcunuz 767.50 tl olup 10 gün içerisinde ödemeniz gerekmektedir",
  "timestamp": 1711234567890
}
```

---

## 📊 Microsoft Office Online (Excel Online) Integration

SMS2API seamlessly connects with **Microsoft Office 365 (Excel Online)** using a serverless **Power Automate Cloud Flow (HTTP Webhook)**. Incoming SMS messages are automatically appended as rows in an Excel Online spreadsheet in real time with zero server maintenance.

Complete documentation, schemas, and step-by-step guides are available in:
👉 **[integrations/office_online_power_automate/](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/office_online_power_automate/README.md)**

* **[JSON Schema](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/office_online_power_automate/schemas/sms_request_schema.json):** Pre-built request schema for the Power Automate HTTP trigger.
* **[Power Automate Setup Guide](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/office_online_power_automate/flow/power_automate_guide.md):** Step-by-step instructions for creating the flow and configuring Excel Online actions.
* **[Office Script](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/office_online_power_automate/excel_template/SetupTable_OfficeScript.ts):** 1-click script to create and format the `SmsTable` in Excel Online.
* **[Formulas & Expressions](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/office_online_power_automate/flow/flow_expressions.txt):** Ready-to-use formulas for converting Unix timestamps to Turkey Time (UTC+3) and validating API keys.

---

## 📈 Google Sheets (Google E-Tablolar) Integration (%100 Free)

For standard personal accounts (`@gmail.com`) where Power Automate requires enterprise licensing, SMS2API includes a **100% free Google Sheets integration** powered by Google Apps Script.

Complete documentation and script:
👉 **[integrations/google_sheets/](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/google_sheets/README.md)**

* **[Google Apps Script Code](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/google_sheets/Code.gs):** Ready-to-paste webhook receiver script with deduplication, OTP extraction, and GMT+3 timestamp formatting.
* **[Setup & Deployment Guide](file:///c:/Users/dogus/repos/auto_sms_forwarder/integrations/google_sheets/google_sheets_guide.md):** 5-minute setup guide to deploy the Web App and connect it with SMS2API.

---

## 🛠️ Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/sms2api/app/
│   ├── SMS2ApiApplication.kt          # App entry, dependency container, notification channels
│   ├── data/
│   │   ├── model/                     # Room Entities, JSON models, Enums
│   │   │   ├── SmsRecord.kt
│   │   │   ├── SmsPayload.kt
│   │   │   ├── DeliveryStatus.kt
│   │   │   ├── AuthType.kt
│   │   │   └── FilterMode.kt
│   │   ├── local/                     # Room Database, DAO & EncryptedPreferences
│   │   │   ├── AppDatabase.kt
│   │   │   ├── SmsDao.kt
│   │   │   └── EncryptedPreferencesManager.kt
│   │   ├── network/                   # OkHttp HTTPS client, SHA-256 payload generator
│   │   │   ├── ApiClient.kt
│   │   │   └── PayloadGenerator.kt
│   │   └── repository/
│   │       └── SmsRepository.kt       # Unified data repository & inbox importer
│   ├── service/                       # Background services & receivers
│   │   ├── SmsForwarderService.kt     # Foreground service with persistent notification
│   │   ├── SmsReceiver.kt             # Incoming SMS broadcast receiver
│   │   └── BootReceiver.kt            # Device reboot receiver
│   ├── worker/                        # WorkManager jobs
│   │   ├── SmsForwardWorker.kt        # Immediate & retry dispatch worker
│   │   ├── SmsSyncWorker.kt           # 15-min periodic safety net worker
│   │   └── HistoricalImportWorker.kt  # Inbox historical bulk worker
│   └── ui/                            # Jetpack Compose UI (Material 3)
│       ├── MainActivity.kt            # Runtime permission handling & host
│       ├── theme/                     # Colors, typography, and theme
│       ├── viewmodel/MainViewModel.kt # State management & UI state flows
│       └── screens/
│           ├── MainScreen.kt          # Bottom navigation scaffold
│           ├── DashboardScreen.kt     # Live metrics, hero status card, action buttons
│           ├── LogsScreen.kt          # Filterable, searchable queue & delivery history
│           └── SettingsScreen.kt      # REST API, Auth, filtering & OS survival configuration
└── res/
    ├── drawable/                      # Vector assets & notification icons
    └── xml/
        └── network_security_config.xml # Strict TLS policy
```

---

## 🧪 Build and Test

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
