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

package com.stario.launcher.glance.extensions.media;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceViewExtension;
import com.stario.launcher.themes.ThemedActivity;

public final class MediaPreview implements GlanceViewExtension {
    private View root;
    private boolean enabled;

    public MediaPreview() {
        this.enabled = false;
    }

    @Override
    public View inflate(ThemedActivity activity, LinearLayout container) {
        root = activity.getLayoutInflater()
                .inflate(R.layout.media_preview, container, false);

        return root;
    }

    @Override
    public void updateData(@NonNull Bundle data) {
        enabled = data.getBoolean(Media.ENABLED_KEY, false);

        update();
    }

    @Override
    public void update() {
        if (enabled) {
            root.setVisibility(View.VISIBLE);
        } else {
            root.setVisibility(View.GONE);
        }
    }
}
