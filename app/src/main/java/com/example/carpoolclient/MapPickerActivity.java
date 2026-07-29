package com.example.carpoolclient;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectionMarker;
    private LatLng selectedLatLng;
    private String selectedAddressName;

    private View bottomSheet;
    private BottomSheetBehavior<View> behavior;
    private TextView tvSelectedAddress;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        bottomSheet = findViewById(R.id.bottom_sheet_picker);
        behavior = BottomSheetBehavior.from(bottomSheet);
        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        btnConfirm = findViewById(R.id.btn_confirm_destination);

        // Initially hide the drawer or set to small peek
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.setHideable(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_picker_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("DEST_LAT", selectedLatLng.latitude);
                resultIntent.putExtra("DEST_LNG", selectedLatLng.longitude);
                resultIntent.putExtra("DEST_NAME", selectedAddressName);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-1.286389, 36.817223), 13f));

        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            if (selectionMarker != null) {
                selectionMarker.remove();
            }
            selectionMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            
            updateAddressName(latLng);
            
            if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    private void updateAddressName(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                selectedAddressName = addresses.get(0).getAddressLine(0);
            } else {
                selectedAddressName = String.format(Locale.getDefault(), "%.5f, %.5f", latLng.latitude, latLng.longitude);
            }
            tvSelectedAddress.setText(selectedAddressName);
        } catch (IOException e) {
            selectedAddressName = String.format(Locale.getDefault(), "%.5f, %.5f", latLng.latitude, latLng.longitude);
            tvSelectedAddress.setText(selectedAddressName);
        }
    }
}
