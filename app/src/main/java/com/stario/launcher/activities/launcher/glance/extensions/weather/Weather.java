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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.LocationManager;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.sisyphsu.dateparser.DateParser;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.stario.launcher.R;
import com.stario.launcher.Stario;
import com.stario.launcher.activities.launcher.glance.GlanceDialogExtension;
import com.stario.launcher.activities.launcher.glance.GlanceViewExtension;
import com.stario.launcher.preferences.Entry;
import com.stario.launcher.ui.common.glance.GlanceConstraintLayout;
import com.stario.launcher.ui.utils.UiUtils;
import com.stario.launcher.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class Weather extends GlanceDialogExtension {
    public static final String ACTION_REQUEST_UPDATE = "com.stario.REQUEST_UPDATE";
    public static final String PRECISE_LOCATION = "com.stario.PRECISE_LOCATION";
    public static final String FORECAST_KEY = "com.stario.WEATHER_FORECAST";
    public static final String IMPERIAL_KEY = "com.stario.IMPERIAL";
    public static final String LOCATION_NAME = "com.stario.LOCATION";
    public static final String LATITUDE_KEY = "com.stario.LATITUDE";
    public static final String LONGITUDE_KEY = "com.stario.LONGITUDE";

    private static final String TAG = "com.stario.launcher.Weather";

    private static final int FORECAST_MAX_ENTRIES = 20;
    private static final int DEFAULT_UPDATE_INTERVAL = 3_600_000;
    private static final int FALLBACK_UPDATE_INTERVAL = 300_000;
    private static final int REQUEST_TIMEOUT = 10_000;
    private static final int DAY = 0;
    private static final int NIGHT = 1;
    private static final int SUMMARY = 2;
    private static final String LOCATION_API_IP_WILDCARD = "$";
    private static final IpApiEntry[] LOCATION_APIS = new IpApiEntry[]{
            new IpApiEntry("https://ip-api.com/json/$", new IpApiEntry.Callback[]{
                    new IpApiEntry.Callback("lat") {
                        @Override
                        void assign(double value) {
                            lat = value;
                        }
                    },
                    new IpApiEntry.Callback("lon") {
                        @Override
                        void assign(double value) {
                            lon = value;
                        }
                    }
            }),
            new IpApiEntry("https://freeipapi.com/api/json/$", new IpApiEntry.Callback[]{
                    new IpApiEntry.Callback("latitude") {
                        @Override
                        void assign(double value) {
                            lat = value;
                        }
                    },
                    new IpApiEntry.Callback("longitude") {
                        @Override
                        void assign(double value) {
                            lon = value;
                        }
                    }
            }),
            new IpApiEntry("https://ipapi.co/$/json/", new IpApiEntry.Callback[]{
                    new IpApiEntry.Callback("latitude") {
                        @Override
                        void assign(double value) {
                            lat = value;
                        }
                    },
                    new IpApiEntry.Callback("longitude") {
                        @Override
                        void assign(double value) {
                            lon = value;
                        }
                    }
            })
    };
    private static final HashMap<String, HashMap<Integer, Integer>> WEATHER_RESOURCES = new HashMap<>() {{
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

    private static volatile double lat = Double.MAX_VALUE;
    private static volatile double lon = Double.MAX_VALUE;

    private final BroadcastReceiver receiver;
    private final WeatherPreview preview;
    private final DateParser dateParser;

    private SharedPreferences weatherPreferences;
    private SharedPreferences settings;
    private GeocoderFallback geocoder;
    private volatile Address address;
    private volatile long lastUpdate;
    private List<Data> weatherData;
    private Future<?> runningTask;
    private RecyclerView recycler;
    private TextView temperature;
    private TextView location;
    private TextView summary;
    private View direction;
    private TextView speed;
    private View container;
    private ImageView icon;

    public Weather() {
        this.weatherData = new CopyOnWriteArrayList<>();
        this.dateParser = DateParser.newBuilder().build();
        this.runningTask = null;
        this.address = null;
        this.lastUpdate = 0;

        this.preview = new WeatherPreview();
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                lastUpdate = 0;
                address = null;
                weatherData.clear();
                preview.update(null);

                synchronized (Weather.this) {
                    if (runningTask != null &&
                            !runningTask.isDone()) {
                        runningTask.cancel(true);
                    }
                }

                update();
            }
        };
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

        Stario stario = activity.getApplicationContext();
        weatherPreferences = stario.getSharedPreferences(Entry.WEATHER);
        settings = stario.getSettings();

        geocoder = new GeocoderFallback(activity);

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
                            preview.update(weatherData.get(index));
                        }
                    }
                });

        //noinspection deprecation
        LocalBroadcastManager.getInstance(activity).registerReceiver(receiver,
                new IntentFilter(ACTION_REQUEST_UPDATE));
    }

    @Override
    public void onDetach() {
        super.onDetach();

        try {
            //noinspection deprecation
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver);
        } catch (Exception exception) {
            Log.e(TAG, "onDetach: Receiver not registered.");
        }
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
    public synchronized void update() {
        if (!weatherPreferences.getBoolean(FORECAST_KEY, true) ||
                (runningTask != null && !runningTask.isDone())) {
            return;
        }

        runningTask = Utils.submitTask(() -> {
            if (Math.abs(System.currentTimeMillis() - lastUpdate) > DEFAULT_UPDATE_INTERVAL) {
                boolean prefersPreciseLocation = weatherPreferences.getBoolean(PRECISE_LOCATION, false);
                boolean fetchedPreciseLocation = false;

                if (prefersPreciseLocation) {
                    if (activity.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        LocationManager locationManager = (LocationManager)
                                activity.getSystemService(Context.LOCATION_SERVICE);
                        android.location.Location location =
                                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            lat = location.getLatitude();
                            lon = location.getLongitude();

                            fetchedPreciseLocation = true;
                        }
                    }
                }

                if (!fetchedPreciseLocation) {
                    if (weatherPreferences.contains(LATITUDE_KEY) &&
                            weatherPreferences.contains(LONGITUDE_KEY)) {
                        lat = Double.longBitsToDouble(
                                weatherPreferences.getLong(LATITUDE_KEY, Long.MAX_VALUE));
                        lon = Double.longBitsToDouble(
                                weatherPreferences.getLong(LONGITUDE_KEY, Long.MAX_VALUE));
                    } else {
                        loadApproximatedLocation(Utils.getPublicIPAddress());
                    }
                }

                if (lat < -90 || lat > 90 ||
                        lon < -180 || lon > 180) {
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

                    if (!entries.isEmpty()) {
                        this.weatherData = entries;
                    }

                    int index = getFirstIndexInTime();

                    if (index > 0) {
                        UiUtils.post(() -> preview.update(this.weatherData.get(index)));
                    }

                    String addressName = weatherPreferences.getString(LOCATION_NAME, null);
                    if (addressName == null) {
                        address = geocoder.getFromLocation(lat, lon);
                    } else {
                        address = new Address(Locale.ENGLISH);
                        address.setLocality(addressName);
                    }

                    // Artificially change the update interval if we want precise location data
                    // But location is not accessible
                    if(!fetchedPreciseLocation && prefersPreciseLocation) {
                        lastUpdate = System.currentTimeMillis() - DEFAULT_UPDATE_INTERVAL + FALLBACK_UPDATE_INTERVAL;
                    } else {
                        lastUpdate = System.currentTimeMillis();
                    }
                } catch (JSONException exception) {
                    Log.e(TAG, "updateWeather: ", exception);
                }
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
                String subLocality = address.getSubLocality();
                String locality = address.getLocality();

                if (locality == null) {
                    locality = address.getSubAdminArea();
                }

                if (locality == null) {
                    locality = address.getAdminArea();
                }

                if (locality == null) {
                    locality = subLocality;
                } else if (subLocality != null) {
                    locality = subLocality + ", " + locality;
                }

                location.setText(locality);
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

        HashMap<Integer, Integer> dataForCode = WEATHER_RESOURCES.get(iconCode);

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
        HashMap<Integer, Integer> dataForCode = WEATHER_RESOURCES.get(iconCode);

        Integer summary;
        if (dataForCode == null) {
            summary = R.string.unavailable;
        } else {
            summary = dataForCode.getOrDefault(SUMMARY, R.string.unavailable);
        }

        return summary != null ? summary : 0;
    }

    private void loadApproximatedLocation(String ip) {
        for (IpApiEntry entry : LOCATION_APIS) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection)
                        (new URL(entry.api.replace(LOCATION_API_IP_WILDCARD, ip)).openConnection());

                connection.setReadTimeout(REQUEST_TIMEOUT);
                connection.setConnectTimeout(REQUEST_TIMEOUT);
                connection.setRequestMethod("GET");
                connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
                connection.addRequestProperty("Content-type", "application/json");

                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject jsonObject = new JSONObject(Utils.readStream(connection.getInputStream()));
                    for (IpApiEntry.Callback callback : entry.callback) {
                        callback.assign(jsonObject.getDouble(callback.field));
                    }
                } else {
                    Log.w(TAG, "getWeatherInfo: Server returned non-OK status: " + responseCode);
                }
            } catch (Exception exception) {
                Log.e(TAG, "updateLocation: " + exception.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private JSONObject getWeatherInfo() {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    (new URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" +
                            lat + "&lon=" + lon).openConnection());

            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(activity));
            connection.setRequestProperty("Content-type", "application/json");

            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new JSONObject(Utils.readStream(connection.getInputStream()));
            } else {
                Log.w(TAG, "getWeatherInfo: Server returned non-OK status: " + responseCode);
            }

            return null;
        } catch (Exception exception) {
            Log.e(TAG, "getWeatherInfo: ", exception);

            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class IpApiEntry {
        private final String api;
        private final Callback[] callback;

        private IpApiEntry(@NonNull String api, @NonNull Callback[] callback) {
            this.api = api;
            this.callback = callback;
        }

        abstract static class Callback {
            private final String field;

            Callback(String field) {
                this.field = field;
            }

            abstract void assign(double value);
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
