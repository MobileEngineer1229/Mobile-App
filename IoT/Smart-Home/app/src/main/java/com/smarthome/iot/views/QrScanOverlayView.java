package com.smarthome.iot.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom overlay view that draws corner brackets for QR code scanning area.
 * Matches the Figma design with white corner lines and a semi-transparent
 * darkened area outside the scan region.
 */
public class QrScanOverlayView extends View {

    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint();
    private final RectF scanRect = new RectF();

    private static final float CORNER_LENGTH_DP = 32f;
    private static final float CORNER_STROKE_DP = 3f;
    private static final float CORNER_RADIUS_DP = 8f;
    private static final int OVERLAY_COLOR = 0x80000000; // 50% black

    public QrScanOverlayView(Context context) {
        super(context);
        init();
    }

    public QrScanOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QrScanOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStrokeWidth(dpToPx(CORNER_STROKE_DP));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        overlayPaint.setColor(OVERLAY_COLOR);
        overlayPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Scan area: centered square, 70% of width
        float scanSize = w * 0.70f;
        float left = (w - scanSize) / 2f;
        float top = (h - scanSize) / 2f;
        float right = left + scanSize;
        float bottom = top + scanSize;
        scanRect.set(left, top, right, bottom);

        // Draw semi-transparent overlay outside scan area
        canvas.drawRect(0, 0, w, top, overlayPaint);           // Top
        canvas.drawRect(0, bottom, w, h, overlayPaint);        // Bottom
        canvas.drawRect(0, top, left, bottom, overlayPaint);   // Left
        canvas.drawRect(right, top, w, bottom, overlayPaint);  // Right

        // Draw corner brackets
        float cornerLen = dpToPx(CORNER_LENGTH_DP);
        float radius = dpToPx(CORNER_RADIUS_DP);
        float halfStroke = cornerPaint.getStrokeWidth() / 2f;

        // Adjust rect inward by half stroke so lines render inside
        float l = left + halfStroke;
        float t = top + halfStroke;
        float r = right - halfStroke;
        float b = bottom - halfStroke;

        // Top-left corner
        canvas.drawLine(l + radius, t, l + cornerLen, t, cornerPaint);
        canvas.drawLine(l, t + radius, l, t + cornerLen, cornerPaint);
        canvas.drawArc(l, t, l + radius * 2, t + radius * 2, 180, 90, false, cornerPaint);

        // Top-right corner
        canvas.drawLine(r - cornerLen, t, r - radius, t, cornerPaint);
        canvas.drawLine(r, t + radius, r, t + cornerLen, cornerPaint);
        canvas.drawArc(r - radius * 2, t, r, t + radius * 2, 270, 90, false, cornerPaint);

        // Bottom-left corner
        canvas.drawLine(l + radius, b, l + cornerLen, b, cornerPaint);
        canvas.drawLine(l, b - cornerLen, l, b - radius, cornerPaint);
        canvas.drawArc(l, b - radius * 2, l + radius * 2, b, 90, 90, false, cornerPaint);

        // Bottom-right corner
        canvas.drawLine(r - cornerLen, b, r - radius, b, cornerPaint);
        canvas.drawLine(r, b - cornerLen, r, b - radius, cornerPaint);
        canvas.drawArc(r - radius * 2, b - radius * 2, r, b, 0, 90, false, cornerPaint);
    }

    /**
     * Returns the scan rectangle in view coordinates for the barcode scanner.
     */
    public RectF getScanRect() {
        int w = getWidth();
        int h = getHeight();
        float scanSize = w * 0.70f;
        float left = (w - scanSize) / 2f;
        float top = (h - scanSize) / 2f;
        return new RectF(left, top, left + scanSize, top + scanSize);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
