package com.example.carpoolclient;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.dtos.TripData;
import com.example.carpoolclient.utils.WebClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ConfirmCarpoolActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private WebClient webClient;
    private String vehicleId, vehicleName, vehiclePlate;
    private LatLng originLatLng, destLatLng;
    private String originName, destName;
    private int capacity;
    private int selectedHour, selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        setContentView(R.layout.activity_confirm_carpool);

        View bottomSheet = findViewById(R.id.confirm_bottom_sheet);
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = systemBars.bottom;
            v.setLayoutParams(mlp);
            return insets;
        });

        webClient = new WebClient(this);

        // Get data from Intent
        vehicleId = getIntent().getStringExtra("VEHICLE_ID");
        vehicleName = getIntent().getStringExtra("VEHICLE_NAME");
        vehiclePlate = getIntent().getStringExtra("VEHICLE_PLATE");
        originName = getIntent().getStringExtra("ORIGIN_NAME");
        destName = getIntent().getStringExtra("DEST_NAME");
        originLatLng = new LatLng(getIntent().getDoubleExtra("ORIGIN_LAT", 0), getIntent().getDoubleExtra("ORIGIN_LNG", 0));
        destLatLng = new LatLng(getIntent().getDoubleExtra("DEST_LAT", 0), getIntent().getDoubleExtra("DEST_LNG", 0));
        capacity = getIntent().getIntExtra("CAPACITY", 4);
        selectedHour = getIntent().getIntExtra("SELECTED_HOUR", -1);
        selectedMinute = getIntent().getIntExtra("SELECTED_MINUTE", -1);

        ((TextView) findViewById(R.id.tv_confirm_origin)).setText("Origin: " + originName);
        ((TextView) findViewById(R.id.tv_confirm_dest)).setText("Destination: " + destName);
        ((TextView) findViewById(R.id.tv_confirm_vehicle_name)).setText(vehicleName);
        ((TextView) findViewById(R.id.tv_confirm_vehicle_plate)).setText(vehiclePlate);
        ((TextView) findViewById(R.id.tv_confirm_capacity)).setText("Capacity: " + capacity);
        ((TextView) findViewById(R.id.tv_confirm_time)).setText(String.format(Locale.US, "Departure: %02d:%02d", selectedHour, selectedMinute));

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
                .title("Origin")
                .snippet(originName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        googleMap.addMarker(new MarkerOptions()
                .position(destLatLng)
                .title("Destination")
                .snippet(destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Draw polyline
        googleMap.addPolyline(new PolylineOptions()
                .add(originLatLng, destLatLng)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true));

        // Move camera to fit both points
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(originLatLng)
                .include(destLatLng)
                .build();
        googleMap.setOnMapLoadedCallback(() -> 
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        );
    }

    private void submitCreateRequest() {
        TripData request = new TripData();
        request.setCapacity(capacity);
        
        // Format the selected time for today in ISO-8601
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, selectedHour);
        cal.set(Calendar.MINUTE, selectedMinute);
        cal.set(Calendar.SECOND, 0);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        request.setDepartureTime(sdf.format(cal.getTime()));
        
        TripData.OriginDestinationCoordinates coords = new TripData.OriginDestinationCoordinates();
        coords.setOriginLatitude(originLatLng.latitude);
        coords.setOriginLongitude(originLatLng.longitude);
        coords.setDestinationLatitude(destLatLng.latitude);
        coords.setDestinationLongitude(destLatLng.longitude);
        request.setOriginDestinationCoordinates(coords);

        webClient.post("/trips/createTrip", request, Void.class, (success, message, data) -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                finishAffinity();
                android.content.Intent intent = new android.content.Intent(this, MainMapActivity.class);
                startActivity(intent);
            }
        });
    }
}
