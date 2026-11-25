/**
 * CampusFind Web — MatchingEngine.js
 * High-precision client-side matching engine ported from Android MatchingEngine.java
 *
 * Capabilities:
 *  1. Punctuation removal & whitespace normalization
 *  2. Plural/singular stemming ("keys" <-> "key", "bottles" <-> "bottle")
 *  3. Substring & prefix matching for compound words ("calculator" <-> "calculators")
 *  4. Campus stopword filtering to prevent false positives
 *  5. Multi-tier scoring: 2+ keyword matches (+2), 1 keyword (+1), category match (+1),
 *     location match (+1), date match (+1). Score >= 2 qualifies as a match!
 */

const STOPWORDS = new Set([
  "the", "and", "for", "with", "from", "lost", "found", "item", "near",
  "campus", "room", "block", "floor", "color", "colour", "please", "help",
  "one", "two", "three", "my", "your", "has", "have", "had", "this", "that"
]);

const MatchingEngine = {
  MIN_MATCH_SCORE: 2,

  /**
   * Tokenize text into normalized lowercase tokens >= 3 chars, without stopwords
   */
  tokenize(text) {
    if (!text) return [];
    const clean = text.replace(/[^a-zA-Z0-9\s]/g, " ").toLowerCase().trim();
    const words = clean.split(/\s+/);
    const tokens = [];
    for (const w of words) {
      const trimmed = w.trim();
      if (trimmed.length >= 3 && !STOPWORDS.has(trimmed)) {
        tokens.push(trimmed);
      }
    }
    return tokens;
  },

  /**
   * Check if two words match (exact, plural/singular, or substring)
   */
  wordsMatch(w1, w2) {
    if (w1 === w2) return true;

    // Plural / singular checks
    if ((w1 + "s") === w2 || (w2 + "s") === w1) return true;
    if ((w1 + "es") === w2 || (w2 + "es") === w1) return true;

    // Substring / prefix check for words >= 4 chars
    if (w1.length >= 4 && w2.length >= 4) {
      if (w1.startsWith(w2) || w2.startsWith(w1)) return true;
      if (w1.includes(w2) || w2.includes(w1)) return true;
    }

    return false;
  },

  /**
   * Extracts overlapping keywords between two text strings
   */
  getMatchingKeywords(text1, text2) {
    const matches = [];
    if (!text1 || !text2) return matches;

    const tokens1 = this.tokenize(text1);
    const tokens2 = this.tokenize(text2);

    for (const w1 of tokens1) {
      for (const w2 of tokens2) {
        if (this.wordsMatch(w1, w2) && !matches.includes(w1)) {
          matches.push(w1);
          break;
        }
      }
    }
    return matches;
  },

  /**
   * Check if two categories match
   */
  sameCategory(cat1, cat2) {
    if (!cat1 || !cat2) return false;
    const c1 = cat1.trim().toLowerCase();
    const c2 = cat2.trim().toLowerCase();
    return c1 === c2 || c1.includes(c2) || c2.includes(c1);
  },

  /**
   * Check if two locations match
   */
  locationMatch(loc1, loc2) {
    if (!loc1 || !loc2) return false;
    const l1 = loc1.trim().toLowerCase();
    const l2 = loc2.trim().toLowerCase();
    if (l1 === l2 || l1.includes(l2) || l2.includes(l1)) return true;

    const t1 = this.tokenize(l1);
    const t2 = this.tokenize(l2);
    for (const w1 of t1) {
      for (const w2 of t2) {
        if (this.wordsMatch(w1, w2)) return true;
      }
    }
    return false;
  },

  /**
   * Check if two dates match
   */
  dateMatch(d1, d2) {
    if (!d1 || !d2) return false;
    return d1.trim().toLowerCase() === d2.trim().toLowerCase();
  },

  /**
   * Compute match score between two items (0 to 5) and detail breakdown
   */
  computeMatch(targetItem, candidateItem) {
    if (!targetItem || !candidateItem) return { score: 0, percentage: 0, reasons: [] };

    let score = 0;
    const reasons = [];

    const text1 = `${targetItem.name || targetItem.itemName || ''} ${targetItem.description || ''}`;
    const text2 = `${candidateItem.name || candidateItem.itemName || ''} ${candidateItem.description || ''}`;

    const keywords = this.getMatchingKeywords(text1, text2);
    if (keywords.length >= 2) {
      score += 2;
      reasons.push(`Strong keyword match: "${keywords.join(', ')}"`);
    } else if (keywords.length === 1) {
      score += 1;
      reasons.push(`Keyword match: "${keywords[0]}"`);
    }

    if (this.sameCategory(targetItem.category, candidateItem.category)) {
      score += 1;
      reasons.push(`Category match: ${targetItem.category}`);
    }

    if (this.locationMatch(targetItem.location, candidateItem.location)) {
      score += 1;
      reasons.push(`Location proximity: ${candidateItem.location}`);
    }

    if (this.dateMatch(targetItem.date, candidateItem.date)) {
      score += 1;
      reasons.push(`Matching date: ${candidateItem.date}`);
    }

    // Convert score (0 - 5) to percentage (up to 100%)
    const percentage = Math.min(100, Math.round((score / 4) * 100));

    return {
      score,
      isMatch: score >= this.MIN_MATCH_SCORE,
      percentage,
      keywords,
      reasons
    };
  },

  /**
   * Find all matching items from candidates list
   */
  findMatches(targetItem, candidates) {
    if (!targetItem || !candidates || !Array.isArray(candidates)) return [];

    const results = [];
    for (const candidate of candidates) {
      // Don't match item with itself
      if (candidate.id && targetItem.id && candidate.id === targetItem.id) continue;
      // Lost should match Found and vice versa
      if (candidate.type && targetItem.type && candidate.type === targetItem.type) continue;

      const matchData = this.computeMatch(targetItem, candidate);
      if (matchData.isMatch) {
        results.push({
          candidate,
          ...matchData
        });
      }
    }

    // Sort by highest match score/percentage first
    results.sort((a, b) => b.score - a.score || b.percentage - a.percentage);
    return results;
  }
};

if (typeof module !== 'undefined' && module.exports) {
  module.exports = MatchingEngine;
}
