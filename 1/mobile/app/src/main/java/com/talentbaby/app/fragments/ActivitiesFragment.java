package com.talentbaby.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.talentbaby.app.R;
import com.talentbaby.app.activities.ActivityDetailActivity;
import com.talentbaby.app.adapters.ActivityAdapter;
import com.talentbaby.app.models.Activity;
import com.talentbaby.app.models.ApiResponse;
import com.talentbaby.app.models.Baby;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.utils.ApiClient;
import com.talentbaby.app.utils.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitiesFragment extends Fragment {

    private RecyclerView recyclerActivities;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutMonthPills;
    private ActivityAdapter activityAdapter;
    private List<Activity> activities = new ArrayList<>();
    private ApiService apiService;

    private Integer selectedMonth = null;
    private String selectedCategory = null;
    private TextView activeMonthPill = null;
    private TextView activeCategoryChip = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupRecyclerView();
        loadBabyAndSetupMonths();
    }

    private void initViews(View view) {
        recyclerActivities = view.findViewById(R.id.recyclerActivities);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutMonthPills = view.findViewById(R.id.layoutMonthPills);

        view.findViewById(R.id.btnParentingSupportAct).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.parenting_support), Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnMenuActivities).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).openDrawer();
            }
        });

        // Category chips
        TextView chipAll = view.findViewById(R.id.chipAll);
        TextView chipPhysical = view.findViewById(R.id.chipPhysical);
        TextView chipCognitive = view.findViewById(R.id.chipCognitive);
        TextView chipCommunication = view.findViewById(R.id.chipCommunication);
        TextView chipSocial = view.findViewById(R.id.chipSocial);

        activeCategoryChip = chipAll;

        View.OnClickListener chipListener = v -> {
            String category = null;
            if (v == chipPhysical) category = "physical";
            else if (v == chipCognitive) category = "cognitive";
            else if (v == chipCommunication) category = "communication";
            else if (v == chipSocial) category = "social";

            selectCategoryChip((TextView) v);
            selectedCategory = category;
            loadActivities();
        };

        chipAll.setOnClickListener(chipListener);
        chipPhysical.setOnClickListener(chipListener);
        chipCognitive.setOnClickListener(chipListener);
        chipCommunication.setOnClickListener(chipListener);
        chipSocial.setOnClickListener(chipListener);
    }

    private void selectCategoryChip(TextView chip) {
        if (activeCategoryChip != null) {
            activeCategoryChip.setBackgroundResource(R.drawable.bg_date_unselected);
            activeCategoryChip.setTextColor(requireContext().getColor(R.color.text_secondary));
        }
        chip.setBackgroundResource(R.drawable.bg_teal_button);
        chip.setTextColor(requireContext().getColor(android.R.color.white));
        activeCategoryChip = chip;
    }

    private void selectMonthPill(TextView pill, int month) {
        if (activeMonthPill != null) {
            activeMonthPill.setBackgroundResource(R.drawable.bg_date_unselected);
            activeMonthPill.setTextColor(requireContext().getColor(R.color.date_unselected_text));
        }
        pill.setBackgroundResource(R.drawable.bg_date_selected);
        pill.setTextColor(requireContext().getColor(android.R.color.white));
        activeMonthPill = pill;
        selectedMonth = month;
    }

    private void loadBabyAndSetupMonths() {
        int babyId = TokenManager.getBabyId(requireContext());
        if (babyId == -1) {
            // No baby — just load all activities
            setupMonthPills(6);
            return;
        }

        apiService.getBaby(babyId).enqueue(new Callback<ApiResponse<Baby>>() {
            @Override
            public void onResponse(Call<ApiResponse<Baby>> call, Response<ApiResponse<Baby>> response) {
                int ageMonths = 6;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Baby baby = response.body().getData();
                    ageMonths = calculateAgeInMonths(baby.getBirthDate());
                }
                setupMonthPills(Math.max(1, ageMonths));
            }

            @Override
            public void onFailure(Call<ApiResponse<Baby>> call, Throwable t) {
                setupMonthPills(6);
            }
        });
    }

    private void setupMonthPills(int currentMonth) {
        if (layoutMonthPills == null || getContext() == null) return;
        layoutMonthPills.removeAllViews();

        int startMonth = Math.max(1, currentMonth - 2);
        int endMonth = Math.min(36, currentMonth + 2);

        for (int m = startMonth; m <= endMonth; m++) {
            final int month = m;
            TextView pill = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (getResources().getDisplayMetrics().density * 36)
            );
            params.setMarginEnd((int) (getResources().getDisplayMetrics().density * 8));
            pill.setLayoutParams(params);
            pill.setText(getString(R.string.month_num, m));
            pill.setTextSize(13f);
            pill.setPaddingRelative(
                    (int) (getResources().getDisplayMetrics().density * 16), 0,
                    (int) (getResources().getDisplayMetrics().density * 16), 0
            );
            pill.setGravity(android.view.Gravity.CENTER);
            pill.setClickable(true);
            pill.setFocusable(true);

            if (m == currentMonth) {
                pill.setBackgroundResource(R.drawable.bg_date_selected);
                pill.setTextColor(requireContext().getColor(android.R.color.white));
                activeMonthPill = pill;
                selectedMonth = month;
            } else {
                pill.setBackgroundResource(R.drawable.bg_date_unselected);
                pill.setTextColor(requireContext().getColor(R.color.date_unselected_text));
            }

            pill.setOnClickListener(v -> {
                selectMonthPill(pill, month);
                loadActivities();
            });

            layoutMonthPills.addView(pill);
        }

        loadActivities();
    }

    private void setupRecyclerView() {
        activityAdapter = new ActivityAdapter(activities, activity -> {
            Intent intent = new Intent(getActivity(), ActivityDetailActivity.class);
            intent.putExtra("activity_id", activity.getId());
            startActivity(intent);
        });
        recyclerActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerActivities.setAdapter(activityAdapter);
        recyclerActivities.setNestedScrollingEnabled(false);
    }

    private void loadActivities() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        recyclerActivities.setVisibility(View.GONE);

        apiService.getActivities(selectedCategory, null, selectedMonth, null)
                .enqueue(new Callback<ApiResponse<List<Activity>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                           Response<ApiResponse<List<Activity>>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            activities.clear();
                            activities.addAll(response.body().getData());
                            activityAdapter.notifyDataSetChanged();
                            boolean empty = activities.isEmpty();
                            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                            recyclerActivities.setVisibility(empty ? View.GONE : View.VISIBLE);
                        } else {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private int calculateAgeInMonths(String birthDate) {
        if (birthDate == null) return 6;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birth = sdf.parse(birthDate);
            if (birth == null) return 6;
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birth);
            Calendar now = Calendar.getInstance();
            int months = (now.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12
                    + (now.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH));
            return Math.max(1, months);
        } catch (Exception e) {
            return 6;
        }
    }
}
