package com.talentbaby.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.talentbaby.app.R;

import java.util.HashSet;
import java.util.Set;

public class GetStartedGoalsActivity extends AppCompatActivity {

    private final Set<Integer> selectedGoals = new HashSet<>();

    // Goal view ids and their check indicator ids
    private static final int[] GOAL_IDS = {
            R.id.goalActivities, R.id.goalMilestones, R.id.goalStories,
            R.id.goalNutrition, R.id.goalSleep
    };
    private static final int[] CHECK_IDS = {
            R.id.checkGoal1, R.id.checkGoal2, R.id.checkGoal3,
            R.id.checkGoal4, R.id.checkGoal5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started_goals);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // Set up each goal toggle
        for (int i = 0; i < GOAL_IDS.length; i++) {
            final int index = i;
            findViewById(GOAL_IDS[i]).setOnClickListener(v -> toggleGoal(index));
        }

        // Continue → auth screen
        findViewById(R.id.btnContinue).setOnClickListener(v ->
                startActivity(new Intent(this, GetStartedAuthActivity.class)));
    }

    private void toggleGoal(int index) {
        LinearLayout goalView = findViewById(GOAL_IDS[index]);
        TextView checkView = findViewById(CHECK_IDS[index]);

        if (selectedGoals.contains(index)) {
            // Deselect
            selectedGoals.remove(index);
            goalView.setBackgroundResource(R.drawable.bg_goal_unselected);
            checkView.setText("○");
            checkView.setTextColor(getColor(R.color.text_secondary));
        } else {
            // Select
            selectedGoals.add(index);
            goalView.setBackgroundResource(R.drawable.bg_goal_selected);
            checkView.setText("✓");
            checkView.setTextColor(getColor(R.color.primary));
        }
    }
}
