package com.talentbaby.app.ui.milestones;

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
import com.talentbaby.app.adapters.MilestoneAdapter;
import com.talentbaby.app.models.ApiResponse;
import com.talentbaby.app.models.Baby;
import com.talentbaby.app.models.BabyMilestone;
import com.talentbaby.app.models.MilestoneDefinition;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.utils.ApiClient;
import com.talentbaby.app.utils.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MilestonesFragment extends Fragment {

    private RecyclerView recyclerMilestones;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutMonthPills;
    private MilestoneAdapter milestoneAdapter;
    private ApiService apiService;

    private int selectedMonth = 2;
    private int babyId = -1;
    private TextView activeMonthPill = null;

    // Loaded definitions grouped by type
    private Map<String, List<MilestoneDefinition>> groupedDefinitions = new HashMap<>();
    // Current status for each definition id
    private Map<Integer, String> statusMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_milestones, container, false);
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
        recyclerMilestones = view.findViewById(R.id.recyclerMilestones);
        progressBar        = view.findViewById(R.id.progressBarMilestones);
        layoutEmpty        = view.findViewById(R.id.layoutMilestonesEmpty);
        layoutMonthPills   = view.findViewById(R.id.layoutMonthPillsMilestones);

        view.findViewById(R.id.btnMenuMilestones).setOnClickListener(v -> {
            if (getActivity() instanceof com.talentbaby.app.MainActivity) {
                ((com.talentbaby.app.MainActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.btnParentingSupportMs).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.parenting_support), Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        milestoneAdapter = new MilestoneAdapter((definitionId, status) -> {
            // Optimistic update
            if (status == null || status.isEmpty()) {
                statusMap.remove(definitionId);
            } else {
                statusMap.put(definitionId, status);
            }
            milestoneAdapter.updateStatus(definitionId, status);

            // Persist to API
            if (babyId != -1 && status != null && !status.isEmpty()) {
                upsertMilestoneStatus(definitionId, status);
            }
        });
        recyclerMilestones.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMilestones.setAdapter(milestoneAdapter);
        recyclerMilestones.setNestedScrollingEnabled(false);
    }

    private void loadBabyAndSetupMonths() {
        babyId = TokenManager.getBabyId(requireContext());
        if (babyId == -1) {
            setupMonthPills(2);
            return;
        }

        apiService.getBaby(babyId).enqueue(new Callback<ApiResponse<Baby>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Baby>> call,
                                   @NonNull Response<ApiResponse<Baby>> response) {
                int ageMonths = 2;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    ageMonths = calculateAgeInMonths(response.body().getData().getBirthDate());
                }
                setupMonthPills(Math.max(1, ageMonths));
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Baby>> call, @NonNull Throwable t) {
                setupMonthPills(2);
            }
        });
    }

    private void setupMonthPills(int currentMonth) {
        if (layoutMonthPills == null || getContext() == null) return;
        layoutMonthPills.removeAllViews();

        int startMonth = Math.max(1, currentMonth - 2);
        int endMonth   = Math.min(36, currentMonth + 2);

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
                    (int) (getResources().getDisplayMetrics().density * 16), 0);
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
                loadMilestonesForMonth(month);
            });
            layoutMonthPills.addView(pill);
        }

        loadMilestonesForMonth(currentMonth);
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

    private void loadMilestonesForMonth(int month) {
        showLoading(true);
        groupedDefinitions.clear();
        statusMap.clear();

        // Load definitions for the month
        apiService.getMilestoneDefinitionsByMonth(month)
                .enqueue(new Callback<ApiResponse<Map<String, List<MilestoneDefinition>>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Map<String, List<MilestoneDefinition>>>> call,
                                           @NonNull Response<ApiResponse<Map<String, List<MilestoneDefinition>>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            groupedDefinitions = response.body().getData();
                        } else {
                            groupedDefinitions = mockDefinitions(month);
                        }
                        // Now load baby statuses
                        if (babyId != -1) {
                            loadBabyStatuses(month);
                        } else {
                            renderList();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Map<String, List<MilestoneDefinition>>>> call,
                                          @NonNull Throwable t) {
                        groupedDefinitions = mockDefinitions(month);
                        renderList();
                    }
                });
    }

    private void loadBabyStatuses(int month) {
        apiService.getBabyMilestones(babyId, month)
                .enqueue(new Callback<ApiResponse<List<BabyMilestone>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<BabyMilestone>>> call,
                                           @NonNull Response<ApiResponse<List<BabyMilestone>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            for (BabyMilestone bm : response.body().getData()) {
                                if (bm.getStatus() != null) {
                                    statusMap.put(bm.getMilestoneDefinitionId(), bm.getStatus());
                                }
                            }
                        }
                        renderList();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<BabyMilestone>>> call,
                                          @NonNull Throwable t) {
                        renderList();
                    }
                });
    }

    private void renderList() {
        showLoading(false);
        if (groupedDefinitions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerMilestones.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerMilestones.setVisibility(View.VISIBLE);
            milestoneAdapter.setData(groupedDefinitions, statusMap);
        }
    }

    private void upsertMilestoneStatus(int definitionId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("baby_id", babyId);
        body.put("milestone_definition_id", definitionId);
        body.put("status", status);

        apiService.upsertBabyMilestone(body).enqueue(new Callback<ApiResponse<BabyMilestone>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<BabyMilestone>> call,
                                   @NonNull Response<ApiResponse<BabyMilestone>> response) {
                // Status saved silently
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<BabyMilestone>> call, @NonNull Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerMilestones.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // ── Mock data (shown when API is unreachable) ─────────────────────────────

    private Map<String, List<MilestoneDefinition>> mockDefinitions(int month) {
        Map<String, List<MilestoneDefinition>> map = new HashMap<>();

        String[][] physical = {
                {"Perceives light intensity and tries to follow its movement.", null},
                {"Makes eye contact and observe facial expressions", null},
                {"Lifts head briefly when on tummy", null},
                {"Moves arms and legs symmetrically", null},
        };
        String[][] cognitive = {
                {"Responds to sudden sounds by startling", null},
                {"Follows moving objects with eyes briefly", null},
                {"Prefers looking at high-contrast patterns", null},
                {"Shows preference for faces over objects", null},
        };
        String[][] communication = {
                {"Differentiates between high and low-pitched voices", null},
                {"Makes small throaty sounds", null},
                {"Cries differently for hunger vs discomfort", null},
                {"Responds to soothing sounds and music", null},
        };
        String[][] social = {
                {"Calms when picked up or held", null},
                {"Responds positively to caregiver's face and voice", null},
                {"Shows excitement with arm/leg movement when spoken to", null},
                {"Recognises primary caregiver's voice", null},
        };

        map.put("physical",         buildMockList(1, physical));
        map.put("cognitive",        buildMockList(5, cognitive));
        map.put("communication",    buildMockList(9, communication));
        map.put("social_emotional", buildMockList(13, social));
        return map;
    }

    private List<MilestoneDefinition> buildMockList(int startId, String[][] data) {
        List<MilestoneDefinition> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            MilestoneDefinition def = new MilestoneDefinition();
            def.setId(startId + i);
            def.setTitle(data[i][0]);
            def.setQuestion(data[i][0]);
            def.setRelatedActivity(data[i].length > 1 && data[i][1] != null ? data[i][1] : "");
            def.setDoctorVerified(i == 0);
            list.add(def);
        }
        return list;
    }

    private int calculateAgeInMonths(String birthDate) {
        if (birthDate == null) return 2;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birth = sdf.parse(birthDate);
            if (birth == null) return 2;
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birth);
            Calendar now = Calendar.getInstance();
            int months = (now.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12
                    + (now.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH));
            return Math.max(1, months);
        } catch (Exception e) {
            return 2;
        }
    }
}
