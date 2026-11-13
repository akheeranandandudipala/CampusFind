package com.campusfind.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusfind.app.R;
import com.campusfind.app.adapters.ItemAdapter;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.FirestoreHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchActivity — searches Lost and Found items from Firebase Firestore.
 *
 * ════════════════════════════════════════════════════════════════
 * KEY DESIGN DECISIONS:
 *
 *  1. BOTH collections are fetched on load (lost_items + found_items).
 *     This means a single swipe-to-refresh loads everything.
 *
 *  2. BROWSE mode (no query): show only the selected tab's items.
 *  3. SEARCH mode (query typed): show results from BOTH collections.
 *     → User A searching "ID Card" will see Found items even if
 *       they're on the "Lost" tab.
 *
 *  4. Keyword matching: OR logic per word.
 *     "College ID" → tokens ["college","id"] → "id card" contains "id" → MATCH ✓
 *     "Block 1"    → tokens ["block","1"]   → location "block 1" matches → MATCH ✓
 *
 *  5. Empty state is hidden during loading — never shows "No results"
 *     before data arrives.
 * ════════════════════════════════════════════════════════════════
 */
public class SearchActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────
    private MaterialToolbar       toolbar;
    private TextInputEditText     etSearchQuery;
    private AutoCompleteTextView  actvFilterCategory;
    private TabLayout             tabLayoutItemType;
    private SwipeRefreshLayout    swipeRefresh;
    private RecyclerView          rvSearchResults;
    private LinearLayout          layoutEmptyState;

    private ItemAdapter adapter;

    // ─── Data caches (both collections loaded once, filtered client-side) ────
    private final List<Item> allLostItems  = new ArrayList<>();   // from lost_items
    private final List<Item> allFoundItems = new ArrayList<>();   // from found_items
    private final List<Item> displayList   = new ArrayList<>();   // shown in RecyclerView

    // ─── State ───────────────────────────────────────────────────────────────
    private boolean isLostTab       = true;
    private boolean isFetchingLost  = false;   // true while Firestore call in progress
    private boolean isFetchingFound = false;
    private boolean initialLoadDone = false;   // becomes true after first successful fetch
    private String  selectedCategory = "All Categories";
    private String  currentQuery     = "";

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupToolbar();
        setupCategoryFilter();
        setupTabs();
        setupRecyclerView();
        setupSearchInput();
        setupSwipeRefresh();

        // Hide empty state initially — data hasn't loaded yet
        layoutEmptyState.setVisibility(View.GONE);

        fetchBothCollections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-fetch when user navigates back (new reports may exist)
        fetchBothCollections();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View setup
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        toolbar            = findViewById(R.id.toolbarSearch);
        etSearchQuery      = findViewById(R.id.etSearchQuery);
        actvFilterCategory = findViewById(R.id.actvFilterCategory);
        tabLayoutItemType  = findViewById(R.id.tabLayoutItemType);
        swipeRefresh       = findViewById(R.id.swipeRefreshSearch);
        rvSearchResults    = findViewById(R.id.rvSearchResults);
        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryFilter() {
        String[] categories = getResources().getStringArray(R.array.filter_categories_array);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categories);
        actvFilterCategory.setAdapter(catAdapter);
        actvFilterCategory.setText(categories[0], false);

        actvFilterCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
            applyFilters(); // No Firestore read — filter cached data instantly
        });
    }

    private void setupTabs() {
        tabLayoutItemType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLostTab = (tab.getPosition() == 0);
                applyFilters(); // Both collections already cached — no new fetch needed
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(this, displayList, item -> {
            Intent intent = new Intent(SearchActivity.this, ItemDetailActivity.class);
            intent.putExtra("ITEM_EXTRA", item);
            startActivity(intent);
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchInput() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                // Filter cached data instantly — no network call needed
                applyFilters();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::fetchBothCollections);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data Loading — Firestore
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches BOTH lost_items AND found_items from Firestore simultaneously.
     *
     * Both fetches run in parallel. When BOTH complete, applyFilters() is called.
     * This ensures:
     *   • Items from ALL users on ALL devices are visible
     *   • Searching shows results from both Lost and Found without extra taps
     *   • Tab switching is instant (no extra network call needed)
     */
    private void fetchBothCollections() {
        swipeRefresh.setRefreshing(true);
        layoutEmptyState.setVisibility(View.GONE); // Never show "No results" while loading

        isFetchingLost  = true;
        isFetchingFound = true;

        // ── Fetch Lost items ──────────────────────────────────────────────────
        FirestoreHelper.getInstance().getLostItems(new FirestoreHelper.ItemsCallback() {
            @Override
            public void onSuccess(List<Item> items) {
                allLostItems.clear();
                allLostItems.addAll(items);
                isFetchingLost = false;
                onFetchComplete();
            }

            @Override
            public void onError(String message) {
                // Network error — keep previously cached lost items
                isFetchingLost = false;
                onFetchComplete();
            }
        });

        // ── Fetch Found items (in parallel) ───────────────────────────────────
        FirestoreHelper.getInstance().getFoundItems(new FirestoreHelper.ItemsCallback() {
            @Override
            public void onSuccess(List<Item> items) {
                allFoundItems.clear();
                allFoundItems.addAll(items);
                isFetchingFound = false;
                onFetchComplete();
            }

            @Override
            public void onError(String message) {
                isFetchingFound = false;
                onFetchComplete();
            }
        });
    }

    /**
     * Called each time one of the two Firestore fetches finishes.
     * Only applies filters and stops the refresh indicator when BOTH are done.
     */
    private void onFetchComplete() {
        if (isFetchingLost || isFetchingFound) {
            return; // The other fetch is still running — wait for it
        }

        // Seamlessly merge local SQLite items so offline or newly added items always appear
        DatabaseHelper db = com.campusfind.app.database.DatabaseHelper.getInstance(this);
        try {
            List<Item> localLost = db.getLostItems(null, null);
            for (Item local : localLost) {
                boolean exists = false;
                for (Item item : allLostItems) {
                    if (local.getItemId() == item.getItemId() ||
                            (local.getItemName() != null && local.getItemName().equalsIgnoreCase(item.getItemName())
                                    && local.getDate() != null && local.getDate().equalsIgnoreCase(item.getDate()))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    allLostItems.add(local);
                }
            }

            List<Item> localFound = db.getFoundItems(null, null);
            for (Item local : localFound) {
                boolean exists = false;
                for (Item item : allFoundItems) {
                    if (local.getItemId() == item.getItemId() ||
                            (local.getItemName() != null && local.getItemName().equalsIgnoreCase(item.getItemName())
                                    && local.getDate() != null && local.getDate().equalsIgnoreCase(item.getDate()))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    allFoundItems.add(local);
                }
            }
        } catch (Exception ignored) {}

        initialLoadDone = true;
        applyFilters();
        swipeRefresh.setRefreshing(false);

        if (allLostItems.isEmpty() && allFoundItems.isEmpty() && initialLoadDone) {
            Toast.makeText(this,
                "Tip: If you see no items, check Firestore rules in Firebase Console.",
                Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtering — runs entirely on cached data (no network)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Decides which items to show based on current tab, query, and category.
     *
     * ┌────────────────────────────────────────────────────────────────┐
     * │ SEARCH mode (query typed):  show from BOTH Lost + Found        │
     * │ BROWSE mode (no query):     show only the selected tab         │
     * └────────────────────────────────────────────────────────────────┘
     */
    private void applyFilters() {
        String q = currentQuery.trim().toLowerCase();

        // Build the source list
        List<Item> source = new ArrayList<>();
        if (!q.isEmpty()) {
            // SEARCH — include items from both collections
            source.addAll(allLostItems);
            source.addAll(allFoundItems);
        } else {
            // BROWSE — show only the selected tab
            source.addAll(isLostTab ? allLostItems : allFoundItems);
        }

        // Filter source
        List<Item> filtered = new ArrayList<>();
        for (Item item : source) {
            if (passesFilters(item, q)) {
                filtered.add(item);
            }
        }

        displayList.clear();
        displayList.addAll(filtered);
        adapter.updateList(displayList);
        updateEmptyState();
    }

    /**
     * Returns true if the item passes the category filter AND keyword filter.
     *
     * Keyword matching: OR-logic per word.
     *   Query "College ID" → tokens ["college","id"]
     *   Item "ID Card"     → blob contains "id" → MATCH ✓
     *
     * Category: case-insensitive equality check.
     */
    private boolean passesFilters(Item item, String lowerQuery) {

        // 1. Category filter (case-insensitive)
        if (!selectedCategory.equalsIgnoreCase("All Categories")) {
            String cat = safe(item.getCategory()).trim();
            if (!cat.equalsIgnoreCase(selectedCategory.trim())) {
                return false;
            }
        }

        // 2. Text / keyword filter
        if (!lowerQuery.isEmpty()) {
            // Build searchable blob from all relevant fields
            String blob = safe(item.getItemName()).toLowerCase()    + " "
                        + safe(item.getCategory()).toLowerCase()    + " "
                        + safe(item.getDescription()).toLowerCase() + " "
                        + safe(item.getLocation()).toLowerCase()    + " "
                        + safe(item.getReporterName()).toLowerCase();

            // Split query into words; the item matches if ANY word appears in the blob
            String[] words = lowerQuery.split("\\s+");
            boolean matched = false;
            for (String word : words) {
                if (!word.isEmpty() && blob.contains(word)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Empty state
    // ─────────────────────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (!initialLoadDone) {
            // Still fetching — hide empty state to avoid showing "No results" too early
            layoutEmptyState.setVisibility(View.GONE);
        } else if (displayList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String safe(String s) {
        return s != null ? s : "";
    }
}
