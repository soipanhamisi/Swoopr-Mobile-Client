package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.utils.SecureTokenStore;
import com.example.carpoolclient.utils.WebClient;
import com.google.firebase.messaging.FirebaseMessaging;

public class LandingPageActivity extends AppCompatActivity {
    private SecureTokenStore tokenStore;
    private WebClient webClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e("landing_activity", "!!! LANDING ACTIVITY CREATED !!!");
        setContentView(R.layout.activity_landing);
        tokenStore = SecureTokenStore.getInstance(this);
        webClient = new WebClient(this);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.e("TOKEN_CHECK", "FAILED TO GET TOKEN: " + task.getException());
                        return;
                    }
                    String token = task.getResult();
                    android.util.Log.e("TOKEN_CHECK", "FOUND_FCM_TOKEN: " + token);
                });

        String storedToken = tokenStore.getFcmToken();
        android.util.Log.d("FCM_DEBUG", "FCM Token from Storage: " + (storedToken != null ? storedToken : "None stored"));

        handleStartupRouting();

        findViewById(R.id.btn_get_started).setOnClickListener(v ->
                startActivity(new Intent(this, EmailVerificationActivity.class)));

        findViewById(R.id.btn_verifyEmail).setOnClickListener(v -> {
            Intent intent = new Intent(this, EmailVerificationActivity.class);
            intent.putExtra("REFRESH_TOKEN", true);
            startActivity(intent);
        });
    }

    private void handleStartupRouting() {
        String jwt = tokenStore.getJwtToken();
        if (jwt == null || jwt.trim().isEmpty()) {
            return;
        }
        testJwt(jwt);
    }

    private void testJwt(String jwt) {
        webClient.post("/auth/testEndpoint", String.class, (success, message, data) -> {
            if (success) {
                navigateToMainMap();
            }
        });
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}



