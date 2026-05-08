package com.smarthome.iot.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsCustomDatePickerDialog extends Dialog {
    private Context context;
    private OnDateSelectedListener listener;
    private Calendar currentCalendar;
    private Calendar selectedCalendar;
    private RecyclerView recyclerViewCalendar;
    private TextView textViewMonthYear;
    private CalendarAdapter calendarAdapter;
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    public interface OnDateSelectedListener {
        void onDateSelected(Date startDate, Date endDate);
    }

    public ReportsCustomDatePickerDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.currentCalendar = Calendar.getInstance();
        this.selectedCalendar = Calendar.getInstance();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_custom_date_picker);

        // Make dialog background transparent
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        initViews();
        setupCalendar();
    }

    private void initViews() {
        textViewMonthYear = findViewById(R.id.textViewMonthYear);
        recyclerViewCalendar = findViewById(R.id.recyclerViewCalendar);
        
        // Ensure RecyclerView is properly initialized
        if (recyclerViewCalendar == null) {
            return;
        }
        
        ImageButton buttonPreviousMonth = findViewById(R.id.buttonPreviousMonth);
        ImageButton buttonNextMonth = findViewById(R.id.buttonNextMonth);
        TextView buttonCancel = findViewById(R.id.buttonCancel);
        TextView buttonConfirm = findViewById(R.id.buttonConfirm);

        buttonPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        buttonNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        buttonCancel.setOnClickListener(v -> dismiss());

        buttonConfirm.setOnClickListener(v -> {
            if (listener != null && selectedCalendar != null) {
                // For custom range, we'll use the selected date as both start and end
                // In a full implementation, you'd allow selecting start and end dates
                Date selectedDate = selectedCalendar.getTime();
                listener.onDateSelected(selectedDate, selectedDate);
            }
            dismiss();
        });
    }

    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter();
        GridLayoutManager layoutManager = new GridLayoutManager(context, 7);
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewCalendar.setLayoutManager(layoutManager);
        recyclerViewCalendar.setAdapter(calendarAdapter);
        recyclerViewCalendar.setHasFixedSize(false);
        recyclerViewCalendar.setNestedScrollingEnabled(false);
        
        // Update calendar after RecyclerView is laid out
        updateCalendar();
    }

    private void updateCalendar() {
        textViewMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));
        calendarAdapter.updateCalendar(currentCalendar);
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DateViewHolder> {
        private List<CalendarItem> calendarItems = new ArrayList<>();

        public void updateCalendar(Calendar calendar) {
            calendarItems.clear();
            
            if (calendar == null) {
                return;
            }
            
            Calendar cal = (Calendar) calendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            
            // Get first day of week (adjust for Monday = 0)
            // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            // Convert to Monday=0, Tuesday=1, ..., Sunday=6
            int offset = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - Calendar.MONDAY;
            
            // Add empty cells for days before the first day of the month
            Calendar prevMonth = (Calendar) cal.clone();
            prevMonth.add(Calendar.MONTH, -1);
            int daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            // Add empty cells for days before the first day of the month
            // We need to show the last few days of the previous month
            for (int i = offset - 1; i >= 0; i--) {
                int day = daysInPrevMonth - i;
                calendarItems.add(new CalendarItem(day, false, false, false));
            }
            
            // Add days of current month
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar today = Calendar.getInstance();
            
            for (int day = 1; day <= daysInMonth; day++) {
                boolean isToday = (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        day == today.get(Calendar.DAY_OF_MONTH));
                boolean isSelected = (selectedCalendar != null &&
                        cal.get(Calendar.YEAR) == selectedCalendar.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH) &&
                        day == selectedCalendar.get(Calendar.DAY_OF_MONTH));
                calendarItems.add(new CalendarItem(day, true, isToday, isSelected));
            }
            
            // Fill remaining cells to complete the grid (next month days)
            int remainingCells = 42 - calendarItems.size(); // 6 rows * 7 days
            if (remainingCells > 0) {
                for (int day = 1; day <= remainingCells; day++) {
                    calendarItems.add(new CalendarItem(day, false, false, false));
                }
            }
            
            // Ensure we have exactly 42 items (6 rows * 7 days)
            while (calendarItems.size() < 42) {
                calendarItems.add(new CalendarItem(0, false, false, false));
            }
            
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_date, parent, false);
            return new DateViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
            CalendarItem item = calendarItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return calendarItems.size();
        }

        class DateViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            DateViewHolder(@NonNull View itemView) {
                super(itemView);
                // Find TextView inside FrameLayout
                if (itemView instanceof TextView) {
                    textView = (TextView) itemView;
                } else {
                    textView = itemView.findViewById(R.id.textViewDate);
                }
            }

            void bind(CalendarItem item) {
                if (textView == null) {
                    return;
                }
                
                // Handle empty cells (day = 0)
                if (item.day == 0) {
                    textView.setText("");
                    textView.setVisibility(View.INVISIBLE);
                    textView.setClickable(false);
                    return;
                }
                
                // Always set text, regardless of month
                textView.setText(String.valueOf(item.day));
                textView.setVisibility(View.VISIBLE);
                textView.setClickable(true);
                
                if (item.isCurrentMonth) {
                    textView.setAlpha(1.0f);
                    textView.setTextColor(context.getResources().getColor(R.color.white));
                    
                    if (item.isToday) {
                        // Create a circular background for today
                        textView.setBackgroundResource(R.drawable.bg_circle_orange);
                        textView.setTextColor(context.getResources().getColor(R.color.white));
                    } else if (item.isSelected) {
                        // Selected date styling - orange circle background (matching Figma)
                        textView.setBackgroundResource(R.drawable.bg_calendar_date_selected);
                        textView.setTextColor(context.getResources().getColor(R.color.white));
                    } else {
                        textView.setBackground(null);
                        textView.setTextColor(context.getResources().getColor(R.color.white));
                    }
                    
                    textView.setOnClickListener(v -> {
                        Calendar cal = (Calendar) currentCalendar.clone();
                        cal.set(Calendar.DAY_OF_MONTH, item.day);
                        selectedCalendar = cal;
                        ReportsCustomDatePickerDialog.this.updateCalendar();
                    });
                } else {
                    // Previous/next month days
                    textView.setAlpha(0.3f);
                    textView.setTextColor(context.getResources().getColor(R.color.white));
                    textView.setBackground(null);
                    textView.setOnClickListener(null);
                }
            }
        }
    }

    private static class CalendarItem {
        int day;
        boolean isCurrentMonth;
        boolean isToday;
        boolean isSelected;

        CalendarItem(int day, boolean isCurrentMonth, boolean isToday, boolean isSelected) {
            this.day = day;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.isSelected = isSelected;
        }
    }
}
