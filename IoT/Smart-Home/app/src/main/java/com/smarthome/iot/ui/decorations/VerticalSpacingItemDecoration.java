package com.smarthome.iot.ui.decorations;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class VerticalSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private int spacing;

    public VerticalSpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        
        // Add spacing below each item except the last one
        if (position < state.getItemCount() - 1) {
            outRect.bottom = spacing;
        }
    }
}
