/**
 * CampusFind Web — api.js
 * Universal Data Layer: Automatically detects Express REST backend or uses persistent localStorage
 */

const SEED_CAMPUS_ITEMS = [
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
];

const SEED_CLAIMS = [
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
];

class CampusFindDataLayer {
  constructor() {
    this.storageKeyItems = "campusfind_items_v2";
    this.storageKeyClaims = "campusfind_claims_v2";
    this.apiBase = window.location.origin.includes("localhost") || window.location.origin.includes("127.0.0.1")
      ? "/api"
      : null;
    this.useApi = false;
    this.init();
  }

  async init() {
    // Check if backend API is reachable
    if (this.apiBase && !window.location.protocol.startsWith("file")) {
      try {
        const res = await fetch(`${this.apiBase}/health`, { signal: AbortSignal.timeout(1500) });
        if (res.ok) {
          this.useApi = true;
          console.log("Connected to CampusFind Node.js REST API Backend");
          return;
        }
      } catch (e) {
        // Fall through to localStorage
      }
    }

    // Initialize local storage seed if not present
    if (!localStorage.getItem(this.storageKeyItems)) {
      localStorage.setItem(this.storageKeyItems, JSON.stringify(SEED_CAMPUS_ITEMS));
    }
    if (!localStorage.getItem(this.storageKeyClaims)) {
      localStorage.setItem(this.storageKeyClaims, JSON.stringify(SEED_CLAIMS));
    }
    console.log("CampusFind DataLayer running in Browser LocalStorage mode (100% offline-ready)");
  }

  // ── Items ───────────────────────────────────────────────
  async getItems() {
    if (this.useApi) {
      try {
        const res = await fetch(`${this.apiBase}/items`);
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn("API fetch failed, falling back to local storage:", err);
      }
    }

    const raw = localStorage.getItem(this.storageKeyItems);
    return raw ? JSON.parse(raw) : SEED_CAMPUS_ITEMS;
  }

  async addItem(itemData) {
    const item = {
      id: "item_" + Date.now(),
      status: "Active",
      createdAt: new Date().toISOString(),
      ...itemData
    };

    if (this.useApi) {
      try {
        const res = await fetch(`${this.apiBase}/items`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(item)
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn("API add failed, saving to local storage:", err);
      }
    }

    const items = await this.getItems();
    items.unshift(item);
    localStorage.setItem(this.storageKeyItems, JSON.stringify(items));
    return item;
  }

  async updateItemStatus(itemId, newStatus) {
    if (this.useApi) {
      try {
        const res = await fetch(`${this.apiBase}/items/${itemId}/status`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: newStatus })
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn("API update failed, updating local storage:", err);
      }
    }

    const items = await this.getItems();
    const index = items.findIndex(i => i.id === itemId);
    if (index !== -1) {
      items[index].status = newStatus;
      if (newStatus === "Returned") {
        items[index].returnedAt = new Date().toISOString();
      }
      localStorage.setItem(this.storageKeyItems, JSON.stringify(items));
      return items[index];
    }
    return null;
  }

  // ── Claims ──────────────────────────────────────────────
  async getClaims() {
    if (this.useApi) {
      try {
        const res = await fetch(`${this.apiBase}/claims`);
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn("API claims fetch failed:", err);
      }
    }

    const raw = localStorage.getItem(this.storageKeyClaims);
    return raw ? JSON.parse(raw) : SEED_CLAIMS;
  }

  async addClaim(claimData) {
    const claim = {
      id: "claim_" + Date.now(),
      status: "Pending",
      createdAt: new Date().toISOString(),
      ...claimData
    };

    if (this.useApi) {
      try {
        const res = await fetch(`${this.apiBase}/claims`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(claim)
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn("API claim add failed:", err);
      }
    }

    const claims = await this.getClaims();
    claims.unshift(claim);
    localStorage.setItem(this.storageKeyClaims, JSON.stringify(claims));
    return claim;
  }

  // ── Verification ────────────────────────────────────────
  async verifyPayload(payload) {
    const clean = (payload || "").trim().toUpperCase();
    const items = await this.getItems();
    const claims = await this.getClaims();

    // 1. Check Owner Verification Code (CF-XXXXXX)
    if (clean.startsWith("CF-")) {
      const match = items.find(i => (i.ownerVerificationCode || "").toUpperCase() === clean);
      if (match) {
        return {
          verified: true,
          type: "OWNER_CODE",
          item: match,
          ownerName: match.reporterName,
          ownerRoll: match.reporterRoll,
          ownerEmail: match.reporterEmail,
          code: clean
        };
      }
    }

    // 2. Check Claim QR Payload (CAMPUSFIND_CLAIM_...)
    const claimMatch = claims.find(c => (c.qrPayload || "").trim() === payload.trim());
    if (claimMatch) {
      const associatedItem = items.find(i => i.id === claimMatch.itemId) || null;
      return {
        verified: true,
        type: "CLAIM_QR",
        claim: claimMatch,
        item: associatedItem,
        ownerName: claimMatch.claimantName,
        ownerRoll: claimMatch.claimantRoll,
        ownerEmail: claimMatch.claimantEmail,
        proofDetails: claimMatch.proofDetails,
        code: payload
      };
    }

    // 3. Check by Item ID
    const itemById = items.find(i => i.id.toUpperCase() === clean);
    if (itemById) {
      return {
        verified: true,
        type: "ITEM_ID",
        item: itemById,
        ownerName: itemById.reporterName,
        ownerRoll: itemById.reporterRoll,
        ownerEmail: itemById.reporterEmail,
        code: clean
      };
    }

    return {
      verified: false,
      error: `No item or claim matching "${payload}" was found in active records.`
    };
  }

  // ── Stats ───────────────────────────────────────────────
  async getStats() {
    const items = await this.getItems();
    const total = items.length;
    const lostCount = items.filter(i => i.type === "LOST").length;
    const foundCount = items.filter(i => i.type === "FOUND").length;
    const returnedCount = items.filter(i => i.status === "Returned").length;
    const activeCount = items.filter(i => i.status === "Active").length;
    const recoveryRate = total > 0 ? Math.round((returnedCount / (returnedCount + activeCount || 1)) * 100) : 85;

    // Hotspot breakdown
    const locationCounts = {};
    const categoryCounts = {};
    for (const item of items) {
      locationCounts[item.location] = (locationCounts[item.location] || 0) + 1;
      categoryCounts[item.category] = (categoryCounts[item.category] || 0) + 1;
    }

    return {
      total,
      lostCount,
      foundCount,
      returnedCount,
      activeCount,
      recoveryRate,
      locationCounts,
      categoryCounts
    };
  }
}

// Global instance
window.CampusFindAPI = new CampusFindDataLayer();
