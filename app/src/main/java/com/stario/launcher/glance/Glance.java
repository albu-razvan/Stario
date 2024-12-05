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

package com.stario.launcher.glance;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.GlanceExtension;
import com.stario.launcher.glance.extensions.GlanceViewExtension;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.glance.GlanceConstraintLayout;

import java.util.ArrayList;

public class Glance {
    private final ThemedActivity activity;
    private final ArrayList<GlanceExtension> extensions;
    private GlanceConstraintLayout root;
    private LinearLayout extensionContainer;

    public Glance(ThemedActivity activity) {
        this.activity = activity;
        this.extensions = new ArrayList<>();
    }

    public void attach(ViewGroup container) {
        root = (GlanceConstraintLayout) activity.getLayoutInflater()
                .inflate(R.layout.glance, container, false);

        extensionContainer = root.findViewById(R.id.extensions);

        container.addView(root);
    }

    public GlanceViewExtension attachViewExtension(GlanceViewExtension extension, View.OnClickListener listener) {
        if (root == null) {
            throw new RuntimeException("Glance should attach itself first before attaching extensions.");
        }

        View view = extension.inflate(activity, extensionContainer);

        extensionContainer.addView(view);
        view.setOnClickListener(listener);

        extensions.add(extension);

        return extension;
    }

    public void attachViewExtension(GlanceViewExtension extension) {
        attachViewExtension(extension, null);
    }

    public void attachDialogExtension(GlanceDialogExtension extension, int gravity,
                                      GlanceDialogExtension.TransitionListener listener) {
        if (root == null) {
            throw new RuntimeException("Glance should attach itself first before attaching extensions.");
        }

        extension.attach(this, gravity, progress -> {
            // hide the blur
            root.setScaleX(1f - progress);
            root.setScaleY(1f - progress);
            root.setPivotY(0);

            if (listener != null) {
                listener.onProgressFraction(progress);
            }
        });

        extensions.add(extension);

        root.getViewTreeObserver().addOnPreDrawListener(() -> {
            int[] location = new int[2];
            root.getLocationInWindow(location);

            extension.updateLayout(location, root.getMeasuredWidth(),
                    root.getMeasuredHeight());

            return true;
        });
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    public void post(Runnable runnable) {
        root.post(runnable);
    }

    public float getHeight() {
        return root.getHeight();
    }

    public float getWidth() {
        return root.getWidth();
    }

    public void update() {
        for (GlanceExtension extension : extensions) {
            extension.update();
        }
    }
}
