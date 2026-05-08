package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder> {

    private static final int[] PAGE_LAYOUTS = {
            R.layout.item_onboarding_walkthrough_1,
            R.layout.item_onboarding_walkthrough_2,
            R.layout.item_onboarding_walkthrough_3
    };

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Directly inflate the walkthrough layout for this position
        View view = LayoutInflater.from(parent.getContext())
                .inflate(PAGE_LAYOUTS[viewType], parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        // The walkthrough layouts are already complete, no binding needed
    }

    @Override
    public int getItemCount() {
        return PAGE_LAYOUTS.length;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
