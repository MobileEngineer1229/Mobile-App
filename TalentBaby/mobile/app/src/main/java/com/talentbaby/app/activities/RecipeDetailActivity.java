package com.talentbaby.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.talentbaby.app.R;
import com.talentbaby.app.models.ApiResponse;
import com.talentbaby.app.models.Recipe;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.utils.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeDetailActivity extends AppCompatActivity {

    private ImageView imageRecipe;
    private TextView textTitle;
    private TextView textCategory;
    private TextView textPrepTime;
    private TextView textCookTime;
    private TextView textServings;
    private TextView textCalories;
    private TextView textDescription;
    private TextView labelIngredients;
    private TextView textIngredients;
    private TextView labelInstructions;
    private TextView textInstructions;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        apiService = ApiClient.getClient().create(ApiService.class);

        imageRecipe = findViewById(R.id.imageRecipe);
        textTitle = findViewById(R.id.textTitle);
        textCategory = findViewById(R.id.textCategory);
        textPrepTime = findViewById(R.id.textPrepTime);
        textCookTime = findViewById(R.id.textCookTime);
        textServings = findViewById(R.id.textServings);
        textCalories = findViewById(R.id.textCalories);
        textDescription = findViewById(R.id.textDescription);
        labelIngredients = findViewById(R.id.labelIngredients);
        textIngredients = findViewById(R.id.textIngredients);
        labelInstructions = findViewById(R.id.labelInstructions);
        textInstructions = findViewById(R.id.textInstructions);
        progressBar = findViewById(R.id.progressBar);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId != -1) {
            loadRecipe(recipeId);
        } else {
            finish();
        }
    }

    private void loadRecipe(int id) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getRecipe(id).enqueue(new Callback<ApiResponse<Recipe>>() {
            @Override
            public void onResponse(Call<ApiResponse<Recipe>> call, Response<ApiResponse<Recipe>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    bindRecipe(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Recipe>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RecipeDetailActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindRecipe(Recipe recipe) {
        textTitle.setText(recipe.getTitle() != null ? recipe.getTitle() : "");

        if (recipe.getCategory() != null) {
            textCategory.setText(recipe.getCategory());
        }

        if (recipe.getPrepTimeMinutes() > 0) {
            textPrepTime.setText(getString(R.string.prep_time, recipe.getPrepTimeMinutes()));
        }
        if (recipe.getCookTimeMinutes() > 0) {
            textCookTime.setText(getString(R.string.cook_time, recipe.getCookTimeMinutes()));
        }
        if (recipe.getServings() > 0) {
            textServings.setText(getString(R.string.servings, recipe.getServings()));
        }
        if (recipe.getCalories() > 0) {
            textCalories.setText(getString(R.string.calories, recipe.getCalories()));
        }

        if (recipe.getDescription() != null && !recipe.getDescription().isEmpty()) {
            textDescription.setText(recipe.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        }

        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            labelIngredients.setVisibility(View.VISIBLE);
            textIngredients.setVisibility(View.VISIBLE);
            textIngredients.setText(recipe.getIngredients());
        }

        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            labelInstructions.setVisibility(View.VISIBLE);
            textInstructions.setVisibility(View.VISIBLE);
            textInstructions.setText(recipe.getInstructions());
        }

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            String url = recipe.getImageUrl().startsWith("/")
                    ? ApiClient.getBaseUrl() + recipe.getImageUrl().substring(1)
                    : recipe.getImageUrl();
            Glide.with(this).load(url).centerCrop().into(imageRecipe);
        }
    }
}
