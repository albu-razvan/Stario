/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.sheet.widgets.configurator;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetProviderInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.stario.launcher.R;
import com.stario.launcher.sheet.widgets.WidgetSize;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.measurements.Measurements;
import com.stario.launcher.ui.widgets.RoundedWidgetHost;
import com.stario.launcher.utils.Casing;
import com.stario.launcher.utils.Utils;
import com.stario.launcher.utils.animation.Animation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class WidgetItemAdapter extends RecyclerView.Adapter<WidgetItemAdapter.ViewHolder> {
    private static final String TAG = "WidgetItemAdapter";
    private final WidgetListAdapter.WidgetGroupEntry entry;
    private final WidgetConfigurator.Request requestListener;
    private final ThemedActivity activity;
    private ViewHolder targetHolder;

    public WidgetItemAdapter(ThemedActivity activity, WidgetListAdapter.WidgetGroupEntry entry,
                             WidgetConfigurator.Request requestListener) {
        this.requestListener = requestListener;
        this.activity = activity;
        this.entry = entry;
        this.targetHolder = null;
    }

    void reset() {
        if (targetHolder != null) {
            targetHolder.preview.animate().alpha(1)
                    .setDuration(Animation.SHORT.getDuration());
            targetHolder.options.setVisibility(View.INVISIBLE);

            targetHolder = null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ConstraintLayout preview;
        private final TextView label;
        private final View options;
        private final View small;
        private final View medium;
        private final View large;

        public ViewHolder(View itemView) {
            super(itemView);

            preview = itemView.findViewById(R.id.preview);
            label = itemView.findViewById(R.id.label);
            options = itemView.findViewById(R.id.options);
            small = itemView.findViewById(R.id.small);
            medium = itemView.findViewById(R.id.medium);
            large = itemView.findViewById(R.id.large);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        return new ViewHolder(inflater.inflate(R.layout.widget_picker_preview, container, false));
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppWidgetProviderInfo info = entry.widgets.get(position);

        if (info != null) {
            holder.label.setText(Casing.toTitleCase(
                    info.loadLabel(activity.getPackageManager())));

            holder.itemView.setOnClickListener(view -> {
                if (holder.options.getVisibility() != View.VISIBLE) {
                    holder.preview.animate().alpha(0.3f)
                            .setDuration(Animation.SHORT.getDuration());

                    holder.options.setVisibility(View.VISIBLE);

                    holder.small.setOnClickListener(v -> {
                        if (requestListener != null) {
                            requestListener.requestAddition(info, WidgetSize.SMALL);
                        }
                    });

                    holder.medium.setOnClickListener(v -> {
                        if (requestListener != null) {
                            requestListener.requestAddition(info, WidgetSize.MEDIUM);
                        }
                    });

                    holder.large.setOnClickListener(v -> {
                        if (requestListener != null) {
                            requestListener.requestAddition(info, WidgetSize.LARGE);
                        }
                    });

                    if (!holder.equals(targetHolder)) {
                        reset();
                    }

                    targetHolder = holder;
                } else {
                    holder.preview.animate().alpha(1)
                            .setDuration(Animation.SHORT.getDuration());

                    holder.options.setVisibility(View.INVISIBLE);
                }
            });

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            params.constrainedHeight = true;
            params.matchConstraintMaxHeight = Measurements.dpToPx(180);

            if (Utils.isMinimumSDK(31)) {
                int previewLayout = info.previewLayout;

                if (info.maxResizeWidth > 0) {
                    params.constrainedWidth = true;
                    params.matchConstraintMaxWidth = info.maxResizeWidth;
                }

                if (info.maxResizeHeight > 0) {
                    params.matchConstraintMaxHeight = Math.min(info.maxResizeHeight,
                            params.matchConstraintMaxHeight);
                }

                if (info.minHeight > 0) {
                    params.matchConstraintMinHeight = info.minHeight;
                }

                if (previewLayout != 0) {
                    AppWidgetProviderInfo previewInfo = info.clone();
                    previewInfo.initialLayout = info.previewLayout;

                    try {
                        Resources resources = activity.getPackageManager()
                                .getResourcesForApplication(info.provider.getPackageName());

                        XmlResourceParser parser = resources.getLayout(previewLayout);
                        boolean found = false;
                        int state;

                        do {
                            state = parser.next();

                            if (state == XmlPullParser.START_TAG) {
                                AttributeSet set = Xml.asAttributeSet(parser);

                                int height = set.getAttributeIntValue("http://schemas.android.com/apk/res/android",
                                        "layout_height", Integer.MIN_VALUE); // if it exists, it will always be >= -2

                                if (height != Integer.MIN_VALUE) {
                                    if (height == ViewGroup.LayoutParams.MATCH_PARENT) {
                                        if (previewInfo.targetCellHeight > 0 &&
                                                previewInfo.targetCellWidth > 0) {
                                            params.height = 0;
                                            params.dimensionRatio = "W," + (previewInfo.targetCellHeight * 2 + 1) + ":" +
                                                    previewInfo.targetCellWidth * 2; // fake bigger cell height, as in launcher3
                                        } else {
                                            params.height = params.matchConstraintMaxHeight;
                                        }
                                    }

                                    found = true;
                                }
                            }
                        } while (state != XmlPullParser.END_DOCUMENT && !found);

                        if (!found) {
                            if (previewInfo.targetCellHeight > 0 &&
                                    previewInfo.targetCellWidth > 0) {
                                params.height = 0;
                                params.dimensionRatio = "W," + (previewInfo.targetCellHeight * 2 + 1) + ":" +
                                        previewInfo.targetCellWidth * 2;
                            } else {
                                params.height = params.matchConstraintMaxHeight;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException |
                             XmlPullParserException | IOException exception) {
                        Log.e(TAG, "onBindViewHolder: Exception parsing layout. ", exception);
                    }

                    RoundedWidgetHost host = new RoundedWidgetHost(activity, params);

                    host.setAppWidget(-1, previewInfo);
                    host.updateAppWidget(null);

                    host.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        View child = host.getChildAt(0);

                        if (child != null) {
                            float scale = Math.min(0.85f, Math.min((float) host.getMeasuredWidth() / child.getMeasuredWidth(),
                                    (float) host.getMeasuredHeight() / child.getMeasuredHeight()));

                            if (!Float.isNaN(scale)) {
                                child.setScaleY(scale);
                                child.setScaleX(scale);
                            }
                        }

                        forwardGroupClicks(host, holder.itemView);
                    });

                    holder.preview.addView(host);

                    return;
                }
            }

            Drawable previewImage = info.loadPreviewImage(activity, Measurements.getDotsPerInch());

            if (previewImage == null) {
                previewImage = entry.icon;
                params.height = Measurements.getIconSize();
            } else {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            ImageView imageView = new ImageView(activity);
            Glide.with(activity)
                    .load(previewImage)
                    .apply(RequestOptions.bitmapTransform(
                                    new RoundedCornersTransformation(
                                            Measurements.dpToPx(RoundedWidgetHost.RADIUS_DP), 0)
                            )
                    ).into(imageView);
            imageView.setLayoutParams(params);

            imageView.setImageDrawable(previewImage);

            holder.preview.addView(imageView);
        }
    }

    static void forwardGroupClicks(ViewGroup viewGroup, View forwardTarget) {
        for (int index = 0; index < viewGroup.getChildCount(); index++) {
            View view = viewGroup.getChildAt(index);

            view.setOnTouchListener(null);
            view.setOnClickListener(v -> forwardTarget.performClick());

            if (view instanceof ViewGroup) {
                forwardGroupClicks((ViewGroup) view, forwardTarget);
            }
        }
    }

    @Override
    public int getItemCount() {
        return entry.widgets.size();
    }
}