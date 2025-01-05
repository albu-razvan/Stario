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

package com.stario.launcher.activities.settings.dialogs.license;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;

import java.util.List;
import java.util.regex.Pattern;

import de.philipp_bobek.oss_licenses_parser.OssLicensesParser;
import de.philipp_bobek.oss_licenses_parser.ThirdPartyLicense;

public class OSSLicensesRecyclerAdapter extends RecyclerView.Adapter<OSSLicensesRecyclerAdapter.ViewHolder> {
    private final List<ThirdPartyLicense> licenses;
    private final LayoutInflater inflater;
    private final Context context;

    public OSSLicensesRecyclerAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        licenses = OssLicensesParser.parseAllLicenses(
                context.getResources().openRawResource(R.raw.third_party_license_metadata),
                context.getResources().openRawResource(R.raw.third_party_licenses)
        );
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;

        public ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(OSSLicensesRecyclerAdapter.ViewHolder viewHolder, int position) {
        ThirdPartyLicense license = licenses.get(position);

        viewHolder.name.setText(license.getLibraryName() + " ↗");

        String url = license.getLicenseContent();
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        if (Pattern.matches("^(https?)://[\\w\\-.]+(?::\\d+)?(/\\S*)?$", url)) {
            String finalUrl = url;

            viewHolder.itemView.setOnClickListener(v ->
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))));
        }
    }

    @Override
    public int getItemCount() {
        return licenses.size();
    }

    @NonNull
    @Override
    public OSSLicensesRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.oss_licenses_item, container, false));
    }
}