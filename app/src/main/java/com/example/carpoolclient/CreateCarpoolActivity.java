package com.example.carpoolclient;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.carpoolclient.dtos.VehicleDto;
import com.example.carpoolclient.utils.WebClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CreateCarpoolActivity extends AppCompatActivity {

    private TextView tvVehicleName, tvVehiclePlate, tvOriginAddress, tvCapacity, tvDepartureTime;
    private AutoCompleteTextView actvDestination;
    private WebClient webClient;
    private List<VehicleDto> registeredVehicles;
    private VehicleDto selectedVehicle;
    
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private LatLng originLatLng, destLatLng;
    private String originName, destName;
    private int capacity = 4;
    private int selectedHour = -1, selectedMinute = -1;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    tvOriginAddress.setText("Permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_carpool);

        // Initialize SDKs
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        webClient = new WebClient(this);

        tvVehicleName = findViewById(R.id.tv_vehicle_name);
        tvVehiclePlate = findViewById(R.id.tv_vehicle_plate);
        tvOriginAddress = findViewById(R.id.tv_origin_address);
        tvCapacity = findViewById(R.id.tv_capacity);
        tvDepartureTime = findViewById(R.id.tv_departure_time);
        actvDestination = findViewById(R.id.actv_destination);

        setupLocation();
        setupDestinationAutocomplete();
        setupCapacityControls();
        setupTimePicker();

        findViewById(R.id.card_vehicle_selection).setOnClickListener(v -> showVehicleSelectionDialog());
        findViewById(R.id.tv_select_vehicle).setOnClickListener(v -> showVehicleSelectionDialog());
        findViewById(R.id.btn_register_new).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterVehicleActivity.class));
        });

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            if (selectedVehicle == null) {
                Toast.makeText(this, "Please select a vehicle", Toast.LENGTH_SHORT).show();
            } else if (originLatLng == null) {
                Toast.makeText(this, "Wait, fetching your location...", Toast.LENGTH_SHORT).show();
            } else if (destLatLng == null) {
                Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            } else if (selectedHour == -1) {
                Toast.makeText(this, "Please select a departure time", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ConfirmCarpoolActivity.class);
                intent.putExtra("VEHICLE_ID", selectedVehicle.getRegNo());
                intent.putExtra("VEHICLE_NAME", selectedVehicle.getDesc());
                intent.putExtra("VEHICLE_PLATE", selectedVehicle.getRegNo());
                intent.putExtra("ORIGIN_LAT", originLatLng.latitude);
                intent.putExtra("ORIGIN_LNG", originLatLng.longitude);
                intent.putExtra("ORIGIN_NAME", originName);
                intent.putExtra("DEST_LAT", destLatLng.latitude);
                intent.putExtra("DEST_LNG", destLatLng.longitude);
                intent.putExtra("DEST_NAME", destName);
                intent.putExtra("CAPACITY", capacity);
                intent.putExtra("SELECTED_HOUR", selectedHour);
                intent.putExtra("SELECTED_MINUTE", selectedMinute);
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

    private void setupLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                geocodeOrigin(location);
            } else {
                tvOriginAddress.setText("USIU, Gate B (Default)");
                originLatLng = new LatLng(-1.2189, 36.8885);
                originName = "USIU, Gate B";
            }
        });
    }

    private void geocodeOrigin(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                originName = address.getAddressLine(0);
                tvOriginAddress.setText(originName);
            }
        } catch (Exception e) {
            originName = "USIU, Gate B";
            tvOriginAddress.setText(originName);
        }
    }

    private void setupDestinationAutocomplete() {
        actvDestination.setFocusable(false);
        actvDestination.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .setCountries(Collections.singletonList("KE"))
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                destLatLng = place.getLocation();
                destName = place.getDisplayName();
                actvDestination.setText(destName);
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupCapacityControls() {
        findViewById(R.id.iv_minus).setOnClickListener(v -> {
            if (capacity > 1) {
                capacity--;
                tvCapacity.setText(String.valueOf(capacity));
            }
        });
        findViewById(R.id.iv_plus).setOnClickListener(v -> {
            if (capacity < 4) {
                capacity++;
                tvCapacity.setText(String.valueOf(capacity));
            }
        });
    }

    private void setupTimePicker() {
        findViewById(R.id.ll_time_selector).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                // Validation: 5:00 AM to 9:30 PM (21:30)
                if (hourOfDay < 5 || (hourOfDay == 21 && minuteOfHour > 30) || hourOfDay > 21) {
                    Toast.makeText(this, "Carpools only allowed between 5:00 AM and 9:30 PM", Toast.LENGTH_LONG).show();
                } else {
                    selectedHour = hourOfDay;
                    selectedMinute = minuteOfHour;
                    tvDepartureTime.setText(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute));
                }
            }, hour, minute, true);
            timePickerDialog.show();
        });
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
