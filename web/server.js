/**
 * CampusFind Web — server.js
 * Express REST API Server & Static Asset Host
 * Provides persistent JSON database storage, CORS, and QR verification endpoints.
 */

const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const DATA_DIR = path.join(__dirname, 'data');
const DB_FILE = path.join(DATA_DIR, 'database.json');

// Middleware
app.use(cors());
app.use(express.json({ limit: '15mb' })); // Support Base64 compressed image uploads
app.use(express.static(path.join(__dirname, 'public')));

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// Default Seed Data
const DEFAULT_DATABASE = {
  items: [
    {
      id: "item_lost_001",
      type: "LOST",
      itemName: "HP Pavilion Laptop (Matte Black)",
      category: "Electronics",
      location: "Central Library 2nd Floor",
      date: "2026-11-24",
      time: "14:30",
      description: "Matte black HP Pavilion 15-inch with blue Intel sticker on bottom left corner and minor scratch near touchpad. Left at study desk 14.",
      reporterName: "Gangula Muktheswar Reddy",
      reporterRoll: "2403A54023",
      reporterEmail: "muktheswar.reddy@sru.edu.in",
      reporterPhone: "+91 98765 43210",
      status: "Active",
      ownerVerificationCode: "CF-8L2M9X",
      imageUrl: "assets/laptop_hp.svg",
      createdAt: "2026-11-24T09:00:00Z"
    },
    {
      id: "item_found_002",
      type: "FOUND",
      itemName: "Black HP 15-inch Laptop Charger & Bag",
      category: "Electronics",
      location: "Central Library Reading Hall",
      date: "2026-11-24",
      time: "16:00",
      description: "Found HP laptop charger with black carrying pouch and a wireless mouse left unattended near cubicle 18.",
      reporterName: "Faiza Iram",
      reporterRoll: "FACULTY-CS",
      reporterEmail: "faiza.iram@sru.edu.in",
      reporterPhone: "+91 98480 12345",
      status: "Active",
      ownerVerificationCode: "CF-4K1N7Q",
      imageUrl: "assets/laptop_hp.svg",
      createdAt: "2026-11-24T10:30:00Z"
    },
    {
      id: "item_lost_003",
      type: "LOST",
      itemName: "SR University Student Identity Card",
      category: "ID Cards",
      location: "Main Canteen Food Court",
      date: "2026-11-25",
      time: "12:15",
      description: "Blue lanyard with SR University ID badge, CSE Branch. Name: Akheeranandan, Roll No: 2403A53001.",
      reporterName: "Akheeranandan",
      reporterRoll: "2403A53001",
      reporterEmail: "akheeranandan@sru.edu.in",
      reporterPhone: "+91 91234 56789",
      status: "Active",
      ownerVerificationCode: "CF-3R9P4W",
      imageUrl: "assets/id_card.svg",
      createdAt: "2026-11-25T06:45:00Z"
    },
    {
      id: "item_found_004",
      type: "FOUND",
      itemName: "Student ID Card with Blue Lanyard",
      category: "ID Cards",
      location: "Main Canteen Cash Counter",
      date: "2026-11-25",
      time: "13:00",
      description: "Found college ID card with lanyard handed over to the counter manager. CSE department.",
      reporterName: "Campus Security Team",
      reporterRoll: "SEC-DESK-01",
      reporterEmail: "security@sru.edu.in",
      reporterPhone: "+91 94400 99887",
      status: "Active",
      ownerVerificationCode: "CF-9X4Z1T",
      imageUrl: "assets/id_card.svg",
      createdAt: "2026-11-25T07:30:00Z"
    },
    {
      id: "item_lost_005",
      type: "LOST",
      itemName: "Leather Bi-fold Wallet (Brown Wildhorn)",
      category: "Wallets",
      location: "Tech Park Lab 304",
      date: "2026-11-23",
      time: "17:10",
      description: "Brown leather wallet containing metro pass, hostel key, and 2 passport photos. Crucial documents inside.",
      reporterName: "Rahul Sharma",
      reporterRoll: "2403A54012",
      reporterEmail: "rahul.s@sru.edu.in",
      reporterPhone: "+91 98765 11223",
      status: "Active",
      ownerVerificationCode: "CF-5T7V2K",
      imageUrl: "assets/wallet.svg",
      createdAt: "2026-11-23T11:40:00Z"
    },
    {
      id: "item_found_006",
      type: "FOUND",
      itemName: "Bike Keys with Pulsar Keychain",
      category: "Keys",
      location: "East Parking Lot Gate 2",
      date: "2026-11-25",
      time: "09:30",
      description: "Set of 2 keys with black rubber Bajaj Pulsar keychain found on bike stand pavement.",
      reporterName: "Venkatesh Rao",
      reporterRoll: "2403A54088",
      reporterEmail: "venkat.rao@sru.edu.in",
      reporterPhone: "+91 99887 76655",
      status: "Active",
      ownerVerificationCode: "CF-2M6Q8Y",
      imageUrl: "assets/keys.svg",
      createdAt: "2026-11-25T04:00:00Z"
    },
    {
      id: "item_lost_007",
      type: "LOST",
      itemName: "Milton Insulated Stainless Water Bottle (Cyan)",
      category: "Bottles",
      location: "Sports Complex Badminton Court",
      date: "2026-11-22",
      time: "18:00",
      description: "Cyan blue 750ml Milton thermosteel bottle with small dent at the base.",
      reporterName: "Sneha Patel",
      reporterRoll: "2403A53045",
      reporterEmail: "sneha.patel@sru.edu.in",
      reporterPhone: "+91 97654 32109",
      status: "Returned",
      ownerVerificationCode: "CF-1H8K3P",
      imageUrl: "assets/bottle.svg",
      createdAt: "2026-11-22T12:30:00Z",
      returnedAt: "2026-11-23T10:00:00Z"
    },
    {
      id: "item_lost_008",
      type: "LOST",
      itemName: "Casio FX-991EX Scientific Calculator",
      category: "Electronics",
      location: "Block-A Room 210",
      date: "2026-11-21",
      time: "11:45",
      description: "Casio ClassWiz FX-991EX scientific calculator with solar panel and engraved initials 'MR'.",
      reporterName: "Gangula Muktheswar Reddy",
      reporterRoll: "2403A54023",
      reporterEmail: "muktheswar.reddy@sru.edu.in",
      reporterPhone: "+91 98765 43210",
      status: "Returned",
      ownerVerificationCode: "CF-6J4W9L",
      imageUrl: "assets/calculator.svg",
      createdAt: "2026-11-21T06:15:00Z",
      returnedAt: "2026-11-22T08:00:00Z"
    }
  ],
  claims: [
    {
      id: "claim_001",
      itemId: "item_found_004",
      claimantName: "Akheeranandan",
      claimantRoll: "2403A53001",
      claimantEmail: "akheeranandan@sru.edu.in",
      proofDetails: "ID has my name Akheeranandan and roll number 2403A53001 on the front. Left it at the canteen cashier.",
      qrPayload: "CAMPUSFIND_CLAIM_item_found_004_1732532400000",
      status: "Pending",
      createdAt: "2026-11-25T08:00:00Z"
    }
  ]
};

// Database Read / Write Helper
function readDatabase() {
  try {
    if (!fs.existsSync(DB_FILE)) {
      fs.writeFileSync(DB_FILE, JSON.stringify(DEFAULT_DATABASE, null, 2), 'utf-8');
      return DEFAULT_DATABASE;
    }
    const raw = fs.readFileSync(DB_FILE, 'utf-8');
    return JSON.parse(raw);
  } catch (err) {
    console.error('Error reading database file, returning default:', err);
    return DEFAULT_DATABASE;
  }
}

function writeDatabase(data) {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf-8');
  } catch (err) {
    console.error('Error writing database file:', err);
  }
}

// ── API Routes ──────────────────────────────────────────────

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    app: 'CampusFind Web System',
    version: '2.0.0',
    university: 'SR University',
    timestamp: new Date().toISOString()
  });
});

// GET all items
app.get('/api/items', (req, res) => {
  const db = readDatabase();
  res.json(db.items || []);
});

// POST new item (Lost or Found)
app.post('/api/items', (req, res) => {
  const db = readDatabase();
  const newItem = {
    id: req.body.id || `item_${Date.now()}`,
    status: req.body.status || 'Active',
    createdAt: new Date().toISOString(),
    ...req.body
  };
  db.items.unshift(newItem);
  writeDatabase(db);
  res.status(201).json(newItem);
});

// PATCH item status (e.g. Return item)
app.patch('/api/items/:id/status', (req, res) => {
  const db = readDatabase();
  const index = db.items.findIndex(i => i.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: 'Item not found' });
  }

  db.items[index].status = req.body.status;
  if (req.body.status === 'Returned') {
    db.items[index].returnedAt = new Date().toISOString();
  }
  writeDatabase(db);
  res.json(db.items[index]);
});

// GET all claims
app.get('/api/claims', (req, res) => {
  const db = readDatabase();
  res.json(db.claims || []);
});

// POST new claim
app.post('/api/claims', (req, res) => {
  const db = readDatabase();
  const newClaim = {
    id: req.body.id || `claim_${Date.now()}`,
    status: req.body.status || 'Pending',
    createdAt: new Date().toISOString(),
    ...req.body
  };
  db.claims.unshift(newClaim);
  writeDatabase(db);
  res.status(201).json(newClaim);
});

// POST verify QR payload
app.post('/api/verify-qr', (req, res) => {
  const { payload } = req.body;
  if (!payload) {
    return res.status(400).json({ error: 'Missing payload parameter' });
  }

  const clean = payload.trim().toUpperCase();
  const db = readDatabase();

  // 1. Owner Code Check: CF-XXXXXX
  if (clean.startsWith('CF-')) {
    const match = db.items.find(i => (i.ownerVerificationCode || '').toUpperCase() === clean);
    if (match) {
      return res.json({
        verified: true,
        type: 'OWNER_CODE',
        item: match,
        ownerName: match.reporterName,
        ownerRoll: match.reporterRoll,
        ownerEmail: match.reporterEmail,
        code: clean
      });
    }
  }

  // 2. Claim QR Check: CAMPUSFIND_CLAIM_...
  const claimMatch = db.claims.find(c => (c.qrPayload || '').trim() === payload.trim());
  if (claimMatch) {
    const associatedItem = db.items.find(i => i.id === claimMatch.itemId) || null;
    return res.json({
      verified: true,
      type: 'CLAIM_QR',
      claim: claimMatch,
      item: associatedItem,
      ownerName: claimMatch.claimantName,
      ownerRoll: claimMatch.claimantRoll,
      ownerEmail: claimMatch.claimantEmail,
      proofDetails: claimMatch.proofDetails,
      code: payload
    });
  }

  // 3. Fallback Item ID Check
  const itemMatch = db.items.find(i => i.id.toUpperCase() === clean);
  if (itemMatch) {
    return res.json({
      verified: true,
      type: 'ITEM_ID',
      item: itemMatch,
      ownerName: itemMatch.reporterName,
      ownerRoll: itemMatch.reporterRoll,
      ownerEmail: itemMatch.reporterEmail,
      code: clean
    });
  }

  return res.status(404).json({
    verified: false,
    error: `No record matching "${payload}" was found in active university records.`
  });
});

// GET statistics
app.get('/api/stats', (req, res) => {
  const db = readDatabase();
  const items = db.items || [];
  const total = items.length;
  const lostCount = items.filter(i => i.type === 'LOST').length;
  const foundCount = items.filter(i => i.type === 'FOUND').length;
  const returnedCount = items.filter(i => i.status === 'Returned').length;
  const activeCount = items.filter(i => i.status === 'Active').length;
  const recoveryRate = total > 0 ? Math.round((returnedCount / (returnedCount + activeCount || 1)) * 100) : 85;

  const locationCounts = {};
  const categoryCounts = {};
  for (const item of items) {
    locationCounts[item.location] = (locationCounts[item.location] || 0) + 1;
    categoryCounts[item.category] = (categoryCounts[item.category] || 0) + 1;
  }

  res.json({
    total,
    lostCount,
    foundCount,
    returnedCount,
    activeCount,
    recoveryRate,
    locationCounts,
    categoryCounts
  });
});

// Fallback to index.html for SPA routing
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Server
app.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`🚀 CampusFind Web Server running at: http://localhost:${PORT}`);
  console.log(`📍 Serving UI from: ${path.join(__dirname, 'public')}`);
  console.log(`💾 Persistent JSON database at: ${DB_FILE}`);
  console.log(`=======================================================`);
});
