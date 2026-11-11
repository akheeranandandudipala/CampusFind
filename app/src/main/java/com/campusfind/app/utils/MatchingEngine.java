package com.campusfind.app.utils;

import com.campusfind.app.models.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchingEngine — compares a newly submitted Found item against all active Lost items
 * and returns matches that score 2 or more matching criteria.
 *
 * Matching criteria (each scores 1 point):
 *   1. Same category
 *   2. Item name keyword overlap (any word in common, case-insensitive)
 *   3. Same location (case-insensitive contains check)
 *   4. Same date
 *
 * A match is triggered when score >= 2.
 */
public class MatchingEngine {

    private static final int MIN_MATCH_SCORE = 2;

    /**
     * Find all lost items that match the given found item (score >= 2).
     *
     * @param foundItem   The newly submitted found item
     * @param allLostItems All active lost items from the database
     * @return List of matching lost items (with reporterEmail set for notification)
     */
    public static List<Item> findMatches(Item foundItem, List<Item> allLostItems) {
        List<Item> matches = new ArrayList<>();
        if (foundItem == null || allLostItems == null) return matches;

        for (Item lostItem : allLostItems) {
            int score = computeMatchScore(foundItem, lostItem);
            if (score >= MIN_MATCH_SCORE) {
                matches.add(lostItem);
            }
        }
        return matches;
    }

    /**
     * Compute match score between a found item and a lost item.
     * Returns a number from 0 to 4.
     */
    public static int computeMatchScore(Item foundItem, Item lostItem) {
        int score = 0;

        // Criterion 1: Same category
        if (sameCategory(foundItem.getCategory(), lostItem.getCategory())) {
            score++;
        }

        // Criterion 2: Item name keyword overlap
        if (nameKeywordMatch(foundItem.getItemName(), lostItem.getItemName())) {
            score++;
        }

        // Criterion 3: Location overlap
        if (locationMatch(foundItem.getLocation(), lostItem.getLocation())) {
            score++;
        }

        // Criterion 4: Same date
        if (dateMatch(foundItem.getDate(), lostItem.getDate())) {
            score++;
        }

        return score;
    }

    /**
     * Extracts overlapping keywords between two item names (ignoring short stopwords < 3 chars).
     */
    public static List<String> getMatchingKeywords(String name1, String name2) {
        List<String> matches = new ArrayList<>();
        if (name1 == null || name2 == null) return matches;
        String[] words1 = name1.trim().toLowerCase().split("\\s+");
        String[] words2 = name2.trim().toLowerCase().split("\\s+");
        for (String w1 : words1) {
            if (w1.length() < 3) continue;
            for (String w2 : words2) {
                if (w1.equals(w2) && !matches.contains(w1)) {
                    matches.add(w1);
                }
            }
        }
        return matches;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static boolean sameCategory(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        return cat1.trim().equalsIgnoreCase(cat2.trim());
    }

    private static boolean nameKeywordMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String[] words1 = name1.trim().toLowerCase().split("\\s+");
        String[] words2 = name2.trim().toLowerCase().split("\\s+");
        for (String w1 : words1) {
            if (w1.length() < 3) continue; // skip very short words like "a", "of"
            for (String w2 : words2) {
                if (w1.equals(w2)) return true;
            }
        }
        return false;
    }

    private static boolean locationMatch(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return false;
        String l1 = loc1.trim().toLowerCase();
        String l2 = loc2.trim().toLowerCase();
        return l1.contains(l2) || l2.contains(l1) || l1.equalsIgnoreCase(l2);
    }

    private static boolean dateMatch(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.trim().equalsIgnoreCase(date2.trim());
    }
}
