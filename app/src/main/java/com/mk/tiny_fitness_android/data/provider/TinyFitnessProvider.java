package com.mk.tiny_fitness_android.data.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.mk.tiny_fitness_android.data.entity.Training;
import com.mk.tiny_fitness_android.data.service.RetrofitService;
import com.mk.tiny_fitness_android.data.util.Helper;
import com.mk.tiny_fitness_android.data.util.SharedPreferencesHelper;
import com.mk.tiny_fitness_android.ui.LoginActivity;
import com.mk.tiny_fitness_android.ui.MainActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TinyFitnessProvider {

    private static TinyFitnessProvider ourInstance = new TinyFitnessProvider();
    public static TinyFitnessProvider getInstance() {
        return ourInstance;
    }

    private final String HTTPS_TINY_FITNESS_URL = "https://tiny-fitness.ru/api/";
    private final String HTTP_TINY_FITNESS_URL = "http://tiny-fitness.ru/api/";

    final String TAG = "myLogs";

    public void authorize(Context context) {
        SharedPreferencesHelper prefs = SharedPreferencesHelper.getInstance(context);
        String apiKey = prefs.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "API Key missing. Redirecting to login.");
            redirectToLogin(context);
            return;
        }

        RetrofitService api = getRetrofitService();

        // Call refresh-api-key
        api.refreshApiKey(apiKey).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Extract new API key from response body
                        String newApiKey = response.body().string().trim();
                        Log.d(TAG, "API Key refreshed successfully: " + newApiKey);

                        // Save updated API key
                        prefs.saveApiKey(newApiKey);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading API key from response: " + e.getMessage());
                        redirectToLogin(context);
                    }
                } else {
                    int statusCode = response.code();
                    Log.e(TAG, "API Key refresh failed with status: " + statusCode);

                    // Handle authentication failures
                    if (statusCode == 401 || statusCode == 403 || statusCode == 404) {
                        redirectToLogin(context);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API Request Failed: " + t.getMessage());
                redirectToLogin(context);
            }
        });
    }

    private void redirectToLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private RetrofitService getRetrofitService() {
        RetrofitService api;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTPS_TINY_FITNESS_URL);
        } else {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTP_TINY_FITNESS_URL);
        }
        return api;
    }

    public void uploadTraining(Training training) {
        Log.d(TAG, "uploadTraining start: " + Build.VERSION.SDK_INT);

        RetrofitService api = getRetrofitService();
        String date = Helper.getStringDateTimeForApi(training.getDateTime());
        Log.d(TAG, date);

        api.saveTraining(training.getInternalCode(), date, training.getDistance(), training.getDuration(), training.getType())
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d(TAG, "onResponse " + response.isSuccessful());

                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                // Parse the response as a string
                                String responseBody = response.body().string();
                                Log.d(TAG, "Response Body: " + responseBody);

                                // Convert the response string to a JSONObject
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                JSONObject result = jsonResponse.getJSONObject("result"); // Access the "result" object
                                int code = result.getInt("code"); // Get the "code" value

                                if (code == 1) {
                                    Log.d(TAG, "Training was successfully saved.");
                                    MainActivity.tinyFitnessHandler.sendMessage(getTinyFitnessMessage(training.getDuration(), training.getDistance()));
                                } else if (code == 0) {
                                    Log.d(TAG, "Training already exists in the database.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                            }
                        } else {
                            Log.d(TAG, "Response unsuccessful or empty body.");
                        }
                    }


                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure");
            }
        });
    }

    private Message getTinyFitnessMessage(int duration, double distance) {
        Bundle bundle = new Bundle();
        bundle.putInt("duration", duration);
        bundle.putDouble("distance", distance);

        Message message = new Message();
        message.setData(bundle);

        return message;
    }

    public void uploadLastTraining(List<Training> trainings) {
        if (trainings == null || trainings.isEmpty()) {
            Log.d(TAG, "No trainings to save.");
            return;
        }

        Collections.sort(trainings, new Comparator<Training>() {
            @Override
            public int compare(Training t1, Training t2) {
                return t2.getDateTime().compareTo(t1.getDateTime());
            }
        });

        Training lastTraining = trainings.get(0);
        Log.d(TAG, "Most recent training found: " + lastTraining);

        uploadTraining(lastTraining);
    }

}
