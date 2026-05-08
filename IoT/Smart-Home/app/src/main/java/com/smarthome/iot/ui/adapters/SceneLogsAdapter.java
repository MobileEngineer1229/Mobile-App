package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SceneLogsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_LOG_ENTRY = 1;

    private List<Object> items; // Can be DateHeader or SceneLog

    public static class DateHeader {
        public Date date;
        public String dayNumber;
        public String monthYear;
        public String dayOfWeek;

        public DateHeader(Date date) {
            this.date = date;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            this.dayNumber = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
            
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
            this.monthYear = monthYearFormat.format(date);
            
            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                this.dayOfWeek = "Today, " + new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
            } else if (cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                       cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
                this.dayOfWeek = "Yesterday, " + new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
            } else {
                this.dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
            }
        }
    }

    public SceneLogsAdapter() {
        this.items = new ArrayList<>();
    }

    public void setLogs(List<SceneLog> logs) {
        items.clear();
        if (logs == null || logs.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Group logs by date
        Date currentDate = null;
        for (SceneLog log : logs) {
            Date logDate = log.getExecutionTimestamp();
            if (logDate == null) continue;

            Calendar logCal = Calendar.getInstance();
            logCal.setTime(logDate);
            logCal.set(Calendar.HOUR_OF_DAY, 0);
            logCal.set(Calendar.MINUTE, 0);
            logCal.set(Calendar.SECOND, 0);
            logCal.set(Calendar.MILLISECOND, 0);
            Date logDateOnly = logCal.getTime();

            // Add date header if this is a new date
            if (currentDate == null || !logDateOnly.equals(currentDate)) {
                currentDate = logDateOnly;
                items.add(new DateHeader(logDateOnly));
            }
            items.add(log);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        return item instanceof DateHeader ? TYPE_DATE_HEADER : TYPE_LOG_ENTRY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_scene_log_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_scene_log_entry, parent, false);
            return new LogEntryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            DateHeader header = (DateHeader) items.get(position);
            DateHeaderViewHolder vh = (DateHeaderViewHolder) holder;
            vh.textViewDayNumber.setText(header.dayNumber);
            vh.textViewMonthYear.setText(header.monthYear);
            vh.textViewDayOfWeek.setText(header.dayOfWeek);
        } else if (holder instanceof LogEntryViewHolder) {
            SceneLog log = (SceneLog) items.get(position);
            LogEntryViewHolder vh = (LogEntryViewHolder) holder;
            
            vh.textViewSceneName.setText(log.getSceneName() != null ? log.getSceneName() : "");
            
            // Format time and status
            String timeStr = "";
            if (log.getExecutionTimestamp() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeStr = timeFormat.format(log.getExecutionTimestamp());
            }
            
            String statusText = "succeeded".equals(log.getStatus()) ? 
                "Processing Succeeded" : "Processing Failed";
            String details = timeStr + " · " + statusText;
            vh.textViewDetails.setText(details);
            
            // Set status icon and background
            if ("succeeded".equals(log.getStatus())) {
                vh.imageViewStatus.setImageResource(R.drawable.ic_check_circle);
                vh.imageViewStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                vh.imageViewStatus.setBackgroundResource(R.drawable.bg_circle_primary_small);
            } else {
                vh.imageViewStatus.setImageResource(R.drawable.ic_error);
                vh.imageViewStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                vh.imageViewStatus.setBackgroundResource(R.drawable.bg_circle_error);
            }
            
            // Show timeline if not last item
            boolean isLast = position == getItemCount() - 1;
            boolean isNextItemDateHeader = position < getItemCount() - 1 && 
                getItemViewType(position + 1) == TYPE_DATE_HEADER;
            vh.viewTimeline.setVisibility((isLast || isNextItemDateHeader) ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDayNumber;
        TextView textViewMonthYear;
        TextView textViewDayOfWeek;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDayNumber = itemView.findViewById(R.id.textViewDayNumber);
            textViewMonthYear = itemView.findViewById(R.id.textViewMonthYear);
            textViewDayOfWeek = itemView.findViewById(R.id.textViewDayOfWeek);
        }
    }

    static class LogEntryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewStatus;
        View viewTimeline;
        TextView textViewSceneName;
        TextView textViewDetails;

        LogEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewStatus = itemView.findViewById(R.id.imageViewStatus);
            viewTimeline = itemView.findViewById(R.id.viewTimeline);
            textViewSceneName = itemView.findViewById(R.id.textViewSceneName);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
        }
    }
}
