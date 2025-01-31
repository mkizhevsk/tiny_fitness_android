package com.mk.tiny_fitness_android.data.provider;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mk.tiny_fitness_android.data.dto.weather.Weather;
import com.mk.tiny_fitness_android.data.service.RetrofitService;
import com.mk.tiny_fitness_android.data.util.Helper;
import com.mk.tiny_fitness_android.data.util.SharedPreferencesHelper;
import com.mk.tiny_fitness_android.ui.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherProvider {

    private static WeatherProvider instance;
    private final SharedPreferencesHelper prefs;

    public static synchronized WeatherProvider getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherProvider(context.getApplicationContext());
        }
        return instance;
    }

    private final String OPEN_WEATHER_API_URL = "https://api.openweathermap.org";
    private final String OPEN_WEATHER_APP_ID = "6e71959cff1c0c71a6049226d45c69a1";
    private final String OPEN_WEATHER_UNITS = "metric";

    final String TAG = "myLogs";

    public WeatherProvider(Context context) {
        this.prefs = SharedPreferencesHelper.getInstance(context);
    }

    public void getTemperature() {
        Log.d(TAG, "temperature start");

        String city = prefs.getCity();
        Log.d(TAG, "Fetching weather for: " + city);

        RetrofitService api = Helper.getRetrofitApiWithUrl(OPEN_WEATHER_API_URL);

        api.loadPojoCityWeather(OPEN_WEATHER_APP_ID, OPEN_WEATHER_UNITS, city)
                .enqueue(new Callback<Weather>() {

            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Error fetching weather: " + response.code() + " " + response.message());
                    MainActivity.weatherHandler.sendMessage(getWeatherHandlerMessage(Double.NaN)); // Indicate failure
                    return;
                }

                Weather weather = response.body();
                if (weather.getMain() == null) {
                    Log.e(TAG, "Invalid response structure: missing 'main' field");
                    return;
                }

                double temperature = weather.getMain().getTemp();
                MainActivity.weatherHandler.sendMessage(getWeatherHandlerMessage(temperature));

                Log.d(TAG, " temperature " + weather.getVisibility() + " " + temperature);
            }

            @Override
            public void onFailure(Call<Weather> call, Throwable t) {
                Log.e(TAG, t.toString());
            }
        });
    }

    public void checkNetworkAndFetchWeather(Context context) {
        Handler handler = new Handler();
        Runnable fetchWeatherRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable(context)) {
                    getTemperature();
                } else {
                    handler.postDelayed(this, 5000); // Retry every 5 seconds
                }
            }
        };
        handler.post(fetchWeatherRunnable); // Start the check immediately
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private Message getWeatherHandlerMessage(double temperature) {
        Bundle bundle = new Bundle();
        bundle.putDouble("temperature", temperature);

        Message message = new Message();
        message.setData(bundle);

        return message;
    }
}
