package com.example.carpoolclient.auth.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.R;
import com.example.carpoolclient.auth.services.AuthService;
import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.example.carpoolclient.tripManagement.MainMapActivity;

public class LandingActivity extends AppCompatActivity {
    private SecureTokenStore tokenStore;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        tokenStore = SecureTokenStore.getInstance(this);
        authService = new AuthService(this);

        handleStartupRouting();

        findViewById(R.id.btn_get_started).setOnClickListener(v ->
                startActivity(new Intent(this, EmailVerification.class)));

        findViewById(R.id.btn_verifyEmail).setOnClickListener(v ->
                startActivity(new Intent(this, EmailVerification.class)));
    }

    private void handleStartupRouting() {
        String jwt = tokenStore.getJwtToken();
        if (jwt == null || jwt.trim().isEmpty()) {
            return;
        }

        authService.testEndpoint(jwt, (success, message) -> runOnUiThread(() -> {
            if (success) {
                navigateToMainMap();
                return;
            }

            tokenStore.saveJwtToken(null);
            Intent intent = new Intent(this, EmailVerification.class);
            intent.putExtra("REFRESH_TOKEN", true);
            startActivity(intent);
            finish();
        }));
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}



