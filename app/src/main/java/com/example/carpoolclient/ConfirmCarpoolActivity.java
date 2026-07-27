package com.example.carpoolclient;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.dtos.TripData;
import com.example.carpoolclient.utils.WebClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ConfirmCarpoolActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private WebClient webClient;
    private String vehicleId, vehicleName, vehiclePlate;
    private LatLng originLatLng, destLatLng;
    private String originName, destName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_carpool);

        webClient = new WebClient(this);

        // Get data from Intent
        vehicleId = getIntent().getStringExtra("VEHICLE_ID");
        vehicleName = getIntent().getStringExtra("VEHICLE_NAME");
        vehiclePlate = getIntent().getStringExtra("VEHICLE_PLATE");
        
        // Sample coordinates for demonstration as per plan
        originLatLng = new LatLng(-1.2189, 36.8885); // USIU Area
        destLatLng = new LatLng(-1.2864, 36.8172);  // Nairobi CBD Area
        originName = "USIU, Gate B";
        destName = "Moi Avenue, Nairobi";

        ((TextView) findViewById(R.id.tv_confirm_origin)).setText(originName);
        ((TextView) findViewById(R.id.tv_confirm_dest)).setText(destName);
        ((TextView) findViewById(R.id.tv_confirm_vehicle_name)).setText(vehicleName);
        ((TextView) findViewById(R.id.tv_confirm_vehicle_plate)).setText(vehiclePlate);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_confirm);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.btn_create_carpool_final).setOnClickListener(v -> submitCreateRequest());
        findViewById(R.id.nav_home).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        // Add markers
        googleMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .title("Origin: " + originName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        googleMap.addMarker(new MarkerOptions()
                .position(destLatLng)
                .title("Destination: " + destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Draw polyline
        googleMap.addPolyline(new PolylineOptions()
                .add(originLatLng, destLatLng)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true));

        // Move camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLatLng, 12f));
    }

    private void submitCreateRequest() {
        TripData request = new TripData();
        request.setCapacity(4);
        request.setDepartureTime("2026-07-26T22:00:00"); // Sample time
        
        TripData.OriginDestinationCoordinates coords = new TripData.OriginDestinationCoordinates();
        coords.setOriginLatitude(originLatLng.latitude);
        coords.setOriginLongitude(originLatLng.longitude);
        coords.setDestinationLatitude(destLatLng.latitude);
        coords.setDestinationLongitude(destLatLng.longitude);
        request.setOriginDestinationCoordinates(coords);

        webClient.post("/trips/createTrip", request, Void.class, (success, message, data) -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                // Clear activity stack and go back to Map
                finishAffinity();
                android.content.Intent intent = new android.content.Intent(this, MainMapActivity.class);
                startActivity(intent);
            }
        });
    }
}
