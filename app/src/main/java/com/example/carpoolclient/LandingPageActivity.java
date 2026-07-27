package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.NetworkUtils;
import com.example.carpoolclient.utils.SecureTokenStore;
import com.example.carpoolclient.utils.WebClient;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Properties;

public class LandingPageActivity extends AppCompatActivity {
    private SecureTokenStore tokenStore;
    private WebClient webClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        android.util.Log.e("landing_activity", "!!! LANDING ACTIVITY CREATED !!!");

        tokenStore = SecureTokenStore.getInstance(this);
        webClient = new WebClient(this);
        GlobalContext globalContext = (GlobalContext) getApplication();

        // DEV: Check for bearer token in dev.env to bypass verification
        String devToken = checkForDevToken();
        if (devToken != null && !devToken.isEmpty()) {
            android.util.Log.i("landing_activity", "DEV: Using bearer token from dev.env");
            tokenStore.saveJwtToken(devToken);
            globalContext.setRegistered(true);
            navigateToMainMap();
            return;
        }

        // Check for internet connectivity first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please check your Wi-Fi or Mobile Data.", Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_landing);
            showLandingPage();
            return;
        }

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

                    // Submit FCM token after acquiring Bearer token (via auto-login success)
                    String fcmToken = tokenStore.getFcmToken();
                    if (fcmToken != null) {
                        webClient.post("/auth/submitMessagingToken", fcmToken, Void.class, (s, m, d) -> {
                            if (s) Log.i("landing_activity", "FCM token submitted successfully");
                        });
                    }

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
        View root = findViewById(R.id.main_landing_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            ((GlobalContext) getApplication()).setRegistered(false);
            goToEmailVerification();
        });
        findViewById(R.id.btn_verifyEmail).setOnClickListener(v -> {
            ((GlobalContext) getApplication()).setRegistered(true);
            goToEmailVerification();
        });
    }

    private String checkForDevToken() {
        try {
            Properties properties = new Properties();
            properties.load(getAssets().open("dev.env"));
            String token = properties.getProperty("BEARER_TOKEN");
            if (token != null && token.toLowerCase().startsWith("bearer ")) {
                return token.substring(7).trim();
            }
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        startActivity(intent);
        finish();
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
                });
    }

}



