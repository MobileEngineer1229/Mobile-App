package com.talentbaby.app.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.talentbaby.app.MainActivity;
import com.talentbaby.app.R;
import com.talentbaby.app.models.ApiResponse;
import com.talentbaby.app.models.Baby;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.utils.ApiClient;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBabyActivity extends AppCompatActivity {

    private LinearLayout layoutStep1;
    private LinearLayout layoutStep2;

    private TextInputEditText editBabyName;
    private TextView btnGirl;
    private TextView btnBoy;
    private TextInputEditText editBirthDate;
    private MaterialCheckBox checkboxPremature;
    private TextInputLayout layoutScheduledDob;
    private TextInputEditText editScheduledDob;
    private TextView textScheduledDobHint;

    private TextInputEditText editParentName;
    private RadioGroup radioGroupRelationship;

    private Button buttonContinue;
    private ApiService apiService;

    private String selectedGender = "male"; // default Boy
    private int currentStep = 1;
    private boolean isEditMode = false;
    private int babyId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_baby);

        apiService = ApiClient.getClient().create(ApiService.class);

        babyId = getIntent().getIntExtra("baby_id", -1);
        isEditMode = (babyId != -1);

        initViews();
        prefillIfEditing();
    }

    private void initViews() {
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);

        editBabyName = findViewById(R.id.editBabyName);
        btnGirl = findViewById(R.id.btnGirl);
        btnBoy = findViewById(R.id.btnBoy);
        editBirthDate = findViewById(R.id.editBirthDate);
        checkboxPremature = findViewById(R.id.checkboxPremature);
        layoutScheduledDob = findViewById(R.id.layoutScheduledDob);
        editScheduledDob = findViewById(R.id.editScheduledDob);
        textScheduledDobHint = findViewById(R.id.textScheduledDobHint);

        editParentName = findViewById(R.id.editParentName);
        radioGroupRelationship = findViewById(R.id.radioGroupRelationship);

        buttonContinue = findViewById(R.id.buttonContinue);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (currentStep == 2) {
                showStep(1);
            } else {
                if (!isEditMode) {
                    // During onboarding, can't go back past baby create
                } else {
                    finish();
                }
            }
        });

        // Gender toggle
        btnBoy.setOnClickListener(v -> selectGender("male"));
        btnGirl.setOnClickListener(v -> selectGender("female"));

        // Date picker
        editBirthDate.setOnClickListener(v -> showDatePicker(editBirthDate));
        editScheduledDob.setOnClickListener(v -> showDatePicker(editScheduledDob));

        // Premature checkbox
        checkboxPremature.setOnCheckedChangeListener((cb, checked) -> {
            layoutScheduledDob.setVisibility(checked ? View.VISIBLE : View.GONE);
            textScheduledDobHint.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // Continue button
        buttonContinue.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (validateStep1()) {
                    showStep(2);
                }
            } else {
                saveBaby();
            }
        });
    }

    private void selectGender(String gender) {
        selectedGender = gender;
        if ("male".equals(gender)) {
            btnBoy.setBackgroundResource(R.drawable.bg_teal_button);
            btnBoy.setTextColor(getColor(android.R.color.white));
            btnGirl.setBackgroundResource(R.drawable.bg_date_unselected);
            btnGirl.setTextColor(getColor(R.color.text_secondary));
        } else {
            btnGirl.setBackgroundResource(R.drawable.bg_teal_button);
            btnGirl.setTextColor(getColor(android.R.color.white));
            btnBoy.setBackgroundResource(R.drawable.bg_date_unselected);
            btnBoy.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, day);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validateStep1() {
        String name = editBabyName.getText() != null ? editBabyName.getText().toString().trim() : "";
        String dob = editBirthDate.getText() != null ? editBirthDate.getText().toString().trim() : "";
        if (name.isEmpty() || dob.isEmpty()) {
            Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkboxPremature.isChecked()) {
            String scheduledDob = editScheduledDob.getText() != null ? editScheduledDob.getText().toString().trim() : "";
            if (scheduledDob.isEmpty()) {
                Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void showStep(int step) {
        currentStep = step;
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        buttonContinue.setText(step == 2
                ? getString(R.string.save)
                : getString(R.string.continue_btn));
    }

    private void prefillIfEditing() {
        if (!isEditMode) return;
        String name = getIntent().getStringExtra("baby_name");
        String dob = getIntent().getStringExtra("baby_birth_date");
        String gender = getIntent().getStringExtra("baby_gender");
        if (name != null) editBabyName.setText(name);
        if (dob != null) editBirthDate.setText(dob);
        if ("female".equalsIgnoreCase(gender)) selectGender("female");
        else selectGender("male");
        buttonContinue.setText(getString(R.string.save));
    }

    private void saveBaby() {
        String name = editBabyName.getText() != null ? editBabyName.getText().toString().trim() : "";
        String birthDate = checkboxPremature.isChecked()
                && editScheduledDob.getText() != null
                && !editScheduledDob.getText().toString().trim().isEmpty()
                ? editScheduledDob.getText().toString().trim()
                : (editBirthDate.getText() != null ? editBirthDate.getText().toString().trim() : "");

        if (name.isEmpty() || birthDate.isEmpty()) {
            Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("birth_date", birthDate);
        body.put("gender", selectedGender);

        buttonContinue.setEnabled(false);

        Callback<ApiResponse<Baby>> callback = new Callback<ApiResponse<Baby>>() {
            @Override
            public void onResponse(Call<ApiResponse<Baby>> call, Response<ApiResponse<Baby>> response) {
                buttonContinue.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Baby baby = response.body().getData();
                    if (baby != null) {
                        // Save active baby id so rest of the app can use it
                        com.talentbaby.app.utils.TokenManager tokenManager =
                                new com.talentbaby.app.utils.TokenManager(AddBabyActivity.this);
                        tokenManager.saveActiveBabyId(baby.getId());
                    }
                    // Save parent relation if provided (step 2)
                    saveParentRelation();
                    navigateToMain();
                } else {
                    Toast.makeText(AddBabyActivity.this,
                            getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Baby>> call, Throwable t) {
                buttonContinue.setEnabled(true);
                Toast.makeText(AddBabyActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        };

        if (isEditMode) {
            apiService.updateBaby(babyId, body).enqueue(callback);
        } else {
            apiService.createBaby(body).enqueue(callback);
        }
    }

    private void saveParentRelation() {
        // Resolve selected relation radio button
        int checkedId = radioGroupRelationship.getCheckedRadioButtonId();
        if (checkedId == -1) return;

        android.widget.RadioButton rb = radioGroupRelationship.findViewById(checkedId);
        if (rb == null) return;

        String relation = rb.getText().toString().toLowerCase().trim();
        if (relation.isEmpty()) return;

        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("relation_to_baby", relation);
        apiService.updateProfile(profileUpdate).enqueue(new Callback<ApiResponse<com.talentbaby.app.models.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.talentbaby.app.models.User>> call,
                                   Response<ApiResponse<com.talentbaby.app.models.User>> response) {
                // Silent — relation saved in background
            }
            @Override
            public void onFailure(Call<ApiResponse<com.talentbaby.app.models.User>> call, Throwable t) {
                // Silent
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
