package com.talentbaby.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.talentbaby.app.R;
import com.talentbaby.app.activities.ActivityDetailActivity;
import com.talentbaby.app.adapters.TodayActivityAdapter;
import com.talentbaby.app.models.DailyActivity;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.ui.home.HomeViewModel;
import com.talentbaby.app.utils.ApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private TodayActivityAdapter activityAdapter;

    private RecyclerView recyclerTodayActivities;
    private MaterialCardView cardEmptyActivities;
    private View progressActivities;
    private View progressTips;
    private TextView textTipsContent;

    // Date selector — now single TextViews (not stacked pairs)
    private TextView layoutDatePrev;
    private TextView layoutDateCenter;
    private TextView layoutDateNext;

    /** Offset from today: 0=today, -1=yesterday, +1=tomorrow, etc. */
    private int selectedDayOffset = 0;

    private TextView tabPhysical, tabCognitive, tabSpeech, tabSocial;
    private TextView activeTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        viewModel.init(apiService, requireContext());

        initViews(view);
        setupRecyclerView();
        setupDateSelector();
        setupCategoryTabs();
        observeViewModel();

        viewModel.loadUserProfile();
        viewModel.loadBabies();

        view.postDelayed(this::showSampleTip, 1500);
    }

    private void initViews(View view) {
        recyclerTodayActivities = view.findViewById(R.id.recyclerTodayActivities);
        cardEmptyActivities = view.findViewById(R.id.cardEmptyActivities);
        progressActivities = view.findViewById(R.id.progressActivities);
        progressTips = view.findViewById(R.id.progressTips);
        textTipsContent = view.findViewById(R.id.textTipsContent);

        layoutDatePrev   = view.findViewById(R.id.layoutDateYesterday);
        layoutDateCenter = view.findViewById(R.id.layoutDateToday);
        layoutDateNext   = view.findViewById(R.id.layoutDateTomorrow);

        tabPhysical  = view.findViewById(R.id.tabPhysical);
        tabCognitive = view.findViewById(R.id.tabCognitive);
        tabSpeech    = view.findViewById(R.id.tabSpeech);
        tabSocial    = view.findViewById(R.id.tabSocial);

        // Parenting support (LinearLayout pill)
        view.findViewById(R.id.btnParentingSupport).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.parenting_support), Toast.LENGTH_SHORT).show());

        // Menu → sidebar
        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).openDrawer();
            }
        });

        // View All → switch to Activities tab
        view.findViewById(R.id.textViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).switchToTab(R.id.nav_activities);
            }
        });

        // Category grid
        view.findViewById(R.id.cardCatActivities).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).switchToTab(R.id.nav_activities);
            }
        });
        view.findViewById(R.id.cardCatMilestones).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).switchToTab(R.id.nav_milestones);
            }
        });
        view.findViewById(R.id.cardCatWeight).setOnClickListener(v ->
                startActivity(new Intent(getActivity(),
                        com.talentbaby.app.activities.GrowthTrackerActivity.class)));
        view.findViewById(R.id.cardCatNutrition).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.category_nutrition), Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.cardCatStories).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.category_stories), Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.cardCatArticles).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.category_articles), Toast.LENGTH_SHORT).show());

        // Tracker
        view.findViewById(R.id.cardFeeding).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.feeding), Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.cardDiaper).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.poop_pee), Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.cardSleep).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.sleep), Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.cardViewAnalysis).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.view_analysis), Toast.LENGTH_SHORT).show());

        // Milestone achieved
        view.findViewById(R.id.btnMilestoneAchieved).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.milestone_achieved), Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        List<DailyActivity> dailyActivities = new ArrayList<>();
        activityAdapter = new TodayActivityAdapter(dailyActivities, activity -> {
            Intent intent = new Intent(getActivity(), ActivityDetailActivity.class);
            intent.putExtra("activity_id", activity.getId());
            startActivity(intent);
        });
        recyclerTodayActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTodayActivities.setAdapter(activityAdapter);
        recyclerTodayActivities.setNestedScrollingEnabled(false);
    }

    private void setupDateSelector() {
        refreshDateSelector();

        layoutDatePrev.setOnClickListener(v -> {
            selectedDayOffset--;
            refreshDateSelector();
            onSelectedDateChanged();
        });
        layoutDateCenter.setOnClickListener(v -> { /* already selected */ });
        layoutDateNext.setOnClickListener(v -> {
            selectedDayOffset++;
            refreshDateSelector();
            onSelectedDateChanged();
        });
    }

    /** Formats date as "20th Apr" with ordinal suffix. */
    private String formatDateLabel(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String suffix;
        if (day >= 11 && day <= 13) suffix = "th";
        else switch (day % 10) {
            case 1: suffix = "st"; break;
            case 2: suffix = "nd"; break;
            case 3: suffix = "rd"; break;
            default: suffix = "th";
        }
        String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
        return day + suffix + " " + month;
    }

    private void refreshDateSelector() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, selectedDayOffset - 1);
        layoutDatePrev.setText(formatDateLabel(cal));

        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, selectedDayOffset);
        layoutDateCenter.setText(formatDateLabel(cal));

        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, selectedDayOffset + 1);
        layoutDateNext.setText(formatDateLabel(cal));

        // Visual state
        layoutDateCenter.setBackgroundResource(R.drawable.bg_date_selected);
        layoutDateCenter.setTextColor(0xFFFFFFFF);

        layoutDatePrev.setBackground(null);
        layoutDatePrev.setTextColor(0xFF9A8A80);

        layoutDateNext.setBackground(null);
        layoutDateNext.setTextColor(0xFF9A8A80);
    }

    private void onSelectedDateChanged() {
        Calendar selected = Calendar.getInstance();
        selected.add(Calendar.DAY_OF_MONTH, selectedDayOffset);
        viewModel.loadActivitiesForDate(selected);
    }

    private void setupCategoryTabs() {
        activeTab = tabPhysical;
        View.OnClickListener tabListener = v -> setActiveTab((TextView) v);
        tabPhysical.setOnClickListener(tabListener);
        tabCognitive.setOnClickListener(tabListener);
        tabSpeech.setOnClickListener(tabListener);
        tabSocial.setOnClickListener(tabListener);
    }

    private void setActiveTab(TextView tab) {
        if (activeTab == tab) return;
        activeTab.setBackgroundResource(R.drawable.bg_tab_unselected);
        activeTab.setTextColor(0xFF5C4A42);
        tab.setBackgroundResource(R.drawable.bg_coral_tab);
        tab.setTextColor(0xFFFFFFFF);
        activeTab = tab;
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
                progressActivities.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getActivities().observe(getViewLifecycleOwner(), dailyList -> {
            if (dailyList != null) {
                activityAdapter.updateActivities(dailyList);
                boolean isEmpty = dailyList.isEmpty();
                cardEmptyActivities.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                recyclerTodayActivities.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSampleTip() {
        if (progressTips == null || textTipsContent == null) return;
        progressTips.setVisibility(View.GONE);
        textTipsContent.setText("Tummy time helps strengthen your baby's neck, shoulder, and arm muscles. " +
                "Try 2–3 short sessions of 3–5 minutes each day.");
        textTipsContent.setVisibility(View.VISIBLE);
    }
}
