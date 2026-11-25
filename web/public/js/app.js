/**
 * CampusFind Web — app.js
 * Main Application Controller, UI state machine, and workflow orchestrator
 */

const USERS = {
  muktheswar: {
    id: "user_muktheswar",
    name: "Gangula Muktheswar Reddy",
    roll: "2403A54023",
    email: "muktheswar.reddy@sru.edu.in",
    phone: "+91 98765 43210",
    initials: "MR"
  },
  akheeranandan: {
    id: "user_akheeranandan",
    name: "Akheeranandan",
    roll: "2403A53001",
    email: "akheeranandan@sru.edu.in",
    phone: "+91 91234 56789",
    initials: "AN"
  },
  security: {
    id: "user_security",
    name: "Campus Security Desk",
    roll: "SEC-DESK-01",
    email: "security@sru.edu.in",
    phone: "+91 94400 99887",
    initials: "SC"
  }
};

const CATEGORY_ASSET_MAP = {
  "Electronics": "assets/laptop_hp.svg",
  "ID Cards": "assets/id_card.svg",
  "Wallets": "assets/wallet.svg",
  "Keys": "assets/keys.svg",
  "Bottles": "assets/bottle.svg",
  "Backpacks": "assets/default_item.svg",
  "Others": "assets/default_item.svg"
};

const App = {
  currentUser: USERS.muktheswar,
  currentCategoryFilter: "ALL",
  currentLocationFilter: "ALL",
  currentStatusFilter: "ALL",
  searchQuery: "",
  html5QrScanner: null,
  activePendingVerification: null,
  currentFoundImageBase64: null,

  async init() {
    this.updateUserUI();
    this.setupEventListeners();
    this.setDefaultFormDates();
    await this.renderCatalog();
    await this.refreshAnalytics();
    console.log("CampusFind Web Application initialized successfully.");
  },

  // ── User Management ─────────────────────────────────────
  updateUserUI() {
    document.getElementById("navUserName").textContent = this.currentUser.name;
    document.getElementById("navUserRoll").textContent = this.currentUser.roll;
    document.getElementById("navUserAvatar").textContent = this.currentUser.initials;

    // Fill form owner fields
    const lostName = document.getElementById("lostOwnerName");
    const lostRoll = document.getElementById("lostOwnerRoll");
    const lostEmail = document.getElementById("lostOwnerEmail");
    if (lostName) lostName.value = this.currentUser.name;
    if (lostRoll) lostRoll.value = this.currentUser.roll;
    if (lostEmail) lostEmail.value = this.currentUser.email;

    const fName = document.getElementById("foundFinderName");
    const fRoll = document.getElementById("foundFinderRoll");
    if (fName) fName.value = this.currentUser.name;
    if (fRoll) fRoll.value = this.currentUser.roll;
  },

  openUserSwitcher() {
    document.getElementById("modalUserSwitcher").classList.add("active");
  },

  selectUser(key) {
    if (USERS[key]) {
      this.currentUser = USERS[key];
      this.updateUserUI();
      this.closeModals();
    }
  },

  setDefaultFormDates() {
    const today = new Date().toISOString().split("T")[0];
    const now = new Date();
    const timeStr = now.toTimeString().substring(0, 5);

    const lostDate = document.getElementById("lostDate");
    const lostTime = document.getElementById("lostTime");
    if (lostDate) lostDate.value = today;
    if (lostTime) lostTime.value = timeStr;

    const foundDate = document.getElementById("foundDate");
    const foundTime = document.getElementById("foundTime");
    if (foundDate) foundDate.value = today;
    if (foundTime) foundTime.value = timeStr;
  },

  // ── Navigation & View Switching ─────────────────────────
  switchView(viewName) {
    // Hide all views
    document.querySelectorAll(".app-view").forEach(v => v.classList.remove("active"));
    // Deactivate nav links
    document.querySelectorAll(".nav-link").forEach(l => l.classList.remove("active"));

    // Activate selected view
    const targetView = document.getElementById(`view-${viewName}`);
    if (targetView) targetView.classList.add("active");

    const activeLink = document.querySelector(`.nav-link[data-view="${viewName}"]`);
    if (activeLink) activeLink.classList.add("active");

    window.scrollTo({ top: 380, behavior: 'smooth' });

    if (viewName === 'analytics') {
      this.refreshAnalytics();
    }
  },

  // ── Event Listeners ─────────────────────────────────────
  setupEventListeners() {
    // Search input
    const searchInput = document.getElementById("catalogSearchInput");
    if (searchInput) {
      searchInput.addEventListener("input", (e) => {
        this.searchQuery = e.target.value.toLowerCase().trim();
        this.renderCatalog();
      });
    }

    // Location filter
    const locFilter = document.getElementById("catalogLocationFilter");
    if (locFilter) {
      locFilter.addEventListener("change", (e) => {
        this.currentLocationFilter = e.target.value;
        this.renderCatalog();
      });
    }

    // Status filter
    const statusFilter = document.getElementById("catalogStatusFilter");
    if (statusFilter) {
      statusFilter.addEventListener("change", (e) => {
        this.currentStatusFilter = e.target.value;
        this.renderCatalog();
      });
    }

    // Drag and drop for image upload
    const dropzone = document.getElementById("foundDropzone");
    if (dropzone) {
      dropzone.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropzone.style.borderColor = "var(--primary-light)";
      });
      dropzone.addEventListener("dragleave", (e) => {
        e.preventDefault();
        dropzone.style.borderColor = "rgba(255, 255, 255, 0.15)";
      });
      dropzone.addEventListener("drop", (e) => {
        e.preventDefault();
        dropzone.style.borderColor = "rgba(255, 255, 255, 0.15)";
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
          this.processImageFile(e.dataTransfer.files[0]);
        }
      });
    }
  },

  setCategoryFilter(category) {
    this.currentCategoryFilter = category;
    document.querySelectorAll(".pill-btn").forEach(btn => {
      if (btn.getAttribute("data-category") === category) {
        btn.classList.add("active");
      } else {
        btn.classList.remove("active");
      }
    });
    this.renderCatalog();
  },

  filterByMapZone(zoneName) {
    this.currentLocationFilter = zoneName;
    const locDropdown = document.getElementById("catalogLocationFilter");
    if (locDropdown) locDropdown.value = zoneName;
    this.switchView('catalog');
    this.renderCatalog();
  },

  // ── Catalog Rendering ───────────────────────────────────
  async renderCatalog() {
    const grid = document.getElementById("catalogItemsGrid");
    if (!grid) return;

    const items = await window.CampusFindAPI.getItems();

    // Filter items
    const filtered = items.filter(item => {
      // Category filter
      if (this.currentCategoryFilter !== "ALL" && item.category !== this.currentCategoryFilter) {
        return false;
      }
      // Location filter
      if (this.currentLocationFilter !== "ALL") {
        const itemLoc = (item.location || "").toLowerCase();
        const targetLoc = this.currentLocationFilter.toLowerCase();
        if (!itemLoc.includes(targetLoc)) return false;
      }
      // Status filter
      if (this.currentStatusFilter === "LOST" && item.type !== "LOST") return false;
      if (this.currentStatusFilter === "FOUND" && item.type !== "FOUND") return false;
      if (this.currentStatusFilter === "RETURNED" && item.status !== "Returned") return false;

      // Search query
      if (this.searchQuery) {
        const searchStr = `${item.itemName || ''} ${item.description || ''} ${item.location || ''} ${item.reporterName || ''} ${item.reporterRoll || ''} ${item.ownerVerificationCode || ''}`.toLowerCase();
        if (!searchStr.includes(this.searchQuery)) return false;
      }
      return true;
    });

    if (filtered.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; background: var(--bg-card); border-radius: var(--radius-md); border: 1px dashed var(--border-color);">
          <div style="font-size: 2.5rem; margin-bottom: 12px;">🔍</div>
          <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 6px;">No Items Match Your Filter Criteria</h3>
          <p style="color: var(--text-secondary); font-size: 0.9rem;">Try adjusting your keyword search, selecting "All Categories", or checking other campus locations.</p>
        </div>
      `;
      return;
    }

    grid.innerHTML = filtered.map(item => {
      const isLost = item.type === "LOST";
      const isReturned = item.status === "Returned";

      let badgeClass = isLost ? "badge-lost" : "badge-found";
      let badgeText = item.type;
      if (isReturned) {
        badgeClass = "badge-returned";
        badgeText = "RETURNED";
      }

      const displayImg = item.imageUrl || CATEGORY_ASSET_MAP[item.category] || "assets/default_item.svg";

      return `
        <div class="item-card">
          <div class="card-image-wrap">
            <span class="badge-type ${badgeClass}">${badgeText}</span>
            <img src="${displayImg}" alt="${item.itemName}" loading="lazy" onerror="this.src='assets/default_item.svg'">
          </div>

          <div class="card-content">
            <div class="card-category">${item.category || 'General Item'}</div>
            <h3 class="card-title">${item.itemName}</h3>
            <p class="card-desc">${item.description || 'No additional description provided.'}</p>

            <div class="card-meta">
              <div class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                <span>${item.location}</span>
              </div>
              <div class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                <span>${item.date} • ${item.time || 'Campus Time'}</span>
              </div>
              ${item.ownerVerificationCode ? `
                <div class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                  <span>Code: <strong style="color: var(--primary-light); font-family: var(--font-mono);">${item.ownerVerificationCode}</strong></span>
                </div>
              ` : ''}
            </div>

            <div class="card-actions">
              <button class="btn btn-secondary btn-sm" onclick="App.openItemDetail('${item.id}')">
                Details & Claim
              </button>
              ${item.ownerVerificationCode ? `
                <button class="btn btn-outline btn-sm" onclick="App.openOwnerBadge('${item.id}')" title="View Printable QR Badge">
                  QR Badge
                </button>
              ` : ''}
            </div>
          </div>
        </div>
      `;
    }).join("");
  },

  // ── Report Lost Submission ──────────────────────────────
  async handleLostSubmit(e) {
    e.preventDefault();

    const title = document.getElementById("lostTitle").value.trim();
    const category = document.getElementById("lostCategory").value;
    const location = document.getElementById("lostLocation").value;
    const date = document.getElementById("lostDate").value;
    const time = document.getElementById("lostTime").value;
    const desc = document.getElementById("lostDesc").value.trim();
    const phone = document.getElementById("lostOwnerPhone").value.trim();

    // Generate Owner Verification Code: CF-XXXXXX
    const ownerCode = QRHelper.generateOwnerCode();

    const newItem = {
      type: "LOST",
      itemName: title,
      category,
      location,
      date,
      time,
      description: desc,
      reporterName: this.currentUser.name,
      reporterRoll: this.currentUser.roll,
      reporterEmail: this.currentUser.email,
      reporterPhone: phone,
      ownerVerificationCode: ownerCode,
      imageUrl: CATEGORY_ASSET_MAP[category] || "assets/default_item.svg"
    };

    const saved = await window.CampusFindAPI.addItem(newItem);

    // Reset form
    document.getElementById("formReportLost").reset();
    this.setDefaultFormDates();
    this.updateUserUI();

    // Show Owner QR Badge Modal
    this.openOwnerBadge(saved.id);
    await this.renderCatalog();
    await this.refreshAnalytics();
  },

  // ── Image Auto-Compression (ImageHelper.java equivalent) ─
  handleImageUpload(e) {
    if (e.target.files && e.target.files[0]) {
      this.processImageFile(e.target.files[0]);
    }
  },

  processImageFile(file) {
    if (!file.type.startsWith("image/")) {
      alert("Please select an image file (JPG, PNG, WebP).");
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        // Compress to max 800px width/height and JPEG 0.7 quality (~25-35 KB)
        const canvas = document.createElement("canvas");
        const MAX_DIM = 800;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > MAX_DIM) {
            height = Math.round((height * MAX_DIM) / width);
            width = MAX_DIM;
          }
        } else {
          if (height > MAX_DIM) {
            width = Math.round((width * MAX_DIM) / height);
            height = MAX_DIM;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, width, height);

        // Convert to Base64 JPEG data URL
        const base64Data = canvas.toDataURL("image/jpeg", 0.72);
        this.currentFoundImageBase64 = base64Data;

        // Show preview
        const previewBox = document.getElementById("foundImgPreviewBox");
        const previewImg = document.getElementById("foundImgPreview");
        const dropzone = document.getElementById("foundDropzone");
        if (previewImg && previewBox) {
          previewImg.src = base64Data;
          previewBox.style.display = "block";
          if (dropzone) dropzone.style.display = "none";
        }
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  },

  removeFoundImage() {
    this.currentFoundImageBase64 = null;
    const previewBox = document.getElementById("foundImgPreviewBox");
    const dropzone = document.getElementById("foundDropzone");
    const fileInput = document.getElementById("foundFileInput");
    if (previewBox) previewBox.style.display = "none";
    if (dropzone) dropzone.style.display = "block";
    if (fileInput) fileInput.value = "";
  },

  // ── Report Found Submission & Matching Engine ───────────
  async handleFoundSubmit(e) {
    e.preventDefault();

    const title = document.getElementById("foundTitle").value.trim();
    const category = document.getElementById("foundCategory").value;
    const location = document.getElementById("foundLocation").value;
    const date = document.getElementById("foundDate").value;
    const time = document.getElementById("foundTime").value;
    const desc = document.getElementById("foundDesc").value.trim();

    const ownerCode = QRHelper.generateOwnerCode();
    const finalImage = this.currentFoundImageBase64 || CATEGORY_ASSET_MAP[category] || "assets/default_item.svg";

    const newItem = {
      type: "FOUND",
      itemName: title,
      category,
      location,
      date,
      time,
      description: desc,
      reporterName: this.currentUser.name,
      reporterRoll: this.currentUser.roll,
      reporterEmail: this.currentUser.email,
      ownerVerificationCode: ownerCode,
      imageUrl: finalImage
    };

    const saved = await window.CampusFindAPI.addItem(newItem);

    // Reset Form
    document.getElementById("formReportFound").reset();
    this.removeFoundImage();
    this.setDefaultFormDates();
    this.updateUserUI();

    // Run Real-Time Matching Engine against all active Lost items!
    const allItems = await window.CampusFindAPI.getItems();
    const activeLostItems = allItems.filter(i => i.type === "LOST" && i.status === "Active");
    const matches = MatchingEngine.findMatches(saved, activeLostItems);

    await this.renderCatalog();
    await this.refreshAnalytics();

    if (matches.length > 0) {
      // High-confidence match found!
      this.showMatchAlert(saved, matches[0]);
    } else {
      alert(`Found item "${title}" published successfully! Our matching engine will monitor incoming lost reports.`);
      this.switchView('catalog');
    }
  },

  showMatchAlert(foundItem, matchResult) {
    const lostItem = matchResult.candidate;
    const modal = document.getElementById("modalMatchAlert");
    const details = document.getElementById("matchAlertDetails");
    const emailBtn = document.getElementById("btnMatchSendEmail");

    details.innerHTML = `
      <div style="display: flex; gap: 14px; align-items: center; margin-bottom: 14px;">
        <div style="font-size: 2rem; font-weight: 900; color: var(--found-color);">${matchResult.percentage}% Match</div>
        <div>
          <div style="font-weight: 800; font-size: 1.1rem; color: #f8fafc;">Lost: ${lostItem.itemName}</div>
          <div style="font-size: 0.85rem; color: var(--text-secondary);">Reported by ${lostItem.reporterName} (${lostItem.reporterRoll})</div>
        </div>
      </div>

      <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 10px;">
        <strong>Matching Factors:</strong>
        <ul style="margin: 6px 0 0 20px; color: var(--text-secondary);">
          ${matchResult.reasons.map(r => `<li>${r}</li>`).join("")}
        </ul>
      </div>

      <div style="background: var(--bg-card); padding: 12px; border-radius: var(--radius-sm); font-size: 0.85rem;">
        <div><strong>Lost Location:</strong> ${lostItem.location}</div>
        <div><strong>Owner Verification Code:</strong> <code>${lostItem.ownerVerificationCode}</code></div>
      </div>
    `;

    // Create Pre-filled Email Subject and Body (Manual Intent)
    const emailSubject = `[CampusFind] Potential Match Found for your lost ${lostItem.itemName}`;
    const emailBody = `Hi ${lostItem.reporterName},\n\nA found item matching your report was just submitted on CampusFind:\n\nFound Item: ${foundItem.itemName}\nFound Location: ${foundItem.location}\nFinder Name: ${foundItem.reporterName} (${foundItem.reporterRoll})\nFinder Email: ${foundItem.reporterEmail}\n\nPlease visit the campus security desk or contact the finder with your Owner Verification Code (${lostItem.ownerVerificationCode}) to claim your item.\n\nRegards,\nCampusFind Automated Match Engine\nSR University`;

    const mailto = QRHelper.createMailtoLink(lostItem.reporterEmail, emailSubject, emailBody);
    emailBtn.setAttribute("href", mailto);

    modal.classList.add("active");
  },

  // ── Item Details & Claim Modal ──────────────────────────
  async openItemDetail(itemId) {
    const items = await window.CampusFindAPI.getItems();
    const item = items.find(i => i.id === itemId);
    if (!item) return;

    const modal = document.getElementById("modalItemDetail");
    const content = document.getElementById("modalDetailContent");
    const isFound = item.type === "FOUND";
    const isReturned = item.status === "Returned";
    const displayImg = item.imageUrl || CATEGORY_ASSET_MAP[item.category] || "assets/default_item.svg";

    content.innerHTML = `
      <div style="display: flex; gap: 20px; flex-wrap: wrap;">
        <div style="flex: 1; min-width: 240px;">
          <img src="${displayImg}" alt="${item.itemName}" style="width: 100%; border-radius: var(--radius-md); max-height: 240px; object-fit: cover; background: #000;">
        </div>

        <div style="flex: 1.4; min-width: 260px;">
          <div style="font-size: 0.75rem; font-weight: 800; color: var(--primary-light); text-transform: uppercase;">
            ${item.type} • ${item.category}
          </div>
          <h2 style="font-size: 1.4rem; font-weight: 800; margin: 4px 0 10px 0;">${item.itemName}</h2>
          <p style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 16px; line-height: 1.4;">${item.description}</p>

          <div style="font-size: 0.85rem; display: flex; flex-direction: column; gap: 6px; background: var(--bg-elevated); padding: 12px; border-radius: var(--radius-sm); margin-bottom: 16px;">
            <div><strong>Location:</strong> ${item.location}</div>
            <div><strong>Reported Date:</strong> ${item.date} • ${item.time || 'Campus Time'}</div>
            <div><strong>Reported By:</strong> ${item.reporterName} (${item.reporterRoll})</div>
            <div><strong>Status:</strong> <span style="color: ${isReturned ? 'var(--returned-color)' : (item.type === 'LOST' ? 'var(--lost-color)' : 'var(--found-color)')}; font-weight: bold;">${item.status}</span></div>
            ${item.ownerVerificationCode ? `<div><strong>Verification Code:</strong> <code>${item.ownerVerificationCode}</code></div>` : ''}
          </div>
        </div>
      </div>

      ${isFound && !isReturned ? `
        <div style="margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border-color);">
          <h3 style="font-size: 1.15rem; font-weight: 800; margin-bottom: 10px; color: var(--primary-light);">Claim This Item</h3>
          <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 14px;">
            Submit your claim description. A secure Claim QR code will be generated to authenticate your handover.
          </p>
          <div style="display: flex; flex-direction: column; gap: 10px;">
            <input type="text" class="form-input" id="claimProofInput" placeholder="Describe unique features or proof of ownership..." required>
            <div style="display: flex; gap: 10px;">
              <button class="btn btn-found" onclick="App.submitClaim('${item.id}', '${item.reporterEmail}', '${item.itemName}')" style="flex: 1;">
                Submit Claim & Generate QR Code
              </button>
            </div>
          </div>
          <div id="claimQrResultContainer" style="margin-top: 16px; display: none;"></div>
        </div>
      ` : ''}
    `;

    modal.classList.add("active");
  },

  async submitClaim(itemId, finderEmail, itemName) {
    const proofInput = document.getElementById("claimProofInput");
    const proof = proofInput ? proofInput.value.trim() : "";
    if (!proof) {
      alert("Please enter a short proof description of ownership.");
      return;
    }

    const claimPayload = QRHelper.generateClaimPayload(itemId);

    const claimData = {
      itemId,
      claimantName: this.currentUser.name,
      claimantRoll: this.currentUser.roll,
      claimantEmail: this.currentUser.email,
      proofDetails: proof,
      qrPayload: claimPayload
    };

    await window.CampusFindAPI.addClaim(claimData);

    const resultBox = document.getElementById("claimQrResultContainer");
    if (resultBox) {
      resultBox.style.display = "block";
      resultBox.innerHTML = `
        <div style="background: var(--bg-card); border: 1px solid var(--found-color); border-radius: var(--radius-sm); padding: 18px; text-align: center;">
          <h4 style="color: var(--found-color); font-weight: 800; margin-bottom: 6px;">Claim Generated Successfully!</h4>
          <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 14px;">Present this QR code to the finder or campus security:</p>
          <div id="claimQrCanvas" style="display: flex; justify-content: center; margin: 12px 0;"></div>
          <div style="font-family: var(--font-mono); font-size: 0.85rem; word-break: break-all; margin: 8px 0; color: var(--primary-light);">${claimPayload}</div>
          <a class="btn btn-primary btn-sm" href="${QRHelper.createMailtoLink(
            finderEmail,
            `[CampusFind] Claim Request for: ${itemName}`,
            `Hi Finder,\n\nI have claimed the ${itemName} on CampusFind.\nMy Roll No: ${this.currentUser.roll}\nProof: ${proof}\n\nMy Claim QR Payload: ${claimPayload}\n\nLet me know when we can meet for verification.\n\nThanks,\n${this.currentUser.name}`
          )}" target="_blank" style="margin-top: 10px;">
            Open Pre-Filled Email to Finder
          </a>
        </div>
      `;
      QRHelper.renderQR(document.getElementById("claimQrCanvas"), claimPayload, 160);
    }
  },

  // ── Owner QR Badge Modal ────────────────────────────────
  async openOwnerBadge(itemId) {
    const items = await window.CampusFindAPI.getItems();
    const item = items.find(i => i.id === itemId);
    if (!item) return;

    const modal = document.getElementById("modalOwnerBadge");
    document.getElementById("badgeItemTitle").textContent = item.itemName;
    document.getElementById("badgeOwnerCode").textContent = item.ownerVerificationCode || "CF-ONLINE";
    document.getElementById("badgeOwnerName").textContent = item.reporterName;
    document.getElementById("badgeOwnerRoll").textContent = item.reporterRoll;

    const container = document.getElementById("badgeQrContainer");
    QRHelper.renderQR(container, item.ownerVerificationCode || item.id, 180);

    modal.classList.add("active");
  },

  closeModals() {
    document.querySelectorAll(".modal-overlay").forEach(m => m.classList.remove("active"));
  },

  // ── QR Scanner & Handover Verification ──────────────────
  async startCameraScanner() {
    const readerDiv = document.getElementById("reader");
    if (!readerDiv) return;

    document.getElementById("btnStartCamera").style.display = "none";
    document.getElementById("btnStopCamera").style.display = "inline-flex";

    try {
      if (typeof Html5Qrcode === "undefined") {
        alert("Camera scanner library loading. Please try manual entry or wait a moment.");
        return;
      }

      this.html5QrScanner = new Html5Qrcode("reader");
      await this.html5QrScanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText) => {
          this.handleScannedCode(decodedText);
        },
        (error) => {
          // ignore stream parse frames
        }
      );
    } catch (err) {
      console.warn("Camera start failed, falling back to manual entry:", err);
      alert("Camera access was not granted or not available on this device. You can verify using manual code entry!");
      this.stopCameraScanner();
    }
  },

  async stopCameraScanner() {
    if (this.html5QrScanner) {
      try {
        await this.html5QrScanner.stop();
        this.html5QrScanner.clear();
      } catch (e) {}
      this.html5QrScanner = null;
    }
    const btnStart = document.getElementById("btnStartCamera");
    const btnStop = document.getElementById("btnStopCamera");
    if (btnStart) btnStart.style.display = "inline-flex";
    if (btnStop) btnStop.style.display = "none";
  },

  async handleManualVerify() {
    const input = document.getElementById("manualCodeInput");
    const code = input ? input.value.trim() : "";
    if (!code) {
      alert("Please enter a verification code (e.g. CF-8L2M9X).");
      return;
    }
    await this.handleScannedCode(code);
  },

  async handleScannedCode(payload) {
    const result = await window.CampusFindAPI.verifyPayload(payload);
    if (!result.verified) {
      alert(`Verification Failed:\n${result.error}`);
      return;
    }

    this.activePendingVerification = result;
    const card = document.getElementById("verifyResultCard");
    card.style.display = "block";

    document.getElementById("vItemName").textContent = result.item ? result.item.itemName : "Claimed Item";
    document.getElementById("vItemCategory").textContent = result.item ? result.item.category : "Campus Asset";
    document.getElementById("vOwnerName").textContent = result.ownerName;
    document.getElementById("vOwnerRoll").textContent = result.ownerRoll;
    document.getElementById("vCode").textContent = result.code;
    document.getElementById("vLocation").textContent = result.item ? result.item.location : "Campus Registry";

    if (result.item && result.item.status === "Returned") {
      document.getElementById("verifyResultSubtitle").textContent = "Item already marked Returned in registry.";
      document.getElementById("btnConfirmHandover").style.display = "none";
    } else {
      document.getElementById("verifyResultSubtitle").textContent = "Matches valid record in campus registry. Ready for handover.";
      document.getElementById("btnConfirmHandover").style.display = "inline-flex";
    }

    card.scrollIntoView({ behavior: "smooth", block: "center" });
  },

  dismissVerifyResult() {
    this.activePendingVerification = null;
    const card = document.getElementById("verifyResultCard");
    if (card) card.style.display = "none";
  },

  async confirmHandover() {
    if (!this.activePendingVerification || !this.activePendingVerification.item) {
      alert("No active pending item to confirm.");
      return;
    }

    const item = this.activePendingVerification.item;
    await window.CampusFindAPI.updateItemStatus(item.id, "Returned");

    alert(`Physical handover verified and confirmed!\n\nItem "${item.itemName}" has been officially marked as RETURNED to ${this.activePendingVerification.ownerName} (${this.activePendingVerification.ownerRoll}).`);

    this.dismissVerifyResult();
    await this.renderCatalog();
    await this.refreshAnalytics();
    this.switchView('analytics');
  },

  // ── Analytics & Map Metrics ─────────────────────────────
  async refreshAnalytics() {
    const stats = await window.CampusFindAPI.getStats();

    // Stats banner
    const totalEl = document.getElementById("statTotalItems");
    const returnedEl = document.getElementById("statReturnedItems");
    const rateEl = document.getElementById("statRecoveryRate");
    if (totalEl) totalEl.textContent = stats.total;
    if (returnedEl) returnedEl.textContent = stats.returnedCount;
    if (rateEl) rateEl.textContent = `${stats.recoveryRate}%`;

    // Map zone item counts
    const libCount = stats.locationCounts["Central Library 2nd Floor"] || stats.locationCounts["Central Library Reading Hall"] || 3;
    const canteenCount = stats.locationCounts["Main Canteen Food Court"] || stats.locationCounts["Main Canteen Cash Counter"] || 2;
    const techCount = stats.locationCounts["Tech Park Lab 304"] || 2;
    const blockACount = stats.locationCounts["Block-A Room 210"] || 2;
    const sportsCount = stats.locationCounts["Sports Complex Badminton Court"] || 1;

    const elLib = document.getElementById("mapCountLibrary");
    const elTech = document.getElementById("mapCountTechPark");
    const elCan = document.getElementById("mapCountCanteen");
    const elSports = document.getElementById("mapCountSports");
    const elBlockA = document.getElementById("mapCountBlockA");

    if (elLib) elLib.textContent = `${libCount} Items`;
    if (elTech) elTech.textContent = `${techCount} Items`;
    if (elCan) elCan.textContent = `${canteenCount} Items`;
    if (elSports) elSports.textContent = `${sportsCount} Items`;
    if (elBlockA) elBlockA.textContent = `${blockACount} Items`;

    // Hotspot list
    const hotspotsContainer = document.getElementById("hotspotsContainer");
    if (hotspotsContainer) {
      const sortedLocations = Object.entries(stats.locationCounts).sort((a, b) => b[1] - a[1]);
      hotspotsContainer.innerHTML = sortedLocations.slice(0, 5).map(([loc, count], idx) => `
        <div class="hotspot-item">
          <div class="hotspot-name">#${idx + 1} • ${loc}</div>
          <div class="hotspot-count">${count} Reports</div>
        </div>
      `).join("");
    }

    // Audit table
    const auditBody = document.getElementById("auditTableBody");
    if (auditBody) {
      const items = await window.CampusFindAPI.getItems();
      const returnedItems = items.filter(i => i.status === "Returned");
      auditBody.innerHTML = returnedItems.map(item => `
        <tr style="border-bottom: 1px solid var(--border-color);">
          <td style="padding: 12px 16px; font-weight: 700;">${item.itemName}</td>
          <td style="padding: 12px 16px;">${item.category}</td>
          <td style="padding: 12px 16px;">${item.reporterName} (${item.reporterRoll})</td>
          <td style="padding: 12px 16px;">${item.location}</td>
          <td style="padding: 12px 16px;"><code style="color: var(--primary-light); font-weight: bold;">${item.ownerVerificationCode || 'VERIFIED'}</code></td>
          <td style="padding: 12px 16px;"><span class="badge-type badge-returned" style="position: static; font-size: 0.7rem;">RETURNED</span></td>
        </tr>
      `).join("");
    }
  }
};

// Bootstrap on window load
window.addEventListener("DOMContentLoaded", () => {
  App.init();
});
