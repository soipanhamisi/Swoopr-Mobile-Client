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
import com.example.carpoolclient.auth.dtos.RegisterRequest;
import com.example.carpoolclient.auth.services.AuthService;
import com.example.carpoolclient.tripManagement.MainMapActivity;
import com.example.carpoolclient.utils.LoadingDialog;

public class RegisterActivity extends AppCompatActivity {
    private AuthService authService;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        authService = new AuthService(this);
        loadingDialog = new LoadingDialog(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom);
            return insets;
        });

        EditText etFirstName = findViewById(R.id.et_first_name);
        EditText etLastName = findViewById(R.id.et_last_name);
        EditText etPhoneNumber = findViewById(R.id.et_phone_number);
        EditText etStudentId = findViewById(R.id.et_student_id);
        Button btnFinish = findViewById(R.id.btn_complete_registration);
        ScrollView scrollView = findViewById(R.id.scroll_view);

        String email = getIntent().getStringExtra("EMAIL");
        if (email == null) {
            email = "";
        }

        EditText[] editTexts = new EditText[]{etFirstName, etLastName, etPhoneNumber, etStudentId};
        for (EditText et : editTexts) {
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, et.getBottom() + 100), 100);
                }
            });
        }

        final String finalEmail = email;
        btnFinish.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || studentId.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }


            RegisterRequest registerRequest = new RegisterRequest(
                    firstName,
                    lastName,
                    finalEmail,
                    phoneNumber,
                    studentId
            );

            btnFinish.setEnabled(false);
            loadingDialog.show();
            authService.registerUser(registerRequest, (success, message) -> runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    authService.sendMessagingToken((tokenSuccess, tokenMessage) -> runOnUiThread(() -> {
                        btnFinish.setEnabled(true);
                        loadingDialog.dismiss();

                        if (!tokenSuccess) {
                            Toast.makeText(this, "Failed to submit messaging token: " + tokenMessage, Toast.LENGTH_SHORT).show();
                        }

                        Intent intent = new Intent(this, MainMapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }));
                } else {
                    btnFinish.setEnabled(true);
                    loadingDialog.dismiss();
                    String displayMessage = message != null && message.toLowerCase().contains("user exists")
                            ? "Account already registered"
                            : message;
                    Toast.makeText(this, "Registration failed: " + displayMessage, Toast.LENGTH_LONG).show();
                }
            }));
        });
    }
}


