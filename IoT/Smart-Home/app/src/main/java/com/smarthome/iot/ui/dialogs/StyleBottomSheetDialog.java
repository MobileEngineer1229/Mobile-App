package com.smarthome.iot.ui.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.ColorSwatchAdapter;
import com.smarthome.iot.ui.adapters.IconSwatchAdapter;

public class StyleBottomSheetDialog {
    private BottomSheetDialog dialog;
    private Context context;
    private OnStyleSelectedListener listener;
    private String selectedColor;
    private String selectedIcon;
    private int selectedIconRes;
    
    public interface OnStyleSelectedListener {
        void onStyleSelected(String color, String icon, int iconRes);
    }
    
    public StyleBottomSheetDialog(Context context, String currentColor, String currentIcon, int currentIconRes, OnStyleSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.selectedColor = currentColor;
        this.selectedIcon = currentIcon;
        this.selectedIconRes = currentIconRes;
        createDialog();
    }
    
    private void createDialog() {
        dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_style, null);
        dialog.setContentView(view);
        
        MaterialButton buttonColorTab = view.findViewById(R.id.buttonColorTab);
        MaterialButton buttonIconTab = view.findViewById(R.id.buttonIconTab);
        RecyclerView recyclerViewColors = view.findViewById(R.id.recyclerViewColors);
        RecyclerView recyclerViewIcons = view.findViewById(R.id.recyclerViewIcons);
        MaterialButton buttonCancel = view.findViewById(R.id.buttonCancel);
        MaterialButton buttonOk = view.findViewById(R.id.buttonOk);
        
        // Setup color grid
        String[] colors = {
            "#405FF2", "#FF5722", "#E91E63", "#9C27B0",
            "#673AB7", "#3F51B5", "#2196F3", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
        };
        
        // Create adapter first, then set up listener to avoid initialization error
        final ColorSwatchAdapter[] colorAdapterRef = new ColorSwatchAdapter[1];
        colorAdapterRef[0] = new ColorSwatchAdapter(colors, selectedColor, color -> {
            selectedColor = color;
            if (colorAdapterRef[0] != null) {
                colorAdapterRef[0].setSelectedColor(color);
            }
        });
        recyclerViewColors.setLayoutManager(new GridLayoutManager(context, 4));
        recyclerViewColors.setAdapter(colorAdapterRef[0]);
        
        // Setup icon grid - using only existing drawable resources
        int[] icons = {
            R.drawable.ic_tap, R.drawable.ic_sun, R.drawable.ic_sun, R.drawable.ic_clock,
            R.drawable.ic_water_drop, R.drawable.ic_wind, R.drawable.ic_thermometer, R.drawable.ic_location,
            R.drawable.ic_arrow_back, R.drawable.ic_sun, R.drawable.ic_thermometer, R.drawable.ic_briefcase,
            R.drawable.ic_location, R.drawable.ic_clock, R.drawable.ic_briefcase, R.drawable.ic_shield,
            R.drawable.ic_notifications, R.drawable.ic_sun, R.drawable.ic_briefcase, R.drawable.ic_arrow_forward,
            R.drawable.ic_settings, R.drawable.ic_briefcase, R.drawable.ic_briefcase, R.drawable.ic_device,
            R.drawable.ic_camera, R.drawable.ic_envelope, R.drawable.ic_star
        };
        
        String[] iconNames = {
            "ic_tap", "ic_sun", "ic_sunrise", "ic_clock",
            "ic_water_drop", "ic_wind", "ic_thermometer", "ic_tag",
            "ic_arrow_back", "ic_moon", "ic_flame", "ic_book",
            "ic_location", "ic_clock", "ic_briefcase", "ic_shield",
            "ic_notifications", "ic_plant", "ic_dollar", "ic_play",
            "ic_settings", "ic_cart", "ic_bag", "ic_controller",
            "ic_camera", "ic_mail", "ic_heart"
        };
        
        // Create adapter first, then set up listener to avoid initialization error
        final IconSwatchAdapter[] iconAdapterRef = new IconSwatchAdapter[1];
        iconAdapterRef[0] = new IconSwatchAdapter(icons, iconNames, selectedIconRes, (iconName, iconRes) -> {
            selectedIcon = iconName;
            selectedIconRes = iconRes;
            if (iconAdapterRef[0] != null) {
                iconAdapterRef[0].setSelectedIcon(iconRes);
            }
        });
        recyclerViewIcons.setLayoutManager(new GridLayoutManager(context, 4));
        recyclerViewIcons.setAdapter(iconAdapterRef[0]);
        
        // Tab switching
        buttonColorTab.setOnClickListener(v -> {
            buttonColorTab.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary));
            buttonColorTab.setTextColor(ContextCompat.getColor(context, R.color.white));
            buttonIconTab.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent));
            buttonIconTab.setTextColor(ContextCompat.getColor(context, R.color.white_alpha_70));
            recyclerViewColors.setVisibility(View.VISIBLE);
            recyclerViewIcons.setVisibility(View.GONE);
        });
        
        buttonIconTab.setOnClickListener(v -> {
            buttonIconTab.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary));
            buttonIconTab.setTextColor(ContextCompat.getColor(context, R.color.white));
            buttonColorTab.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.transparent));
            buttonColorTab.setTextColor(ContextCompat.getColor(context, R.color.white_alpha_70));
            recyclerViewIcons.setVisibility(View.VISIBLE);
            recyclerViewColors.setVisibility(View.GONE);
        });
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonOk.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStyleSelected(selectedColor, selectedIcon, selectedIconRes);
            }
            dialog.dismiss();
        });
    }
    
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
