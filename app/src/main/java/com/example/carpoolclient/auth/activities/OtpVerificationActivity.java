package com.example.carpoolclient.auth.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.R;
import com.example.carpoolclient.auth.services.AuthService;
import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.example.carpoolclient.tripManagement.MainMapActivity;
import com.example.carpoolclient.utils.LoadingDialog;

public class OtpVerificationActivity extends AppCompatActivity {
    private static final int OTP_LENGTH = 3;

    private AuthService authService;
    private LoadingDialog loadingDialog;
    private boolean isRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

        isRefresh = getIntent().getBooleanExtra("REFRESH_TOKEN", false);
        authService = new AuthService(this);
        loadingDialog = new LoadingDialog(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom);
            return insets;
        });

        EditText etOtp = findViewById(R.id.et_otp);
        Button btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        TextView tvMessage = findViewById(R.id.tv_otp_message);
        ScrollView scrollView = findViewById(R.id.scroll_view);

        etOtp.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, btnVerifyOtp.getBottom() + 100), 100);
            }
        });

        String email = getIntent().getStringExtra("EMAIL");
        if (email == null) {
            email = "";
        }
        if (!email.isEmpty()) {
            tvMessage.setText(getString(R.string.otp_sent_message, email));
        }

        final String finalEmail = email;
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtpClick(etOtp, btnVerifyOtp, finalEmail));
    }

    private void handleVerifyOtpClick(EditText etOtp, Button btnVerifyOtp, String email) {
        String otp = etOtp.getText().toString().trim();
        
        if (otp.length() == OTP_LENGTH) {
            verifyOtp(otp, email, btnVerifyOtp);
        } else {
            etOtp.setError("Please enter the 3-digit code");
        }
    }

    private void verifyOtp(String otp, String email, Button btnVerifyOtp) {
        btnVerifyOtp.setEnabled(false);
        loadingDialog.show();
        
        authService.authenticateUser(otp, email, (success, message) -> 
            runOnUiThread(() -> handleAuthenticationResponse(success, message, email, btnVerifyOtp))
        );
    }

    private void handleAuthenticationResponse(boolean success, String message, String email, Button btnVerifyOtp) {
        btnVerifyOtp.setEnabled(true);
        loadingDialog.dismiss();
        
        if (success) {
            showSuccessMessage();

            if (isRefresh) {
                handleTokenRefresh(email);
            } else {
                handleFirstInstallOrNewDevice(email);
            }
        } else {
            showErrorMessage(message);
        }
    }

    private void showSuccessMessage() {
        Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, "Verification failed: " + message, Toast.LENGTH_LONG).show();
    }

    private void handleTokenRefresh(String email) {
        authService.refreshToken(email, (result, token) -> {
            runOnUiThread(() -> {
                if (!result) {
                    showErrorMessage(token);
                    return;
                }

                SecureTokenStore.getInstance(this).saveJwtToken(token);
                authService.sendMessagingToken((syncSuccess, syncMessage) -> runOnUiThread(this::navigateToMainMap));
            });
        });
    }

    private void handleFirstInstallOrNewDevice(String email) {
        authService.refreshToken(email, (result, tokenOrMessage) -> runOnUiThread(() -> {
            if (result) {
                SecureTokenStore.getInstance(this).saveJwtToken(tokenOrMessage);
                authService.sendMessagingToken((syncSuccess, syncMessage) -> runOnUiThread(this::navigateToMainMap));
                return;
            }

            navigateToRegister(email);
        }));
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister(String email) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("EMAIL", email);
        startActivity(intent);
        finish();
    }
}
