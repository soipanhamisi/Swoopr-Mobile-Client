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
import com.example.carpoolclient.utils.LoadingDialog;

public class OtpVerificationActivity extends AppCompatActivity {
    private AuthService authService;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);

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
        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();

            if (otp.length() == 3) {
                btnVerifyOtp.setEnabled(false);
                loadingDialog.show();
                authService.authenticateUser(otp, finalEmail, (success, message) -> runOnUiThread(() -> {
                    btnVerifyOtp.setEnabled(true);
                    loadingDialog.dismiss();
                    if (success) {
                        Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, RegisterActivity.class);
                        intent.putExtra("EMAIL", finalEmail);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Verification failed: " + message, Toast.LENGTH_LONG).show();
                    }
                }));
            } else {
                etOtp.setError("Please enter the 3-digit code");
            }
        });
    }
}


