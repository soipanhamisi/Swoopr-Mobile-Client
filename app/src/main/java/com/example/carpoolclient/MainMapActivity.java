package com.example.carpoolclient;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.carpoolclient.dtos.Coordinates;
import com.example.carpoolclient.dtos.JoinCarpoolDto;
import com.example.carpoolclient.dtos.TripData;
import com.example.carpoolclient.dtos.VehicleDto;
import com.example.carpoolclient.utils.WebClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainMapActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private static final float DEFAULT_ZOOM = 14f;

    private enum SelectionState { NONE, SELECTING_ORIGIN, SELECTING_DESTINATION }
    private enum ActionType { NONE, JOIN, CREATE }

    private GoogleMap googleMap;
    private Marker originMarker;
    private Marker destinationMarker;
    private Coordinates selectedOrigin;
    private Coordinates selectedDestination;
    
    private DrawerLayout drawerLayout;
    private TextView tvHint;
    private TextView tvSelectionDetails;
    private Button btnConfirm;
    
    private SelectionState selectionState = SelectionState.NONE;
    private ActionType currentAction = ActionType.NONE;
    private boolean pendingTripCreationAfterVehicleRegistration = false;

    private WebClient webClient;

    private final ActivityResultLauncher<String> fineLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.trip_location_permission_required, Toast.LENGTH_LONG).show();
                    return;
                }
                enableMyLocationOnMap();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_map_acitvity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        webClient = new WebClient(this);

        tvHint = findViewById(R.id.tv_hint);
        tvSelectionDetails = findViewById(R.id.tv_selection_details);
        btnConfirm = findViewById(R.id.btn_confirm_selection);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            android.util.Log.d("MainMapActivity", "Confirm button clicked");
            handleConfirmation();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (selectionState == SelectionState.SELECTING_DESTINATION) {
                    // Go back to selecting origin
                    selectionState = SelectionState.SELECTING_ORIGIN;
                    if (destinationMarker != null) destinationMarker.remove();
                    selectedDestination = null;
                    btnConfirm.setVisibility(View.GONE);
                    tvHint.setText(R.string.trip_hint_select_origin);
                    updateSelectionDetails();
                } else if (selectionState == SelectionState.SELECTING_ORIGIN) {
                    // Cancel selection and go back to initial state
                    resetToInitialState();
                } else {
                    setEnabled(false);
                    MainMapActivity.this.onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingTripCreationAfterVehicleRegistration) {
            pendingTripCreationAfterVehicleRegistration = false;
            startSelectionProcess(ActionType.CREATE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setOnMapClickListener(this::handleMapClick);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1.286389, 36.817223), DEFAULT_ZOOM));
        enableMyLocationOnMap();
    }

    private void handleMapClick(LatLng latLng) {
        if (selectionState == SelectionState.SELECTING_ORIGIN) {
            selectedOrigin = new Coordinates();
            selectedOrigin.setLatitude(latLng.latitude);
            selectedOrigin.setLongitude(latLng.longitude);

            if (originMarker != null) originMarker.remove();
            originMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
            selectionState = SelectionState.SELECTING_DESTINATION;
            tvHint.setText(R.string.trip_hint_select_destination);
            updateSelectionDetails();
            
        } else if (selectionState == SelectionState.SELECTING_DESTINATION) {
            selectedDestination = new Coordinates();
            selectedDestination.setLatitude(latLng.latitude);
            selectedDestination.setLongitude(latLng.longitude);

            if (destinationMarker != null) destinationMarker.remove();
            destinationMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
            
            btnConfirm.setVisibility(View.VISIBLE);
            tvHint.setText(R.string.trip_hint_confirm);
            updateSelectionDetails();
        }
    }

    private void updateSelectionDetails() {
        StringBuilder sb = new StringBuilder();
        if (selectedOrigin != null) {
            sb.append(getString(R.string.trip_origin_label))
              .append(String.format(Locale.US, "%.4f, %.4f", selectedOrigin.getLatitude(), selectedOrigin.getLongitude()));
        }
        if (selectedDestination != null) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(getString(R.string.trip_dest_label))
              .append(String.format(Locale.US, "%.4f, %.4f", selectedDestination.getLatitude(), selectedDestination.getLongitude()));
        }
        tvSelectionDetails.setText(sb.toString());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_join) {
            startSelectionProcess(ActionType.JOIN);
        } else if (id == R.id.nav_create) {
            checkVehiclesBeforeCreatingTrip();
        } else if (id == R.id.nav_cancel) {
            cancelCurrentTrip();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkVehiclesBeforeCreatingTrip() {
        Type listType = new TypeToken<List<VehicleDto>>(){}.getType();
        webClient.post("/trips/queryRegisteredVehicle", null, listType, false, (success, message, data) -> {
            if (success && data != null) {
                List<VehicleDto> vehicles = (List<VehicleDto>) data;
                if (vehicles.isEmpty()) {
                    showNoVehiclesDialog();
                } else {
                    showVehicleSelectionDialog(vehicles);
                }
            } else {
                Toast.makeText(this, "Failed to fetch vehicles: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoVehiclesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Vehicles Found")
                .setMessage("You need to register a vehicle before you can create a trip.")
                .setPositiveButton("Register Now", (dialog, which) -> {
                    pendingTripCreationAfterVehicleRegistration = true;
                    startActivity(new Intent(this, RegisterVehicleActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showVehicleSelectionDialog(List<VehicleDto> vehicles) {
        String[] vehicleNames = new String[vehicles.size() + 1];
        for (int i = 0; i < vehicles.size(); i++) {
            vehicleNames[i] = vehicles.get(i).getRegNo() + " (" + vehicles.get(i).getDesc() + ")";
        }
        vehicleNames[vehicles.size()] = "Register a new vehicle...";

        new AlertDialog.Builder(this)
                .setTitle("Select Vehicle")
                .setItems(vehicleNames, (dialog, which) -> {
                    if (which == vehicles.size()) {
                        pendingTripCreationAfterVehicleRegistration = true;
                        startActivity(new Intent(this, RegisterVehicleActivity.class));
                    } else {
                        // Selected a vehicle
                        Toast.makeText(this, "Vehicle selected: " + vehicles.get(which).getRegNo(), Toast.LENGTH_SHORT).show();
                        startSelectionProcess(ActionType.CREATE);
                    }
                })
                .show();
    }


    private void startSelectionProcess(ActionType action) {
        currentAction = action;
        selectionState = SelectionState.SELECTING_ORIGIN;
        resetMarkers();
        btnConfirm.setVisibility(View.GONE);
        tvSelectionDetails.setText("");
        tvHint.setText(R.string.trip_hint_select_origin);
        Toast.makeText(this, R.string.trip_hint_select_origin, Toast.LENGTH_SHORT).show();
    }

    private void resetMarkers() {
        if (originMarker != null) originMarker.remove();
        if (destinationMarker != null) destinationMarker.remove();
        selectedOrigin = null;
        selectedDestination = null;
    }

    private void handleConfirmation() {
        android.util.Log.d("MainMapActivity", "handleConfirmation called. Action: " + currentAction + ", Origin: " + selectedOrigin + ", Dest: " + selectedDestination);
        if (selectedOrigin == null || selectedDestination == null) {
            Toast.makeText(this, "Please select both origin and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentAction == ActionType.CREATE) {
            submitCreateRequest(selectedOrigin.getLatitude(), selectedOrigin.getLongitude());
        } else if (currentAction == ActionType.JOIN) {
            submitJoinRequest(selectedOrigin.getLatitude(), selectedOrigin.getLongitude());
        } else {
            android.util.Log.w("MainMapActivity", "Confirm clicked but no action selected");
            Toast.makeText(this, "Please select an action from the menu first", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelCurrentTrip() {
        webClient.post("/trips/cancelTrip", Void.class, (success, message, data) -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                resetToInitialState();
            }
        });
    }

    private void submitCreateRequest(double originLat, double originLng) {
        TripData request = new TripData();
        request.setCapacity(4);
        request.setDepartureTime("2026-07-13T08:00:00");
        
        TripData.OriginDestinationCoordinates coords = new TripData.OriginDestinationCoordinates();
        coords.setOriginLatitude(originLat);
        coords.setOriginLongitude(originLng);
        coords.setDestinationLatitude(selectedDestination.getLatitude());
        coords.setDestinationLongitude(selectedDestination.getLongitude());
        request.setOriginDestinationCoordinates(coords);

        webClient.post("/trips/createTrip", request, Void.class, (success, message, data) -> {
            if (success) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                resetToInitialState();
            } else {
                handleGeofenceError(message);
            }
        });
    }

    private void submitJoinRequest(double originLat, double originLng) {
        JoinCarpoolDto request = new JoinCarpoolDto();
        request.setDepartureTime("2026-07-13T08:00:00");
        
        JoinCarpoolDto.RsOriginDestination coords = new JoinCarpoolDto.RsOriginDestination();
        coords.setOriginLatitude(originLat);
        coords.setOriginLongitude(originLng);
        coords.setDestinationLatitude(selectedDestination.getLatitude());
        coords.setDestinationLongitude(selectedDestination.getLongitude());
        request.setRsOriginDestination(coords);

        webClient.post("/trips/joinCarpool", request, Void.class, (success, message, data) -> {
            if (success) {
                Toast.makeText(this, R.string.trip_join_success, Toast.LENGTH_SHORT).show();
                resetToInitialState();
            } else {
                handleGeofenceError(message);
            }
        });
    }

    private void handleGeofenceError(String message) {
        if (message != null && (message.contains("401") || message.contains("UNAUTHORIZED"))) {
            Intent intent = new Intent(this, EmailVerificationActivity.class);
            intent.putExtra("REFRESH_TOKEN", true);
            startActivity(intent);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        // If the error is likely due to USIU geofence block
        new AlertDialog.Builder(this)
                .setTitle("Location Blocked")
                .setMessage(message + "\n\nEither origin or destination must be within the USIU geofence. Please reselect your coordinates.")
                .setPositiveButton("Reselect", (dialog, which) -> {
                    // Stay in current selection state but allow re-clicking/tweaking
                    // User can use back button or just click on map again to fix it.
                })
                .setNegativeButton("Cancel", (dialog, which) -> resetToInitialState())
                .show();
    }

    private void resetToInitialState() {
        selectionState = SelectionState.NONE;
        currentAction = ActionType.NONE;
        resetMarkers();
        btnConfirm.setVisibility(View.GONE);
        tvHint.setText(R.string.trip_hint_select_action);
        tvSelectionDetails.setText("");
    }

    private void enableMyLocationOnMap() {
        if (googleMap == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }
}
