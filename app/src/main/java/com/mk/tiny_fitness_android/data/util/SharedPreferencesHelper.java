package com.mk.tiny_fitness_android.data.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREF_NAME = "app_prefs";
    private static final String KEY_CITY = "city_name";
    private static final String KEY_API_KEY = "api_key";

    private static SharedPreferencesHelper instance;
    private final SharedPreferences sharedPreferences;

    private final String DEFAULT_WEATHER_CITY = "izhevsk";

    private SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesHelper(context);
        }
        return instance;
    }

    public void saveCity(String cityName) {
        sharedPreferences.edit().putString(KEY_CITY, cityName).apply();
    }

    public String getCity() {
        return sharedPreferences.getString(KEY_CITY, DEFAULT_WEATHER_CITY);
    }

    public void saveApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return sharedPreferences.getString(KEY_API_KEY, null);
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
}
