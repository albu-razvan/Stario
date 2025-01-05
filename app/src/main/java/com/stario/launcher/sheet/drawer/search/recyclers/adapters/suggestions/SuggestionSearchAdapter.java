package com.stario.launcher.sheet.drawer.search.recyclers.adapters.suggestions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.sheet.drawer.search.recyclers.adapters.AbstractSearchListAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;

public abstract class SuggestionSearchAdapter extends
        AbstractSearchListAdapter<SuggestionSearchAdapter.ViewHolder> {
    private final Activity activity;
    private final boolean hasLinkArrow;

    public SuggestionSearchAdapter(ThemedActivity activity, boolean hasLinkArrow) {
        this.activity = activity;
        this.hasLinkArrow = hasLinkArrow;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView label;
        final AdaptiveIconView icon;

        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(ViewGroup itemView) {
            super(itemView);

            label = itemView.findViewById(R.id.textView);
            icon = itemView.findViewById(R.id.icon);

            if (!hasLinkArrow) {
                itemView.findViewById(R.id.target_arrow).setVisibility(View.GONE);
            }

            itemView.setHapticFeedbackEnabled(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder((ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.suggestion_item, container, false));
    }
}
