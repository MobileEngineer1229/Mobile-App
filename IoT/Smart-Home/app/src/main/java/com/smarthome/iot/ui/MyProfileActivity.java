package com.smarthome.iot.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.User;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyProfileActivity extends AppCompatActivity {
    private ImageView imageViewProfile;
    private ImageButton buttonEditPicture;
    private TextInputEditText editTextFullName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPhone;
    private TextInputEditText editTextGender;
    private TextInputEditText editTextBirthdate;
    private MaterialButton buttonSave;
    
    private ApiService apiService;
    private AuthManager authManager;
    private Calendar birthdateCalendar;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);

        birthdateCalendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        initializeViews();
        setupClickListeners();
        setupGenderDropdown();
        loadUserProfile();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        imageViewProfile = findViewById(R.id.imageViewProfile);
        buttonEditPicture = findViewById(R.id.buttonEditPicture);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextGender = findViewById(R.id.editTextGender);
        editTextBirthdate = findViewById(R.id.editTextBirthdate);
        buttonSave = findViewById(R.id.buttonSave);
        
        // Setup phone number country code selector
        setupPhoneCountryCode();
    }
    
    private void setupPhoneCountryCode() {
        TextInputLayout phoneLayout = (TextInputLayout) editTextPhone.getParent().getParent();
        if (phoneLayout != null) {
            phoneLayout.setEndIconOnClickListener(v -> {
                // TODO: Show country code selector dialog
                Toast.makeText(this, "Country code selector coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupClickListeners() {
        buttonEditPicture.setOnClickListener(v -> {
            // TODO: Implement image picker
            Toast.makeText(this, "Profile picture editing coming soon", Toast.LENGTH_SHORT).show();
        });

        editTextBirthdate.setOnClickListener(v -> showDatePicker());

        buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void setupGenderDropdown() {
        String[] genders = {
            getString(R.string.male),
            getString(R.string.female),
            getString(R.string.other)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, genders);
        
        TextInputLayout genderLayout = (TextInputLayout) editTextGender.getParent().getParent();
        AutoCompleteTextView autoComplete = new AutoCompleteTextView(this);
        autoComplete.setAdapter(adapter);
        
        editTextGender.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.gender)
                .setItems(genders, (dialog, which) -> {
                    editTextGender.setText(genders[which]);
                })
                .show();
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                birthdateCalendar.set(year, month, dayOfMonth);
                editTextBirthdate.setText(dateFormatter.format(birthdateCalendar.getTime()));
            },
            birthdateCalendar.get(Calendar.YEAR),
            birthdateCalendar.get(Calendar.MONTH),
            birthdateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadUserProfile() {
        if (!authManager.isLoggedIn()) {
            // Set demo data for testing
            editTextFullName.setText("Andrew Ainsley");
            editTextEmail.setText("andrew.ainsley@yourdomain.com");
            editTextPhone.setText("+1 111 467 378 399");
            editTextGender.setText("Male");
            editTextBirthdate.setText("12/25/1995");
            return;
        }

        Call<ApiResponse<User>> call = apiService.getProfile();
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        updateUI(user);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                // Use cached data if available
            }
        });
    }

    private void updateUI(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            editTextFullName.setText(user.getFirstName() + " " + user.getLastName());
        }
        if (user.getEmail() != null) {
            editTextEmail.setText(user.getEmail());
        }
        if (user.getPhone() != null) {
            editTextPhone.setText(user.getPhone());
        }
    }

    private void saveProfile() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String gender = editTextGender.getText().toString().trim();
        String birthdate = editTextBirthdate.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("firstName", firstName);
        profileData.put("lastName", lastName);
        if (!phone.isEmpty()) {
            profileData.put("phone", phone);
        }

        Call<ApiResponse<User>> call = apiService.updateProfile(profileData);
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(MyProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(MyProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Toast.makeText(MyProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

