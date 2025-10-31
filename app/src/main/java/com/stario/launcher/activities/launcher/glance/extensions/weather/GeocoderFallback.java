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

package com.stario.launcher.activities.launcher.glance.extensions.weather;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.webkit.WebSettings;

import com.stario.launcher.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeocoderFallback {
    private static final String TAG = "Geocoder";
    private static final String API = "https://photon.komoot.io/";
    private static final int REQUEST_TIMEOUT = 5_000;

    private final Geocoder geocoder;
    private final Activity activity;

    public GeocoderFallback(Activity activity) {
        this.activity = activity;

        if (Geocoder.isPresent()) {
            this.geocoder = new Geocoder(activity);
        } else {
            this.geocoder = null;
        }
    }

    public List<Address> getFromLocationName(String query, int maxResults) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>();
        }

        if (geocoder != null) {
            try {
                return geocoder.getFromLocationName(query, maxResults);
            } catch (IOException e) {
                return null;
            }
        }

        List<Address> addresses = new ArrayList<>();
        HttpURLConnection connection = null;

        try {
            // noinspection CharsetObjectCanBeUsed
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String urlString = API + "api/?q=" + encodedQuery + "&limit=" + maxResults;
            URL url = new URL(urlString);

            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
            connection.addRequestProperty("Content-type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject jsonObject = new JSONObject(Utils.readStream(connection.getInputStream()));
                JSONArray features = jsonObject.optJSONArray("features");

                if (features != null) {
                    for (int index = 0; index < features.length(); index++) {
                        JSONObject feature = features.getJSONObject(index);
                        Address address = parsePhotonFeature(feature);

                        if (address != null) {
                            addresses.add(address);
                        }
                    }
                }
            } else {
                Log.w(TAG, "getFromLocationName: Server returned non-OK status: " + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error in getFromLocationName", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return addresses;
    }

    public Address getFromLocation(double lat, double lon) {
        if (geocoder != null) {
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    return addresses.get(0);
                }

                return null;
            } catch (IOException e) {
                return null;
            }
        }

        String urlString = API + "reverse?lon=" + lon + "&lat=" + lat;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
            connection.addRequestProperty("Content-type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject jsonObject = new JSONObject(Utils.readStream(connection.getInputStream()));
                JSONArray features = jsonObject.optJSONArray("features");

                if (features != null && features.length() > 0) {
                    JSONObject firstFeature = features.getJSONObject(0);

                    return parsePhotonFeature(firstFeature);
                }
            } else {
                Log.w(TAG, "getFromLocation: Server returned non-OK status: " + responseCode);
            }
        } catch (IOException | JSONException exception) {
            Log.e(TAG, "getFromLocation: ", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private Address parsePhotonFeature(JSONObject feature) throws JSONException {
        if (feature == null) {
            return null;
        }

        // API only supports en, de and fr, so just don't bother for now
        Address address = new Address(Locale.ENGLISH);

        JSONObject geometry = feature.optJSONObject("geometry");
        if (geometry != null) {
            JSONArray coordinates = geometry.optJSONArray("coordinates");

            if (coordinates != null && coordinates.length() >= 2) {
                address.setLongitude(coordinates.getDouble(0));
                address.setLatitude(coordinates.getDouble(1));
            }
        }

        JSONObject properties = feature.optJSONObject("properties");
        if (properties != null) {
            String name = properties.optString("name");
            String city = properties.optString("city");
            String type = properties.optString("type");

            address.setFeatureName(name);

            if (city.isEmpty() && "city".equals(type)) {
                address.setLocality(name);
            } else {
                address.setLocality(city);
            }

            String locality = properties.optString("locality");
            String district = properties.optString("district");

            if (!locality.isEmpty()) {
                address.setSubLocality(locality);
            } else {
                address.setSubLocality(district);
            }

            address.setCountryName(properties.optString("country"));
            address.setCountryCode(properties.optString("countrycode"));
            address.setAdminArea(properties.optString("state"));
            address.setSubAdminArea(properties.optString("county"));
            address.setThoroughfare(properties.optString("street"));
        }

        return address;
    }
}
