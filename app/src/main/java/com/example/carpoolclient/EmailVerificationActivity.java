package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
    private LoadingDialog loadingDialog = new LoadingDialog(this);
    private WebClient webClient = new WebClient(this);;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom);
            return insets;
        });

        EditText etEmail = findViewById(R.id.et_email);
        Button btnVerify = findViewById(R.id.btn_verify);
        TextView tvEmailHint = findViewById(R.id.tv_email_hint);

        btnVerify.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            if(!email.endsWith("@usiu.ac.ke")){
                tvEmailHint.setText("Please enter a valid USIU email address");
                etEmail.getText().clear();
            }else
                requestOtp(email);
        });
    }

    private void requestOtp(String email) {
        loadingDialog.show();
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(email);
        webClient.post("auth/getOtp",
                emailDto,
                Void.class,
                (success, message, data) -> {
                    loadingDialog.dismiss();
                    if (success) {
                        Intent intent = new Intent(EmailVerificationActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                    } else {
                        Toast.makeText(EmailVerificationActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


