package com.mk.tiny_fitness_android.data.util;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.util.Base64;

import com.mk.tiny_fitness_android.R;
import com.mk.tiny_fitness_android.data.entity.Training;
import com.mk.tiny_fitness_android.data.service.RetrofitService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Helper {

    private static final SimpleDateFormat dateTimeFormatForApi = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat dateTimeFormatForDb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static final String API_LOGIN = "tiny_fitness_app";
    private static final String API_PASSWORD = "wqlZTpmQcMwqJOnl02QG";

    public static String getStringFromDouble(double value) {
        return String.valueOf(upToOneDecimalPlace(value));
    }

    public static double getDoubleFromInput(Editable inputValue) {
        double doubleValue = Double.parseDouble(inputValue.toString());
        return upToOneDecimalPlace(doubleValue);
    }

    public static double upToOneDecimalPlace(double value) {
        long longValue = Math.round(value * 10);
        return ((double) longValue) / 10;
    }

    public static int getDuration(Date startDateTime) {
        return (int) getDateDiff(startDateTime, new Date(System.currentTimeMillis()), TimeUnit.MINUTES);
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    public static String getStringDateTimeForApi(Date date) {
        return dateTimeFormatForApi.format(date);
    }

    public static String getStringDateTimeForDb(Date date) {
        return dateTimeFormatForDb.format(date);
    }

    public static Date getDateTimeFromString(String date) throws ParseException {
        return dateTimeFormatForDb.parse(date);
    }

    public static String getTrainingHistory(List<Training> trainings) {
        String deletedTracksInfo = "";
        for(Training training : trainings) {
            StringBuilder sb = new StringBuilder();
            if(deletedTracksInfo.isEmpty()) {
                deletedTracksInfo = training.toString();
            } else {
                deletedTracksInfo = sb.append(deletedTracksInfo).append(getNewLine()).append(getNewLine()).append(training.toString()).toString();
            }
        }
        return deletedTracksInfo;
    }

    private static String getNewLine() {
        return System.getProperty("line.separator");
    }

    public static String getStringTemperature(Context context, double temperature) {
        return upToOneDecimalPlace(temperature) + " " + context.getString(R.string.temperature_suffix);
    }

    public static String getStringDuration(Context context, int duration) {
        return duration + " " + context.getString(R.string.duration_suffix);
    }

    public static String getStringDistance(double distance, String kmUnit) {
        return upToOneDecimalPlace(distance) + " " + kmUnit;
    }

    public static String getStringAccuracy(Context context, float accuracy) {
        return Math.round(accuracy) + " " + context.getString(R.string.accuracy_suffix);
    }

    public static float getSpeedInKmHour(float speedInMeterSecond) {
        return (speedInMeterSecond * 3600) / 1000;
    }

    public static RetrofitService getRetrofitApiWithUrl(String url) {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(RetrofitService.class);
    }

    public static RetrofitService getRetrofitApiWithUrlAndAuth(String url) {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator((route, response) -> {
                    Request request = response.request();
                    if (request.header("Authorization") != null) {
                        // Login and password are incorrect
                        return null;
                    }

                    // Custom Base64 encoding for the Authorization header
                    String credentials = API_LOGIN + ":" + API_PASSWORD;
                    String authHeader = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                    return request.newBuilder()
                            .header("Authorization", authHeader)
                            .build();
                })
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(RetrofitService.class);
    }

    public static String generateDeviceId() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String randomId = StringRandomGenerator.getInstance().getShortValue();

        return manufacturer + "_" + model + "_" + randomId;
    }
}
