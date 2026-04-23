package com.talentbaby.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.talentbaby.app.R;
import com.talentbaby.app.models.Article;
import com.talentbaby.app.utils.ApiClient;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    private final List<Article> articles;
    private final OnArticleClickListener listener;

    public interface OnArticleClickListener {
        void onArticleClick(Article article);
    }

    public ArticleAdapter(List<Article> articles, OnArticleClickListener listener) {
        this.articles = articles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(articles.get(position));
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageArticle;
        private final TextView textTitle;
        private final TextView textSummary;
        private final TextView textReadTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageArticle = itemView.findViewById(R.id.imageArticle);
            textTitle = itemView.findViewById(R.id.textArticleTitle);
            textSummary = itemView.findViewById(R.id.textArticleSummary);
            textReadTime = itemView.findViewById(R.id.textArticleReadTime);
        }

        void bind(Article article) {
            textTitle.setText(article.getTitle());
            textSummary.setText(article.getSummary() != null ? article.getSummary() : "");

            if (article.getReadTimeMinutes() > 0) {
                textReadTime.setText(itemView.getContext().getString(R.string.min_read, article.getReadTimeMinutes()));
            } else {
                textReadTime.setText("");
            }

            if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
                String imageUrl = article.getImageUrl();
                if (imageUrl.startsWith("/")) {
                    imageUrl = ApiClient.getBaseUrl() + imageUrl.substring(1);
                }
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.rounded_image_background)
                        .centerCrop()
                        .into(imageArticle);
            }

            itemView.setOnClickListener(v -> listener.onArticleClick(article));
        }
    }
}
