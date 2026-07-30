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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class JoinCarpoolActivity extends AppCompatActivity {

    private TextView tvOriginAddress, tvDepartureTime;
    private AutoCompleteTextView actvDestination;
    
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private LatLng originLatLng, destLatLng;
    private String originName, destName;
    private int selectedHour = -1, selectedMinute = -1;
    
    private static final int MAP_SELECTION_REQUEST_CODE = 2;

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
        setContentView(R.layout.activity_join_carpool);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvOriginAddress = findViewById(R.id.tv_origin_address);
        tvDepartureTime = findViewById(R.id.tv_departure_time);
        actvDestination = findViewById(R.id.actv_destination);

        setupLocation();
        setupDestinationAutocomplete();
        setupTimePicker();

        findViewById(R.id.iv_map_mode).setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, MAP_SELECTION_REQUEST_CODE);
        });

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            if (originLatLng == null) {
                Toast.makeText(this, "Wait, fetching your location...", Toast.LENGTH_SHORT).show();
            } else if (destLatLng == null) {
                Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            } else if (selectedHour == -1) {
                Toast.makeText(this, "Please select a departure time", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ConfirmJoinActivity.class);
                intent.putExtra("ORIGIN_LAT", originLatLng.latitude);
                intent.putExtra("ORIGIN_LNG", originLatLng.longitude);
                intent.putExtra("ORIGIN_NAME", originName);
                intent.putExtra("DEST_LAT", destLatLng.latitude);
                intent.putExtra("DEST_LNG", destLatLng.longitude);
                intent.putExtra("DEST_NAME", destName);
                intent.putExtra("SELECTED_HOUR", selectedHour);
                intent.putExtra("SELECTED_MINUTE", selectedMinute);
                startActivity(intent);
            }
        });

        findViewById(R.id.nav_home).setOnClickListener(v -> finish());
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
        AutocompleteAdapter adapter = new AutocompleteAdapter(this, placesClient);
        actvDestination.setThreshold(1);
        actvDestination.setAdapter(adapter);
        actvDestination.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction item = adapter.getItem(position);
            if (item != null) {
                String placeId = item.getPlaceId();
                List<Place.Field> placeFields = Arrays.asList(Place.Field.LOCATION, Place.Field.DISPLAY_NAME);
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
                placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    destLatLng = place.getLocation();
                    destName = place.getDisplayName();
                    actvDestination.setText(destName);
                    actvDestination.setSelection(destName.length());
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_SELECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                double lat = data.getDoubleExtra("DEST_LAT", 0);
                double lng = data.getDoubleExtra("DEST_LNG", 0);
                destName = data.getStringExtra("DEST_NAME");
                destLatLng = new LatLng(lat, lng);
                actvDestination.setText(destName);
            }
        }
    }

    private void setupTimePicker() {
        findViewById(R.id.ll_time_selector).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                selectedHour = hourOfDay;
                selectedMinute = minuteOfHour;
                tvDepartureTime.setText(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute));
            }, hour, minute, true);
            timePickerDialog.show();
        });
    }
}
