package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.FAQItem;

import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.ViewHolder> {
    private List<FAQItem> faqItems;
    private int expandedPosition = -1;

    public FAQAdapter(List<FAQItem> faqItems) {
        this.faqItems = faqItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FAQItem item = faqItems.get(position);
        holder.textViewQuestion.setText(item.getQuestion());
        holder.textViewAnswer.setText(item.getAnswer());

        boolean isExpanded = position == expandedPosition;
        holder.layoutAnswer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.imageViewExpand.setRotation(isExpanded ? 270 : 90);

        holder.layoutQuestion.setOnClickListener(v -> {
            if (expandedPosition == position) {
                expandedPosition = -1;
                notifyItemChanged(position);
            } else {
                int oldExpanded = expandedPosition;
                expandedPosition = position;
                if (oldExpanded != -1) {
                    notifyItemChanged(oldExpanded);
                }
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return faqItems != null ? faqItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        LinearLayout layoutQuestion;
        LinearLayout layoutAnswer;
        TextView textViewQuestion;
        TextView textViewAnswer;
        ImageView imageViewExpand;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            layoutQuestion = itemView.findViewById(R.id.layoutQuestion);
            layoutAnswer = itemView.findViewById(R.id.layoutAnswer);
            textViewQuestion = itemView.findViewById(R.id.textViewQuestion);
            textViewAnswer = itemView.findViewById(R.id.textViewAnswer);
            imageViewExpand = itemView.findViewById(R.id.imageViewExpand);
        }
    }
}

