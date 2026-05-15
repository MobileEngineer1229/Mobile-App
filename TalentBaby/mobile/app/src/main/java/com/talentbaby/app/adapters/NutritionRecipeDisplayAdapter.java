package com.talentbaby.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.talentbaby.app.R;
import com.talentbaby.app.models.NutritionRecipeDisplay;

import java.util.List;

public class NutritionRecipeDisplayAdapter extends RecyclerView.Adapter<NutritionRecipeDisplayAdapter.ViewHolder> {
    private final List<NutritionRecipeDisplay> recipes;

    public NutritionRecipeDisplayAdapter(List<NutritionRecipeDisplay> recipes) {
        this.recipes = recipes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutrition_baby_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(recipes.get(position));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final View actions;
        private final View lock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageRecipeDisplay);
            title = itemView.findViewById(R.id.textRecipeDisplayTitle);
            actions = itemView.findViewById(R.id.actionsRecipeDisplay);
            lock = itemView.findViewById(R.id.lockRecipeDisplay);
        }

        void bind(NutritionRecipeDisplay recipe) {
            title.setText(recipe.getTitle());
            if (recipe.getImageResId() != 0) {
                image.setVisibility(View.VISIBLE);
                image.setImageResource(recipe.getImageResId());
            } else {
                image.setVisibility(View.INVISIBLE);
            }
            actions.setVisibility(recipe.isLocked() ? View.GONE : View.VISIBLE);
            lock.setVisibility(recipe.isLocked() ? View.VISIBLE : View.GONE);
        }
    }
}
