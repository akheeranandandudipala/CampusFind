package com.campusfind.app.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.EmailSender;
import com.campusfind.app.utils.FirestoreHelper;
import com.campusfind.app.utils.ImageHelper;
import com.campusfind.app.utils.MatchingEngine;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportFoundActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    // ── Photo slot 1 ──
    private ImageView ivPhotoPreview;
    private LinearLayout layoutPhotoPlaceholder;
    private TextView tvPhotoError;
    private MaterialButton btnTakePhoto, btnChooseGallery;

    // ── Photo slots 2 & 3 ──
    private ImageView ivPhoto2Preview, ivPhoto3Preview;
    private LinearLayout layoutPhoto2Placeholder, layoutPhoto3Placeholder;
    private MaterialButton btnTakePhoto2, btnGallery2, btnTakePhoto3, btnGallery3;

    private TextInputLayout tilItemName, tilCategory, tilLocation, tilDate, tilDescription;
    private TextInputEditText etItemName, etLocation, etDate, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSubmitFound;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Calendar calendar = Calendar.getInstance();

    // ── Multi-photo state ──
    private final Uri[]    photoUris  = new Uri[3];
    private final String[] photoPaths = new String[3];
    private int currentPhotoSlot = 0;   // which slot the next camera/gallery result goes to

    // -----------------------------------------------------------------------
    // Activity result launchers — registered before onCreate
    // -----------------------------------------------------------------------

    /** Camera: result goes to currentPhotoSlot */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoUris[currentPhotoSlot] != null) {
                    showPhotoInSlot(currentPhotoSlot, photoUris[currentPhotoSlot]);
                    // Convert to compressed, persistent Base64 so it works offline and across all devices
                    String b64 = ImageHelper.uriToBase64(this, photoUris[currentPhotoSlot]);
                    if (b64 != null) {
                        photoPaths[currentPhotoSlot] = b64;
                    }
                }
            });

    /** Gallery: result goes to currentPhotoSlot */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoUris[currentPhotoSlot]  = uri;
                    showPhotoInSlot(currentPhotoSlot, uri);
                    // Convert to compressed, persistent Base64
                    String b64 = ImageHelper.uriToBase64(this, uri);
                    if (b64 != null) {
                        photoPaths[currentPhotoSlot] = b64;
                    } else {
                        photoPaths[currentPhotoSlot] = uri.toString();
                    }
                }
            });

    /** Camera permission — opens camera for currentPhotoSlot on grant */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera(currentPhotoSlot);
                } else {
                    Toast.makeText(this, "Camera permission needed to take a photo", Toast.LENGTH_SHORT).show();
                }
            });

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_found);

        dbHelper       = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();
        setupPhotoButtons();
        setupSubmitButton();
    }

    // -----------------------------------------------------------------------
    // View setup
    // -----------------------------------------------------------------------

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReportFound);

        ivPhotoPreview       = findViewById(R.id.ivPhotoPreview);
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        tvPhotoError         = findViewById(R.id.tvPhotoError);
        btnTakePhoto         = findViewById(R.id.btnTakePhoto);
        btnChooseGallery     = findViewById(R.id.btnChooseGallery);

        ivPhoto2Preview        = findViewById(R.id.ivPhoto2Preview);
        ivPhoto3Preview        = findViewById(R.id.ivPhoto3Preview);
        layoutPhoto2Placeholder = findViewById(R.id.layoutPhoto2Placeholder);
        layoutPhoto3Placeholder = findViewById(R.id.layoutPhoto3Placeholder);
        btnTakePhoto2          = findViewById(R.id.btnTakePhoto2);
        btnGallery2            = findViewById(R.id.btnGallery2);
        btnTakePhoto3          = findViewById(R.id.btnTakePhoto3);
        btnGallery3            = findViewById(R.id.btnGallery3);

        tilItemName    = findViewById(R.id.tilItemName);
        tilCategory    = findViewById(R.id.tilCategory);
        tilLocation    = findViewById(R.id.tilLocation);
        tilDate        = findViewById(R.id.tilDate);
        tilDescription = findViewById(R.id.tilDescription);

        etItemName     = findViewById(R.id.etItemName);
        actvCategory   = findViewById(R.id.actvCategory);
        etLocation     = findViewById(R.id.etLocation);
        etDate         = findViewById(R.id.etDate);
        etDescription  = findViewById(R.id.etDescription);

        btnSubmitFound = findViewById(R.id.btnSubmitFound);
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
        etDate.setOnClickListener(v -> new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    // -----------------------------------------------------------------------
    // Photo handling
    // -----------------------------------------------------------------------

    private void setupPhotoButtons() {
        // Slot 1
        btnTakePhoto.setOnClickListener(v -> openPhotoSlot(0, true));
        btnChooseGallery.setOnClickListener(v -> openPhotoSlot(0, false));

        // Slot 2
        btnTakePhoto2.setOnClickListener(v -> openPhotoSlot(1, true));
        btnGallery2.setOnClickListener(v -> openPhotoSlot(1, false));

        // Slot 3
        btnTakePhoto3.setOnClickListener(v -> openPhotoSlot(2, true));
        btnGallery3.setOnClickListener(v -> openPhotoSlot(2, false));

        // Tap any filled preview to open in system viewer (pinch-zoom supported)
        ivPhotoPreview.setOnClickListener(v -> openImageViewer(0));
        ivPhoto2Preview.setOnClickListener(v -> openImageViewer(1));
        ivPhoto3Preview.setOnClickListener(v -> openImageViewer(2));
    }

    /** Opens camera or gallery for the given slot. */
    private void openPhotoSlot(int slot, boolean useCamera) {
        currentPhotoSlot = slot;
        if (useCamera) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera(slot);
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        } else {
            galleryLauncher.launch("image/*");
        }
    }

    private void openCamera(int slot) {
        try {
            File photoFile = createImageFile();
            photoUris[slot] = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            photoPaths[slot] = photoFile.getAbsolutePath();
            cameraLauncher.launch(photoUris[slot]);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime());
        String imageFileName = "CAMPUSFIND_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /** Shows the photo in the correct preview ImageView and hides the placeholder. */
    private void showPhotoInSlot(int slot, Uri uri) {
        switch (slot) {
            case 0:
                ivPhotoPreview.setImageURI(uri);
                ivPhotoPreview.setVisibility(View.VISIBLE);
                layoutPhotoPlaceholder.setVisibility(View.GONE);
                tvPhotoError.setVisibility(View.GONE);
                break;
            case 1:
                ivPhoto2Preview.setImageURI(uri);
                ivPhoto2Preview.setVisibility(View.VISIBLE);
                layoutPhoto2Placeholder.setVisibility(View.GONE);
                break;
            case 2:
                ivPhoto3Preview.setImageURI(uri);
                ivPhoto3Preview.setVisibility(View.VISIBLE);
                layoutPhoto3Placeholder.setVisibility(View.GONE);
                break;
        }
    }

    /** Opens the selected photo in the system gallery/viewer which supports pinch-zoom. */
    private void openImageViewer(int slot) {
        if (photoUris[slot] == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(photoUris[slot], "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            Toast.makeText(this, "No image viewer found", Toast.LENGTH_SHORT).show();
        }
    }

    // -----------------------------------------------------------------------
    // Submit
    // -----------------------------------------------------------------------

    private void setupSubmitButton() {
        btnSubmitFound.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        // --- Reset errors ---
        tilItemName.setError(null);
        tilCategory.setError(null);
        tilLocation.setError(null);
        tilDate.setError(null);
        tvPhotoError.setVisibility(View.GONE);

        String itemName = etItemName.getText()   != null ? etItemName.getText().toString().trim()   : "";
        String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        String location = etLocation.getText()   != null ? etLocation.getText().toString().trim()   : "";
        String date     = etDate.getText()       != null ? etDate.getText().toString().trim()       : "";
        String desc     = etDescription.getText()!= null ? etDescription.getText().toString().trim(): "";

        boolean hasError = false;

        // Photo 1 is mandatory
        if (photoPaths[0] == null || photoPaths[0].isEmpty()) {
            tvPhotoError.setVisibility(View.VISIBLE);
            hasError = true;
        }

        if (TextUtils.isEmpty(itemName)) {
            tilItemName.setError("Item name is required");
            if (!hasError) tilItemName.requestFocus();
            hasError = true;
        }
        if (TextUtils.isEmpty(category)) {
            tilCategory.setError("Please select a category");
            if (!hasError) tilCategory.requestFocus();
            hasError = true;
        }
        if (TextUtils.isEmpty(location)) {
            tilLocation.setError("Location found is required");
            if (!hasError) tilLocation.requestFocus();
            hasError = true;
        }
        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Date found is required");
            if (!hasError) tilDate.requestFocus();
            hasError = true;
        }

        if (hasError) return;

        // --- Build combined photo path (pipe-separated) ---
        StringBuilder photoSb = new StringBuilder();
        for (String p : photoPaths) {
            if (p != null && !p.isEmpty()) {
                if (photoSb.length() > 0) photoSb.append("||");
                photoSb.append(p);
            }
        }

        // --- Build and save the found item ---
        Item foundItem = new Item();
        foundItem.setUserId(sessionManager.getUserId());
        foundItem.setItemType("Found");
        foundItem.setItemName(itemName);
        foundItem.setCategory(category);
        foundItem.setLocation(location);
        foundItem.setDate(date);
        foundItem.setDescription(desc);
        foundItem.setStatus("Active");
        foundItem.setPhotoPath(photoSb.toString());

        long id = dbHelper.addFoundItem(foundItem);
        if (id <= 0) {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attach reporter details so owner knows who found it
        foundItem.setReporterName(sessionManager.getUserName());
        foundItem.setReporterEmail(sessionManager.getUserEmail());
        foundItem.setReporterRollNo(sessionManager.getUserRollNo());
        foundItem.setItemId((int) id);

        // ✅ Sync to Firestore — makes this report visible to ALL users on ALL devices
        FirestoreHelper.getInstance().saveFoundItem(foundItem, null);

        Toast.makeText(this, "Report submitted! Checking for matches...", Toast.LENGTH_SHORT).show();

        final Item savedFoundItem = foundItem;

        // Fetch ALL lost items from Firestore (cross-device) and run matching
        FirestoreHelper.getInstance().getLostItems(new FirestoreHelper.ItemsCallback() {
            @Override
            public void onSuccess(List<Item> allLostItems) {
                List<Item> matches = MatchingEngine.findMatches(savedFoundItem, allLostItems);
                if (!matches.isEmpty()) {
                    showMatchFoundDialog(savedFoundItem, matches);
                } else {
                    // Also check local SQLite in case Firestore lost items were submitted offline
                    List<Item> localLost  = dbHelper.getLostItems(null, null);
                    List<Item> localMatch = MatchingEngine.findMatches(savedFoundItem, localLost);
                    if (!localMatch.isEmpty()) {
                        showMatchFoundDialog(savedFoundItem, localMatch);
                    } else {
                        Toast.makeText(ReportFoundActivity.this,
                                "✅ Found item reported! No matches yet.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }

            @Override
            public void onError(String message) {
                // Firestore unavailable — fall back to local SQLite matching
                List<Item> localLost  = dbHelper.getLostItems(null, null);
                List<Item> localMatch = MatchingEngine.findMatches(savedFoundItem, localLost);
                if (!localMatch.isEmpty()) {
                    showMatchFoundDialog(savedFoundItem, localMatch);
                } else {
                    Toast.makeText(ReportFoundActivity.this,
                            "✅ Found item reported successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // Manual match email prompt — shows keywords and owner email for user to send
    // -----------------------------------------------------------------------

    /**
     * Called when a match is found based on keywords, category, or location.
     * Shows a dialog displaying the lost owner's email, matched keywords,
     * and a button to manually send the notification email.
     */
    private void showMatchFoundDialog(Item foundItem, List<Item> matches) {
        Item bestMatch = matches.get(0);
        String ownerEmail = bestMatch.getReporterEmail() != null ? bestMatch.getReporterEmail() : "";
        String ownerName  = bestMatch.getReporterName()  != null ? bestMatch.getReporterName()  : "Lost Item Owner";
        String ownerRoll  = bestMatch.getReporterRollNo() != null ? bestMatch.getReporterRollNo() : "";

        // Extract overlapping keywords between item reports (names + descriptions)
        List<String> keywords = MatchingEngine.getMatchingKeywords(foundItem, bestMatch);
        String keywordsSummary = keywords.isEmpty() ? "Category / Location Match" : TextUtils.join(", ", keywords);

        String subject = "CampusFind: Possible Match for Your Lost " + bestMatch.getItemName();
        String body = "Hi " + ownerName + ",\n\n"
                + "Great news! I found an item on campus that matches your lost report.\n\n"
                + "🔍 Matched Report Details:\n"
                + "  Your Lost Item: " + bestMatch.getItemName() + " (Category: " + bestMatch.getCategory() + ")\n"
                + "  Matched Keywords: " + keywordsSummary + "\n"
                + "  Location Found: " + foundItem.getLocation() + "\n"
                + "  Date Found: " + foundItem.getDate() + "\n"
                + "  Reported by: " + foundItem.getReporterName() + " (" + foundItem.getReporterRollNo() + ")\n\n"
                + "Please check the CampusFind app to verify and arrange collection.\n\n"
                + "— " + foundItem.getReporterName();

        new AlertDialog.Builder(this)
                .setTitle("🎯 Potential Match Found!")
                .setMessage("We found a matching lost item report on campus!\n\n"
                        + "📦 Matched Item: " + bestMatch.getItemName() + "\n"
                        + "🔑 Matched Keywords: " + keywordsSummary + "\n"
                        + "👤 Owner: " + ownerName + (ownerRoll.isEmpty() ? "" : " (" + ownerRoll + ")") + "\n"
                        + "📧 Owner Email: " + ownerEmail + "\n\n"
                        + "Would you like to send an email to notify the owner?")
                .setPositiveButton("✉️ Send Email", (dialog, which) -> {
                    Intent emailIntent = EmailSender.createEmailIntent(ownerEmail, subject, body);
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send Email via..."));
                    } catch (Exception e) {
                        Toast.makeText(ReportFoundActivity.this,
                                "No email app found. Owner email: " + ownerEmail, Toast.LENGTH_LONG).show();
                    }
                    finish();
                })
                .setNegativeButton("Done (Later)", (dialog, which) -> {
                    Toast.makeText(ReportFoundActivity.this,
                            "✅ Found item reported successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
