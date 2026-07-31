package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

        EditText etOtp1 = findViewById(R.id.et_otp_1);
        EditText etOtp2 = findViewById(R.id.et_otp_2);
        EditText etOtp3 = findViewById(R.id.et_otp_3);
        Button btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        TextView tvMessage = findViewById(R.id.tv_otp_message);
        TextView tvResendOtp = findViewById(R.id.tv_resend_otp);
        ScrollView scrollView = findViewById(R.id.scroll_view);

        setupOtpAutoJump(etOtp1, etOtp2, etOtp3);

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
            String otpStr = etOtp1.getText().toString().trim() +
                           etOtp2.getText().toString().trim() +
                           etOtp3.getText().toString().trim();
            if (otpStr.length() < OTP_LENGTH) {
                Toast.makeText(this, "Please enter all " + OTP_LENGTH + " digits", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int otp = Integer.parseInt(otpStr);
                verifyOtp(finalEmail, otp);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid OTP format", Toast.LENGTH_SHORT).show();
            }
        });

        etOtp1.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, btnVerifyOtp.getBottom() + 100), 100);
            }
        });
    }

    private void setupOtpAutoJump(EditText et1, EditText et2, EditText et3) {
        et1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 1) et2.requestFocus();
            }
        });
        et2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 1) et3.requestFocus();
                else if (s.length() == 0) et1.requestFocus();
            }
        });
        et3.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) et2.requestFocus();
            }
        });
    }

    private void verifyOtp(String email, int otp) {
        loadingDialog.show();
        boolean isRegistered = ((GlobalContext) getApplication()).isRegistered();
        String endpoint = isRegistered ? "/auth/getNewToken" : "/auth/authenticateUser";

        AuthenticateRequest request = new AuthenticateRequest(otp, email);
        webClient.post(endpoint,
                request,
                Void.class,
                (success, message, data) -> {
                    loadingDialog.dismiss();
                    if (success) {
                        // Proceed to Registration/Profile screen
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
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
