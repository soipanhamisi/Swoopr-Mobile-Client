package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.NetworkUtils;
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
        
        // Check for internet connectivity first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please check your Wi-Fi or Mobile Data.", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_landing);
            showLandingPage();
            return;
        }

        tokenStore = SecureTokenStore.getInstance(this);
        webClient = new WebClient(this);
        GlobalContext globalContext = (GlobalContext) getApplication();

        LoadingDialog loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        // 1. Secure token store checking before landing page is rendered
        if (tokenStore.getFcmToken() == null) {
            setUpFireBaseMessagingTokens();
        }

        String jwt = tokenStore.getJwtToken();
        if (jwt != null) {
            // Check for validity by hitting the auth/testEndpoint
            webClient.post("/auth/testEndpoint", null, Void.class, (success, message, data) -> {
                if (success) {
                    globalContext.setRegistered(true);
                    Intent intent = new Intent(this, MainMapActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // Token invalid and refresh failed (handled by WebClient's auto-refresh)
                    globalContext.setRegistered(false);
                    loadingDialog.dismiss();
                    showLandingPage();
                }
            });
        } else {
            globalContext.setRegistered(false);
            loadingDialog.dismiss();
            showLandingPage();
        }
    }

    private void showLandingPage() {
        setContentView(R.layout.activity_landing);
        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            ((GlobalContext) getApplication()).setRegistered(false);
            goToEmailVerification();
        });
        findViewById(R.id.btn_verifyEmail).setOnClickListener(v -> {
            ((GlobalContext) getApplication()).setRegistered(true);
            goToEmailVerification();
        });
    }

    public void goToEmailVerification() {
        Intent intent = new Intent(this, EmailVerificationActivity.class);
        startActivity(intent);
    }

    private void setUpFireBaseMessagingTokens() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("landing_activity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    tokenStore.saveFcmToken(token);
                    
                    webClient.post("/auth/submitMessagingToken",
                            token,
                            Void.class,
                            (success, message, data) -> {
                                if (success) {
                                    Log.i("landing_activity", "FCM token submitted successfully");
                                } else {
                                    Log.e("landing_activity", "FCM token submission failed: " + message);
                                }
                            });
                });
    }

}



