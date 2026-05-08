package com.smarthome.iot.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavSpacingHelper {
    
    /**
     * Disable the active indicator (blue background) on selected items
     */
    public static void disableActiveIndicator(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;
        
        // Remove background from all menu items immediately and on layout
        Runnable removeBackgrounds = () -> removeMenuItemBackgrounds(bottomNav);
        
        // Wait for layout to complete
        ViewTreeObserver observer = bottomNav.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bottomNav.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                removeBackgrounds.run();
            }
        });
        
        // Also try after delays as backup
        bottomNav.postDelayed(removeBackgrounds, 50);
        bottomNav.postDelayed(removeBackgrounds, 100);
        bottomNav.postDelayed(removeBackgrounds, 300);
        bottomNav.postDelayed(removeBackgrounds, 500);
    }
    
    /**
     * Call this method when selection changes to remove active indicator background
     */
    public static void onSelectionChanged(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;
        // Remove backgrounds after a short delay to let Material Components finish
        bottomNav.postDelayed(() -> removeMenuItemBackgrounds(bottomNav), 50);
        bottomNav.postDelayed(() -> removeMenuItemBackgrounds(bottomNav), 150);
    }
    
    private static void removeMenuItemBackgrounds(BottomNavigationView bottomNav) {
        try {
            ViewGroup menuView = (ViewGroup) bottomNav.getChildAt(0);
            if (menuView == null) return;
            
            // Remove background from all menu item views
            for (int i = 0; i < menuView.getChildCount(); i++) {
                View itemView = menuView.getChildAt(i);
                
                // Remove background from the item view itself
                if (itemView != null) {
                    itemView.setBackground(null);
                    itemView.setBackgroundResource(android.R.color.transparent);
                    
                    // Force clear any drawable backgrounds
                    itemView.setBackgroundDrawable(null);
                }
                
                // Recursively remove backgrounds from all child views
                if (itemView instanceof ViewGroup) {
                    removeBackgroundsRecursively((ViewGroup) itemView);
                }
            }
            
            // Try to use reflection to access and disable active indicator
            try {
                java.lang.reflect.Method getActiveIndicatorMethod = bottomNav.getClass().getDeclaredMethod("getActiveIndicatorDrawable");
                if (getActiveIndicatorMethod != null) {
                    getActiveIndicatorMethod.setAccessible(true);
                    android.graphics.drawable.Drawable activeIndicator = (android.graphics.drawable.Drawable) getActiveIndicatorMethod.invoke(bottomNav);
                    if (activeIndicator != null) {
                        // Try to hide it
                        activeIndicator.setAlpha(0);
                    }
                }
            } catch (Exception e) {
                // Reflection failed, continue with other methods
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void removeBackgroundsRecursively(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child != null) {
                String className = child.getClass().getName();

                // Only target the active indicator view — never hide NavigationBarItemView
                // as that contains the icon + label and hiding it breaks icon rendering.
                if (className.contains("ActiveIndicator")) {
                    child.setBackground(null);
                    child.setBackgroundResource(android.R.color.transparent);
                    child.setBackgroundDrawable(null);
                    child.setAlpha(0f);
                    child.setVisibility(View.GONE);
                }

                // Clear the active indicator drawable via reflection on item views
                if (className.contains("NavigationBarItemView")) {
                    child.setBackground(null);
                    try {
                        java.lang.reflect.Field activeIndicatorField = child.getClass().getDeclaredField("activeIndicatorDrawable");
                        activeIndicatorField.setAccessible(true);
                        activeIndicatorField.set(child, null);
                    } catch (Exception e) {
                        // Field doesn't exist or can't access, continue
                    }
                }
            }

            if (child instanceof ViewGroup) {
                removeBackgroundsRecursively((ViewGroup) child);
            }
        }
    }
    
    public static void removeSpacing(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;
        
        // Wait for layout to complete
        ViewTreeObserver observer = bottomNav.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                bottomNav.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                adjustSpacing(bottomNav);
            }
        });
        
        // Also try after delays as backup
        bottomNav.postDelayed(() -> adjustSpacing(bottomNav), 100);
        bottomNav.postDelayed(() -> adjustSpacing(bottomNav), 300);
    }
    
    private static void adjustSpacing(BottomNavigationView bottomNav) {
        try {
            float density = bottomNav.getResources().getDisplayMetrics().density;
            
            // Reduce overall height by 4dp by reducing bottom padding
            int paddingReduction = (int) (4 * density);
            int currentPaddingBottom = bottomNav.getPaddingBottom();
            bottomNav.setPadding(
                bottomNav.getPaddingLeft(),
                bottomNav.getPaddingTop(),
                bottomNav.getPaddingRight(),
                Math.max(0, currentPaddingBottom - paddingReduction)
            );
            
            ViewGroup menuView = (ViewGroup) bottomNav.getChildAt(0);
            if (menuView == null) return;
            
            // Disable clipping on menu view
            menuView.setClipChildren(false);
            menuView.setClipToPadding(false);
            
            for (int i = 0; i < menuView.getChildCount(); i++) {
                View itemView = menuView.getChildAt(i);
                if (itemView instanceof ViewGroup) {
                    ViewGroup itemGroup = (ViewGroup) itemView;
                    
                    // Disable clipping on item view
                    itemGroup.setClipChildren(false);
                    itemGroup.setClipToPadding(false);
                    
                    // Find and adjust the internal layout
                    adjustItemSpacing(itemGroup, density);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void adjustItemSpacing(ViewGroup itemView, float density) {
        // Look for LinearLayout that contains icon and text
        for (int i = 0; i < itemView.getChildCount(); i++) {
            View child = itemView.getChildAt(i);
            
            if (child instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) child;
                // If it's a vertical LinearLayout (likely contains icon and text)
                if (linearLayout.getOrientation() == LinearLayout.VERTICAL) {
                    // Reduce spacing between children
                    linearLayout.setClipChildren(false);
                    
                    // Find TextView (label) and ImageView (icon)
                    TextView labelView = null;
                    for (int j = 0; j < linearLayout.getChildCount(); j++) {
                        View innerChild = linearLayout.getChildAt(j);
                        if (innerChild instanceof TextView) {
                            TextView tv = (TextView) innerChild;
                            // Find the visible label (has text and is visible)
                            if (tv.getText() != null && tv.getText().length() > 0 && 
                                tv.getVisibility() == View.VISIBLE) {
                                labelView = tv;
                                break;
                            }
                        }
                    }
                    
                    // Apply negative top margin to move text up
                    if (labelView != null) {
                        ViewGroup.MarginLayoutParams params = 
                            (ViewGroup.MarginLayoutParams) labelView.getLayoutParams();
                        if (params == null) {
                            params = new ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            labelView.setLayoutParams(params);
                        }
                        
                        // Apply negative top margin to reduce spacing
                        // -6dp should be safe and effective
                        int negativeMargin = (int) (-6 * density);
                        params.topMargin = negativeMargin;
                        labelView.setLayoutParams(params);
                    }
                }
            } else if (child instanceof ViewGroup) {
                // Recursively search
                adjustItemSpacing((ViewGroup) child, density);
            } else if (child instanceof TextView) {
                // Direct TextView found - apply negative margin
                TextView textView = (TextView) child;
                if (textView.getText() != null && textView.getText().length() > 0 && 
                    textView.getVisibility() == View.VISIBLE) {
                    
                    ViewGroup.LayoutParams params = textView.getLayoutParams();
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams marginParams = 
                            (ViewGroup.MarginLayoutParams) params;
                        marginParams.topMargin = (int) (-6 * density);
                        textView.setLayoutParams(marginParams);
                    } else if (params != null) {
                        ViewGroup.MarginLayoutParams marginParams = 
                            new ViewGroup.MarginLayoutParams(params);
                        marginParams.topMargin = (int) (-6 * density);
                        textView.setLayoutParams(marginParams);
                    }
                }
            }
        }
    }
}
