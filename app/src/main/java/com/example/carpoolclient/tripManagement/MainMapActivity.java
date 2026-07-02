package com.example.carpoolclient.tripManagement;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.R;
import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.time.LocalDateTime;

public class MainMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM = 14f;

    private GoogleMap googleMap;
    private Marker destinationMarker;
    private Coordinates selectedDestination;
    private TextView selectedDestinationView;
    private RideSeekerService rideSeekerService;
    private FusedLocationProviderClient locationProviderClient;

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

        rideSeekerService = new RideSeekerService(SecureTokenStore.getInstance(this));
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        selectedDestinationView = findViewById(R.id.tv_selected_destination);
        Button joinButton = findViewById(R.id.btn_join);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        joinButton.setOnClickListener(v -> {
            if (selectedDestination == null) {
                Toast.makeText(this, R.string.trip_destination_required, Toast.LENGTH_SHORT).show();
                return;
            }

            requestOriginAndJoin();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setOnMapClickListener(this::onDestinationSelected);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1.286389, 36.817223), DEFAULT_ZOOM));

        enableMyLocationOnMap();
    }

    private void onDestinationSelected(LatLng latLng) {
        selectedDestination = new Coordinates();
        selectedDestination.setLatitude(latLng.latitude);
        selectedDestination.setLongitude(latLng.longitude);

        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        destinationMarker = googleMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
        selectedDestinationView.setText(latLng.latitude + ", " + latLng.longitude);
    }

    private void requestOriginAndJoin() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        locationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                submitJoinRequest(location.getLatitude(), location.getLongitude());
                return;
            }

            // Fall back to a cached location when a live fix is unavailable.
            locationProviderClient.getLastLocation().addOnSuccessListener(lastLocation -> {
                if (lastLocation == null) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.trip_origin_unavailable, Toast.LENGTH_LONG).show());
                    return;
                }

                submitJoinRequest(lastLocation.getLatitude(), lastLocation.getLongitude());
            }).addOnFailureListener(e ->
                    runOnUiThread(() -> Toast.makeText(this, R.string.trip_origin_unavailable, Toast.LENGTH_LONG).show())
            );
        }).addOnFailureListener(e ->
                runOnUiThread(() -> Toast.makeText(this, R.string.trip_origin_unavailable, Toast.LENGTH_LONG).show())
        );
    }

    private void submitJoinRequest(double originLatitude, double originLongitude) {
        Coordinates origin = new Coordinates();
        origin.setLatitude(originLatitude);
        origin.setLongitude(originLongitude);

        OriginDestinationCoordinates routeCoordinates = new OriginDestinationCoordinates();
        routeCoordinates.setOrigin(origin);
        routeCoordinates.setDestination(selectedDestination);

        rideSeekerService.joinCarpool(LocalDateTime.now(), routeCoordinates, (status, message) ->
                runOnUiThread(() -> {
                    if (status) {
                        Toast.makeText(this, R.string.trip_join_success, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                })
        );
    }

    private void enableMyLocationOnMap() {
        if (googleMap == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        googleMap.setMyLocationEnabled(true);
    }
}