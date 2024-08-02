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

package com.stario.launcher.ui.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;

import androidx.constraintlayout.widget.ConstraintLayout;

public class WidgetHost extends AppWidgetHost {
    public static final String REMOVE_WIDGET = "com.stario.launcher.LauncherAppWidgetHost.REMOVE_WIDGET";
    public static final String WIDGET_ID = "com.stario.launcher.LauncherAppWidgetHost.WIDGET_ID";
    private final Context context;

    public WidgetHost(Context context, int hostId) {
        super(context, hostId);

        this.context = context;
    }

    @Override
    public void onAppWidgetRemoved(int identifier) {
        Intent intent = new Intent(REMOVE_WIDGET);
        intent.putExtra(WIDGET_ID, identifier);

        if (context != null) {
            context.sendBroadcast(intent);
        }

        super.onAppWidgetRemoved(identifier);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
                                             AppWidgetProviderInfo appWidget) {
        WidgetContainer.LayoutParams params =
                new WidgetContainer.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                        WidgetContainer.LayoutParams.MATCH_PARENT);

        return new WidgetHostView(context, params);
    }
}