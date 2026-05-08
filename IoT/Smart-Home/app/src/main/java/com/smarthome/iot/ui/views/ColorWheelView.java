package com.smarthome.iot.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom view for color wheel control (three-quarter circle)
 * Used in Smart Lamp Color mode
 * Gradient: Red → Yellow → Green → Cyan → Blue → Magenta → Red
 * Based on Figma Angular gradient specification
 */
public class ColorWheelView extends View {
    private Paint arcPaint;
    private Paint selectorPaint;
    private Paint selectorStrokePaint;
    private RectF arcBounds;
    private float strokeWidth;
    private float selectorWidth;
    private float currentAngle = 0f; // 0-100% progress
    private float startAngle = 135f; // Arc starts at 135° (top-left) for three-quarter circle
    private float sweepAngle = 270f; // Arc spans 270° (three-quarter circle)
    private boolean isFullCircle = false; // Mode: false = three-quarter circle, true = full circle
    private OnColorChangeListener listener;
    
    // Full color spectrum gradient: Red → Yellow → Green → Cyan → Blue → Magenta → Red
    // Based on Figma Angular gradient specification
    private int[] gradientColors = {
        0xFFFF0000, // Red (0%)
        0xFFFFE600, // Yellow (16%)
        0xFF05FF00, // Green (33%)
        0xFF00FFFF, // Cyan (50%)
        0xFF001AFF, // Blue (68%)
        0xFFDB00FF, // Magenta (85%)
        0xFFFF0000  // Red (100%)
    };
    
    private float[] gradientPositions = {
        0.0f,    // 0% - Red
        0.16f,   // 16% - Yellow
        0.33f,   // 33% - Green
        0.50f,   // 50% - Cyan
        0.68f,   // 68% - Blue
        0.85f,   // 85% - Magenta
        1.0f     // 100% - Red
    };
    
    public interface OnColorChangeListener {
        void onColorChanged(float angle, int progress);
    }

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        
        // Arc properties - 64dp stroke width for color circle
        strokeWidth = 64f * density; // 64dp stroke width (thick circle)
        selectorWidth = 64f * density; // 64dp selector size (pointer diameter)
        
        // Arc paint with gradient
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Selector paint (blue circular pointer fill)
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(0xFF0000FF); // Blue (will be set to current color)
        
        // Selector stroke paint for white outline
        selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorStrokePaint.setStyle(Paint.Style.STROKE);
        selectorStrokePaint.setColor(0xFFFFFFFF); // White
        selectorStrokePaint.setStrokeWidth(3f * density); // 3dp white stroke
        
        arcBounds = new RectF();
        
        // Start at blue position (100% = blue segment)
        currentAngle = 100f;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Set mode: false for three-quarter circle, true for full circle
     */
    public void setFullCircleMode(boolean fullCircle) {
        this.isFullCircle = fullCircle;
        if (fullCircle) {
            // Full circle mode: 360 degrees
            startAngle = 0f; // Start at 0° (right side, 3 o'clock)
            sweepAngle = 360f; // Full circle (360°)
        } else {
            // Three-quarter circle mode: 270 degrees
            startAngle = 135f; // Start at top-left (135°)
            sweepAngle = 270f; // Three-quarter circle (270°)
        }
        invalidate();
    }

    public void setColor(int progress) {
        currentAngle = Math.max(0f, Math.min(100f, progress));
        invalidate();
    }

    public int getColor() {
        return (int) currentAngle;
    }
    
    /**
     * Get color from gradient based on progress (0.0 to 1.0)
     */
    private int getColorFromGradient(float progress) {
        if (progress <= 0f) {
            return gradientColors[0];
        }
        if (progress >= 1f) {
            return gradientColors[gradientColors.length - 1];
        }
        
        // Find the two colors to interpolate between
        for (int i = 0; i < gradientPositions.length - 1; i++) {
            if (progress >= gradientPositions[i] && progress <= gradientPositions[i + 1]) {
                float localProgress = (progress - gradientPositions[i]) / 
                                     (gradientPositions[i + 1] - gradientPositions[i]);
                return interpolateColor(gradientColors[i], gradientColors[i + 1], localProgress);
            }
        }
        
        return gradientColors[0];
    }
    
    /**
     * Interpolate between two colors
     */
    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
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
        
        float centerX = w / 2f;
        float centerY = h / 2f;
        float density = getResources().getDisplayMetrics().density;
        float totalSize = Math.min(w, h);
        // Calculate radius so stroke is centered: radius = (totalSize - strokeWidth) / 2
        // This positions the stroke center line at the correct distance
        float radius = (totalSize - strokeWidth) / 2f;
        
        arcBounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );
        
        // Create sweep gradient for color wheel
        SweepGradient sweepGradient = new SweepGradient(
            centerX, centerY,
            gradientColors,
            gradientPositions
        );
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        if (isFullCircle) {
            matrix.setRotate(0f, centerX, centerY); // Start at 0° (right side, 3 o'clock) for full circle
        } else {
            matrix.setRotate(135f, centerX, centerY); // Rotate to start at 135° (top-left, green) for three-quarter
        }
        sweepGradient.setLocalMatrix(matrix);
        arcPaint.setShader(sweepGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        // Radius to center of stroke (arcBounds is the center line of the stroke)
        float radius = (arcBounds.width() / 2f);
        
        // Draw arc based on mode
        float androidStartAngle = isFullCircle ? 0f : 135f; // 0° for full circle, 135° for three-quarter
        float androidSweepAngle = isFullCircle ? 360f : 270f; // 360° for full circle, 270° for three-quarter
        canvas.drawArc(arcBounds, androidStartAngle, androidSweepAngle, false, arcPaint);
        
        // Calculate selector position based on currentAngle
        // Position pointer at the center of the stroke (center line of the 64dp stroke)
        float angleOffset = (currentAngle / 100f) * (isFullCircle ? 360f : sweepAngle);
        float androidAngle = androidStartAngle + angleOffset;
        float angleRad = (float) Math.toRadians(androidAngle);
        // Position at center of stroke: radius is already the center line of the stroke
        float selectorX = centerX + radius * (float) Math.cos(angleRad);
        float selectorY = centerY + radius * (float) Math.sin(angleRad);
        
        // Calculate color based on current angle for pointer fill
        float progress = currentAngle / 100f;
        int pointerColor = getColorFromGradient(progress);
        selectorPaint.setColor(pointerColor);
        
        // Draw circular pointer with current color fill and white outline
        float selectorRadius = selectorWidth / 2f;
        canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorPaint);
        // Draw white outline around selector
        if (selectorStrokePaint != null) {
            float strokeRadius = selectorRadius - selectorStrokePaint.getStrokeWidth() / 2f;
            canvas.drawCircle(selectorX, selectorY, strokeRadius, selectorStrokePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = (arcBounds.width() / 2f);
            
            float x = event.getX() - centerX;
            float y = event.getY() - centerY;
            float distance = (float) Math.sqrt(x * x + y * y);
            
            // Check if touch is near the arc (within reasonable distance)
            // Account for stroke width and pointer size
            // radius is the center of the stroke, so we check within strokeWidth/2 on each side
            float minDistance = radius - strokeWidth / 2f - selectorWidth / 2f;
            float maxDistance = radius + strokeWidth / 2f + selectorWidth / 2f;
            
            if (distance >= minDistance && distance <= maxDistance) {
                // Calculate angle from center
                float angle = (float) Math.toDegrees(Math.atan2(y, x));
                if (angle < 0) {
                    angle += 360f;
                }
                
                if (isFullCircle) {
                    // Full circle mode: map angle directly to 0-100 (0° = 0%, 360° = 100%)
                    float progress = (angle / 360f) * 100f;
                    currentAngle = Math.max(0f, Math.min(100f, progress));
                } else {
                    // Three-quarter circle mode: map angle to 0-100
                    // Arc goes from 135° (top-left, green) to 405° (135° + 270° = 405°, which is 45°)
                    float normalizedAngle = angle;
                    if (normalizedAngle < 135f) {
                        normalizedAngle += 360f; // Wrap around
                    }
                    
                    // Check if angle is within the arc range (135° to 405°)
                    if (normalizedAngle >= 135f && normalizedAngle <= 405f) {
                        // Map from 135°-405° to 0-100
                        float progress = ((normalizedAngle - 135f) / 270f) * 100f;
                        currentAngle = Math.max(0f, Math.min(100f, progress));
                    } else {
                        // Clamp to nearest edge
                        if (normalizedAngle < 135f) {
                            currentAngle = 0f; // Green end
                        } else {
                            currentAngle = 100f; // Blue end
                        }
                    }
                }
                
                invalidate();
                
                int progress = (int) currentAngle;
                if (listener != null) {
                    listener.onColorChanged(currentAngle, progress);
                }
                
                return true;
            }
        }
        
        return super.onTouchEvent(event);
    }
}
