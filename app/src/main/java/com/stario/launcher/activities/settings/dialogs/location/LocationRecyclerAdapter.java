/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.activities.settings.dialogs.location;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.activities.launcher.glance.extensions.weather.GeocoderFallback;
import com.stario.launcher.activities.launcher.glance.extensions.weather.Weather;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationRecyclerAdapter extends RecyclerView.Adapter<LocationRecyclerAdapter.ViewHolder> {
    private static final String TAG = "LocationRecyclerAdapter";
    private static final int MAX_LOCALITIES = 4;

    private final LocalBroadcastManager broadcastManager;
    private final View.OnClickListener clickListener;
    private final List<Address> defaultAddresses;
    private final SharedPreferences preferences;
    private final List<Address> addresses;
    private final ThemedActivity activity;
    private final GeocoderFallback geocoder;

    private String query;

    public LocationRecyclerAdapter(ThemedActivity activity,
                                   View.OnClickListener clickListener) {
        this.geocoder = new GeocoderFallback(activity);
        this.activity = activity;
        this.clickListener = clickListener;
        this.preferences = activity.getApplicationContext()
                .getSharedPreferences(Entry.WEATHER);

        this.defaultAddresses = loadDefaultAddresses();
        this.addresses = new ArrayList<>(this.defaultAddresses);
        this.broadcastManager = LocalBroadcastManager.getInstance(activity);

        this.query = null;
    }

    private ArrayList<Address> loadDefaultAddresses() {
        ArrayList<Address> list = new ArrayList<>();

        Address lindholmen = new Address(Locale.ENGLISH);
        lindholmen.setAddressLine(0, "Lindholmen, Gothenburg, Sweden");
        lindholmen.setFeatureName("Lindholmen");
        lindholmen.setAdminArea("Västra Götaland County");
        lindholmen.setLocality("Gothenburg");
        lindholmen.setCountryCode("SE");
        lindholmen.setCountryName("Sweden");
        lindholmen.setLatitude(57.7077599);
        lindholmen.setLongitude(11.9382865);

        list.add(lindholmen);

        Address boulderCity = new Address(Locale.ENGLISH);
        boulderCity.setAddressLine(0, "Boulder City, NV, USA");
        boulderCity.setFeatureName("Boulder City");
        boulderCity.setAdminArea("Nevada");
        boulderCity.setSubAdminArea("Clark County");
        boulderCity.setLocality("Boulder City");
        boulderCity.setCountryCode("US");
        boulderCity.setCountryName("United States");
        boulderCity.setLatitude(35.9782216);
        boulderCity.setLongitude(-114.8345117);

        list.add(boulderCity);

        Address storo = new Address(Locale.ENGLISH);
        storo.setAddressLine(0, "Storo, Oslo, Norway");
        storo.setFeatureName("Storo");
        storo.setAdminArea("Oslo");
        storo.setLocality("Oslo");
        storo.setCountryCode("NO");
        storo.setCountryName("Norway");
        storo.setLatitude(59.946576199999996);
        storo.setLongitude(10.779069800000002);

        list.add(storo);

        Address sibiu = new Address(Locale.ENGLISH);
        sibiu.setAddressLine(0, "Sibiu, Romania");
        sibiu.setFeatureName("Sibiu");
        sibiu.setAdminArea("Sibiu");
        sibiu.setSubAdminArea("Sibiu");
        sibiu.setLocality("Sibiu");
        sibiu.setCountryCode("Ro");
        sibiu.setCountryName("Romania");
        sibiu.setLatitude(45.803478899999995);
        sibiu.setLongitude(24.1449997);

        list.add(sibiu);

        return list;
    }

    public void update(String query) {
        this.query = query;

        if (query == null || query.isBlank()) {
            addresses.clear();
            addresses.addAll(defaultAddresses);

            // noinspection notifyDataSetChanged
            notifyDataSetChanged();
        } else {
            Utils.submitTask(() -> {
                List<Address> addressList = geocoder.getFromLocationName(query, MAX_LOCALITIES);

                if (query.equals(LocationRecyclerAdapter.this.query)) {
                    UiUtils.runOnUIThread(() -> {
                        addresses.clear();

                        if (addressList != null) {
                            for (Address address : addressList) {
                                if (address.hasLatitude() && address.hasLongitude()) {
                                    addresses.add(address);
                                }
                            }
                        }

                        // noinspection notifyDataSetChanged
                        notifyDataSetChanged();
                    });
                }
            });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView locality;
        private final TextView location;

        public ViewHolder(View itemView) {
            super(itemView);

            locality = itemView.findViewById(R.id.locality);
            location = itemView.findViewById(R.id.location);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        if (position == 0) {
            viewHolder.locality.setText(R.string.location_ip_based);
            viewHolder.location.setVisibility(View.GONE);

            viewHolder.itemView.setOnClickListener(v -> {
                preferences.edit()
                        .remove(Weather.LATITUDE_KEY)
                        .remove(Weather.LONGITUDE_KEY)
                        .remove(Weather.LOCATION_NAME)
                        .apply();

                broadcastManager.sendBroadcastSync(new Intent(Weather.ACTION_REQUEST_UPDATE));

                if (clickListener != null) {
                    clickListener.onClick(v);
                }
            });
        } else {
            Address address = addresses.get(position - 1);

            String locality = address.getSubLocality();

            if (locality == null || locality.isBlank()) {
                locality = address.getLocality();

                if (locality == null || locality.isBlank()) {
                    locality = address.getFeatureName();
                }

                if (locality == null || locality.isBlank()) {
                    locality = address.getAdminArea();
                }

                viewHolder.locality.setText(locality);
                viewHolder.location.setText(address.getCountryName());
            } else {
                viewHolder.locality.setText(locality);

                String mainLocality = address.getLocality();
                if (mainLocality != null) {
                    viewHolder.location.setText(mainLocality + ", " + address.getCountryName());
                } else {
                    viewHolder.location.setText(address.getCountryName());
                }
            }

            viewHolder.location.setVisibility(View.VISIBLE);

            viewHolder.itemView.setOnClickListener(v -> {
                SharedPreferences.Editor editor = preferences.edit()
                        .putLong(Weather.LATITUDE_KEY,
                                Double.doubleToLongBits(address.getLatitude()))
                        .putLong(Weather.LONGITUDE_KEY,
                                Double.doubleToLongBits(address.getLongitude()));

                String city = address.getSubLocality();
                if (city != null && !city.isBlank()) {
                    editor.putString(Weather.LOCATION_NAME, city);
                } else {
                    city = address.getLocality();

                    if (city != null && !city.isBlank()) {
                        editor.putString(Weather.LOCATION_NAME, city);
                    } else {
                        editor.putString(Weather.LOCATION_NAME, null);
                    }
                }

                editor.apply();

                broadcastManager.sendBroadcastSync(new Intent(Weather.ACTION_REQUEST_UPDATE));

                if (clickListener != null) {
                    clickListener.onClick(v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return addresses.size() + 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup container, int viewType) {
        return new ViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.location_item, container, false));
    }
}