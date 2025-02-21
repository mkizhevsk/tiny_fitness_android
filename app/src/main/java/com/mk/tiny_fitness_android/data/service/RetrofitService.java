package com.mk.tiny_fitness_android.data.service;

import com.google.gson.JsonObject;
import com.mk.tiny_fitness_android.data.dto.weather.Weather;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RetrofitService {

    @GET("/data/2.5/weather")
    Call<ResponseBody> loadCityWeather(
            @Query("APPID") String appId,
            @Query("units") String units,
            @Query("q") String city
    );

    @GET("/data/2.5/weather")
    Call<Weather> loadPojoCityWeather(
            @Query("APPID") String appId,
            @Query("units") String units,
            @Query("q") String city
    );

    @POST("addTraining")
    @Headers("Content-Type: application/json")
    Call<ResponseBody> addTraining(
            @Header("x-api-key") String apiKey,
            @Body JsonObject trainingData
    );

    @GET("addTraining")
    Call<ResponseBody> saveTraining(
            @Query("internalCode") String internalCode,
            @Query("date") String date,
            @Query("distance") double distance,
            @Query("duration") int duration,
            @Query("type") int type
    );

    @POST("refresh-api-key")
    Call<ResponseBody> refreshApiKey(@Query("apiKey") String apiKey);

    @POST("request-code")
    Call<ResponseBody> requestCode(@Query("username") String email);

    @POST("verify-code")
    Call<ResponseBody> verifyCode(
            @Query("username") String username,
            @Query("code") String code,
            @Query("deviceId") String deviceId
    );
}
