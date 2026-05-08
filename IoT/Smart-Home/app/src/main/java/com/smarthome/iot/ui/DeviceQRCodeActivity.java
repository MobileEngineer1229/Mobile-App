package com.smarthome.iot.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and displays a QR code for a registered MQTT/FastBee device.
 * The QR code encodes JSON with productId, deviceNum, name, and type,
 * which can be scanned by ScanDeviceActivity to register the same device
 * on another phone or account.
 *
 * Intent extras:
 *   - device_name (String)
 *   - mqtt_product_id (String)
 *   - mqtt_device_num (String)
 *   - mqtt_device_type (String, optional)
 */
public class DeviceQRCodeActivity extends AppCompatActivity {

    private String deviceName;
    private String productId;
    private String deviceNum;
    private String deviceType;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_qr_code);

        deviceName = getIntent().getStringExtra("device_name");
        productId = getIntent().getStringExtra("mqtt_product_id");
        deviceNum = getIntent().getStringExtra("mqtt_device_num");
        deviceType = getIntent().getStringExtra("mqtt_device_type");

        if (productId == null || deviceNum == null) {
            Toast.makeText(this, "Device has no MQTT binding", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (deviceName == null) deviceName = "Unknown Device";
        if (deviceType == null) deviceType = "electronics";

        initializeViews();
        generateQRCode();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        TextView textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewDeviceName.setText(deviceName);

        TextView textViewSetupCode = findViewById(R.id.textViewSetupCode);
        textViewSetupCode.setText(productId + ":" + deviceNum);

        TextView textViewProductId = findViewById(R.id.textViewProductId);
        textViewProductId.setText(productId);

        TextView textViewDeviceNum = findViewById(R.id.textViewDeviceNum);
        textViewDeviceNum.setText(deviceNum);

        TextView textViewDeviceType = findViewById(R.id.textViewDeviceType);
        textViewDeviceType.setText(deviceType.substring(0, 1).toUpperCase() + deviceType.substring(1));

        MaterialButton buttonShare = findViewById(R.id.buttonShare);
        buttonShare.setOnClickListener(v -> shareQRCode());
    }

    private void generateQRCode() {
        try {
            // Build JSON payload for QR code
            JSONObject qrData = new JSONObject();
            qrData.put("productId", productId);
            qrData.put("deviceNum", deviceNum);
            qrData.put("name", deviceName);
            qrData.put("type", deviceType);

            String content = qrData.toString();

            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ImageView imageViewQRCode = findViewById(R.id.imageViewQRCode);
            imageViewQRCode.setImageBitmap(qrBitmap);
        } catch (WriterException | org.json.JSONException e) {
            e.printStackTrace();
            ImageView imageViewQRCode = findViewById(R.id.imageViewQRCode);
            imageViewQRCode.setImageResource(R.drawable.ic_qr_code);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "No QR code to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap to cache directory
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "device_qr_" + productId + "_" + deviceNum + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Share via FileProvider
            Uri contentUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Add this device to Smartify:\nSetup code: " + productId + ":" + deviceNum);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Device QR Code"));
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: share just the text code
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Add this device to Smartify:\nSetup code: " + productId + ":" + deviceNum);
            startActivity(Intent.createChooser(shareIntent, "Share Device Code"));
        }
    }
}
