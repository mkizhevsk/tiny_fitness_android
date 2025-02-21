package com.mk.tiny_fitness_android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mk.tiny_fitness_android.R;
import com.mk.tiny_fitness_android.data.provider.TinyFitnessProvider;

public class RequestCodeActivity extends Activity {
    private static final String TAG = "RequestCodeActivity";

    private EditText emailEditText;
    private Button requestCodeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_code);

        emailEditText = findViewById(R.id.editTextEmail);
        requestCodeButton = findViewById(R.id.buttonRequestCode);

        requestCodeButton.setOnClickListener(v -> requestCode());
    }

    private void requestCode() {
        String email = emailEditText.getText().toString().trim();
        Log.d(TAG, "email: " + email);

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        TinyFitnessProvider.getInstance(this).requestCode(email, new TinyFitnessProvider.RequestCallback<String>() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "requestCode() response: " + response);

                Intent intent = new Intent(RequestCodeActivity.this, CodeSentActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(RequestCodeActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
