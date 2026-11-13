package com.campusfind.app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Claim;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.EmailSender;
import com.campusfind.app.utils.FirestoreHelper;
import com.campusfind.app.utils.ImageHelper;
import com.campusfind.app.utils.QRCodeHelper;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ItemDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvDetailTypeBadge, tvDetailStatusBadge, tvDetailItemName, tvDetailCategory;
    private TextView tvDetailLocationLabel, tvDetailLocation, tvDetailDateLabel, tvDetailDate, tvDetailDescription;
    private TextView tvReporterName, tvReporterDetails, tvOwnershipNotice;
    private MaterialButton btnClaimItem, btnViewClaimQR, btnContactEmail;
    private com.google.android.material.card.MaterialCardView cardItemPhoto;
    private ImageView ivItemPhoto;

    private Item currentItem;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Claim userExistingClaim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        currentItem = (Item) getIntent().getSerializableExtra("ITEM_EXTRA");
        if (currentItem == null) {
            Toast.makeText(this, "Item details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentItem.getItemId() <= 0) {
            int fallbackId = currentItem.getFirestoreId() != null && !currentItem.getFirestoreId().isEmpty()
                    ? Math.abs(currentItem.getFirestoreId().hashCode() % 100000) + 1
                    : Math.abs(((currentItem.getItemName() != null ? currentItem.getItemName() : "item") + currentItem.getDate()).hashCode() % 100000) + 1;
            currentItem.setItemId(fallbackId);
        }

        initViews();
        setupToolbar();
        populateItemData();
        checkClaimStatusAndPermissions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarDetail);
        tvDetailTypeBadge = findViewById(R.id.tvDetailTypeBadge);
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvDetailItemName = findViewById(R.id.tvDetailItemName);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);

        tvDetailLocationLabel = findViewById(R.id.tvDetailLocationLabel);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDateLabel = findViewById(R.id.tvDetailDateLabel);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);

        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterDetails = findViewById(R.id.tvReporterDetails);
        tvOwnershipNotice = findViewById(R.id.tvOwnershipNotice);

        btnClaimItem = findViewById(R.id.btnClaimItem);
        btnViewClaimQR = findViewById(R.id.btnViewClaimQR);
        btnContactEmail = findViewById(R.id.btnContactEmail);
        cardItemPhoto = findViewById(R.id.cardItemPhoto);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateItemData() {
        tvDetailItemName.setText(currentItem.getItemName());
        tvDetailCategory.setText("Category: " + currentItem.getCategory());
        tvDetailLocation.setText(currentItem.getLocation());
        tvDetailDate.setText(currentItem.getDate());

        String desc = currentItem.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            tvDetailDescription.setText(desc.trim());
        } else {
            tvDetailDescription.setText("No description provided.");
        }

        // Item Type Badge
        String itemType = currentItem.getItemType() != null ? currentItem.getItemType().toUpperCase() : "ITEM";
        tvDetailTypeBadge.setText(itemType + " ITEM");
        if ("FOUND".equalsIgnoreCase(itemType)) {
            tvDetailLocationLabel.setText("Location Found");
            tvDetailDateLabel.setText("Date Found");
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvDetailTypeBadge.setBackgroundResource(R.drawable.badge_matched);

            // Show photo using robust ImageHelper (Base64 / Local / Cloud)
            String photoPath = currentItem.getPhotoPath();
            if (photoPath != null && !photoPath.trim().isEmpty()) {
                Bitmap bmp = ImageHelper.loadPhotoBitmap(this, photoPath);
                if (bmp != null) {
                    cardItemPhoto.setVisibility(View.VISIBLE);
                    ivItemPhoto.setImageBitmap(bmp);
                } else {
                    cardItemPhoto.setVisibility(View.GONE);
                }
            } else {
                cardItemPhoto.setVisibility(View.GONE);
            }
        } else {
            tvDetailLocationLabel.setText("Location Lost");
            tvDetailDateLabel.setText("Date Lost");
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.accent));
            tvDetailTypeBadge.setBackgroundResource(R.drawable.badge_active);
            cardItemPhoto.setVisibility(View.GONE);
        }

        // Status Badge
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "Active";
        tvDetailStatusBadge.setText(status);
        if ("returned".equalsIgnoreCase(status)) {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_returned));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_returned);
        } else if ("matched".equalsIgnoreCase(status)) {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_matched));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_matched);
        } else {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_active));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_active);
        }

        // Reporter Information
        String repName = currentItem.getReporterName();
        String repRoll = currentItem.getReporterRollNo();
        String repEmail = currentItem.getReporterEmail();

        if (repName != null && !repName.isEmpty()) {
            tvReporterName.setText(repName);
            StringBuilder sb = new StringBuilder();
            if (repRoll != null && !repRoll.isEmpty()) sb.append("Roll No: ").append(repRoll).append(" • ");
            if (repEmail != null && !repEmail.isEmpty()) sb.append(repEmail);
            tvReporterDetails.setText(sb.toString());
        } else {
            tvReporterName.setText("Campus Member (ID #" + currentItem.getUserId() + ")");
            tvReporterDetails.setText("Verified Campus Student/Staff");
        }

        // Contact via Email Button setup
        if (btnContactEmail != null) {
            if (repEmail != null && !repEmail.trim().isEmpty()) {
                btnContactEmail.setVisibility(View.VISIBLE);
                btnContactEmail.setOnClickListener(v -> {
                    String subject = "CampusFind: Regarding your " + currentItem.getItemType() + " report for " + currentItem.getItemName();
                    String body = "Hi " + (repName != null ? repName : "there") + ",\n\n"
                            + "I am reaching out regarding the " + currentItem.getItemType() + " item on CampusFind:\n\n"
                            + "📦 Item: " + currentItem.getItemName() + "\n"
                            + "📂 Category: " + currentItem.getCategory() + "\n"
                            + "📍 Location: " + currentItem.getLocation() + "\n"
                            + "📅 Date: " + currentItem.getDate() + "\n\n"
                            + "[Please write your message here]\n\n"
                            + "— Sent via CampusFind";
                    Intent emailIntent = EmailSender.createEmailIntent(repEmail, subject, body);
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send Email via..."));
                    } catch (Exception e) {
                        Toast.makeText(this, "No email client found. Email: " + repEmail, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                btnContactEmail.setVisibility(View.GONE);
            }
        }
    }

    private void checkClaimStatusAndPermissions() {
        int currentUserId = sessionManager.getUserId();
        boolean isOwner = (currentUserId > 0 && currentUserId == currentItem.getUserId());
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "Active";

        if ("returned".equalsIgnoreCase(status)) {
            btnClaimItem.setVisibility(View.GONE);
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("This item has already been returned to its owner.");
            return;
        }

        // Check if current user already submitted a claim
        userExistingClaim = dbHelper.getClaimForUserAndItem(currentUserId, currentItem.getItemId(), currentItem.getItemType());
        if (userExistingClaim != null) {
            btnClaimItem.setVisibility(View.GONE);
            btnViewClaimQR.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("You have an active claim for this item.");

            btnViewClaimQR.setOnClickListener(v -> showQRCodeDialog(userExistingClaim.getQrPayload()));
            return;
        }

        if (isOwner) {
            btnClaimItem.setVisibility(View.VISIBLE);
            btnClaimItem.setText("Claim Item (Self/Test)");
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("You reported this item.");
            btnClaimItem.setOnClickListener(v -> confirmAndCreateClaim());
        } else {
            btnClaimItem.setVisibility(View.VISIBLE);
            btnClaimItem.setText("Claim This Item");
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.GONE);
            btnClaimItem.setOnClickListener(v -> confirmAndCreateClaim());
        }
    }

    private void confirmAndCreateClaim() {
        new AlertDialog.Builder(this)
                .setTitle("Claim Item")
                .setMessage("Are you sure you want to claim this item? A unique QR code will be generated for you to present at the handover desk for verification.")
                .setPositiveButton("Claim & Generate QR", (dialog, which) -> generateAndSaveClaim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndSaveClaim() {
        int currentUserId = sessionManager.getUserId();
        if (currentUserId <= 0) currentUserId = 1;

        int itemId = currentItem.getItemId();
        if (itemId <= 0) {
            itemId = Math.abs(currentItem.getItemName().hashCode() % 100000) + 1;
            currentItem.setItemId(itemId);
        }

        long timestamp = System.currentTimeMillis();
        String qrPayload = "CAMPUSFIND_CLAIM_" + itemId + "_" + timestamp;

        Claim claim = new Claim();
        claim.setUserId(currentUserId);
        claim.setItemId(itemId);
        claim.setItemType(currentItem.getItemType());
        claim.setQrPayload(qrPayload);
        claim.setClaimStatus("Pending");
        claim.setClaimerName(sessionManager.getUserName());
        claim.setClaimerRollNo(sessionManager.getUserRollNo());
        claim.setClaimerEmail(sessionManager.getUserEmail());
        claim.setItemName(currentItem.getItemName());

        long claimId = dbHelper.addClaim(claim);

        // Also save claim to Firestore for cross-device verification
        FirestoreHelper.getInstance().saveClaim(claim, null);

        // Update item status to Matched in both SQLite and Firestore
        dbHelper.updateItemStatus(itemId, currentItem.getItemType(), "Matched");
        FirestoreHelper.getInstance().updateItemStatus(currentItem.getItemType(), currentItem.getFirestoreId(), "Matched");

        currentItem.setStatus("Matched");
        populateItemData();

        Toast.makeText(this, "Claim submitted successfully!", Toast.LENGTH_SHORT).show();
        userExistingClaim = claim;
        checkClaimStatusAndPermissions();
        showQRCodeDialog(qrPayload);
    }

    private void showQRCodeDialog(String qrPayload) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null);
        ImageView ivQRCode = dialogView.findViewById(R.id.ivQRCode);
        TextView tvClaimCode = dialogView.findViewById(R.id.tvClaimCode);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvClaimCode.setText(qrPayload);

        Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 500);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(this, "Error generating QR Code Bitmap", Toast.LENGTH_SHORT).show();
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
