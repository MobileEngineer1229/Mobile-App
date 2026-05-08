package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.Language;

import java.util.ArrayList;
import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private List<Language> languages;
    private String selectedLanguageCode;
    private OnLanguageClickListener listener;

    public interface OnLanguageClickListener {
        void onLanguageClick(Language language);
    }

    public LanguageAdapter(List<Language> languages, String selectedLanguageCode, OnLanguageClickListener listener) {
        this.languages = languages;
        this.selectedLanguageCode = selectedLanguageCode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Language language = languages.get(position);
        holder.textViewLanguage.setText(language.getName());
        
        // Set flag - for now use a placeholder, in production use actual flag drawables
        holder.imageViewFlag.setImageResource(language.getFlagResId());

        // Show checkmark if selected
        if (language.getCode().equals(selectedLanguageCode)) {
            holder.imageViewCheck.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewCheck.setVisibility(View.GONE);
        }

        // Set click listener on the entire card view
        holder.cardView.setOnClickListener(v -> {
            android.util.Log.d("LanguageAdapter", "Card clicked for: " + language.getCode());
            if (listener != null) {
                listener.onLanguageClick(language);
            } else {
                android.util.Log.e("LanguageAdapter", "Listener is null!");
            }
        });
        
        // Also set on itemView as backup
        holder.itemView.setOnClickListener(v -> {
            android.util.Log.d("LanguageAdapter", "ItemView clicked for: " + language.getCode());
            if (listener != null) {
                listener.onLanguageClick(language);
            }
        });
    }

    @Override
    public int getItemCount() {
        return languages != null ? languages.size() : 0;
    }

    public void setSelectedLanguage(String languageCode) {
        this.selectedLanguageCode = languageCode;
        notifyDataSetChanged();
    }
    
    public void setLanguages(List<Language> languages) {
        this.languages = languages != null ? languages : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageViewFlag;
        TextView textViewLanguage;
        ImageView imageViewCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            imageViewFlag = itemView.findViewById(R.id.imageViewFlag);
            textViewLanguage = itemView.findViewById(R.id.textViewLanguage);
            imageViewCheck = itemView.findViewById(R.id.imageViewCheck);
        }
    }
}

