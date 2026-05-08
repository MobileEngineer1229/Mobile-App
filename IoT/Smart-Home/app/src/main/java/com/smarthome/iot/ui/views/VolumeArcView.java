package com.smarthome.iot.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class VolumeArcView extends View {
    private Paint arcPaint;
    private Paint inactiveArcPaint;
    private Paint selectorPaint;
    private Paint selectorStrokePaint;
    private Paint tickMarkPaint;
    private RectF arcBounds;
    private float strokeWidth;
    private float selectorWidth;
    private float selectorStrokeWidth;
    private float currentAngle = 0f; // Start at 0% volume
    private float startAngle = 135f; // Arc starts at 135° (top-left) for three-quarter circle
    private float sweepAngle = 270f; // Arc spans 270° (three-quarter circle)
    private OnVolumeChangeListener listener;
    
    public interface OnVolumeChangeListener {
        void onVolumeChanged(int volume); // Volume: 0-100
    }

    public VolumeArcView(Context context) {
        super(context);
        init();
    }

    public VolumeArcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeArcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        
        // Arc stroke width: 24dp (matching color wheel)
        strokeWidth = 24f * density;

        // Selector (progress indicator) size: 30dp (matching design)
        selectorWidth = 30f * density;

        // Selector stroke width: 4dp (blue border matching design)
        selectorStrokeWidth = 4f * density;
        
        // Main arc paint - solid blue color for active portion, dark gray for inactive
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setColor(0xFF405FF2); // Primary blue color
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Inactive arc paint (for the portion not yet reached)
        inactiveArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inactiveArcPaint.setStyle(Paint.Style.STROKE);
        inactiveArcPaint.setStrokeWidth(strokeWidth);
        inactiveArcPaint.setColor(0xFF262A35); // Dark gray for inactive portion
        inactiveArcPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Selector paint (white filled circle)
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(0xFFFFFFFF); // White

        // Selector stroke paint (blue border)
        selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorStrokePaint.setStyle(Paint.Style.STROKE);
        selectorStrokePaint.setColor(0xFF405FF2); // Primary blue
        selectorStrokePaint.setStrokeWidth(selectorStrokeWidth);

        // Tick marks paint (semi-transparent white)
        tickMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickMarkPaint.setStyle(Paint.Style.STROKE);
        tickMarkPaint.setStrokeWidth(1f * density);
        tickMarkPaint.setColor(0x80FFFFFF);

        arcBounds = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Fixed size: 330dp x 330dp
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (330f * density);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Calculate arc bounds for an almost-full circle
        float centerX = w / 2f;
        float centerY = h / 2f; // Circle center at view center
        
        float density = getResources().getDisplayMetrics().density;
        float totalSize = 330f * density;
        
        // Calculate radius
        float radius = (totalSize - strokeWidth) / 2f;
        
        // Arc bounds: full circle centered in view
        arcBounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (arcBounds.isEmpty()) {
            return;
        }
        
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = (arcBounds.right - arcBounds.left) / 2f;
        
        // Draw three-quarter circle arc from 135° (top-left) sweeping 270° clockwise
        float androidStartAngle = 135f; // Top-left
        float androidSweepAngle = sweepAngle; // 270° sweep
        
        // Draw inactive portion first (dark gray)
        float currentSweep = (currentAngle / 100f) * sweepAngle;
        float remainingSweep = sweepAngle - currentSweep;
        if (remainingSweep > 0) {
            float inactiveStartAngle = androidStartAngle + currentSweep;
            canvas.drawArc(arcBounds, inactiveStartAngle, remainingSweep, false, inactiveArcPaint);
        }
        
        // Draw active portion (blue) up to current volume
        if (currentSweep > 0) {
            canvas.drawArc(arcBounds, androidStartAngle, currentSweep, false, arcPaint);
        }
        
        // Draw tick marks (inward from inner arc edge, like a speedometer gauge)
        drawTickMarks(canvas, centerX, centerY, radius, androidStartAngle, androidSweepAngle);

        // Draw selector (white circle with blue border) at current angle
        float androidAngle = androidStartAngle + currentSweep;
        float angleRad = (float) Math.toRadians(androidAngle);
        float selectorX = centerX + radius * (float) Math.cos(angleRad);
        float selectorY = centerY + radius * (float) Math.sin(angleRad);

        // Draw white circular pointer with blue stroke
        float selectorRadius = selectorWidth / 2f;
        canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorPaint);
        if (selectorStrokeWidth > 0) {
            float strokeRadius = selectorRadius - selectorStrokeWidth / 2f;
            canvas.drawCircle(selectorX, selectorY, strokeRadius, selectorStrokePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = (arcBounds.right - arcBounds.left) / 2f;
            
            // Calculate angle from touch position
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            
            // Calculate distance from center
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            // Check if touch is near the arc (within reasonable range)
            float minDistance = radius - strokeWidth / 2f - selectorWidth / 2f;
            float maxDistance = radius + strokeWidth / 2f + selectorWidth / 2f;
            
            if (distance < minDistance || distance > maxDistance) {
                return false; // Touch too far from arc
            }
            
            // Calculate angle in degrees
            float angleRad = (float) Math.atan2(dy, dx);
            
            // Normalize angle to 0-360 range
            float normalizedAngle = (float) Math.toDegrees(angleRad);
            if (normalizedAngle < 0) {
                normalizedAngle += 360f;
            }
            
            // Convert Android angle to volume (0-100) for three-quarter circle
            // Arc goes from 135° (top-left) to 405° (135° + 270° = 405°, which is 45°)
            // Wrap around if angle is before start
            if (normalizedAngle < 135f) {
                normalizedAngle += 360f; // Wrap around
            }
            
            // Check if angle is within the arc range (135° to 405°)
            float volume = 0f;
            if (normalizedAngle >= 135f && normalizedAngle <= 405f) {
                // Map from 135°-405° to 0-100
                float progress = ((normalizedAngle - 135f) / 270f) * 100f;
                volume = Math.max(0f, Math.min(100f, progress));
            } else {
                // Clamp to nearest edge
                if (normalizedAngle < 135f) {
                    volume = 0f; // Start end
                } else {
                    volume = 100f; // End
                }
            }
            
            // Clamp to valid range
            volume = Math.max(0f, Math.min(100f, volume));
            
            currentAngle = volume;
            invalidate();
            
            if (listener != null) {
                listener.onVolumeChanged((int) volume);
            }
            
            return true;
        }
        
        return super.onTouchEvent(event);
    }

    public void setVolume(int volume) {
        // Volume: 0-100
        currentAngle = Math.max(0f, Math.min(100f, volume));
        invalidate();
    }

    public int getVolume() {
        return (int) currentAngle;
    }

    public void setOnVolumeChangeListener(OnVolumeChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Draw tick marks along the arc (pointing inward from inner arc edge toward center, like a speedometer gauge)
     */
    private void drawTickMarks(Canvas canvas, float centerX, float centerY, float radius, float startAngle, float sweepAngle) {
        float density = getResources().getDisplayMetrics().density;
        float majorTickLength = 10f * density;
        float minorTickLength = 5f * density;
        float tickSpacing = 5f;
        int numTicks = (int) (sweepAngle / tickSpacing);

        for (int i = 0; i <= numTicks; i++) {
            float angle = startAngle + (tickSpacing * i);
            float angleRad = (float) Math.toRadians(angle);

            boolean isMajor = (i % 3 == 0);
            float tickLen = isMajor ? majorTickLength : minorTickLength;

            float arcInnerRadius = radius - strokeWidth / 2f;
            float x1 = centerX + arcInnerRadius * (float) Math.cos(angleRad);
            float y1 = centerY + arcInnerRadius * (float) Math.sin(angleRad);

            float tickInnerRadius = arcInnerRadius - tickLen;
            float x2 = centerX + tickInnerRadius * (float) Math.cos(angleRad);
            float y2 = centerY + tickInnerRadius * (float) Math.sin(angleRad);

            tickMarkPaint.setAlpha(isMajor ? 153 : 77);
            canvas.drawLine(x1, y1, x2, y2, tickMarkPaint);
        }
    }
}

