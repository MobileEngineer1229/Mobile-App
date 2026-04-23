package com.talentbaby.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.talentbaby.app.R;

public class GetStartedAuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started_auth);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // "Continue with Email" → Sign up (new account flow)
        findViewById(R.id.btnEmail).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        // "Continue with Google" → placeholder
        findViewById(R.id.btnGoogle).setOnClickListener(v ->
                Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show());
    }
}
