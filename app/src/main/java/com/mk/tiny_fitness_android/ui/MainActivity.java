package com.mk.tiny_fitness_android.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mk.tiny_fitness_android.BuildConfig;
import com.mk.tiny_fitness_android.R;
import com.mk.tiny_fitness_android.data.constant.TrainingType;
import com.mk.tiny_fitness_android.data.entity.Training;
import com.mk.tiny_fitness_android.data.provider.TinyFitnessProvider;
import com.mk.tiny_fitness_android.data.provider.WeatherProvider;
import com.mk.tiny_fitness_android.data.service.AuthorizationService;
import com.mk.tiny_fitness_android.data.service.BaseService;
import com.mk.tiny_fitness_android.data.service.LocationService;
import com.mk.tiny_fitness_android.data.thread.DurationRunnable;
import com.mk.tiny_fitness_android.data.util.Helper;
import com.mk.tiny_fitness_android.data.util.StringRandomGenerator;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Training training;

    public static boolean start;
    private boolean finish;

    private Spinner typeSpinner;
    private TextView durationTextView;
    private TextView distanceTextView;
    private Button startFinishButton;
    private TextView temperatureTextView;
    private TextView accuracyTextView;

    private double temperature;
    private float accuracy = -100;
    private Date startDateTime;

    private final int MINIMUM_DURATION = 1;
    private final int MINIMUM_DISTANCE = 500;

    public static Handler locationHandler;
    public static Handler durationHandler;
    public static Handler weatherHandler;
    public static Handler tinyFitnessHandler;

    private AuthorizationService authorizationService;
    private BaseService baseService;
    private LocationService locationService;

    private int WEATHER_DURATION_COUNTER = 0;
    private final int WEATHER_DURATION_COUNTER_LIMIT = 30;

    final String TAG = "myLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);

        typeSpinner = findViewById(R.id.typeSpinner);
        durationTextView = findViewById(R.id.durationTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        startFinishButton = findViewById(R.id.startFinishButton);
        accuracyTextView = findViewById(R.id.accuracyTextView);

        Log.d(TAG, "onCreate " + Build.VERSION.SDK_INT);

        start = false;
        finish = false;

        if (checkPermissions(this, this)) {
            Log.d(TAG, "permission granted by default");
            startApp();
        }
    }

    public static boolean checkPermissions(Context context, Activity mainActivity) {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };

        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(context, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(mainActivity, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 1) startApp();
    }

    private void startApp() {
        training = new Training(new Date(System.currentTimeMillis()), 0, 0, 1);

        typeSpinner.setSelection(TrainingType.RUNNING_TYPE - 1);
        locationHandler = getLocationHandler();
        durationHandler = getDurationHandler();
        weatherHandler = getWeatherHandler();
        tinyFitnessHandler = getTinyFitnessHandler();

        startAuthorizationService();
    }

    // top right menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "training history");
        menu.add(0, 2, 1, "edit city");
        menu.add(0, 3, 2, "exit");

        String versionName = BuildConfig.VERSION_NAME;
        menu.add(0, 4, 3, "version: " + versionName).setEnabled(false); // Disabled so it's non-clickable

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: // Training history
                String deletedTracksInfo;
                try {
                    deletedTracksInfo = Helper.getTrainingHistory(baseService.getTrainings());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                Log.d(TAG, deletedTracksInfo);
                Intent deletedIntent = new Intent(this, ListActivity.class);
                deletedIntent.putExtra("content", deletedTracksInfo);
                startActivity(deletedIntent);
                break;

            case 2:
                Log.d(TAG, "edit city");
                Intent intent = new Intent(this, EditActivity.class);
                startActivity(intent);
                break;

            case 3: // Exit
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // handlers
    private Handler getLocationHandler() {
        return new Handler(message -> {
            Bundle bundle = message.getData();
            float[] locationInfo = bundle.getFloatArray("locationInfo");

            if(locationInfo != null && locationInfo.length > 1) {
                accuracy = locationInfo[1];

                if(start)
                    training.setDistance(locationInfo[0] / 1000);

                Log.d(TAG, "locationHandler end: training.distance - " + training.getDistance() + " " + locationInfo[0]);
            }

            showCurrentData();

            return true;
        });
    }

    private Handler getDurationHandler() {
        return new Handler(message -> {
            Log.d(TAG, "durationHandler");

            if (startDateTime != null && start && !finish)
                training.setDuration(Helper.getDuration(startDateTime));

            showCurrentData();
            WEATHER_DURATION_COUNTER++;

            return true;
        });
    }

    private Handler getWeatherHandler() {
        return new Handler(message -> {
            Log.d(TAG, "weatherHandler start");

            Bundle bundle = message.getData();
            temperature = bundle.getDouble("temperature");

            temperatureTextView.setText(Helper.getStringTemperature(temperature));
            showCurrentData();

            return true;
        });
    }

    private Handler getTinyFitnessHandler() {
        return new Handler(message -> {
            Log.d(TAG, "tinyFitnessHandler start");

            Bundle bundle = message.getData();
            int duration = bundle.getInt("duration");
            double distance = bundle.getDouble("distance");

            String text = String.format(
                    "training was uploaded: %.1f | %d",
                    Helper.upToOneDecimalPlace(distance),
                    duration
            );
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();

            return true;
        });
    }

    private void showCurrentData() {
        if (WEATHER_DURATION_COUNTER == WEATHER_DURATION_COUNTER_LIMIT) {
            WeatherProvider.getInstance(this).checkNetworkAndFetchWeather(MainActivity.this);
            WEATHER_DURATION_COUNTER = 0;
        }

        durationTextView.setText(Helper.getStringDuration(training.getDuration()));
        String kmUnit = this.getString(R.string.unit_km);
        distanceTextView.setText(Helper.getStringDistance(training.getDistance(), kmUnit));
        if (accuracy != -100)
            accuracyTextView.setText(Helper.getStringAccuracy(accuracy));
    }

    // startFinishButton
    public void onClickStartFinishButton(View view) {
        if(!start && !finish) {  //start training
            Log.d(TAG, "start button");
            startTraining();
        } else if(start && !finish) {  //finish training
            Log.d(TAG, "finish button");
            finishTraining();
        }
    }

    private void startTraining() {
        String finish = this.getString(R.string.finish);
        startFinishButton.setText(finish);
        startFinishButton.setTextColor(Color.WHITE);
        startFinishButton.setBackgroundResource(R.drawable.training_active_background);

        startDateTime = new Date(System.currentTimeMillis());
        training.setDateTime(startDateTime);
        if(locationService != null)
            locationService.clearDistance();

        Thread durationThread = new Thread(new DurationRunnable());
        durationThread.start();

        start = true;
        Log.d(TAG, "startTraining() end: startDateTime " + startDateTime);
    }

    private void finishTraining() {
        startFinishButton.setTextColor(Color.BLACK);
        startFinishButton.setBackgroundResource(R.drawable.training_finished_background);

        training.setType(typeSpinner.getSelectedItemPosition() + 1);
        training.setDuration(Helper.getDuration(startDateTime));

        showCurrentData();
        finish = true;
        LocationService.running = false;
        DurationRunnable.running = false;
        Log.d(TAG, "finishTraining() end: distance " + training.getDistance());
        editDistance();
    }

    private void saveTraining() {
        Log.d(TAG, "MainActivity saveTraining() start: " + training.toString());
        if (training != null) {
            training.setInternalCode(StringRandomGenerator.getInstance().getValue());
            training.setId((int) baseService.insertTraining(training));

            TinyFitnessProvider.getInstance().uploadTraining(training);
        }
    }

    public void editDistance() {
        LayoutInflater di = LayoutInflater.from(this);
        View pathView = di.inflate(R.layout.distance, null);
        AlertDialog.Builder newPathDialogBuilder = new AlertDialog.Builder(this);
        newPathDialogBuilder.setView(pathView);
        final EditText distanceInput = pathView.findViewById(R.id.input_distance);
        distanceInput.setText(Helper.getStringFromDouble(training.getDistance()));
        newPathDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, id) -> {
                            Log.d(TAG, "from input: " + training.getDistance());

                            training.setDistance(Helper.getDoubleFromInput(distanceInput.getText()));
                            saveTraining();

                            distanceTextView.setText(String.valueOf(training.getDistance()));
                            startFinishButton.setText("...");
                        })
                .setNegativeButton("Отмена",
                        (dialog, id) -> dialog.cancel());
        AlertDialog createDialog = newPathDialogBuilder.create();
        createDialog.show();
    }

    // AuthorizationService
    private void startAuthorizationService() {
        Log.d(TAG, "MainActivity startAuthorizationService()");
        Intent intent = new Intent(this, AuthorizationService.class);
        bindService(intent, authorizationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection authorizationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AuthorizationService.LocalBinder binder = (AuthorizationService.LocalBinder) service;
            authorizationService = binder.getService();
            Log.d(TAG, "MainActivity authorizationService onServiceConnected");

            startBaseService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            Log.d(TAG, "MainActivity authorizationService onServiceDisconnected");
        }
    };

    // BaseService
    private void startBaseService() {
        Log.d(TAG, "MainActivity startBaseService()");
        Intent intent = new Intent(this, BaseService.class);
        bindService(intent, baseServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection baseServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BaseService.LocalBinder binder = (BaseService.LocalBinder) service;
            baseService = binder.getService();
            Log.d(TAG, "MainActivity baseService onServiceConnected");

            List<Training> trainings;
            try {
                trainings = baseService.getTrainings();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Log.d(TAG, "trainings " + trainings.size());

            startLocationService();
            WeatherProvider.getInstance(MainActivity.this).checkNetworkAndFetchWeather(MainActivity.this);
            TinyFitnessProvider.getInstance().uploadLastTraining(trainings);
//            for(Training training : trainings) {
//                Log.d(TAG, training.toString());
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            Log.d(TAG, "MainActivity baseService onServiceDisconnected");
        }
    };

    // LocationService
    private void startLocationService() {
        Log.d(TAG, "MainActivity startLocationService()");
        Intent intent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private ServiceConnection locationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            Log.d(TAG, "MainActivity locationService onServiceConnected");

            LocationService.running = true;
            locationService.getLocationUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "MainActivity locationService onServiceDisconnected");
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Start onDestroy");

        if (training != null && training.getDuration() > MINIMUM_DURATION && training.getDistance() > MINIMUM_DISTANCE)
            saveTraining();

        DurationRunnable.running = false;

        stopService(new Intent(this, BaseService.class));
        if (baseServiceConnection != null)
            unbindService(baseServiceConnection);

        LocationService.running = false;
        stopService(new Intent(this, LocationService.class));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && locationServiceConnection != null)
            unbindService(locationServiceConnection);

        training = null;

        super.onDestroy();
    }
}
