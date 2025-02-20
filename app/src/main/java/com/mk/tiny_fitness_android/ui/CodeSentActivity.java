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
import com.mk.tiny_fitness_android.data.util.SharedPreferencesHelper;

public class CodeSentActivity extends Activity {

    private static final String TAG = "CodeSentActivity";

    private EditText codeText;
    private Button verifyCodeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_sent);

        codeText = findViewById(R.id.editTextCode);
        verifyCodeButton = findViewById(R.id.buttonVerify);

        verifyCodeButton.setOnClickListener(v -> verifyCode());
    }

    private void verifyCode() {
        String code = codeText.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "code: " + code);

        TinyFitnessProvider.getInstance().requestCode(code, new TinyFitnessProvider.RequestCallback<String>() {
            @Override
            public void onSuccess(String apiKey) {
                Log.d(TAG, "Login successful, API Key: " + apiKey);

                // Save API key
                SharedPreferencesHelper.getInstance(CodeSentActivity.this).saveApiKey(apiKey);

                // Navigate to MainActivity
                Intent intent = new Intent(CodeSentActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(CodeSentActivity.this, "Login failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
