package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.dtos.VehicleDto;
import com.example.carpoolclient.utils.WebClient;

public class RegisterVehicleActivity extends AppCompatActivity {

    private EditText etRegNo, etDesc;
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

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainMapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.nav_calendar).setOnClickListener(v ->
                Toast.makeText(this, "Calendar coming soon", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.nav_profile).setOnClickListener(v ->
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
        );
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
