package com.example.carpoolclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpoolclient.utils.FireBaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Locale;

public class CreatingTripProgression extends AppCompatActivity {
    private static final String TAG = "CreatingTripProgression";

    private final Gson gson = new Gson();
    private boolean receiverRegistered = false;
    private boolean terminalStateReached = false;

    private ImageView pendingTripIcon;
    private TextView pendingTripText;
    private ImageView vehicleIcon;
    private TextView vehicleText;
    private ImageView geofenceIcon;
    private TextView geofenceText;
    private ImageView neighborhoodIcon;
    private TextView neighborhoodText;
    private ImageView routeIcon;
    private TextView routeText;
    private ImageView finalizationIcon;
    private TextView finalizationText;

    private final BroadcastReceiver tripProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!FireBaseMessaging.ACTION_TRIP_CREATION_EVENT.equals(intent.getAction())) {
                return;
            }

            GlobalContext app = (GlobalContext) context.getApplicationContext();
            drainQueueAndRender(app);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_creating_trip_progression);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindStepViews();
        initializeSteps();
        drainQueueAndRender((GlobalContext) getApplication());

        Button homeButton = findViewById(R.id.btn_home);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainMapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(FireBaseMessaging.ACTION_TRIP_CREATION_EVENT);
            ContextCompat.registerReceiver(this, tripProgressReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(tripProgressReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    private void bindStepViews() {
        pendingTripIcon = findViewById(R.id.icon_pending_trip);
        pendingTripText = findViewById(R.id.text_pending_trip);
        vehicleIcon = findViewById(R.id.icon_vehicle_validation);
        vehicleText = findViewById(R.id.text_vehicle_validation);
        geofenceIcon = findViewById(R.id.icon_geofence_validation);
        geofenceText = findViewById(R.id.text_geofence_validation);
        neighborhoodIcon = findViewById(R.id.icon_neighborhood_resolution);
        neighborhoodText = findViewById(R.id.text_neighborhood_resolution);
        routeIcon = findViewById(R.id.icon_route_resolution);
        routeText = findViewById(R.id.text_route_resolution);
        finalizationIcon = findViewById(R.id.icon_trip_finalization);
        finalizationText = findViewById(R.id.text_trip_finalization);
    }

    private void initializeSteps() {
        setStepState(pendingTripIcon, pendingTripText, getString(R.string.trip_step_pending), null, StepState.PENDING);
        setStepState(vehicleIcon, vehicleText, getString(R.string.trip_step_ownership), null, StepState.PENDING);
        setStepState(geofenceIcon, geofenceText, getString(R.string.trip_step_geofence), null, StepState.PENDING);
        setStepState(neighborhoodIcon, neighborhoodText, getString(R.string.trip_step_neighborhood), null, StepState.PENDING);
        setStepState(routeIcon, routeText, getString(R.string.trip_step_route_resolution), null, StepState.PENDING);
        setStepState(finalizationIcon, finalizationText, getString(R.string.trip_step_finalization), null, StepState.PENDING);
    }

    private void drainQueueAndRender(GlobalContext app) {
        String payload;
        while ((payload = app.dequeue()) != null) {
            renderPayload(payload);
        }
    }

    private void renderPayload(String payload) {
        TripCreationEvent event = parseEvent(payload);
        if (event == null || event.code == null || event.status == null) {
            Log.w(TAG, "Skipping invalid trip creation payload: " + payload);
            return;
        }

        StepTarget target = mapCodeToStep(event.code);
        if (target == null) {
            Log.w(TAG, "Unsupported trip creation code: " + event.code);
            return;
        }

        String normalizedStatus = event.status.trim().toUpperCase(Locale.ROOT);
        boolean isSuccess = "PROGRESS".equals(normalizedStatus) || "SUCCESS".equals(normalizedStatus);
        boolean isFailure = "ERROR".equals(normalizedStatus);

        if (!isSuccess && !isFailure) {
            Log.w(TAG, "Unsupported trip creation status: " + event.status);
            return;
        }

        if (terminalStateReached && isSuccess) {
            Log.i(TAG, "Ignoring success event after terminal state: " + event.code);
            return;
        }

        StepState state = isFailure ? StepState.FAILED : StepState.SUCCESS;
        String detail = resolveDetailMessage(event);

        switch (target) {
            case PENDING_TRIP_CHECK:
                setStepState(pendingTripIcon, pendingTripText, getString(R.string.trip_step_pending), detail, state);
                break;
            case VEHICLE_VALIDATION:
                setStepState(vehicleIcon, vehicleText, getString(R.string.trip_step_ownership), detail, state);
                break;
            case GEOFENCE_VALIDATION:
                setStepState(geofenceIcon, geofenceText, getString(R.string.trip_step_geofence), detail, state);
                break;
            case NEIGHBORHOOD_RESOLUTION:
                setStepState(neighborhoodIcon, neighborhoodText, getString(R.string.trip_step_neighborhood), detail, state);
                break;
            case ROUTE_RESOLUTION:
                setStepState(routeIcon, routeText, getString(R.string.trip_step_route_resolution), detail, state);
                break;
            case FINALIZATION:
                setStepState(finalizationIcon, finalizationText, getString(R.string.trip_step_finalization), detail, state);
                break;
        }

        if (isFailure || target == StepTarget.FINALIZATION) {
            terminalStateReached = true;
        }
    }

    private TripCreationEvent parseEvent(String payload) {
        try {
            return gson.fromJson(payload, TripCreationEvent.class);
        } catch (JsonSyntaxException error) {
            Log.e(TAG, "Failed to parse trip creation payload", error);
            return null;
        }
    }

    private StepTarget mapCodeToStep(String code) {
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        switch (normalizedCode) {
            case "PENDING_TRIP":
                return StepTarget.PENDING_TRIP_CHECK;
            case "VEHICLE_VALIDATED":
            case "VEHICLE_VALIDATION_FAILED":
                return StepTarget.VEHICLE_VALIDATION;
            case "GEOFENCE_CHECK_SUCESSFULL":
            case "GEOFENCE_CHECK_FAILED":
                return StepTarget.GEOFENCE_VALIDATION;
            case "ORIGIN_DESTINATION_NEIGHBORHOOD_RESOLUTION_COMPLETE":
                return StepTarget.NEIGHBORHOOD_RESOLUTION;
            case "ROUTE_POLYLINE_RESOLUTION_COMPLETE":
                return StepTarget.ROUTE_RESOLUTION;
            case "TRIP_CREATED":
            case "ACTIVE_TRIP_ALREADY_EXISTS":
                return StepTarget.FINALIZATION;
            default:
                return null;
        }
    }

    private String resolveDetailMessage(TripCreationEvent event) {
        if ("ROUTE_POLYLINE_RESOLUTION_COMPLETE".equalsIgnoreCase(event.code)) {
            return getString(R.string.trip_step_route_computed);
        }
        if ("TRIP_CREATED".equalsIgnoreCase(event.code) && event.tripId != null && !event.tripId.trim().isEmpty()) {
            return getString(R.string.trip_step_created_with_id, event.tripId);
        }
        if (event.message != null && !event.message.trim().isEmpty()) {
            return event.message.trim();
        }
        return getString(R.string.trip_status_update_received);
    }

    private void setStepState(ImageView iconView, TextView textView, String label, String detail, StepState state) {
        if (state == StepState.SUCCESS) {
            iconView.setImageResource(R.drawable.ic_status_done);
            iconView.setContentDescription(getString(R.string.trip_status_done));
        } else if (state == StepState.FAILED) {
            iconView.setImageResource(R.drawable.ic_status_failed);
            iconView.setContentDescription(getString(R.string.trip_status_failed));
        } else {
            iconView.setImageResource(android.R.drawable.presence_invisible);
            iconView.setContentDescription(getString(R.string.trip_status_pending));
        }

        if (detail == null || detail.trim().isEmpty()) {
            textView.setText(label);
            return;
        }
        textView.setText(getString(R.string.trip_step_with_detail, label, detail));
    }

    private enum StepState {
        PENDING,
        SUCCESS,
        FAILED
    }

    private enum StepTarget {
        PENDING_TRIP_CHECK,
        VEHICLE_VALIDATION,
        GEOFENCE_VALIDATION,
        NEIGHBORHOOD_RESOLUTION,
        ROUTE_RESOLUTION,
        FINALIZATION
    }

    private static class TripCreationEvent {
        String status;
        String code;
        String message;
        String tripId;
    }
}
