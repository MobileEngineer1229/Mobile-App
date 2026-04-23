package com.talentbaby.app.ui.knowledge;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.talentbaby.app.MainActivity;
import com.talentbaby.app.R;
import com.talentbaby.app.activities.ArticleDetailActivity;

public class KnowledgeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_knowledge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyColors(view);
        setupClickListeners(view);
    }

    private void applyColors(View view) {
        // Root cream background
        view.setBackgroundColor(0xFFFDF6EE);

        // Header: blush → peach gradient (top to bottom)
        GradientDrawable headerGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFCE4D6, 0xFFF9D0B8});
        view.findViewById(R.id.headerLibrary).setBackground(headerGrad);

        // Parenting Support coral button
        MaterialButton btnSupport = view.findViewById(R.id.btnParentingSupportLibrary);
        if (btnSupport != null) {
            btnSupport.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF08B6B));
        }

        // Tile gradients (TL→BR diagonal to match design)
        setTileGradient(view, R.id.tileSky,     0xFFD7E4EC, 0xFFB4CADB);
        setTileGradient(view, R.id.tileMustard, 0xFFFCE9C5, 0xFFF5D79A);
        setTileGradient(view, R.id.tilePlum,    0xFFD8CADD, 0xFFB79FC0);
        setTileGradient(view, R.id.tileSage,    0xFFD8E4D4, 0xFFB8CDB2);
    }

    private void setTileGradient(View root, int viewId, int startColor, int endColor) {
        View tile = root.findViewById(viewId);
        if (tile == null) return;
        GradientDrawable grad = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        tile.setBackground(grad);
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnMenuLibrary).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.btnParentingSupportLibrary).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.parenting_support), Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.cardWisdom).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ArticleDetailActivity.class)));

        view.findViewById(R.id.cardNutrition).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.library_coming_soon), Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.cardStories).setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.library_coming_soon), Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.cardGrowthTracker).setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(),
                        com.talentbaby.app.activities.GrowthTrackerActivity.class)));
    }
}
