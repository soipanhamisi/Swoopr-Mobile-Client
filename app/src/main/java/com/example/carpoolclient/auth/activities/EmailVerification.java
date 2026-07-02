package com.example.carpoolclient.auth.activities;

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

import com.example.carpoolclient.R;
import com.example.carpoolclient.auth.services.AuthService;
import com.example.carpoolclient.utils.LoadingDialog;

public class EmailVerification extends AppCompatActivity {
    private final AuthService authService = new AuthService();
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);

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
                authService.getOtp(email, (success, message) -> runOnUiThread(() -> {
                    btnVerify.setEnabled(true);
                    loadingDialog.dismiss();
                    if (success) {
                        Intent intent = new Intent(this, OtpVerificationActivity.class);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                    } else {
                        String displayMessage = message != null && message.toLowerCase().contains("user exists")
                                ? "Account already registered"
                                : message;
                        Toast.makeText(this, "Error: " + displayMessage, Toast.LENGTH_LONG).show();
                    }
                }));
            } else {
                etEmail.setError("Please enter a valid @usiu.ac.ke email address");
            }
        });
    }

    private boolean isValidUsiuEmail(String email) {
        return email.endsWith("@usiu.ac.ke");
    }
}


