package com.smarthome.iot.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smarthome.iot.R;

/**
 * Utility class for applying consistent switch styling across the app
 * Matches Figma UI exactly:
 * Track: 44dp x 24dp (rounded rectangle, pill shape with rounded left and right ends)
 * Thumb: 20dp x 20dp (circle smaller than track height, with vertical padding)
 * Checked (ON): Primary blue track, white circle on LEFT side, fully contained
 * Unchecked (OFF): #35383F dark gray track, white circle on RIGHT side, fully contained
 */
public class SwitchHelper {
    
    /**
     * Apply the standard switch styling to a SwitchMaterial
     * Programmatically sets custom drawables to ensure they display correctly
     * 
     * @param context Android context
     * @param switchMaterial The switch to style
     */
    public static void applySwitchStyle(Context context, SwitchMaterial switchMaterial) {
        if (switchMaterial == null || context == null) {
            return;
        }
        
        try {
            // First, try to load and set custom drawables directly
            // This ensures our custom drawables are used instead of Material's defaults
            Drawable trackDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_track_selector);
            Drawable thumbDrawable = AppCompatResources.getDrawable(context, R.drawable.switch_thumb);
            
            if (trackDrawable != null && thumbDrawable != null) {
                // Use reflection to set drawables if method exists
                try {
                    java.lang.reflect.Method setTrackDrawable = switchMaterial.getClass()
                        .getMethod("setTrackDrawable", Drawable.class);
                    setTrackDrawable.invoke(switchMaterial, trackDrawable);
                } catch (Exception e) {
                    // Method doesn't exist, will use tinting fallback
                }
                
                try {
                    java.lang.reflect.Method setThumbDrawable = switchMaterial.getClass()
                        .getMethod("setThumbDrawable", Drawable.class);
                    setThumbDrawable.invoke(switchMaterial, thumbDrawable);
                } catch (Exception e) {
                    // Method doesn't exist, will use tinting fallback
                }
            }
            
            // Get colors for tinting fallback
            int primaryColor = ContextCompat.getColor(context, R.color.primary);
            int dark5Color = ContextCompat.getColor(context, R.color.dark_5);
            int whiteColor = ContextCompat.getColor(context, R.color.white);
            
            // Set track tint with state selector as fallback/backup
            ColorStateList trackTint = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
                },
                new int[]{
                    primaryColor,  // Checked (ON) - Primary blue (#405FF2)
                    dark5Color     // Unchecked (OFF) - Dark gray (#35383F)
                }
            );
            switchMaterial.setTrackTintList(trackTint);
            
            // Set thumb tint (always pure white) - ensure maximum visibility
            // Use pure white (#FFFFFFFF) to ensure clear visibility against track
            ColorStateList thumbTint = ColorStateList.valueOf(0xFFFFFFFF);
            switchMaterial.setThumbTintList(thumbTint);
            
            // Ensure thumb drawable is properly applied and visible
            // Material Components might override, so we set tint as backup
            if (thumbDrawable != null) {
                // Force the drawable to be opaque
                thumbDrawable.setAlpha(255);
            }
            
            // Ensure switch is visible and clickable
            switchMaterial.setVisibility(android.view.View.VISIBLE);
            switchMaterial.setClickable(true);
            switchMaterial.setFocusable(true);
            switchMaterial.setAlpha(1.0f); // Ensure full opacity
            
            // Force immediate refresh
            switchMaterial.invalidate();
            switchMaterial.requestLayout();
            
            // Post another refresh to ensure drawables are applied
            switchMaterial.post(() -> {
                switchMaterial.invalidate();
                switchMaterial.requestLayout();
            });
            
        } catch (Exception e) {
            android.util.Log.e("SwitchHelper", "Error applying switch style", e);
        }
    }
}
