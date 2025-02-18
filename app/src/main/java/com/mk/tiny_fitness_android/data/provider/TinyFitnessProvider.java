package com.mk.tiny_fitness_android.data.provider;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.mk.tiny_fitness_android.data.entity.Training;
import com.mk.tiny_fitness_android.data.service.RetrofitService;
import com.mk.tiny_fitness_android.data.util.Helper;
import com.mk.tiny_fitness_android.ui.MainActivity;

import org.json.JSONObject;

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



    public void uploadTraining(Training training) {
        Log.d(TAG, "uploadTraining start: " + Build.VERSION.SDK_INT);

        RetrofitService api;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTPS_TINY_FITNESS_URL);
        } else {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTP_TINY_FITNESS_URL);
        }

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
