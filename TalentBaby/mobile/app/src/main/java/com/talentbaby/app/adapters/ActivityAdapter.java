package com.talentbaby.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.talentbaby.app.R;
import com.talentbaby.app.models.Activity;
import com.talentbaby.app.utils.ApiClient;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
    private final List<Activity> activities;
    private final OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
    }

    public ActivityAdapter(List<Activity> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Activity activity = activities.get(position);
        holder.bind(activity);
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageActivity;
        private final ImageView iconStatus;
        private final TextView textTitle;
        private final TextView textCategory;
        private final TextView textDuration;
        private final TextView textAge;
        private final TextView textDoneByCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageActivity = itemView.findViewById(R.id.imageActivity);
            iconStatus = itemView.findViewById(R.id.iconVerified);
            textTitle = itemView.findViewById(R.id.textActivityTitle);
            textCategory = itemView.findViewById(R.id.textActivityCategory);
            textDuration = itemView.findViewById(R.id.textActivityDuration);
            textAge = itemView.findViewById(R.id.textActivityAge);
            textDoneByCount = itemView.findViewById(R.id.textDoneByCount);
        }

        void bind(Activity activity) {
            textTitle.setText(activity.getTitle());
            String sub = activity.getSubCategory();
            textCategory.setText(sub != null && !sub.isEmpty() ? sub : (activity.getCategory() != null ? activity.getCategory() : ""));
            if (iconStatus != null) {
                iconStatus.setImageResource(R.drawable.ic_lock);
                iconStatus.setVisibility(View.VISIBLE);
            }
            if (textDoneByCount != null) {
                if (activity.getDoneCount() > 0) {
                    textDoneByCount.setText(itemView.getContext().getString(R.string.done_by_babies, activity.getDoneCount()));
                    textDoneByCount.setVisibility(View.VISIBLE);
                } else {
                    textDoneByCount.setVisibility(View.GONE);
                }
            }

            if (activity.getDurationMinutes() > 0) {
                textDuration.setText(itemView.getContext().getString(R.string.minutes, activity.getDurationMinutes()));
                textDuration.setVisibility(View.VISIBLE);
            } else {
                textDuration.setVisibility(View.GONE);
            }

            if (activity.getMinAgeMonths() > 0 || activity.getMaxAgeMonths() > 0) {
                textAge.setText(itemView.getContext().getString(R.string.age_range,
                        activity.getMinAgeMonths(), activity.getMaxAgeMonths()));
                textAge.setVisibility(View.VISIBLE);
            } else {
                textAge.setVisibility(View.GONE);
            }

            if (activity.getImageUrl() != null && !activity.getImageUrl().isEmpty()) {
                String imageUrl = activity.getImageUrl();
                if (imageUrl.startsWith("/")) {
                    imageUrl = ApiClient.getBaseUrl() + imageUrl.substring(1);
                }
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.rounded_image_background)
                        .centerCrop()
                        .into(imageActivity);
            }

            itemView.setOnClickListener(v -> listener.onActivityClick(activity));
        }
    }
}
