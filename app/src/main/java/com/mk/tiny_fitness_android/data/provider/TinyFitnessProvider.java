package com.mk.tiny_fitness_android.data.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonObject;
import com.mk.tiny_fitness_android.data.entity.Training;
import com.mk.tiny_fitness_android.data.service.RetrofitService;
import com.mk.tiny_fitness_android.data.util.Helper;
import com.mk.tiny_fitness_android.data.util.SharedPreferencesHelper;
import com.mk.tiny_fitness_android.ui.MainActivity;
import com.mk.tiny_fitness_android.ui.RequestCodeActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TinyFitnessProvider {

    private static TinyFitnessProvider instance;
    private final Context context;
    private List<Training> trainings;
    private String email;

    private final String HTTPS_TINY_FITNESS_URL = "https://tiny-fitness.ru/api/";
    private final String HTTP_TINY_FITNESS_URL = "http://tiny-fitness.ru/api/";

    final String TAG = "myLogs";

    private TinyFitnessProvider(Context context) {
        this.context = context.getApplicationContext(); // Avoid Activity Context to prevent memory leaks
    }

    public static synchronized TinyFitnessProvider getInstance(Context context) {
        if (instance == null) {
            instance = new TinyFitnessProvider(context);
        }
        return instance;
    }

    public void authorize(List<Training> trainings) {
        this.trainings = trainings;

        SharedPreferencesHelper prefs = SharedPreferencesHelper.getInstance(context);
        Log.d(TAG, "Stored API Key: " + prefs.getApiKey());
        String apiKey = prefs.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "API Key missing. Redirecting to login.");
            redirectToRequestCode(context);
            return;
        }

        RetrofitService api = getApiBySdk();

        api.refreshApiKey(apiKey).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String newApiKey = jsonResponse.getString("refreshedKey");

                        Log.d(TAG, "API Key refreshed successfully: " + newApiKey);

                        prefs.saveApiKey(newApiKey);
                        uploadLastTraining();
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error reading API key from response: " + e.getMessage());
                        redirectToRequestCode(context);
                    }
                } else {
                    Log.e(TAG, "API key refresh failed. Response code: " + response.code());
                    redirectToRequestCode(context);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API Request Failed: " + t.getMessage());
                redirectToRequestCode(context);
            }
        });
    }

    private void redirectToRequestCode(Context context) {
        Intent intent = new Intent(context, RequestCodeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void requestCode(String email, RequestCallback<String> callback) {

        this.email = email;
        RetrofitService api = getApiWithAuthBySdk();

        Call<ResponseBody> call = api.requestCode(email);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Verification code sent");
                } else {
                    callback.onFailure(new Exception("Failed to send verification code"));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(new Exception("Network error: " + t.getMessage()));
            }
        });
    }

    public void verifyCode(String code, RequestCallback<String> callback) {

        SharedPreferencesHelper prefs = SharedPreferencesHelper.getInstance(context);
        String deviceId = prefs.getDeviceId();
        RetrofitService api = getApiWithAuthBySdk();

        Call<ResponseBody> call = api.verifyCode(this.email, code, deviceId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String apiKey = getJsonObject(response).getString("apiKey");
                        prefs.saveApiKey(apiKey);
                        Log.d(TAG, "saved apiKey: " + apiKey);

                        callback.onSuccess(apiKey);
                    } catch (IOException | JSONException e) {
                        callback.onFailure(new Exception("Error reading response"));
                    }
                } else {
                    callback.onFailure(new Exception("Verification failed"));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(new Exception("Network error: " + t.getMessage()));
            }
        });
    }

    public interface RequestCallback<T> {
        void onSuccess(T response);
        void onFailure(Throwable t);
    }

    private JSONObject getJsonObject(Response<ResponseBody> response) throws IOException, JSONException {
        String responseBody = response.body().string();
        return new JSONObject(responseBody);
    }

    private RetrofitService getApiBySdk() {
        RetrofitService api;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            api = Helper.getRetrofitApiWithUrl(HTTPS_TINY_FITNESS_URL);
        } else {
            api = Helper.getRetrofitApiWithUrl(HTTP_TINY_FITNESS_URL);
        }
        return api;
    }

    private RetrofitService getApiWithAuthBySdk() {
        RetrofitService api;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTPS_TINY_FITNESS_URL);
        } else {
            api = Helper.getRetrofitApiWithUrlAndAuth(HTTP_TINY_FITNESS_URL);
        }
        return api;
    }

    public void uploadTrainingPost(Training training) {
        Log.d(TAG, "uploadTrainingPost start: " + Build.VERSION.SDK_INT);

        RetrofitService api = getApiBySdk();

        SharedPreferencesHelper prefs = SharedPreferencesHelper.getInstance(context);
        String apiKey = prefs.getApiKey();

        if (apiKey == null) {
            Log.e(TAG, "API Key is missing from SharedPreferences");
            return;
        }

        String date = Helper.getStringDateTimeForApi(training.getDateTime());
        Log.d(TAG, "Formatted date: " + date);

        // Prepare request body
        JsonObject trainingData = new JsonObject();
        trainingData.addProperty("internalCode", training.getInternalCode());
        trainingData.addProperty("date", date);
        trainingData.addProperty("distance", training.getDistance());
        trainingData.addProperty("duration", training.getDuration());
        trainingData.addProperty("type", training.getType());

        api.addTraining(apiKey, trainingData)
                .enqueue(new Callback<ResponseBody>() {

                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d(TAG, "onResponse: " + response.isSuccessful());

                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                int code = getJsonObject(response).getInt("code");
                                if (code == 1) {
                                    Log.d(TAG, "Training successfully saved.");
                                    MainActivity.tinyFitnessHandler.sendMessage(
                                            getTinyFitnessMessage(training.getDuration(), training.getDistance())
                                    );
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
                        Log.e(TAG, "onFailure: " + t.getMessage());
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

    public void uploadLastTraining() {
        if (this.trainings == null || this.trainings.isEmpty()) {
            Log.d(TAG, "No trainings to save.");
            return;
        }

        Collections.sort(trainings, (t1, t2) -> t2.getDateTime().compareTo(t1.getDateTime()));

        Training lastTraining = trainings.get(0);
        Log.d(TAG, "Most recent training found: " + lastTraining);

        uploadTrainingPost(lastTraining);
    }

}