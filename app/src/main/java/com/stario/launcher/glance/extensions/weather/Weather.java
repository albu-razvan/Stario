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

package com.stario.launcher.glance.extensions.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.sisyphsu.dateparser.DateParser;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.stario.launcher.R;
import com.stario.launcher.glance.extensions.GlanceDialogExtension;
import com.stario.launcher.glance.extensions.GlanceViewExtension;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.common.glance.GlanceConstraintLayout;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Weather extends GlanceDialogExtension {
    private static final String TAG = "com.stario.launcher.Weather";
    private static final String LATITUDE_KEY = "com.stario.LATITUDE";
    private static final String LONGITUDE_KEY = "com.stario.LONGITUDE";
    public static final String IMPERIAL_KEY = "com.stario.IMPERIAL";
    private static final int FORECAST_MAX_ENTRIES = 20;
    private static final int UPDATE_INTERVAL = 3_600_000;
    private static final int REQUEST_TIMEOUT = 10_000;
    private static final int DAY = 0;
    private static final int NIGHT = 1;
    private static final int SUMMARY = 2;
    private static final HashMap<String, HashMap<Integer, Integer>> weatherResources = new HashMap<>() {{
        put("clearsky", new HashMap<>() {{
            put(DAY, R.drawable.clear_day);
            put(NIGHT, R.drawable.clear_night);
            put(SUMMARY, R.string.clear_sky);
        }});
        put("cloudy", new HashMap<>() {{
            put(DAY, R.drawable.cloudy);
            put(NIGHT, R.drawable.cloudy);
            put(SUMMARY, R.string.cloudy);
        }});
        put("fair", new HashMap<>() {{
            put(DAY, R.drawable.mostly_clear_day);
            put(NIGHT, R.drawable.mostly_clear_night);
            put(SUMMARY, R.string.fair);
        }});
        put("fog", new HashMap<>() {{
            put(DAY, R.drawable.haze_fog_dust_smoke);
            put(NIGHT, R.drawable.haze_fog_dust_smoke);
            put(SUMMARY, R.string.fog);
        }});
        put("heavyrain", new HashMap<>() {{
            put(DAY, R.drawable.heavy_rain);
            put(NIGHT, R.drawable.heavy_rain);
            put(SUMMARY, R.string.heavy_rain);
        }});
        put("heavyrainandthunder", new HashMap<>() {{
            put(DAY, R.drawable.strong_thunderstorms);
            put(NIGHT, R.drawable.strong_thunderstorms);
            put(SUMMARY, R.string.heavy_rain_and_thunder);
        }});
        put("heavyrainshowers", new HashMap<>() {{
            put(DAY, R.drawable.scattered_showers_day);
            put(NIGHT, R.drawable.scattered_showers_night);
            put(SUMMARY, R.string.heavy_rain_showers);
        }});
        put("heavyrainshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.isolated_scattered_thunderstorms_day);
            put(NIGHT, R.drawable.isolated_scattered_thunderstorms_night);
            put(SUMMARY, R.string.heavy_rain_showers_and_thunder);
        }});
        put("heavysleet", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.heavy_sleet);
        }});
        put("heavysleetandthunder", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.heavy_sleet_and_thunder);
        }});
        put("heavysleetshowers", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.heavy_sleet_showers);
        }});
        put("heavysleetshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.heavy_sleet_showers_and_thunder);
        }});
        put("heavysnow", new HashMap<>() {{
            put(DAY, R.drawable.heavy_snow);
            put(NIGHT, R.drawable.heavy_snow);
            put(SUMMARY, R.string.heavy_snow);
        }});
        put("heavysnowandthunder", new HashMap<>() {{
            put(DAY, R.drawable.blowing_snow);
            put(NIGHT, R.drawable.blowing_snow);
            put(SUMMARY, R.string.heavy_snow_and_thunder);
        }});
        put("heavysnowshowers", new HashMap<>() {{
            put(DAY, R.drawable.scattered_snow_showers_day);
            put(NIGHT, R.drawable.scattered_snow_showers_night);
            put(SUMMARY, R.string.heavy_snow_showers);
        }});
        put("heavysnowshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.heavy_snow);
            put(NIGHT, R.drawable.heavy_snow);
            put(SUMMARY, R.string.heavy_snow_showers_and_thunder);
        }});
        put("lightrain", new HashMap<>() {{
            put(DAY, R.drawable.drizzle);
            put(NIGHT, R.drawable.drizzle);
            put(SUMMARY, R.string.light_rain);
        }});
        put("lightrainandthunder", new HashMap<>() {{
            put(DAY, R.drawable.isolated_scattered_thunderstorms_day);
            put(NIGHT, R.drawable.isolated_scattered_thunderstorms_night);
            put(SUMMARY, R.string.light_rain_and_thunder);
        }});
        put("lightrainshowers", new HashMap<>() {{
            put(DAY, R.drawable.scattered_showers_day);
            put(NIGHT, R.drawable.scattered_showers_night);
            put(SUMMARY, R.string.light_rain_showers);
        }});
        put("lightrainshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.isolated_scattered_thunderstorms_day);
            put(NIGHT, R.drawable.isolated_scattered_thunderstorms_night);
            put(SUMMARY, R.string.light_rain_showers_and_thunder);
        }});
        put("lightsleet", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_sleet);
        }});
        put("lightsleetandthunder", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_sleet_and_thunder);
        }});
        put("lightsleetshowers", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_sleet_showers);
        }});
        put("lightsnow", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_snow);
        }});
        put("lightsnowandthunder", new HashMap<>() {{
            put(DAY, R.drawable.scattered_snow_showers_day);
            put(NIGHT, R.drawable.scattered_snow_showers_night);
            put(SUMMARY, R.string.light_snow_and_thunder);
        }});
        put("lightsnowshowers", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_snow_showers);
        }});
        put("lightssleetshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_sleet_showers_and_thunder);
        }});
        put("lightssnowshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.flurries);
            put(NIGHT, R.drawable.flurries);
            put(SUMMARY, R.string.light_snow_showers_and_thunder);
        }});
        put("partlycloudy", new HashMap<>() {{
            put(DAY, R.drawable.partly_cloudy_day);
            put(NIGHT, R.drawable.partly_cloudy_night);
            put(SUMMARY, R.string.partly_cloudy);
        }});
        put("rain", new HashMap<>() {{
            put(DAY, R.drawable.showers_rain);
            put(NIGHT, R.drawable.showers_rain);
            put(SUMMARY, R.string.rain);
        }});
        put("rainandthunder", new HashMap<>() {{
            put(DAY, R.drawable.isolated_thunderstorms);
            put(NIGHT, R.drawable.isolated_thunderstorms);
            put(SUMMARY, R.string.rain_and_thunder);
        }});
        put("rainshowers", new HashMap<>() {{
            put(DAY, R.drawable.showers_rain);
            put(NIGHT, R.drawable.showers_rain);
            put(SUMMARY, R.string.rain_showers);
        }});
        put("rainshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.isolated_scattered_thunderstorms_day);
            put(NIGHT, R.drawable.isolated_scattered_thunderstorms_night);
            put(SUMMARY, R.string.rain_showers_and_thunder);
        }});
        put("sleet", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.sleet);
        }});
        put("sleetandthunder", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.sleet_and_thunder);
        }});
        put("sleetshowers", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.sleet_showers);
        }});
        put("sleetshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.sleet_hail);
            put(NIGHT, R.drawable.sleet_hail);
            put(SUMMARY, R.string.sleet_showers_and_thunder);
        }});
        put("snow", new HashMap<>() {{
            put(DAY, R.drawable.showers_snow);
            put(NIGHT, R.drawable.showers_snow);
            put(SUMMARY, R.string.snow);
        }});
        put("snowandthunder", new HashMap<>() {{
            put(DAY, R.drawable.heavy_snow);
            put(NIGHT, R.drawable.heavy_snow);
            put(SUMMARY, R.string.snow_and_thunder);
        }});
        put("snowshowers", new HashMap<>() {{
            put(DAY, R.drawable.scattered_snow_showers_day);
            put(NIGHT, R.drawable.scattered_snow_showers_night);
            put(SUMMARY, R.string.snow_showers);
        }});
        put("snowshowersandthunder", new HashMap<>() {{
            put(DAY, R.drawable.showers_snow);
            put(NIGHT, R.drawable.showers_snow);
            put(SUMMARY, R.string.snow_showers_and_thunder);
        }});
    }};

    private static double lat = -1;
    private static double lon = -1;

    private final WeatherPreview preview;
    private final DateParser dateParser;

    private SharedPreferences coordinates;
    private ArrayList<Data> weatherData;
    private SharedPreferences settings;
    private RecyclerView recycler;
    private TextView temperature;
    private TextView location;
    private TextView summary;
    private boolean updating;
    private Address address;
    private long lastUpdate;
    private View direction;
    private TextView speed;
    private View container;
    private ImageView icon;

    public Weather() {
        this.weatherData = new ArrayList<>();
        this.dateParser = DateParser.newBuilder().build();
        this.lastUpdate = -UPDATE_INTERVAL;

        this.preview = new WeatherPreview();
    }

    @Override
    public String getTAG() {
        return TAG;
    }

    @Override
    protected GlanceViewExtension getViewExtensionPreview() {
        return preview;
    }

    @Override
    protected GlanceConstraintLayout inflateExpanded(LayoutInflater inflater, ConstraintLayout container) {
        GlanceConstraintLayout root = (GlanceConstraintLayout) inflater.inflate(R.layout.weather,
                container, false);

        coordinates = activity.getSharedPreferences(Entry.WEATHER);
        settings = activity.getSharedPreferences(Entry.STARIO);

        this.container = root.findViewById(R.id.container);
        temperature = root.findViewById(R.id.temperature);
        location = root.findViewById(R.id.location);
        summary = root.findViewById(R.id.summary);
        direction = root.findViewById(R.id.direction);
        speed = root.findViewById(R.id.speed);
        recycler = root.findViewById(R.id.forecast);
        icon = root.findViewById(R.id.icon);

        recycler.setLayoutManager(new LinearLayoutManager(activity,
                LinearLayoutManager.HORIZONTAL, false));

        updateData();

        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        activity.getLifecycle()
                .addObserver(new DefaultLifecycleObserver() {
                    @Override
                    public void onResume(@NonNull LifecycleOwner owner) {
                        int index = getFirstIndexInTime();

                        if (index > 0) {
                            preview.updateTemperature(weatherData.get(index).temperature);
                            preview.updateIcon(weatherData.get(index).iconCode);
                        }
                    }
                });
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected void updateScaling(@FloatRange(from = 0f, to = 1f) float fraction,
                                 float scale) {
        container.setScaleY(scale);
        container.setAlpha(fraction);
    }

    public HashMap<String, Object> data = new HashMap<>();

    @Override
    public void update() {
        if (updating) {
            return;
        }

        Utils.submitTask(() -> {
            if (Math.abs(System.currentTimeMillis() - lastUpdate) > UPDATE_INTERVAL) {
                updating = true;

                if (coordinates.contains(LATITUDE_KEY) &&
                        coordinates.contains(LONGITUDE_KEY)) {
                    lat = Double.longBitsToDouble(
                            coordinates.getLong(LATITUDE_KEY, -1));
                    lon = Double.longBitsToDouble(
                            coordinates.getLong(LONGITUDE_KEY, -1));
                } else {
                    updateLocation(Utils.getPublicIPAddress());
                }

                if (lat < 0 || lon < 0) {
                    return;
                }

                JSONObject weatherData = getWeatherInfo();

                if (weatherData == null) {
                    return;
                }

                try {
                    JSONObject properties = weatherData.getJSONObject("properties");
                    JSONArray timeSeries = properties.getJSONArray("timeseries");

                    ArrayList<Data> entries = new ArrayList<>();

                    for (int index = 0; index < timeSeries.length() &&
                            entries.size() <= FORECAST_MAX_ENTRIES; index++) {
                        try {
                            JSONObject container = timeSeries.getJSONObject(index);

                            String time = container.getString("time");

                            JSONObject data = container.getJSONObject("data");
                            JSONObject instant = data.getJSONObject("instant");
                            JSONObject details = instant.getJSONObject("details");

                            double temperature = details.getDouble("air_temperature");
                            double windDirection = details.getDouble("wind_from_direction");
                            double windSpeed = details.getDouble("wind_speed");

                            JSONObject nextHour = data.getJSONObject("next_6_hours");
                            JSONObject summary = nextHour.getJSONObject("summary");

                            String iconCode = summary.getString("symbol_code");

                            entries.add(new Data(dateParser.parseDate(time), iconCode,
                                    temperature, windDirection, windSpeed));
                        } catch (JSONException exception) {
                            Log.e(TAG, "update: Parse exception for item " + index + ".");
                        }
                    }

                    if (entries.size() > 0) {
                        this.weatherData = entries;
                    }

                    int index = getFirstIndexInTime();

                    if (index > 0) {
                        UiUtils.runOnUIThread(() -> {
                            preview.updateTemperature(this.weatherData.get(index).temperature);
                            preview.updateIcon(this.weatherData.get(index).iconCode);

                            updateData();
                        });
                    }

                    lastUpdate = System.currentTimeMillis() - lastUpdate;
                } catch (JSONException exception) {
                    Log.e(TAG, "update: ", exception);
                }

                updating = false;
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateData() {
        int index = getFirstIndexInTime();

        if (index > 0) {
            Data data = weatherData.get(index);

            icon.setImageResource(getIcon(data.iconCode));

            if (settings.getBoolean(Weather.IMPERIAL_KEY, false)) {
                temperature.setText((int) Math.round(Utils.toFahrenheit(data.temperature)) + "°");
            } else {
                temperature.setText((int) Math.round(data.temperature) + "°");
            }

            summary.setText(getSummary(data.iconCode));

            if (address != null) {
                String locationString = address.getLocality();

                if (locationString == null) {
                    locationString = address.getSubLocality();
                }

                location.setText(locationString);
            } else {
                location.setText(null);
            }

            direction.setRotation((float) data.windDirection + 180f);
            if (settings.getBoolean(Weather.IMPERIAL_KEY, false)) {
                speed.setText((int) Math.round(Utils.msToMph(data.windSpeed)) + "mi/h");
            } else {
                speed.setText((int) Math.round(data.windSpeed) + "m/s");
            }

            recycler.setAdapter(new ForecastAdapter(activity, weatherData, index));
        }
    }

    @Override
    protected void show() {
        updateData();

        super.show();
    }

    private int getFirstIndexInTime() {
        if (weatherData != null) {
            Calendar calendar = Calendar.getInstance();

            for (int index = 0; index < weatherData.size(); index++) {
                if (weatherData.get(index).date.after(calendar.getTime())) {
                    return index;
                }
            }
        }

        return -1;
    }

    public static int getIcon(String iconCode) {
        Calendar calendar = Calendar.getInstance();

        Location location = new Location(lat, lon);
        SunriseSunsetCalculator calculator =
                new SunriseSunsetCalculator(location, calendar.getTimeZone());

        Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(calendar);
        Calendar sunset = calculator.getOfficialSunsetCalendarForDate(calendar);

        HashMap<Integer, Integer> dataForCode = weatherResources.get(iconCode);

        if (dataForCode == null) {
            return R.drawable.unavailable;
        }

        Integer icon;
        if (sunrise.getTimeInMillis() < calendar.getTimeInMillis() &&
                calendar.getTimeInMillis() < sunset.getTimeInMillis()) {
            icon = dataForCode.getOrDefault(DAY, R.drawable.unavailable);
        } else {
            icon = dataForCode.getOrDefault(NIGHT, R.drawable.unavailable);
        }

        return icon != null ? icon : 0;
    }

    public static int getSummary(String iconCode) {
        HashMap<Integer, Integer> dataForCode = weatherResources.get(iconCode);

        Integer summary;
        if (dataForCode == null) {
            summary = R.string.unavailable;
        } else {
            summary = dataForCode.getOrDefault(SUMMARY, R.string.unavailable);
        }

        return summary != null ? summary : 0;
    }

    private void updateLocation(String ip) {
        StringBuilder response = new StringBuilder();

        try {
            HttpURLConnection connection = (HttpURLConnection)
                    (new URL("http://ip-api.com/json/" + ip).openConnection());

            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
            connection.addRequestProperty("Content-type", "application/json");

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            connection.disconnect();

            JSONObject jsonObject = new JSONObject(response.toString());
            lat = jsonObject.getDouble("lat");
            lon = jsonObject.getDouble("lon");

            List<Address> addresses = new Geocoder(activity)
                    .getFromLocation(lat, lon, 1);

            if (addresses != null && addresses.size() > 0) {
                address = addresses.get(0);
            }
        } catch (Exception exception) {
            Log.e(TAG, "getLocationInfo: ", exception);
        }
    }

    private JSONObject getWeatherInfo() {
        StringBuilder response = new StringBuilder();

        try {
            HttpURLConnection connection = (HttpURLConnection)
                    (new URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" +
                            lat + "&lon=" + lon).openConnection());

            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
            connection.setRequestProperty("Content-type", "application/json");

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            connection.disconnect();

            return new JSONObject(response.toString());
        } catch (Exception exception) {
            Log.e(TAG, "getWeatherInfo: ", exception);

            return null;
        }
    }

    public static class Data {
        public final Date date;
        public final String iconCode;
        public final double temperature; // Celsius
        public final double windDirection; // Degrees
        public final double windSpeed; // m/s

        private Data(Date date, String iconCode, double temperature,
                     double windDirection, double windSpeed) {
            this.date = date;
            this.iconCode = iconCode != null ? iconCode.split("[_%.-]")[0] : null;
            this.temperature = temperature;
            this.windDirection = windDirection;
            this.windSpeed = windSpeed;
        }
    }
}
