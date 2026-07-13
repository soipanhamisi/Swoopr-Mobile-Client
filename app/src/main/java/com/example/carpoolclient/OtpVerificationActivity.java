package com.example.carpoolclient;

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

import com.example.carpoolclient.dtos.AuthenticateRequest;
import com.example.carpoolclient.dtos.EmailDto;
import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.WebClient;

public class OtpVerificationActivity extends AppCompatActivity {
    private static final int OTP_LENGTH = 3;

    private WebClient webClient;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

        webClient = new WebClient(this);
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
        TextView tvResendOtp = findViewById(R.id.tv_resend_otp);
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
        tvResendOtp.setOnClickListener(v -> handleResendOtp(finalEmail));
    }

    private void handleResendOtp(String email) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(email);

        webClient.post("/auth/getOtp", emailDto, Void.class, (success, message, data) -> {
            loadingDialog.dismiss();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void handleVerifyOtpClick(EditText etOtp, Button btnVerifyOtp, String email) {
        String otpStr = etOtp.getText().toString().trim();
        
        if (otpStr.length() == OTP_LENGTH) {
            try {
                int otp = Integer.parseInt(otpStr);
                verifyOtp(otp, email, btnVerifyOtp);
            } catch (NumberFormatException e) {
                etOtp.setError("Invalid OTP format");
            }
        } else {
            etOtp.setError("Please enter the 3-digit code");
        }
    }

    private void verifyOtp(int otp, String email, Button btnVerifyOtp) {
        btnVerifyOtp.setEnabled(false);
        loadingDialog.show();

        AuthenticateRequest request = new AuthenticateRequest(otp, email);
        webClient.post("/auth/getNewToken", request, Void.class, (success, message, data) -> {
            btnVerifyOtp.setEnabled(true);
            loadingDialog.dismiss();

            if (success) {
                showSuccessMessage();
                checkUserStatusAndNavigate(email);
            } else {
                showErrorMessage(message);
            }
        });
    }

    private void showSuccessMessage() {
        Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, "Verification failed: " + message, Toast.LENGTH_LONG).show();
    }

    private void checkUserStatusAndNavigate(String email) {
        // Try to refresh token as a probe to check if user profile exists
        webClient.post("/auth/refreshToken", Void.class, (success, message, data) -> {
            if (success) {
                syncMessagingTokenAndNavigate();
            } else {
                // If refresh fails, they likely need to register (first time user)
                navigateToRegister(email);
            }
        });
    }

    private void syncMessagingTokenAndNavigate() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        // Submit token to server then navigate
                        webClient.post("/auth/submitMessagingToken", token, Void.class, (s, m, d) -> navigateToMainMap());
                    } else {
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

    private void navigateToRegister(String email) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("EMAIL", email);
        startActivity(intent);
        finish();
    }
}
