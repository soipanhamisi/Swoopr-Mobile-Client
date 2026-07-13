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
import com.example.carpoolclient.utils.WebClient;
import com.google.firebase.messaging.FirebaseMessaging;

public class RegisterActivity extends AppCompatActivity {
    private LoadingDialog loadingDialog;
    private WebClient webClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        webClient = new WebClient(this);
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

            btnFinish.setEnabled(false);
            loadingDialog.show();

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                String fcmToken = task.isSuccessful() ? task.getResult() : null;
                String fullName = firstName + " " + lastName;
                UserDto userDto = new UserDto(fullName, finalEmail, "NORMAL_USER", fcmToken);

                webClient.post("/auth/saveUser", userDto, Void.class, (success, message, data) -> {
                    btnFinish.setEnabled(true);
                    loadingDialog.dismiss();

                    if (success) {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        navigateToMainMap();
                    } else {
                        String displayMessage = message != null && message.toLowerCase().contains("user exists")
                                ? "Account already registered"
                                : message;
                        Toast.makeText(this, "Registration failed: " + displayMessage, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    private void navigateToMainMap() {
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


