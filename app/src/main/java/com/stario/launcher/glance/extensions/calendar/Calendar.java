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

package com.stario.launcher.glance.extensions.calendar;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceViewExtension;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.utils.Casing;

import java.text.SimpleDateFormat;

public final class Calendar implements GlanceViewExtension {
    private TextView text;

    @Override
    public View inflate(ThemedActivity activity, LinearLayout container) {
        View root = activity.getLayoutInflater()
                .inflate(R.layout.calendar, container, false);

        text = root.findViewById(R.id.calendar);

        text.setOnClickListener(v -> {
            Vibrations.getInstance().vibrate();

            try {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();

                builder.appendPath("time");

                ContentUris.appendId(builder,
                        java.util.Calendar.getInstance().getTimeInMillis());

                text.getContext().startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(builder.build()));
            } catch (Exception exception) {
                Log.e("Calendar", "inflate: ", exception);
            }
        });

        return root;
    }

    @Override
    public void update() {
        if (text != null) {
            String value = new SimpleDateFormat("EEEE, MMM d",
                    text.getTextLocale())
                    .format(android.icu.util.Calendar.getInstance()
                            .getTime().getTime());

            text.post(() -> text.setText(Casing.toTitleCase(value)));
        }
    }
}
