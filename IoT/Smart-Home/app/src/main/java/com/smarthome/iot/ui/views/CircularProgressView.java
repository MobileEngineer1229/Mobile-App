package com.smarthome.iot.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.smarthome.iot.R;

public class CircularProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private float progress = 0f; // 0-10000 (matching ProgressBar max)
    private float strokeWidth = 4f; // 4dp converted to pixels
    private RectF boundsRect = new RectF();

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int backgroundColor = 0x1A405FF2; // #1A405FF2 (semi-transparent primary)
        int progressColor = ContextCompat.getColor(getContext(), R.color.primary);

        // Background paint (semi-transparent)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        // Progress paint (primary color)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Convert dp to pixels when we have valid dimensions
        float density = getResources().getDisplayMetrics().density;
        strokeWidth = 4f * density; // 4dp
        backgroundPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Calculate radius (accounting for stroke width)
        float radius = Math.min(width, height) / 2f - strokeWidth / 2f;

        // Set up bounds for arc drawing
        boundsRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );

        // Draw background circle (full 360 degrees)
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        // Draw progress arc starting from 12 o'clock (270 degrees = top)
        // Progress is 0-10000, convert to 0-360 degrees
        if (progress > 0) {
            float sweepAngle = (progress / 10000f) * 360f;
            canvas.drawArc(boundsRect, 270f, sweepAngle, false, progressPaint);
        }
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(10000, progress));
        invalidate();
    }

    public int getProgress() {
        return (int) progress;
    }
}
