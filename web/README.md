# CampusFind Web — University Smart Lost & Found Network

A modern, responsive, full-stack web application designed for university campuses (SR University) to streamline lost item recovery through **automated keyword matching**, **owner verification QR codes (`CF-XXXXXX`)**, **claim authorization QR payloads**, and **physical handover authentication**.

---

## 🌟 Key Features

1. **Live Campus Catalog & Multi-Tier Filter Engine**:
   - Real-time instant search across item titles, descriptions, reporter roll numbers, and categories.
   - Filter pills: Electronics, ID Cards, Wallets & Money, Keys, Bottles, Backpacks & Books, Others.
   - Status filters: Active Lost, Active Found, and Returned / Reunited.
   - Campus Zone dropdown filter (Central Library, Tech Park, Main Canteen, Block-A, Sports Complex, etc.).

2. **Report Lost Item & Owner QR Generation**:
   - Detailed lost item registration form with date, time, and campus location.
   - Generates a unique **6-character Owner Verification Code (`CF-XXXXXX`)** on the fly.
   - Displays a printable / saveable **Owner Verification QR Badge**.

3. **Report Found Item & In-Browser Image Compression**:
   - Drag-and-drop or camera file upload.
   - Client-side auto-compression of uploaded photos into lightweight Base64 JPEGs (~25–35 KB), mirroring Android's `ImageHelper.java`.
   - **Real-Time Automated Keyword Matching**: Port of `MatchingEngine.java` running client-side with tokenization, stopword cleaning, plural/singular stemming, and weighted scoring.
   - Instant **Match Alert Modal** with a one-click pre-filled email button (`mailto:`) to notify the lost item owner.

4. **Item Details & Claim Workflow**:
   - Full zoomable preview of item photos and security metadata.
   - "Claim This Item" form generating a timestamped Claim QR payload: `CAMPUSFIND_CLAIM_<itemId>_<timestamp>`.
   - Direct pre-filled email to the finder or security desk.

5. **Handover Verification Station (Security / Finder Portal)**:
   - Live web camera scanner via `html5-qrcode`.
   - Manual code entry for `CF-XXXXXX` and `CAMPUSFIND_CLAIM_...`.
   - Authenticates owner identity against database records and provides a one-click **"Confirm Physical Handover & Mark Returned"** button.

6. **Interactive Campus Zone Map & Analytics**:
   - Vector SVG floorplan of SR University with interactive clickable zones.
   - Live recovery success meter and zone activity ranking.
   - Verified handovers audit log table.

7. **Zero-Setup Universal Compatibility**:
   - Runs out-of-the-box by double-clicking `public/index.html` (pure offline `localStorage` mode).
   - Or run as a full-stack REST API application using Node.js Express (`server.js`).

---

## 🚀 Quick Start Guide

### Option 1: Full-Stack Mode (Recommended)
```bash
# Navigate to project folder
cd campusfind-web

# Install dependencies
npm install

# Start the Express server
npm start
```
Open **`http://localhost:3000`** in your browser.

### Option 2: Zero-Server Standalone Mode
Double-click `public/index.html` in your file explorer or open it in Chrome, Edge, Safari, or Firefox. The data layer will automatically use persistent browser `localStorage`.

---

## 📡 REST API Endpoints (Express Server)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health status and uptime |
| `GET` | `/api/items` | List all reported lost and found items |
| `POST` | `/api/items` | Submit a new lost or found item |
| `PATCH` | `/api/items/:id/status` | Update item status (e.g. `Returned`) |
| `GET` | `/api/claims` | List all active ownership claims |
| `POST` | `/api/claims` | Submit an item ownership claim |
| `POST` | `/api/verify-qr` | Validate `CF-XXXXXX` or claim QR payload |
| `GET` | `/api/stats` | Retrieve campus recovery metrics |

---

## 📂 Project Structure

```
campusfind-web/
├── server.js              # Express REST API & static server
├── package.json           # Dependencies and scripts
├── README.md              # Project documentation
├── data/
│   └── database.json      # Persistent JSON file database
└── public/
    ├── index.html         # Modern SPA layout
    ├── css/
    │   └── style.css      # Dark campus theme & responsive styles
    ├── js/
    │   ├── app.js         # Core UI state machine & routing
    │   ├── api.js         # Dual-mode data layer (API / localStorage)
    │   ├── matching.js    # Client-side MatchingEngine
    │   ├── qr-helper.js   # QR generator & scanner helper
    │   ├── qrcode.min.js  # Offline client QR generator
    │   └── html5-qrcode.min.js # Offline camera QR scanner
    └── assets/
        ├── laptop_hp.svg
        ├── id_card.svg
        ├── wallet.svg
        ├── keys.svg
        ├── bottle.svg
        ├── calculator.svg
        └── default_item.svg
```

---

## 👨‍💻 Project Authors
- **Gangula Muktheswar Reddy** (Roll: `2403A54023`)
- **Akheeranandan** (Roll: `2403A53001`)
- **Guide**: Ms. Faiza Iram, Assistant Professor
- **Institution**: SR University
