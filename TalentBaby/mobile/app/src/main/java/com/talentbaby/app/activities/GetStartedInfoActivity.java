package com.talentbaby.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.talentbaby.app.R;

public class GetStartedInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started_info);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // "Set Parenting Goals" → goal selection
        findViewById(R.id.btnSetGoals).setOnClickListener(v ->
                startActivity(new Intent(this, GetStartedGoalsActivity.class)));
    }
}
