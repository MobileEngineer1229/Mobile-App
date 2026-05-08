package com.smarthome.iot.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.dialogs.ShareDialog;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddMemberQRActivity extends AppCompatActivity {
    private MaterialButton buttonInviteEmail;
    private MaterialButton buttonInviteQR;
    private ImageView imageViewQRCode;
    private TextView textViewInvitationCode;
    private RadioGroup radioGroupRole;
    private RadioButton radioAdmin;
    private RadioButton radioMember;
    private MaterialButton buttonShareInvite;
    
    private int homeId;
    private String invitationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member_qr);

        setStatusBarColor();
        
        homeId = getIntent().getIntExtra("home_id", -1);
        
        // Generate invitation code
        invitationCode = generateInvitationCode();

        initializeViews();
        setupClickListeners();
        generateQRCode();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonInviteEmail = findViewById(R.id.buttonInviteEmail);
        buttonInviteQR = findViewById(R.id.buttonInviteQR);
        imageViewQRCode = findViewById(R.id.imageViewQRCode);
        textViewInvitationCode = findViewById(R.id.textViewInvitationCode);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioAdmin = findViewById(R.id.radioAdmin);
        radioMember = findViewById(R.id.radioMember);
        buttonShareInvite = findViewById(R.id.buttonShareInvite);

        buttonBack.setOnClickListener(v -> finish());
        textViewInvitationCode.setText(invitationCode);
    }

    private void setupClickListeners() {
        buttonInviteEmail.setOnClickListener(v -> {
            // Switch to email mode
            Intent intent = new Intent(this, AddMemberActivity.class);
            intent.putExtra("home_id", homeId);
            startActivity(intent);
            finish();
        });

        buttonInviteQR.setOnClickListener(v -> {
            // Already on QR mode
        });

        buttonShareInvite.setOnClickListener(v -> {
            String role = radioMember.isChecked() ? "Member" : "Admin";
            ShareDialog shareDialog = new ShareDialog(this, invitationCode, role);
            shareDialog.show();
        });
    }

    private String generateInvitationCode() {
        // Generate random 8-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void generateQRCode() {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = writer.encode(invitationCode, BarcodeFormat.QR_CODE, 512, 512, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            imageViewQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            // Fallback: show placeholder
            imageViewQRCode.setImageResource(R.drawable.ic_qr_code);
        }
    }
}
