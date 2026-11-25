/**
 * CampusFind Web — qr-helper.js
 * QR generation, parsing, validation, and camera scanner controllers
 */

const QRHelper = {
  /**
   * Generates a 6-character alphanumeric Owner Verification Code: CF-XXXXXX
   */
  generateOwnerCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Avoid ambiguous 0, O, 1, I
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `CF-${code}`;
  },

  /**
   * Generates a Claim QR payload string: CAMPUSFIND_CLAIM_<itemId>_<timestamp>
   */
  generateClaimPayload(itemId) {
    const safeId = itemId || 'item_' + Date.now();
    return `CAMPUSFIND_CLAIM_${safeId}_${Date.now()}`;
  },

  /**
   * Renders a QR code into a container element (or canvas)
   * Uses qrcode-generator (window.qrcode) with graceful SVG fallback
   */
  renderQR(containerElement, text, size = 180) {
    if (!containerElement) return;
    containerElement.innerHTML = '';

    try {
      if (typeof qrcode === 'function') {
        // qrcode-generator syntax: qrcode(typeNumber, errorCorrectionLevel)
        // 0 = auto-detect type number, 'M' = Medium error correction
        const qr = qrcode(0, 'M');
        qr.addData(text);
        qr.make();
        const imgTag = qr.createImgTag(Math.floor(size / qr.getModuleCount()), 8);
        containerElement.innerHTML = imgTag;
        const img = containerElement.querySelector('img');
        if (img) {
          img.style.maxWidth = '100%';
          img.style.height = 'auto';
          img.style.borderRadius = '8px';
          img.style.background = '#ffffff';
          img.style.padding = '8px';
          img.style.boxShadow = '0 8px 24px rgba(0,0,0,0.35)';
        }
        return;
      }
    } catch (err) {
      console.warn('qrcode() render error, falling back to Canvas QR renderer:', err);
    }

    // Fallback simple visual QR-like representation if library fails
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, size, size);
    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('CampusFind QR', size / 2, size / 2 - 10);
    ctx.font = '10px monospace';
    ctx.fillText(text.slice(0, 18), size / 2, size / 2 + 10);
    containerElement.appendChild(canvas);
  },

  /**
   * Parses and validates a QR payload
   * Supports:
   *  - Route 1: CF-XXXXXX (Owner Verification Code)
   *  - Route 2: CAMPUSFIND_CLAIM_<itemId>_<timestamp> (Claim Payload)
   */
  parsePayload(rawPayload) {
    if (!rawPayload) return { valid: false, error: 'Empty payload' };
    const trimmed = rawPayload.trim();
    const upper = trimmed.toUpperCase();

    if (upper.startsWith('CF-')) {
      return {
        valid: true,
        type: 'OWNER_VERIFICATION',
        code: upper,
        raw: trimmed
      };
    }

    if (upper.startsWith('CAMPUSFIND_CLAIM_')) {
      const parts = trimmed.split('_');
      // CAMPUSFIND, CLAIM, <itemId>, <timestamp>
      const itemId = parts[2] || '';
      const timestamp = parts[3] || '';
      return {
        valid: true,
        type: 'CLAIM_VERIFICATION',
        itemId,
        timestamp,
        raw: trimmed
      };
    }

    // Generic fallback if user scans a plain item ID
    return {
      valid: true,
      type: 'GENERIC_CODE',
      code: trimmed,
      raw: trimmed
    };
  },

  /**
   * Formats mailto intent link with pre-populated subject & body
   */
  createMailtoLink(toEmail, subject, body) {
    const cleanEmail = encodeURIComponent(toEmail || '');
    const cleanSubject = encodeURIComponent(subject || '');
    const cleanBody = encodeURIComponent(body || '');
    return `mailto:${cleanEmail}?subject=${cleanSubject}&body=${cleanBody}`;
  }
};

if (typeof module !== 'undefined' && module.exports) {
  module.exports = QRHelper;
}
