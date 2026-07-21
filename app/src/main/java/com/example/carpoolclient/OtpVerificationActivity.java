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
import com.example.carpoolclient.dtos.TokenResponse;
import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.SecureTokenStore;
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
        String email = getIntent().getStringExtra("EMAIL");
        if (email == null) {
            email = "";
        }
        if (!email.isEmpty()) {
            tvMessage.setText(getString(R.string.otp_sent_message, email));
        }

        final String finalEmail = email;
        tvResendOtp.setOnClickListener(v -> {
            if (!finalEmail.isEmpty()) {
                resendOtp(finalEmail);
            } else {
                Toast.makeText(OtpVerificationActivity.this, "Email not found", Toast.LENGTH_SHORT).show();
            }
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otpStr = etOtp.getText().toString().trim();
            if (otpStr.isEmpty()) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int otp = Integer.parseInt(otpStr);
                verifyOtp(finalEmail, otp);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid OTP format", Toast.LENGTH_SHORT).show();
            }
        });

        etOtp.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, btnVerifyOtp.getBottom() + 100), 100);
            }
        });

    }

    private void verifyOtp(String email, int otp) {
        loadingDialog.show();
        AuthenticateRequest request = new AuthenticateRequest(otp, email);
        webClient.post("/auth/getNewToken",
                request,
                TokenResponse.class,
                (success, message, data) -> {
                    loadingDialog.dismiss();
                    if (success) {
                        // Submit FCM token after acquiring Bearer token
                        String fcmToken = SecureTokenStore.getInstance(this).getFcmToken();
                        if (fcmToken != null) {
                            webClient.post("/auth/submitMessagingToken", fcmToken, Void.class, (s, m, d) -> {
                                if (s) android.util.Log.i("otp_verification", "FCM token submitted successfully");
                            });
                        }

                        GlobalContext globalContext = (GlobalContext) getApplication();
                        if (globalContext.isRegistered()) {
                            // Proceed to Main Map if user was already registered (clicked verifyEmail)
                            Intent intent = new Intent(this, MainMapActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            // Proceed to Registration if new user (clicked get started)
                            Intent intent = new Intent(this, RegisterActivity.class);
                            intent.putExtra("EMAIL", email);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        Toast.makeText(this, message != null ? message : "Verification failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendOtp(String email) {
        loadingDialog.show();
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(email);
        webClient.post("/auth/getOtp",
                emailDto,
                Void.class,
                (success, message, data) -> {
                    loadingDialog.dismiss();
                    Toast.makeText(OtpVerificationActivity.this, message, Toast.LENGTH_SHORT).show();
                });
    }

}
