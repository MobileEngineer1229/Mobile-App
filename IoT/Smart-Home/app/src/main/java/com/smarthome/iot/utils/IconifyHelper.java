package com.smarthome.iot.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import com.smarthome.iot.R;

/**
 * Helper utility for working with Iconify icons in the Android app.
 * 
 * This class provides convenience methods for:
 * - Loading Iconify icons (converted to VectorDrawable)
 * - Applying tints to icons
 * - Managing icon resources
 * 
 * Note: Icons must be converted from Iconify SVG to Android VectorDrawable
 * using Android Studio's Vector Asset Studio before use.
 * 
 * @see ICONIFY_INTEGRATION.md for conversion instructions
 */
public class IconifyHelper {
    
    /**
     * Get an Iconify icon drawable by resource ID
     * 
     * @param context Android context
     * @param iconResId Resource ID of the icon (e.g., R.drawable.ic_home_mdi)
     * @return Drawable object, or null if not found
     */
    public static Drawable getIcon(Context context, int iconResId) {
        try {
            return AppCompatResources.getDrawable(context, iconResId);
        } catch (Exception e) {
            android.util.Log.e("IconifyHelper", "Error loading icon: " + iconResId, e);
            return null;
        }
    }
    
    /**
     * Get an Iconify icon drawable with a specific tint color
     * 
     * @param context Android context
     * @param iconResId Resource ID of the icon
     * @param tintColorResId Resource ID of the color (e.g., R.color.white)
     * @return Tinted Drawable object, or null if icon not found
     */
    public static Drawable getIconWithTint(Context context, int iconResId, int tintColorResId) {
        Drawable drawable = getIcon(context, iconResId);
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            int tintColor = context.getResources().getColor(tintColorResId, context.getTheme());
            DrawableCompat.setTint(drawable, tintColor);
            drawable = drawable.mutate(); // Create a new instance to avoid affecting other uses
        }
        return drawable;
    }
    
    /**
     * Get an Iconify icon drawable with a specific tint color (using color int)
     * 
     * @param context Android context
     * @param iconResId Resource ID of the icon
     * @param tintColor Color int (e.g., 0xFFFFFFFF for white)
     * @return Tinted Drawable object, or null if icon not found
     */
    public static Drawable getIconWithTintColor(Context context, int iconResId, int tintColor) {
        Drawable drawable = getIcon(context, iconResId);
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);
            drawable = drawable.mutate();
        }
        return drawable;
    }
    
    /**
     * Common Iconify icon resource IDs
     * Add your converted Iconify icons here for easy reference
     */
    public static class Icons {
        // Material Design Icons (mdi)
        // public static final int HOME = R.drawable.ic_home_mdi;
        // public static final int SETTINGS = R.drawable.ic_settings_mdi;
        // public static final int ACCOUNT = R.drawable.ic_account_mdi;
        
        // Material Symbols (material-symbols)
        // public static final int HOME = R.drawable.ic_home_material;
        // public static final int SETTINGS = R.drawable.ic_settings_material;
        
        // Add more icon constants as you convert them
    }
    
    /**
     * Icon set identifiers for reference
     */
    public static class IconSets {
        public static final String MATERIAL_DESIGN_ICONS = "mdi";
        public static final String MATERIAL_SYMBOLS = "material-symbols";
        public static final String HEROICONS = "heroicons";
        public static final String TABLER_ICONS = "tabler";
        public static final String FEATHER = "feather";
        public static final String LUCIDE = "lucide";
    }
    
    /**
     * Get Iconify API URL for downloading SVG
     * 
     * @param iconSet Icon set name (e.g., "mdi", "material-symbols")
     * @param iconName Icon name (e.g., "home", "settings")
     * @return Full API URL for the SVG
     */
    public static String getIconifyApiUrl(String iconSet, String iconName) {
        return String.format("https://api.iconify.design/%s/%s.svg", iconSet, iconName);
    }
    
    /**
     * Get Iconify website URL for viewing icon
     * 
     * @param iconSet Icon set name
     * @param iconName Icon name
     * @return Full website URL
     */
    public static String getIconifyWebUrl(String iconSet, String iconName) {
        return String.format("https://iconify.design/icon-sets/%s/%s.html", iconSet, iconName);
    }
}
