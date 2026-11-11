# CampusFind 🎒🔍
> **Find Lost. Return Found.** — A smart, student-centric Lost & Found management platform for university campuses.

---

## 📌 Overview

**CampusFind** connects students who have lost valuable items on campus with students or campus staff who have found them. It reduces lost property friction through automated keyword-based matching, QR-code verified handovers, and multi-photo inspection.

---

## ✨ Features

- **🎒 Report Lost & Found**:
  - Categorize items (Electronics, Bags, IDs, Books, Keys, Accessories, etc.).
  - Campus location tagging, date pickers, and detailed descriptions.
  - Multi-photo capture and gallery upload.
- **🎯 Smart Keyword & Attribute Matching Engine**:
  - Automatically compares reported items against active listings across 4 criteria (Category, Name Keywords, Location, and Date).
  - Highlights overlapping keywords and surfaces direct contact details.
  - Prompted manual email notification: Finder/Owner can review details and send an email notification directly with pre-filled match data.
- **📱 QR Code Verification & Handover**:
  - Generates secure unique owner QR codes via ZXing engine.
  - In-app QR scanner to verify authentic ownership before handing over found items.
- **🔒 Secure Claims System**:
  - Claim tracking and status management (`Pending`, `Approved`, `Returned`).
- **☁️ Dual-Layer Architecture**:
  - Local SQLite database for offline caching and fast querying.
  - Firebase Firestore synchronization for real-time campus-wide updates.
- **🎨 Modern UI & Branding**:
  - Clean Material 3 design with adaptive squircle launcher icons and custom brand typography.

---

## 🛠 Tech Stack

- **Platform**: Android (minSdk 24, targetSdk 34)
- **Language**: Java 17
- **UI & Components**: AndroidX, Google Material Components 3, ViewBinding / ConstraintLayout
- **Barcode & QR Engine**: ZXing Embedded (`com.journeyapps:zxing-android-embedded`)
- **Backend & Cloud**: Firebase Firestore, Google Services
- **CI/CD**: GitHub Actions automated APK build & artifact publishing

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK Platform 34

### Building Locally

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Nandhu954/Akheeranandan.git
   cd Akheeranandan
   ```

2. **Open in Android Studio** and let Gradle sync dependencies.

3. **Build Debug APK via Gradle**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.
