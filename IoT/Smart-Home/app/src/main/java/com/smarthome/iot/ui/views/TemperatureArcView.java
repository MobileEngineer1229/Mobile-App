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

public class TemperatureArcView extends View {
    private Paint arcPaint;
    private Paint inactiveArcPaint; // For inactive segment (dark gray)
    private Paint selectorPaint;
    private Paint selectorStrokePaint;
    private Paint tickMarkPaint; // For tick marks along the arc
    private RectF arcBounds;
    private float strokeWidth;
    private float selectorWidth;
    private float selectorStrokeWidth;
    private float currentAngle = 0f; // Start at 0 (warm/left side)
    private float startAngle = 135f; // Arc starts at 135° (top-left) for three-quarter circle
    private float sweepAngle = 270f; // Arc spans 270° (three-quarter circle)
    private OnTemperatureChangeListener listener;
    private boolean isFullCircle = false; // Mode: false = three-quarter circle (lamp), true = full circle (AC)
    private boolean isACMode = false; // true = AC (solid blue), false = Lamp (gradient)
    
    // Gradient colors - exact Figma colors: warm yellow (left) to cool blue (right)
    // Figma values: 0% = CDD6FF (cool blue), 51% = FFFFFF (white), 100% = FFE29F (warm yellow)
    // Reversed for arc: left (0%) = warm yellow, right (100%) = cool blue
    private int[] gradientColors = {
        0xFFFFE29F, // 0% position -> warm yellow/orange (FFE29F from Figma)
        0xFFFFFFFF, // 51% position -> white (FFFFFF from Figma)
        0xFFCDD6FF  // 100% position -> cool blue (CDD6FF from Figma)
    };
    
    private float[] gradientPositions = {
        0.0f,  // 0% - warm yellow (left side of arc)
        0.51f, // 51% - white (middle)
        1.0f   // 100% - cool blue (right side of arc)
    };
    
    public interface OnTemperatureChangeListener {
        void onTemperatureChanged(float angle, int progress);
    }

    public TemperatureArcView(Context context) {
        super(context);
        init();
    }

    public TemperatureArcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TemperatureArcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        
        // Arc stroke width: 32dp
        strokeWidth = 32f * density;
        
        // Selector (progress indicator) size: 60dp
        selectorWidth = 60f * density;
        
        // Selector stroke width: 2dp (blue border around selector for AC mode)
        selectorStrokeWidth = 8f * density;
        
        // Main arc paint (for active segment - blue for AC, gradient for lamp)
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(strokeWidth);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // Inactive arc paint (dark gray for inactive segment)
        inactiveArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inactiveArcPaint.setStyle(Paint.Style.STROKE);
        inactiveArcPaint.setStrokeWidth(strokeWidth);
        inactiveArcPaint.setStrokeCap(Paint.Cap.ROUND);
        inactiveArcPaint.setColor(0xFF35383F); // dark_5 color
        
        // Tick marks paint (light gray vertical lines)
        tickMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickMarkPaint.setStyle(Paint.Style.STROKE);
        tickMarkPaint.setStrokeWidth(1f * density);
        tickMarkPaint.setColor(0x80FFFFFF); // Semi-transparent white for tick marks
        
        // Selector paint (white filled circle)
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(0xFFFFFFFF);
        
        // Selector stroke paint (blue border for AC mode, white for lamp mode)
        selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorStrokePaint.setStyle(Paint.Style.STROKE);
        selectorStrokePaint.setColor(0xFF405FF2); // Primary blue for AC mode
        selectorStrokePaint.setStrokeWidth(selectorStrokeWidth);
        
        arcBounds = new RectF();
    }

    /**
     * Set mode: false for semi-circle (lamp), true for full circle (AC)
     */
    public void setFullCircleMode(boolean fullCircle) {
        this.isFullCircle = fullCircle;
        if (fullCircle) {
            // Full circle mode: 360 degrees
            startAngle = 0f; // Start at 0° (right side, 3 o'clock)
            sweepAngle = 360f; // Full circle (360°)
            currentAngle = 0f; // Start at beginning
            // Keep gradient for lamp white mode, solid color for AC
            // Will be set in onSizeChanged based on whether it's lamp or AC
        } else {
            // Three-quarter circle mode: 270 degrees
            // Start at top-left (135°) for both AC and Lamp
            startAngle = 135f; // Start at top-left (135°)
            sweepAngle = 270f; // Three-quarter circle (270°)
            currentAngle = 0f; // Start at warm position
            arcPaint.setShader(null); // Will be set in onSizeChanged
        }
        invalidate();
    }
    
    /**
     * Set mode: true = AC (solid blue), false = Lamp White (gradient)
     * Works for both full circle and three-quarter circle modes
     */
    public void setFullCircleType(boolean isAC) {
        this.isACMode = isAC;
        if (isAC) {
            // AC mode: solid blue color (#405FF2) for active segment
            arcPaint.setColor(0xFF405FF2); // Primary blue color
            arcPaint.setShader(null); // No gradient for AC
            // Selector border: blue for AC mode
            selectorStrokePaint.setColor(0xFF405FF2); // Primary blue border
        } else {
            // Lamp White mode: gradient (warm to cool)
            arcPaint.setShader(null); // Will be set in onSizeChanged
            // Selector border: white for lamp mode
            selectorStrokePaint.setColor(0xFFFFFFFF); // White border
        }
        invalidate();
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
        float centerY = h / 2f; // Center of view for full circle
        float density = getResources().getDisplayMetrics().density;
        float totalSize = 330f * density;
        
        // Both full circle and three-quarter circle use centered circle
        float radius = (totalSize - strokeWidth) / 2f;
        arcBounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        );
        
        if (isFullCircle) {
            // Full circle mode: create gradient starting at 0° (right side, 3 o'clock)
            // For lamp white mode: warm yellow (0°) to cool blue (360°)
            SweepGradient sweepGradient = new SweepGradient(
                centerX, centerY,
                gradientColors,
                gradientPositions
            );
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setRotate(0f, centerX, centerY); // Start at 0° (right side)
            sweepGradient.setLocalMatrix(matrix);
            // Only set gradient if not AC (AC uses solid color set elsewhere)
            if (arcPaint.getColor() != 0xFF405FF2) {
                arcPaint.setShader(sweepGradient);
            }
        } else {
            // Three-quarter circle mode
            if (isACMode) {
                // AC mode: solid blue color (already set in setFullCircleType)
                // No gradient needed
            } else {
                // Lamp White mode: create gradient starting at 135° (top-left, warm yellow)
                SweepGradient sweepGradient = new SweepGradient(
                    centerX, centerY,
                    gradientColors,
                    gradientPositions
                );
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.setRotate(135f, centerX, centerY); // Rotate to start at 135° (top-left)
                sweepGradient.setLocalMatrix(matrix);
                arcPaint.setShader(sweepGradient);
            }
        }
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
        
        if (isFullCircle) {
            // Full circle mode: draw complete 360° circle
            float androidStartAngle = 0f; // Start at 0° (right side, 3 o'clock)
            float androidSweepAngle = 360f; // Full circle
            canvas.drawArc(arcBounds, androidStartAngle, androidSweepAngle, false, arcPaint);
            
            // Draw selector at current position (0-100 maps to 0-360°)
            float angleOffset = (currentAngle / 100f) * 360f;
            float androidAngle = androidStartAngle + angleOffset;
            float angleRad = (float) Math.toRadians(androidAngle);
            float selectorX = centerX + radius * (float) Math.cos(angleRad);
            float selectorY = centerY + radius * (float) Math.sin(angleRad);
            
            float selectorRadius = selectorWidth / 2f;
            canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorPaint);
            if (selectorStrokeWidth > 0) {
                float strokeRadius = selectorRadius - selectorStrokeWidth / 2f;
                canvas.drawCircle(selectorX, selectorY, strokeRadius, selectorStrokePaint);
            }
        } else {
            // Three-quarter circle mode: draw arc sweeping 270° clockwise
            float androidStartAngle = startAngle; // Use the configured start angle (210° for AC, 135° for lamp)
            float androidSweepAngle = 270f; // Three-quarter circle
            
            if (isACMode) {
                // AC mode: draw inactive segment first (dark gray), then active segment (blue)
                // Calculate current progress angle
                float angleOffset = (currentAngle / 100f) * sweepAngle;
                float activeSweep = angleOffset; // Active segment from start to current position
                float inactiveSweep = sweepAngle - activeSweep; // Remaining inactive segment
                
                // Draw inactive segment (dark gray) - from current position to end
                if (inactiveSweep > 0) {
                    float inactiveStartAngle = androidStartAngle + activeSweep;
                    canvas.drawArc(arcBounds, inactiveStartAngle, inactiveSweep, false, inactiveArcPaint);
                }
                
                // Draw active segment (blue) - from start to current position
                if (activeSweep > 0) {
                    canvas.drawArc(arcBounds, androidStartAngle, activeSweep, false, arcPaint);
                }
                
                // Draw tick marks along the entire arc
                drawTickMarks(canvas, centerX, centerY, radius, androidStartAngle, androidSweepAngle);
            } else {
                // Lamp mode: draw full arc with gradient
                canvas.drawArc(arcBounds, androidStartAngle, androidSweepAngle, false, arcPaint);
            }
            
            // Calculate selector position based on currentAngle (0-100 maps to 0-270°)
            float angleOffset = (currentAngle / 100f) * sweepAngle;
            float androidAngle = androidStartAngle + angleOffset;
            float angleRad = (float) Math.toRadians(androidAngle);
            float selectorX = centerX + radius * (float) Math.cos(angleRad);
            float selectorY = centerY + radius * (float) Math.sin(angleRad);
            
            // Draw selector: white fill with border (blue for AC, white for lamp)
            float selectorRadius = selectorWidth / 2f;
            canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorPaint);
            if (selectorStrokeWidth > 0) {
                float strokeRadius = selectorRadius - selectorStrokeWidth / 2f;
                canvas.drawCircle(selectorX, selectorY, strokeRadius, selectorStrokePaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = (arcBounds.right - arcBounds.left) / 2f;
            
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            float minDistance = radius - strokeWidth / 2f - selectorWidth / 2f;
            float maxDistance = radius + strokeWidth / 2f + selectorWidth / 2f;
            
            if (distance < minDistance || distance > maxDistance) {
                return false;
            }
            
            float angleRad = (float) Math.atan2(dy, dx);
            float angle = (float) Math.toDegrees(angleRad);
            if (angle < 0) {
                angle += 360f;
            }
            
            if (isFullCircle) {
                // Full circle mode: map angle to 0-100 (0° = 0%, 360° = 100%)
                // Full circle: any angle maps directly to 0-100
                float progress = (angle / 360f) * 100f;
                currentAngle = Math.max(0f, Math.min(100f, progress));
            } else {
                // Three-quarter circle mode: map angle to 0-100
                // Arc goes from 135° (top-left, warm) to 405° (135° + 270° = 405°, which is 45°)
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
                        currentAngle = 0f; // Warm end
                    } else {
                        currentAngle = 100f; // Cool end
                    }
                }
            }
            
            invalidate();
            
            int progress = (int) currentAngle;
            if (listener != null) {
                listener.onTemperatureChanged(currentAngle, progress);
            }
            
            return true;
        }
        
        return super.onTouchEvent(event);
    }

    public void setTemperature(int progress) {
        // Progress: 0-100
        currentAngle = Math.max(0f, Math.min(100f, progress));
        invalidate();
    }

    public int getTemperature() {
        return (int) currentAngle;
    }

    public void setOnTemperatureChangeListener(OnTemperatureChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * Draw tick marks along the arc (pointing inward from inner arc edge toward center, like a speedometer gauge)
     */
    private void drawTickMarks(Canvas canvas, float centerX, float centerY, float radius, float startAngle, float sweepAngle) {
        float density = getResources().getDisplayMetrics().density;
        float majorTickLength = 10f * density; // Major tick: 10dp
        float minorTickLength = 5f * density;  // Minor tick: 5dp
        float tickSpacing = 5f; // Tick every 5 degrees
        int numTicks = (int) (sweepAngle / tickSpacing);

        for (int i = 0; i <= numTicks; i++) {
            float angle = startAngle + (tickSpacing * i);
            float angleRad = (float) Math.toRadians(angle);

            // Determine if this is a major tick (every 15 degrees) or minor
            boolean isMajor = (i % 3 == 0);
            float tickLen = isMajor ? majorTickLength : minorTickLength;

            // Start point: inner edge of arc (radius - half stroke width)
            float arcInnerRadius = radius - strokeWidth / 2f;
            float x1 = centerX + arcInnerRadius * (float) Math.cos(angleRad);
            float y1 = centerY + arcInnerRadius * (float) Math.sin(angleRad);

            // End point: further inward toward center
            float tickInnerRadius = arcInnerRadius - tickLen;
            float x2 = centerX + tickInnerRadius * (float) Math.cos(angleRad);
            float y2 = centerY + tickInnerRadius * (float) Math.sin(angleRad);

            // Adjust paint for major vs minor
            tickMarkPaint.setAlpha(isMajor ? 153 : 77); // 60% vs 30% opacity
            canvas.drawLine(x1, y1, x2, y2, tickMarkPaint);
        }
    }
}
