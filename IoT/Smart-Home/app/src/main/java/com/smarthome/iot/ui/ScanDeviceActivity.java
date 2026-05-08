package com.smarthome.iot.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * QR Code scanner activity for FastBee/MQTT IoT device registration.
 *
 * Uses an embedded camera preview matching the Figma design with:
 * - Full-screen live camera feed
 * - Corner-bracket scan overlay
 * - Flash toggle, gallery picker, and manual code entry
 *
 * === Supported QR Code Formats ===
 * Format 1 - JSON: {"productId":"1001","deviceNum":"lamp-01","name":"Smart Lamp","type":"lamp"}
 * Format 2 - Simple: 1001:lamp-01
 *
 * @see EnterSetupCodeActivity Manual setup code entry alternative
 * @see DeviceDetectedActivity Next step: MQTT verification and device connection
 */
public class ScanDeviceActivity extends AppCompatActivity {
    private static final String TAG = "ScanDeviceActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private DecoratedBarcodeView barcodeScanner;
    private ImageView iconFlash;
    private boolean isFlashOn = false;
    private boolean hasProcessedResult = false;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        decodeQrFromImage(imageUri);
                    }
                }
            });

    private final BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result == null || result.getText() == null || hasProcessedResult) {
                return;
            }
            hasProcessedResult = true;
            barcodeScanner.pause();
            handleScanResult(result.getText());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);

        initializeViews();
        setupScanner();
        checkCameraPermission();
    }

    private void initializeViews() {
        barcodeScanner = findViewById(R.id.barcodeScanner);
        iconFlash = findViewById(R.id.iconFlash);

        ImageButton buttonClose = findViewById(R.id.buttonClose);
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        MaterialButton buttonEnterCode = findViewById(R.id.buttonEnterCode);

        buttonClose.setOnClickListener(v -> finish());

        buttonMore.setOnClickListener(v -> {
            // Reset and resume scanning
            hasProcessedResult = false;
            barcodeScanner.resume();
        });

        buttonEnterCode.setOnClickListener(v -> {
            Intent intent = new Intent(this, EnterSetupCodeActivity.class);
            startActivity(intent);
            finish();
        });

        // Flash toggle
        findViewById(R.id.navFlash).setOnClickListener(v -> toggleFlash());

        // Gallery picker — scan QR from image
        findViewById(R.id.navGallery).setOnClickListener(v -> openGallery());
    }

    private void setupScanner() {
        List<BarcodeFormat> formats = Collections.singletonList(BarcodeFormat.QR_CODE);
        barcodeScanner.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeScanner.setStatusText("");
        barcodeScanner.decodeContinuous(barcodeCallback);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            barcodeScanner.resume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                barcodeScanner.resume();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        if (isFlashOn) {
            barcodeScanner.setTorchOn();
            iconFlash.setImageResource(R.drawable.ic_flash_on);
        } else {
            barcodeScanner.setTorchOff();
            iconFlash.setImageResource(R.drawable.ic_flash_off);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void decodeQrFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);
            handleScanResult(result.getText());
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode QR from image", e);
            Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Parse scanned QR content and navigate to DeviceDetectedActivity with MQTT metadata.
     */
    private void handleScanResult(String contents) {
        Log.d(TAG, "QR scanned: " + contents);

        String productId = null;
        String deviceNum = null;
        String deviceName = null;
        String deviceType = null;

        // Try JSON format first
        try {
            JSONObject json = new JSONObject(contents);
            productId = json.optString("productId", null);
            deviceNum = json.optString("deviceNum", null);
            deviceName = json.optString("name", null);
            deviceType = json.optString("type", null);
        } catch (Exception e) {
            // Not JSON — try simple format: productId:deviceNum
            if (contents.contains(":")) {
                String[] parts = contents.split(":", 2);
                if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    productId = parts[0].trim();
                    deviceNum = parts[1].trim();
                }
            }
        }

        if (productId == null || deviceNum == null) {
            Toast.makeText(this, "Invalid QR code. Expected FastBee device code.", Toast.LENGTH_LONG).show();
            hasProcessedResult = false;
            barcodeScanner.resume();
            return;
        }

        // Derive defaults if not in QR
        if (deviceName == null) {
            deviceName = inferDeviceName(productId, deviceNum);
        }
        if (deviceType == null) {
            deviceType = inferDeviceType(productId);
        }

        // Navigate to DeviceDetectedActivity with MQTT metadata
        Intent intent = new Intent(this, DeviceDetectedActivity.class);
        intent.putExtra("device_name", deviceName);
        intent.putExtra("connection_source", "qr_scan");
        intent.putExtra("mqtt_product_id", productId);
        intent.putExtra("mqtt_device_num", deviceNum);
        intent.putExtra("mqtt_device_type", deviceType);
        startActivity(intent);
        finish();
    }

    private String inferDeviceName(String productId, String deviceNum) {
        String type = inferDeviceType(productId);
        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
        return "Smart " + typeName + " (" + deviceNum + ")";
    }

    private String inferDeviceType(String productId) {
        switch (productId) {
            case "1001": return "lamp";
            case "1002": return "electronics";
            case "2001":
            case "2002": return "sensor";
            case "3001": return "camera";
            case "4001": return "speaker";
            case "5001": return "thermostat";
            case "6001": return "lock";
            default: return "electronics";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasProcessedResult = false;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeScanner.pause();
    }
}
