package com.example.carpoolclient;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.dtos.Coordinates;
import com.example.carpoolclient.dtos.JoinCarpoolDto;
import com.example.carpoolclient.dtos.PendingTripDto;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class MainMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM = 14f;

    private enum SelectionState { NONE, SELECTING_ORIGIN, SELECTING_DESTINATION }
    private enum ActionType { NONE, JOIN, CREATE }

    private GoogleMap googleMap;
    private Marker originMarker;
    private Marker destinationMarker;
    private Coordinates selectedOrigin;
    private Coordinates selectedDestination;
    
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvHint;
    private TextView tvSelectionDetails;
    private TextView tvGreeting;
    private TextView tvTripStart;
    private TextView tvTripDest;
    private TextView tvDashboardTrip;
    private Button btnConfirm;
    private FloatingActionButton btnMyLocation;
    
    private SelectionState selectionState = SelectionState.NONE;
    private ActionType currentAction = ActionType.NONE;

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

        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        btnMyLocation = findViewById(R.id.btn_my_location);

        btnMyLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                } else {
                    googleMap.setMyLocationEnabled(true);
                    android.location.Location location = googleMap.getMyLocation();
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                    } else {
                        Toast.makeText(this, "Wait, fetching your location...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        findViewById(R.id.btn_nav_join).setOnClickListener(v -> {
            startSelectionProcess(ActionType.JOIN);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });
        findViewById(R.id.btn_nav_create).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateCarpoolActivity.class));
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });
        findViewById(R.id.btn_register_vehicle).setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterVehicleActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.btn_nav_cancel).setOnClickListener(v -> {
            cancelCurrentTrip();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        webClient = new WebClient(this);

        tvHint = findViewById(R.id.tv_hint);
        tvSelectionDetails = findViewById(R.id.tv_selection_details);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvTripStart = findViewById(R.id.tv_trip_start);
        tvTripDest = findViewById(R.id.tv_trip_dest);
        tvDashboardTrip = findViewById(R.id.tv_dashboard_trip);
        btnConfirm = findViewById(R.id.btn_confirm_selection);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            android.util.Log.d("MainMapActivity", "Confirm button clicked");
            handleConfirmation();
        });

        View logo = findViewById(R.id.img_logo_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) logo.getLayoutParams();
            lp.topMargin = systemBars.top + (int)(24 * getResources().getDisplayMetrics().density);
            logo.setLayoutParams(lp);
            
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (selectionState == SelectionState.SELECTING_DESTINATION) {
                    selectionState = SelectionState.SELECTING_ORIGIN;
                    if (destinationMarker != null) destinationMarker.remove();
                    selectedDestination = null;
                    btnConfirm.setVisibility(View.GONE);
                    tvHint.setText(R.string.trip_hint_select_origin);
                    updateSelectionDetails();
                } else if (selectionState == SelectionState.SELECTING_ORIGIN) {
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
        fetchDashboardData();
    }

    private void fetchDashboardData() {
        // Update greeting
        String name = ((GlobalContext) getApplication()).getFullName();
        if (name != null && !name.isEmpty()) {
            tvGreeting.setText("Hello " + name + "...");
        } else {
            tvGreeting.setText("Hello User...");
        }

        // 1. Scheduled Trips
        webClient.get("/trips/queryPendingTrips", PendingTripDto.class, (success, message, data) -> {
            View card = findViewById(R.id.card_pending_trip);
            if (success && data != null) {
                PendingTripDto trip = (PendingTripDto) data;
                card.setVisibility(View.VISIBLE);
                
                // Populating with available data
                tvTripStart.setText("Departure: " + formatDateTime(trip.getTripData().getDepartureTime()));
                tvTripDest.setText("Capacity: " + trip.getTripData().getCapacity());
                
                if (trip.getCarpoolMemberNames() != null && !trip.getCarpoolMemberNames().isEmpty()) {
                    tvDashboardTrip.setText(String.join("\n", trip.getCarpoolMemberNames()));
                } else {
                    tvDashboardTrip.setText("No other members");
                }
            } else {
                // If no pending trip, we could either hide the card or show a "No trips" state
                // Based on the overhaul, let's keep it visible but show empty state or hide it.
                // For now, let's hide it if no data.
                card.setVisibility(View.GONE);
            }
        });

        // 2. Registered Vehicles
        Type listType = new TypeToken<List<VehicleDto>>(){}.getType();
        webClient.get("/trips/queryRegisteredVehicle", listType, true, (success, message, data) -> {
            // Data fetched, can be used for other UI elements if needed
        });
    }

    private String formatDateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) return "";
        try {
            // Endpoints return ISO 8601 like 2026-07-23T12:54:44.916005
            // SimpleDateFormat works best with a truncated string if there are many fractional digits
            String partToParse = isoString.split("\\.")[0]; // "2026-07-23T12:54:44"
            
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(partToParse);
            
            if (date == null) return isoString;
            
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, hh:mm a", Locale.US);
            return formatter.format(date);
        } catch (Exception e) {
            return isoString;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false); // Disable default button
        googleMap.setOnMapClickListener(this::handleMapClick);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1.286389, 36.817223), DEFAULT_ZOOM));
        
        // Remove padding since drawer is hidden
        googleMap.setPadding(0, 0, 0, 0);
        
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

        if (currentAction == ActionType.JOIN) {
            submitJoinRequest(selectedOrigin.getLatitude(), selectedOrigin.getLongitude());
        } else {
            android.util.Log.w("MainMapActivity", "Confirm clicked but no action selected");
            Toast.makeText(this, "Please select an action from the bottom drawer first", Toast.LENGTH_SHORT).show();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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

        new AlertDialog.Builder(this)
                .setTitle("Location Blocked")
                .setMessage(message + "\n\nEither origin or destination must be within the USIU geofence. Please reselect your coordinates.")
                .setPositiveButton("Reselect", null)
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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        fetchDashboardData();
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
