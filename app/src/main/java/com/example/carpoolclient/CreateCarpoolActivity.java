package com.example.carpoolclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.carpoolclient.dtos.TripData;
import com.example.carpoolclient.dtos.VehicleDto;
import com.example.carpoolclient.utils.WebClient;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class CreateCarpoolActivity extends AppCompatActivity {

    private TextView tvVehicleName, tvVehiclePlate;
    private WebClient webClient;
    private List<VehicleDto> registeredVehicles;
    private VehicleDto selectedVehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_carpool);

        webClient = new WebClient(this);

        tvVehicleName = findViewById(R.id.tv_vehicle_name);
        tvVehiclePlate = findViewById(R.id.tv_vehicle_plate);

        findViewById(R.id.tv_select_vehicle).setOnClickListener(v -> showVehicleSelectionDialog());
        findViewById(R.id.btn_register_new).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterVehicleActivity.class));
        });

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            if (selectedVehicle == null) {
                Toast.makeText(this, "Please select a vehicle", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ConfirmCarpoolActivity.class);
                intent.putExtra("VEHICLE_ID", selectedVehicle.getRegNo()); // Using RegNo as ID for now
                intent.putExtra("VEHICLE_NAME", selectedVehicle.getDesc());
                intent.putExtra("VEHICLE_PLATE", selectedVehicle.getRegNo());
                startActivity(intent);
            }
        });

        findViewById(R.id.nav_home).setOnClickListener(v -> finish());
        findViewById(R.id.nav_calendar).setOnClickListener(v -> 
            Toast.makeText(this, "Calendar coming soon", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.nav_profile).setOnClickListener(v -> 
            Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show()
        );

        fetchVehicles();
    }

    private void fetchVehicles() {
        Type listType = new TypeToken<List<VehicleDto>>(){}.getType();
        webClient.get("/trips/queryRegisteredVehicle", listType, true, (success, message, data) -> {
            if (success && data != null) {
                registeredVehicles = (List<VehicleDto>) data;
                if (!registeredVehicles.isEmpty()) {
                    selectedVehicle = registeredVehicles.get(0);
                    updateVehicleUI();
                } else {
                    tvVehicleName.setText("No vehicle registered");
                    tvVehiclePlate.setText("--");
                }
            }
        });
    }

    private void updateVehicleUI() {
        if (selectedVehicle != null) {
            tvVehicleName.setText(selectedVehicle.getDesc());
            tvVehiclePlate.setText(selectedVehicle.getRegNo());
        }
    }

    private void showVehicleSelectionDialog() {
        if (registeredVehicles == null || registeredVehicles.isEmpty()) {
            Toast.makeText(this, "No vehicles registered", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] vehicleNames = new String[registeredVehicles.size()];
        for (int i = 0; i < registeredVehicles.size(); i++) {
            vehicleNames[i] = registeredVehicles.get(i).getRegNo() + " (" + registeredVehicles.get(i).getDesc() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Vehicle")
                .setItems(vehicleNames, (dialog, which) -> {
                    selectedVehicle = registeredVehicles.get(which);
                    updateVehicleUI();
                })
                .show();
    }
}
