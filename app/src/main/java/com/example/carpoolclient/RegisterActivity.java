package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.dtos.UserDto;
import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.SecureTokenStore;
import com.example.carpoolclient.utils.WebClient;
import com.google.firebase.messaging.FirebaseMessaging;

public class RegisterActivity extends AppCompatActivity {
    private LoadingDialog loadingDialog;
    private WebClient webClient;
    private SecureTokenStore tokenStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        webClient = new WebClient(this);
        loadingDialog = new LoadingDialog(this);
        tokenStore = SecureTokenStore.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom);
            return insets;
        });

        EditText etFullName = findViewById(R.id.et_full_name);
        Button btnFinish = findViewById(R.id.btn_complete_registration);
        ScrollView scrollView = findViewById(R.id.scroll_view);

        String email = getIntent().getStringExtra("EMAIL");
        if (email == null) {
            email = "";
        }

        etFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, etFullName.getBottom() + 100), 100);
            }
        });

        final String finalEmail = email;
        btnFinish.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            btnFinish.setEnabled(false);
            loadingDialog.show();

            boolean isRegistered = ((GlobalContext) getApplication()).isRegistered();

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                String fcmToken = task.isSuccessful() ? task.getResult() : null;

                if (isRegistered) {
                    // Existing user flow: just save name locally and submit FCM token
                    saveUserDataLocally(fullName);
                    
                    if (fcmToken != null) {
                        webClient.post("/auth/submitMessagingToken", fcmToken, Void.class, (s, m, d) -> {
                            if (s) android.util.Log.i("register_activity", "FCM token submitted successfully");
                            loadingDialog.dismiss();
                            navigateToMainMap();
                        });
                    } else {
                        loadingDialog.dismiss();
                        navigateToMainMap();
                    }
                } else {
                    // New user flow: call /auth/saveUser
                    UserDto userDto = new UserDto(fullName, finalEmail, "NORMAL_USER", fcmToken);

                    webClient.post("/auth/saveUser", userDto, Void.class, (success, message, data) -> {
                        btnFinish.setEnabled(true);
                        loadingDialog.dismiss();

                        if (success) {
                            saveUserDataLocally(fullName);
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            
                            if (fcmToken != null) {
                                webClient.post("/auth/submitMessagingToken", fcmToken, Void.class, (s, m, d) -> {
                                    if (s) android.util.Log.i("register_activity", "FCM token submitted successfully");
                                });
                            }
                            
                            navigateToMainMap();
                        } else {
                            boolean userExists = message != null && (message.toLowerCase().contains("user exists") || message.toLowerCase().contains("already registered"));
                            if (userExists) {
                                saveUserDataLocally(fullName);
                                if (fcmToken != null) {
                                    webClient.post("/auth/submitMessagingToken", fcmToken, Void.class, (s, m, d) -> {
                                        if (s) android.util.Log.i("register_activity", "FCM token submitted successfully (fallback)");
                                    });
                                }
                                
                                Toast.makeText(this, "Account already registered. Logging you in...", Toast.LENGTH_SHORT).show();
                                navigateToMainMap();
                            } else {
                                Toast.makeText(this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });
        });
    }

    private void saveUserDataLocally(String fullName) {
        tokenStore.saveFullName(fullName);
        ((GlobalContext) getApplication()).setFullName(fullName);
        ((GlobalContext) getApplication()).setRegistered(true);
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


