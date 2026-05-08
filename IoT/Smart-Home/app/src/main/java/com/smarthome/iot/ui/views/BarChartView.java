package com.smarthome.iot.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.smarthome.iot.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {
    private List<BarData> barDataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private Paint circlePaint;
    private Paint circleTextPaint;
    private float barWidth;
    private float barSpacing;
    private float bottomTextHeight = 50f;
    private int highlightedIndex = -1;
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");

    public static class BarData {
        public String label;
        public float value;
        public float maxValue;

        public BarData(String label, float value, float maxValue) {
            this.label = label;
            this.value = value;
            this.maxValue = maxValue;
        }
    }

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary));
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        labelPaint.setAlpha(178); // 0.7 * 255
        labelPaint.setTextSize(20f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Tooltip background paint (white oval)
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        circlePaint.setStyle(Paint.Style.FILL);

        // Tooltip text paint (black text)
        circleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        circleTextPaint.setTextSize(14f);
        circleTextPaint.setTextAlign(Paint.Align.CENTER);
        circleTextPaint.setFakeBoldText(true);

        // Set demo data
        setDemoData();
    }

    public void setDemoData() {
        barDataList.clear();
        barDataList.add(new BarData("Jul", 650.30f, 1000f));
        barDataList.add(new BarData("Aug", 720.45f, 1000f));
        barDataList.add(new BarData("Sept", 890.20f, 1000f));
        barDataList.add(new BarData("Oct", 785.48f, 1000f));
        barDataList.add(new BarData("Nov", 958.75f, 1000f));
        barDataList.add(new BarData("Dec", 825.40f, 1000f));
        highlightedIndex = 3; // Highlight October
        invalidate();
    }

    public void setData(List<BarData> data, int highlightIndex) {
        this.barDataList = data;
        this.highlightedIndex = highlightIndex;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (barDataList.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int barCount = barDataList.size();

        // Calculate bar dimensions with proper spacing
        // Use fixed spacing between bars for consistent appearance
        float desiredBarSpacing = 16f * getResources().getDisplayMetrics().density; // 16dp spacing
        float totalSpacing = desiredBarSpacing * (barCount - 1); // Spacing between bars
        float totalBarWidth = width - totalSpacing;
        barWidth = totalBarWidth / barCount;
        barSpacing = desiredBarSpacing;

        float chartHeight = height - bottomTextHeight;
        float maxBarHeight = chartHeight - 100f; // Leave space for circle and text

        // Draw bars
        for (int i = 0; i < barDataList.size(); i++) {
            BarData barData = barDataList.get(i);
            // Calculate x position: start from left edge, add spacing between bars
            float x = (barWidth + barSpacing) * i;
            float normalizedHeight = (barData.value / barData.maxValue) * maxBarHeight;
            float y = chartHeight - normalizedHeight - 30f;

            // Draw bar with fully rounded top edge and flat vertical bottom
            float barTop = y;
            float barBottom = chartHeight - 30f;
            float cornerRadius = barWidth / 2f; // Full radius for semicircular top
            
            // Create path: fully rounded top (semicircle), flat vertical bottom
            Path barPath = new Path();
            
            // Start from bottom-left corner (sharp corner, no rounding)
            barPath.moveTo(x, barBottom);
            // Line to bottom-right corner (sharp corner, no rounding)
            barPath.lineTo(x + barWidth, barBottom);
            // Line up the right side to where the rounded top begins
            barPath.lineTo(x + barWidth, barTop + cornerRadius);
            // Draw semicircular arc for fully rounded top
            RectF topArcRect = new RectF(x, barTop, x + barWidth, barTop + cornerRadius * 2);
            // Start at 0° (rightmost point), sweep -180° (counterclockwise to leftmost point)
            barPath.arcTo(topArcRect, 0, -180, false);
            // Close path (connects back to starting point, completing the shape)
            barPath.close();
            
            canvas.drawPath(barPath, barPaint);

            // Draw label
            float labelY = chartHeight + 8f;
            canvas.drawText(barData.label, x + barWidth / 2f, labelY, labelPaint);

            // Draw highlighted tooltip (white oval with black text)
            if (i == highlightedIndex) {
                float tooltipY = y - 50f;
                float tooltipWidth = 120f;
                float tooltipHeight = 50f;
                float tooltipX = x + barWidth / 2f - tooltipWidth / 2f;
                float tooltipCornerRadius = 25f; // Fully rounded oval

                // Draw white oval/rounded rectangle background
                RectF tooltipRect = new RectF(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight);
                canvas.drawRoundRect(tooltipRect, tooltipCornerRadius, tooltipCornerRadius, circlePaint);

                // Draw value text inside tooltip (black text)
                String valueText = decimalFormat.format(barData.value) + " kWh";
                Paint.FontMetrics fontMetrics = circleTextPaint.getFontMetrics();
                float textHeight = fontMetrics.descent - fontMetrics.ascent;
                float textY = tooltipY + tooltipHeight / 2f + textHeight / 2f - fontMetrics.descent;
                
                canvas.drawText(valueText, x + barWidth / 2f, textY, circleTextPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
