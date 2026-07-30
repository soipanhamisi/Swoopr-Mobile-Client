package com.example.carpoolclient;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.dtos.JoinCarpoolDto;
import com.example.carpoolclient.utils.PolylineDecoder;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ConfirmJoinActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ConfirmJoinActivity";
    private GoogleMap googleMap;
    private WebClient webClient;
    private LatLng originLatLng, destLatLng;
    private String originName, destName;
    private int selectedHour, selectedMinute;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT), SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        setContentView(R.layout.activity_confirm_join);

        View stationaryMenu = findViewById(R.id.stationary_bottom_nav);
        View bottomSheet = findViewById(R.id.confirm_bottom_sheet);
        View sheetContent = findViewById(R.id.ll_confirm_sheet_content);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        ViewCompat.setOnApplyWindowInsetsListener(stationaryMenu, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            
            v.post(() -> {
                int menuHeight = v.getHeight();
                int basePeekHeight = (int) (40 * getResources().getDisplayMetrics().density);
                bottomSheetBehavior.setPeekHeight(basePeekHeight + menuHeight);
                sheetContent.setPadding(sheetContent.getPaddingLeft(), sheetContent.getPaddingTop(),
                                        sheetContent.getPaddingRight(), menuHeight);
            });
            return insets;
        });

        webClient = new WebClient(this);

        originName = getIntent().getStringExtra("ORIGIN_NAME");
        destName = getIntent().getStringExtra("DEST_NAME");
        originLatLng = new LatLng(getIntent().getDoubleExtra("ORIGIN_LAT", 0), getIntent().getDoubleExtra("ORIGIN_LNG", 0));
        destLatLng = new LatLng(getIntent().getDoubleExtra("DEST_LAT", 0), getIntent().getDoubleExtra("DEST_LNG", 0));
        selectedHour = getIntent().getIntExtra("SELECTED_HOUR", -1);
        selectedMinute = getIntent().getIntExtra("SELECTED_MINUTE", -1);

        ((TextView) findViewById(R.id.tv_confirm_origin)).setText(String.format("Origin: %s", originName));
        ((TextView) findViewById(R.id.tv_confirm_dest)).setText(String.format("Destination: %s", destName));
        ((TextView) findViewById(R.id.tv_confirm_time)).setText(String.format(Locale.US, "Departure: %02d:%02d", selectedHour, selectedMinute));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_confirm);
        if (mapFragment != null) {
            android.util.Log.d(TAG, "Found map fragment, calling getMapAsync");
            mapFragment.getMapAsync(this);
        } else {
            android.util.Log.e(TAG, "Error: SupportMapFragment not found in layout!");
        }

        findViewById(R.id.btn_join_carpool_final).setOnClickListener(v -> submitJoinRequest());
        findViewById(R.id.nav_home).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Add markers immediately
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

        // Immediate camera move to show points while fetching route
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(originLatLng)
                .include(destLatLng)
                .build();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));

        // Fetch and draw actual road route
        fetchRoute();
    }

    private void fetchRoute() {
        android.util.Log.d(TAG, "fetchRoute called");
        String url = String.format(Locale.US,
                "https://maps.googleapis.com/maps/api/directions/json?origin=%f,%f&destination=%f,%f&key=%s",
                originLatLng.latitude, originLatLng.longitude,
                destLatLng.latitude, destLatLng.longitude,
                getString(R.string.google_maps_key));

        webClient.get(url, JsonObject.class, false, (success, message, data) -> {
            if (success && data instanceof JsonObject) {
                try {
                    JsonObject jsonResponse = (JsonObject) data;
                    JsonArray routes = jsonResponse.getAsJsonArray("routes");
                    if (routes != null && routes.size() > 0) {
                        JsonObject route = routes.get(0).getAsJsonObject();
                        String encodedPolyline = route.getAsJsonObject("overview_polyline").get("points").getAsString();
                        List<LatLng> path = PolylineDecoder.decode(encodedPolyline);

                        if (!path.isEmpty()) {
                            PolylineOptions options = new PolylineOptions()
                                    .addAll(path)
                                    .width(12)
                                    .color(Color.BLUE)
                                    .geodesic(true);
                            googleMap.addPolyline(options);

                            // Fit camera to the actual route when map is ready
                            googleMap.setOnMapLoadedCallback(() -> {
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng point : path) {
                                    builder.include(point);
                                }
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
                            });
                        }
                    } else {
                        drawFallbackLine();
                    }
                } catch (Exception e) {
                    drawFallbackLine();
                }
            } else {
                drawFallbackLine();
            }
        });
    }

    private void drawFallbackLine() {
        googleMap.addPolyline(new PolylineOptions()
                .add(originLatLng, destLatLng)
                .width(10)
                .color(Color.GRAY)
                .geodesic(true));

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(originLatLng)
                .include(destLatLng)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    }

    private void submitJoinRequest() {
        JoinCarpoolDto request = new JoinCarpoolDto();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, selectedHour);
        cal.set(Calendar.MINUTE, selectedMinute);
        cal.set(Calendar.SECOND, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        request.setDepartureTime(sdf.format(cal.getTime()));
        
        JoinCarpoolDto.RsOriginDestination coords = new JoinCarpoolDto.RsOriginDestination();
        coords.setOriginLatitude(originLatLng.latitude);
        coords.setOriginLongitude(originLatLng.longitude);
        coords.setDestinationLatitude(destLatLng.latitude);
        coords.setDestinationLongitude(destLatLng.longitude);
        request.setRsOriginDestination(coords);

        webClient.post("/trips/joinCarpool", request, Void.class, (success, message, data) -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (success) {
                finishAffinity();
                android.content.Intent intent = new android.content.Intent(this, MainMapActivity.class);
                startActivity(intent);
            }
        });
    }
}
