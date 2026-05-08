package com.smarthome.iot.ui.drawables;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.smarthome.iot.R;

public class CircularProgressDrawable extends Drawable {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private float progress = 0f;
    private float strokeWidth = 4f; // Thin stroke in pixels
    private int backgroundColor;
    private int progressColor;
    private RectF boundsRect = new RectF();

    public CircularProgressDrawable(int backgroundColor, int progressColor, float strokeWidthDp) {
        this.backgroundColor = backgroundColor;
        this.progressColor = progressColor;
        this.strokeWidth = strokeWidthDp;

        // Background paint (semi-transparent)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        // Progress paint (primary color)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        
        // Calculate radius (accounting for stroke width)
        float radius = Math.min(bounds.width(), bounds.height()) / 2f - strokeWidth / 2f;
        
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
        // Progress is 0-100, convert to 0-360 degrees
        float sweepAngle = (progress / 10000f) * 360f;
        canvas.drawArc(boundsRect, 270f, sweepAngle, false, progressPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        backgroundPaint.setAlpha(alpha);
        progressPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        progressPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(10000, progress));
        invalidateSelf();
    }

    public float getProgress() {
        return progress;
    }
}
