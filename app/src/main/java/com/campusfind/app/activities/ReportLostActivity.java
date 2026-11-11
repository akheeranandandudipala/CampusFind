package com.campusfind.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.EmailSender;
import com.campusfind.app.utils.FirestoreHelper;
import com.campusfind.app.utils.MatchingEngine;
import com.campusfind.app.utils.QRCodeHelper;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportLostActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilItemName, tilCategory, tilLocation, tilDate, tilDescription;
    private TextInputEditText etItemName, etLocation, etDate, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSubmitLost;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_lost);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();
        setupSubmitButton();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReportLost);
        tilItemName = findViewById(R.id.tilItemName);
        tilCategory = findViewById(R.id.tilCategory);
        tilLocation = findViewById(R.id.tilLocation);
        tilDate = findViewById(R.id.tilDate);
        tilDescription = findViewById(R.id.tilDescription);

        etItemName = findViewById(R.id.etItemName);
        actvCategory = findViewById(R.id.actvCategory);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);

        btnSubmitLost = findViewById(R.id.btnSubmitLost);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.categories_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        updateDateField();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateField();
        };
        etDate.setOnClickListener(v -> new DatePickerDialog(ReportLostActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    private void setupSubmitButton() {
        btnSubmitLost.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String itemName  = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
        String category  = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        String location  = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String date      = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String desc      = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        tilItemName.setError(null);
        tilCategory.setError(null);
        tilLocation.setError(null);
        tilDate.setError(null);

        boolean hasError = false;
        if (TextUtils.isEmpty(itemName)) { tilItemName.setError("Item name is required"); tilItemName.requestFocus(); hasError = true; }
        if (TextUtils.isEmpty(category)) { tilCategory.setError("Please select a category"); if (!hasError) { tilCategory.requestFocus(); } hasError = true; }
        if (TextUtils.isEmpty(location)) { tilLocation.setError("Location lost is required"); if (!hasError) { tilLocation.requestFocus(); } hasError = true; }
        if (TextUtils.isEmpty(date))     { tilDate.setError("Date lost is required");     if (!hasError) { tilDate.requestFocus(); } hasError = true; }
        if (hasError) return;

        Item lostItem = new Item();
        lostItem.setUserId(sessionManager.getUserId());
        lostItem.setItemType("Lost");
        lostItem.setItemName(itemName);
        lostItem.setCategory(category);
        lostItem.setLocation(location);
        lostItem.setDate(date);
        lostItem.setDescription(desc);
        lostItem.setStatus("Active");

        long id = dbHelper.addLostItem(lostItem);
        if (id > 0) {
            // Attach reporter details so finders can contact this user
            lostItem.setReporterName(sessionManager.getUserName());
            lostItem.setReporterEmail(sessionManager.getUserEmail());
            lostItem.setReporterRollNo(sessionManager.getUserRollNo());

            // ✅ Sync to Firestore — makes this Lost report visible to ALL devices
            FirestoreHelper.getInstance().saveLostItem(lostItem, null);

            // Show the QR owner code dialog immediately
            showOwnerQRDialog(lostItem);

            // ── In parallel: check if someone already found this item ────────────
            final Item savedLostItem = lostItem;
            FirestoreHelper.getInstance().getFoundItems(new FirestoreHelper.ItemsCallback() {
                @Override
                public void onSuccess(List<Item> foundItems) {
                    List<Item> matches = MatchingEngine.findMatches(savedLostItem, foundItems);
                    if (!matches.isEmpty()) {
                        Item bestMatch = matches.get(0);
                        // Show match toast — non-blocking (QR dialog may still be visible)
                        Toast.makeText(ReportLostActivity.this,
                                "🎯 Possible match! Someone found \"" + bestMatch.getItemName()
                                + "\" — check the Search screen!",
                                Toast.LENGTH_LONG).show();
                        // Also notify the finder's email that the owner just reported
                        notifyFinder(savedLostItem, bestMatch);
                    }
                }

                @Override
                public void onError(String message) {
                    // Firestore unavailable — silently skip match check
                }
            });

        } else {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a match dialog to the owner with the finder's email and matching keywords,
     * allowing them to review and manually send an email.
     */
    private void notifyFinder(Item lostItem, Item foundItem) {
        String finderEmail = foundItem.getReporterEmail();
        if (finderEmail == null || finderEmail.isEmpty()) return;

        List<String> keywords = MatchingEngine.getMatchingKeywords(lostItem.getItemName(), foundItem.getItemName());
        String keywordsSummary = keywords.isEmpty() ? "Category / Location Match" : TextUtils.join(", ", keywords);

        String subject = "CampusFind: Possible Match for Your Found " + foundItem.getItemName();
        String body = "Hi " + (foundItem.getReporterName() != null ? foundItem.getReporterName() : "there") + ",\n\n"
                + "The owner of the item you found has just submitted a lost report that matches yours!\n\n"
                + "🔍 Matched Lost Report Details:\n"
                + "  Lost Item: " + lostItem.getItemName() + "\n"
                + "  Category: " + lostItem.getCategory() + "\n"
                + "  Matched Keywords: " + keywordsSummary + "\n"
                + "  Location Lost: " + lostItem.getLocation() + "\n"
                + "  Date Lost: " + lostItem.getDate() + "\n"
                + "  Reported by: " + lostItem.getReporterName()
                + " (" + lostItem.getReporterRollNo() + ")\n\n"
                + "Please arrange to return the item.\n"
                + "The owner will show you their OWNER QR CODE for safe handover verification.\n\n"
                + "— " + lostItem.getReporterName();

        new AlertDialog.Builder(this)
                .setTitle("🎯 Potential Match Found!")
                .setMessage("Someone reported finding an item matching your lost report!\n\n"
                        + "📦 Found Item: " + foundItem.getItemName() + "\n"
                        + "🔑 Matched Keywords: " + keywordsSummary + "\n"
                        + "👤 Found By: " + (foundItem.getReporterName() != null ? foundItem.getReporterName() : "Campus Student") + "\n"
                        + "📧 Finder Email: " + finderEmail + "\n\n"
                        + "Would you like to send an email to the finder?")
                .setPositiveButton("✉️ Send Email", (dialog, which) -> {
                    Intent emailIntent = EmailSender.createEmailIntent(finderEmail, subject, body);
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send Email via..."));
                    } catch (Exception e) {
                        Toast.makeText(ReportLostActivity.this,
                                "No email app found. Finder email: " + finderEmail, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Done", null)
                .show();
    }

    /**
     * Shows a full-screen dialog with:
     *  • The unique owner verification QR code
     *  • The text code (e.g. CF-A3K9PL)
     *  • Instructions to show this to the finder at handover
     */
    private void showOwnerQRDialog(Item item) {
        String ownerCode = item.getOwnerCode();
        if (ownerCode == null || ownerCode.isEmpty()) {
            Toast.makeText(this, "Lost item reported successfully!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Generate QR bitmap
        Bitmap qrBitmap = QRCodeHelper.generateQRCode(ownerCode, 600);

        // Inflate custom dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_owner_qr, null);
        ImageView ivQR = dialogView.findViewById(R.id.ivOwnerQR);
        TextView tvCode = dialogView.findViewById(R.id.tvOwnerCode);
        TextView tvItemName = dialogView.findViewById(R.id.tvOwnerQRItemName);

        if (qrBitmap != null) ivQR.setImageBitmap(qrBitmap);
        tvCode.setText(ownerCode);
        tvItemName.setText("Lost: " + item.getItemName());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("✅ I've Saved It — Done", (dialog, which) -> {
                    Toast.makeText(this, "Report submitted! Your owner code: " + ownerCode, Toast.LENGTH_LONG).show();
                    finish();
                })
                .setNeutralButton("📋 Show Again Later", (dialog, which) -> {
                    // User can see it later in My Reports → Item Detail
                    Toast.makeText(this, "Find this QR anytime in My Reports", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }
}
