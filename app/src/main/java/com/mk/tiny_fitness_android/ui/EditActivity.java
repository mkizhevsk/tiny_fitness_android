package com.mk.tiny_fitness_android.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mk.tiny_fitness_android.R;
import com.mk.tiny_fitness_android.data.util.SharedPreferencesHelper;


public class EditActivity extends AppCompatActivity {

    private EditText cityEditText;
    private Button saveButton;
    private SharedPreferencesHelper sharedPreferencesHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        cityEditText = findViewById(R.id.etCity);
        saveButton = findViewById(R.id.btnSave);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance(this);

        String currentCity = sharedPreferencesHelper.getCity();
        cityEditText.setText(currentCity);

        saveButton.setOnClickListener(v -> {
            String newCity = cityEditText.getText().toString().trim();
            if (!newCity.isEmpty()) {
                sharedPreferencesHelper.saveCity(newCity);
                Toast.makeText(EditActivity.this, "City saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditActivity.this, "City cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }
}