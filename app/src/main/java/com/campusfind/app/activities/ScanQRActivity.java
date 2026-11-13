package com.campusfind.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Claim;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.FirestoreHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class ScanQRActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private MaterialToolbar toolbar;
    private DecoratedBarcodeView barcodeScannerView;
    private FloatingActionButton fabTorch;
    private TextInputEditText etManualPayload;
    private MaterialButton btnVerifyManual;

    private DatabaseHelper dbHelper;
    private boolean isTorchOn = false;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        setupManualInput();
        if (barcodeScannerView != null) {
            barcodeScannerView.initializeFromIntent(getIntent());
        }
        checkCameraPermissionAndStart();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarScanQR);
        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        fabTorch = findViewById(R.id.fabTorch);
        etManualPayload = findViewById(R.id.etManualPayload);
        btnVerifyManual = findViewById(R.id.btnVerifyManual);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupManualInput() {
        btnVerifyManual.setOnClickListener(v -> {
            String code = etManualPayload.getText() != null ? etManualPayload.getText().toString().trim() : "";
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(ScanQRActivity.this, "Please enter a claim code", Toast.LENGTH_SHORT).show();
                return;
            }
            processScannedPayload(code);
        });

        fabTorch.setOnClickListener(v -> toggleTorch());
    }

    private void toggleTorch() {
        if (isTorchOn) {
            barcodeScannerView.setTorchOff();
            isTorchOn = false;
        } else {
            barcodeScannerView.setTorchOn();
            isTorchOn = true;
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void startScanning() {
        barcodeScannerView.setStatusText("Scanning for CampusFind Claim QR...");
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isProcessing) {
                    runOnUiThread(() -> processScannedPayload(result.getText()));
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    private void processScannedPayload(String payload) {
        if (isProcessing) return;
        isProcessing = true;

        String trimmed = payload.trim().toUpperCase();

        // ── Route 1: Owner Verification Code (CF-XXXXXX) ──────────────────
        if (trimmed.startsWith("CF-") && trimmed.length() <= 12) {
            processOwnerVerificationCode(trimmed);
            return;
        }

        // ── Route 2: Claim QR (CampusFind Claim System) ───────────────────
        processClaimPayload(payload.trim());
    }

    /**
     * Handle Claim QR payload (e.g. CAMPUSFIND_CLAIM_...).
     * 1. Checks local SQLite first.
     * 2. Falls back to Firestore if not found locally (cross-device scanning).
     */
    private void processClaimPayload(String payload) {
        // 1. Try local SQLite (instant)
        Claim localClaim = dbHelper.getClaimByQrPayload(payload);
        if (localClaim != null) {
            showClaimVerifiedDialog(localClaim, payload, false);
            return;
        }

        // 2. Not in local SQLite — query Firestore (claim was created on another device)
        FirestoreHelper.getInstance().getClaimByQrPayload(payload, new FirestoreHelper.ClaimCallback() {
            @Override
            public void onSuccess(Claim cloudClaim) {
                runOnUiThread(() -> {
                    if (cloudClaim == null) {
                        isProcessing = false;
                        showResultDialog("❌ Invalid QR Code",
                                "No matching claim or owner code found for:\n\n" + payload
                                + "\n\nMake sure the student presents either their Owner QR from My Reports or Claim QR from Item Detail.",
                                false);
                    } else {
                        showClaimVerifiedDialog(cloudClaim, payload, true);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showResultDialog("❌ Verification Failed",
                            "Could not verify claim with server:\n\n" + message,
                            false);
                });
            }
        });
    }

    /**
     * Displays the "Claim Verified" confirmation dialog.
     */
    private void showClaimVerifiedDialog(Claim claim, String payload, boolean fromCloud) {
        if ("Returned".equalsIgnoreCase(claim.getClaimStatus())) {
            showResultDialog("✅ Already Returned",
                    "This item has already been verified and returned to "
                            + (claim.getClaimerName() != null ? claim.getClaimerName() : "the claimant")
                            + ".\n\nItem: " + (claim.getItemName() != null ? claim.getItemName() : "N/A"),
                    false);
            return;
        }

        String itemName    = claim.getItemName()     != null ? claim.getItemName()     : "Campus Item";
        String studentName = claim.getClaimerName()  != null ? claim.getClaimerName()  : "Student";
        String studentRoll = claim.getClaimerRollNo() != null ? claim.getClaimerRollNo() : "--";
        String studentEmail = claim.getClaimerEmail() != null ? claim.getClaimerEmail() : "--";
        String itemType    = claim.getItemType()     != null ? claim.getItemType()     : "Item";

        String message = "📦 Student Claim Verified!\n\n"
                + "A campus member is claiming this item:\n"
                + "• Item: " + itemName + " (" + itemType + ")\n"
                + "• Claimed by: " + studentName + "\n"
                + "• Roll No: " + studentRoll + "\n"
                + "• Email: " + studentEmail + "\n\n"
                + "Confirm this matches the student standing in front of you, then tap 'Complete Handover'.";

        new AlertDialog.Builder(this)
                .setTitle("📦 Claim Verified!")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("✅ Complete Handover", (dialog, which) -> {
                    // Update local SQLite
                    dbHelper.verifyAndCompleteHandover(payload);
                    // Update cloud Firestore (both claim and item status)
                    FirestoreHelper.getInstance().markClaimReturned(payload);

                    showResultDialog("🎉 Handover Verified!",
                            "Item handover completed!\n\n"
                            + "• Item: " + itemName + " (" + itemType + ")\n"
                            + "• Handed over to: " + studentName + " (" + studentRoll + ")\n\n"
                            + "Status updated to 'Returned' across CampusFind.",
                            true);
                })
                .setNegativeButton("⚠️ Cancel", (dialog, which) -> {
                    isProcessing = false;
                    Toast.makeText(this, "Handover cancelled.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Handle Owner Verification Code (CF-XXXXXX).
     * 1. Checks local SQLite first (fast, works offline on the owner's own device).
     * 2. Falls back to Firestore if not found locally (cross-device: finder uses a different phone).
     */
    private void processOwnerVerificationCode(String ownerCode) {
        // 1. Try local SQLite (instant)
        Item lostItem = dbHelper.getLostItemByOwnerCode(ownerCode);
        if (lostItem != null) {
            showOwnerVerifiedDialog(lostItem, ownerCode, false);
            return;
        }

        // 2. Not in local DB — query Firestore (owner used a different device)
        FirestoreHelper.getInstance().getLostItemByOwnerCode(ownerCode,
                new FirestoreHelper.ItemsCallback() {
                    @Override
                    public void onSuccess(java.util.List<Item> items) {
                        runOnUiThread(() -> {
                            if (items.isEmpty()) {
                                isProcessing = false;
                                showResultDialog("❌ Code Not Found",
                                        "No lost item report was found for owner code:\n\n" + ownerCode
                                        + "\n\nAsk the owner to show you the QR from their CampusFind app (My Reports).",
                                        false);
                            } else {
                                showOwnerVerifiedDialog(items.get(0), ownerCode, true);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            isProcessing = false;
                            showResultDialog("❌ Code Not Found",
                                    "No lost item report found. Ask the owner to re-open their app.\n\nCode: " + ownerCode,
                                    false);
                        });
                    }
                });
    }

    /**
     * Displays the "Owner Verified" confirmation dialog.
     * fromCloud = true means the item was fetched from Firestore (not local SQLite),
     * so completeOwnerHandover must update Firestore too.
     */
    private void showOwnerVerifiedDialog(Item lostItem, String ownerCode, boolean fromCloud) {
        if ("Returned".equalsIgnoreCase(lostItem.getStatus())) {
            showResultDialog("✅ Already Returned",
                    "This item has already been marked as returned.\n\n"
                    + "Item: " + lostItem.getItemName(),
                    false);
            return;
        }

        String ownerName  = lostItem.getReporterName()  != null ? lostItem.getReporterName()  : "Unknown";
        String ownerRoll  = lostItem.getReporterRollNo() != null ? lostItem.getReporterRollNo() : "--";
        String ownerEmail = lostItem.getReporterEmail()  != null ? lostItem.getReporterEmail()  : "--";

        String confirmMsg = "✅ Owner Identity Verified!\n\n"
                + "This person is the REAL owner of:\n"
                + "• Item: " + lostItem.getItemName() + "\n"
                + "• Category: " + lostItem.getCategory() + "\n"
                + "• Lost at: " + lostItem.getLocation() + "\n"
                + "• Date: " + lostItem.getDate() + "\n\n"
                + "Owner:\n"
                + "• Name: " + ownerName + "\n"
                + "• Roll No: " + ownerRoll + "\n"
                + "• Email: " + ownerEmail + "\n\n"
                + "Hand over the item and tap 'Complete Handover' to finish.";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔐 Owner Verified!")
                .setMessage(confirmMsg)
                .setCancelable(false)
                .setPositiveButton("✅ Complete Handover", (dialog, which) -> {
                    // Update both local SQLite and Firestore cloud database
                    dbHelper.completeOwnerHandover(ownerCode);
                    FirestoreHelper.getInstance().markReturnedByOwnerCode(ownerCode);

                    showResultDialog("🎉 Handover Complete!",
                            "The item \"" + lostItem.getItemName() + "\" has been\n"
                            + "successfully returned to " + ownerName + ".\n\n"
                            + "Status updated to Returned across CampusFind. Thank you! 🙏",
                            true);
                })
                .setNegativeButton("⚠️ Cancel — Wrong Person", (dialog, which) -> {
                    isProcessing = false;
                    Toast.makeText(this, "Handover cancelled. Do NOT give the item.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showResultDialog(String title, String message, boolean isSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(isSuccess ? "Finish" : "Scan Again", (dialog, which) -> {
                    dialog.dismiss();
                    if (isSuccess) {
                        finish();
                    } else {
                        isProcessing = false;
                        if (etManualPayload != null) etManualPayload.setText("");
                    }
                });

        if (isSuccess) {
            builder.setIcon(android.R.drawable.ic_dialog_info);
        } else {
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        }

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission denied. You can still use manual verification.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }
}
