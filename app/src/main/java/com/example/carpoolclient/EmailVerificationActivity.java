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

import com.example.carpoolclient.dtos.EmailDto;
import com.example.carpoolclient.utils.LoadingDialog;
import com.example.carpoolclient.utils.WebClient;

public class EmailVerificationActivity extends AppCompatActivity {
    private LoadingDialog loadingDialog;
    private WebClient webClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);
        boolean refresh = getIntent().getBooleanExtra("REFRESH_TOKEN", false);

        webClient = new WebClient(this);
        loadingDialog = new LoadingDialog(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom);
            return insets;
        });

        EditText etEmail = findViewById(R.id.et_email);
        Button btnVerify = findViewById(R.id.btn_verify);
        ScrollView scrollView = findViewById(R.id.scroll_view);

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, btnVerify.getBottom() + 100), 100);
            }
        });

        btnVerify.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }

            if (isValidUsiuEmail(email)) {
                btnVerify.setEnabled(false);
                loadingDialog.show();
                EmailDto emailDto = new EmailDto();
                emailDto.setEmail(email);
                webClient.post("/auth/getOtp", emailDto, Void.class, (success, message, data) -> {
                        if (success) {
                            loadingDialog.dismiss();
                            goToOtpVerification(email);
                        } else {
                            btnVerify.setEnabled(true);
                            loadingDialog.dismiss();
                            Toast.makeText(EmailVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
               } else {
                etEmail.setError("Please enter a valid @usiu.ac.ke email address");
            }
        });
    }

    private void goToOtpVerification(String email) {
        Intent intent = new Intent(EmailVerificationActivity.this, OtpVerificationActivity.class);
        intent.putExtra("EMAIL", email);
        intent.putExtra("REFRESH_TOKEN", getIntent().getBooleanExtra("REFRESH_TOKEN", false));
        startActivity(intent);
        finish();
    }

    private boolean isValidUsiuEmail(String email) {
        return email.endsWith("@usiu.ac.ke");
    }
}


