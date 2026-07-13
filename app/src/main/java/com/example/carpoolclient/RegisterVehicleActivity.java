package com.example.carpoolclient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.dtos.VehicleDto;
import com.example.carpoolclient.utils.WebClient;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterVehicleActivity extends AppCompatActivity {

    private TextInputEditText etRegNo, etDesc;
    private Button btnRegister;
    private WebClient webClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_vehicle);

        webClient = new WebClient(this);

        etRegNo = findViewById(R.id.et_reg_no);
        etDesc = findViewById(R.id.et_desc);
        btnRegister = findViewById(R.id.btn_register_vehicle);

        btnRegister.setOnClickListener(v -> registerVehicle());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void registerVehicle() {
        String regNo = etRegNo.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (regNo.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        VehicleDto vehicle = new VehicleDto(regNo, desc);
        btnRegister.setEnabled(false);

        webClient.post("/trips/registerVehicle", vehicle, Void.class, (success, message, data) -> {
            btnRegister.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                finish(); // Go back to MapActivity
            }
        });
    }
}
