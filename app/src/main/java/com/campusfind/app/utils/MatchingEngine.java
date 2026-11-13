package com.campusfind.app.utils;

import com.campusfind.app.models.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MatchingEngine — compares a newly submitted Found item against active Lost items (and vice-versa).
 *
 * Enhanced features:
 *   1. Punctuation removal & normalization (e.g. "bag," matches "bag")
 *   2. Plural/singular stemming (e.g. "keys" matches "key", "bottles" matches "bottle")
 *   3. Substring & prefix matching for compound words (e.g. "calculator" matches "calculators")
 *   4. Campus stopword filtering to prevent false positives on common filler words
 *   5. Scoring: 2+ keyword matches trigger an automatic match; 1 keyword + category/location/date also triggers.
 */
public class MatchingEngine {

    private static final int MIN_MATCH_SCORE = 2;

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "and", "for", "with", "from", "lost", "found", "item", "near",
            "campus", "room", "block", "floor", "color", "colour", "please", "help",
            "one", "two", "three", "my", "your", "has", "have", "had", "this", "that"
    ));

    /**
     * Find all items from the candidate list that match the given target item (score >= 2).
     */
    public static List<Item> findMatches(Item targetItem, List<Item> candidates) {
        List<Item> matches = new ArrayList<>();
        if (targetItem == null || candidates == null) return matches;

        for (Item candidate : candidates) {
            int score = computeMatchScore(targetItem, candidate);
            if (score >= MIN_MATCH_SCORE) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    /**
     * Compute match score between two items (0 to 5).
     */
    public static int computeMatchScore(Item item1, Item item2) {
        if (item1 == null || item2 == null) return 0;
        int score = 0;

        // Keywords from names & descriptions
        List<String> keywords = getMatchingKeywords(item1, item2);
        if (keywords.size() >= 2) {
            score += 2; // Strong keyword match alone qualifies!
        } else if (keywords.size() == 1) {
            score += 1;
        }

        // Category match
        if (sameCategory(item1.getCategory(), item2.getCategory())) {
            score += 1;
        }

        // Location match
        if (locationMatch(item1.getLocation(), item2.getLocation())) {
            score += 1;
        }

        // Date match
        if (dateMatch(item1.getDate(), item2.getDate())) {
            score += 1;
        }

        return score;
    }

    /**
     * Extracts overlapping keywords between two Item objects (names + descriptions).
     */
    public static List<String> getMatchingKeywords(Item item1, Item item2) {
        if (item1 == null || item2 == null) return new ArrayList<>();
        String text1 = (item1.getItemName() != null ? item1.getItemName() : "") + " "
                + (item1.getDescription() != null ? item1.getDescription() : "");
        String text2 = (item2.getItemName() != null ? item2.getItemName() : "") + " "
                + (item2.getDescription() != null ? item2.getDescription() : "");
        return getMatchingKeywords(text1, text2);
    }

    /**
     * Extracts overlapping keywords between two strings.
     * Cleans punctuation, removes stopwords, and checks plural/stem equivalences.
     */
    public static List<String> getMatchingKeywords(String text1, String text2) {
        List<String> matches = new ArrayList<>();
        if (text1 == null || text2 == null) return matches;

        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        for (String w1 : tokens1) {
            for (String w2 : tokens2) {
                if (wordsMatch(w1, w2) && !matches.contains(w1)) {
                    matches.add(w1);
                    break;
                }
            }
        }
        return matches;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;

        // Replace all non-alphanumeric characters with spaces
        String clean = text.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase().trim();
        String[] words = clean.split("\\s+");
        for (String w : words) {
            w = w.trim();
            if (w.length() >= 3 && !STOPWORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private static boolean wordsMatch(String w1, String w2) {
        if (w1.equals(w2)) return true;

        // Plural / singular checks: e.g. "key" <-> "keys", "watch" <-> "watches"
        if ((w1 + "s").equals(w2) || (w2 + "s").equals(w1)) return true;
        if ((w1 + "es").equals(w2) || (w2 + "es").equals(w1)) return true;

        // Substring check for longer words (length >= 4)
        if (w1.length() >= 4 && w2.length() >= 4) {
            if (w1.startsWith(w2) || w2.startsWith(w1)) return true;
            if (w1.contains(w2) || w2.contains(w1)) return true;
        }

        return false;
    }

    private static boolean sameCategory(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        String c1 = cat1.trim().toLowerCase();
        String c2 = cat2.trim().toLowerCase();
        return c1.equals(c2) || c1.contains(c2) || c2.contains(c1);
    }

    private static boolean locationMatch(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return false;
        String l1 = loc1.trim().toLowerCase();
        String l2 = loc2.trim().toLowerCase();
        if (l1.equals(l2) || l1.contains(l2) || l2.contains(l1)) return true;

        // Tokenized location match (e.g. "Central Library 2nd Floor" matches "Library")
        List<String> t1 = tokenize(l1);
        List<String> t2 = tokenize(l2);
        for (String w1 : t1) {
            for (String w2 : t2) {
                if (wordsMatch(w1, w2)) return true;
            }
        }
        return false;
    }

    private static boolean dateMatch(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.trim().equalsIgnoreCase(date2.trim());
    }
}
