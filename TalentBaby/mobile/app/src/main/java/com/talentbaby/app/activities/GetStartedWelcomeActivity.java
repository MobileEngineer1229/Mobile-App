package com.talentbaby.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.talentbaby.app.R;

public class GetStartedWelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started_welcome);

        // "Let's Get Started" → onboarding info screen
        findViewById(R.id.btnGetStarted).setOnClickListener(v ->
                startActivity(new Intent(this, GetStartedInfoActivity.class)));

        // "I am already a user?" → login
        findViewById(R.id.textAlreadyUser).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }
}
