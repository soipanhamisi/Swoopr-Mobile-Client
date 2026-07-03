package com.example.carpoolclient.tripManagement;

import androidx.annotation.NonNull;

import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.google.gson.Gson;

import java.io.IOException;
import java.time.LocalDateTime;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RideSeekerService {
    private static final String BASE_URL = "https://swooprserver-373496068484.europe-west1.run.app/trips";
    private final SecureTokenStore secureTokenStore;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final MediaType MEDIA_TYPE = MediaType.get("application/json");
    private final Gson GSON = new Gson();

    public RideSeekerService(SecureTokenStore secureTokenStore) {
        this.secureTokenStore = secureTokenStore;
    }

    private String getJwt() {
        if (secureTokenStore == null) {
            return null;
        }
        return secureTokenStore.getJwtToken();
    }

    public interface TripManagementCallback {
        void responseHandler(boolean status, String message);
    }

    private void executeRequest(Request request, TripManagementCallback callback){
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.responseHandler(false, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try(Response res = response){
                    String responseBody = res.body() != null ? res.body().string() : "";
                    if (res.isSuccessful()) {
                        callback.responseHandler(true, responseBody);
                        return;
                    }

                    callback.responseHandler(false, responseBody.isEmpty() ? "Error: " + res.code() : responseBody);
                }
            }
        });
    }

    public void joinCarpool(LocalDateTime departureTime,
                            OriginDestinationCoordinates originDestinationCoordinates,
                            TripManagementCallback callback){
        String jwt = getJwt();

        JoinCarpoolRequest payload = new JoinCarpoolRequest();
        payload.setDepartureTime(departureTime.toString());
        payload.setRsOriginDestination(originDestinationCoordinates);

        Request request = new Request.Builder()
                .url(BASE_URL + "/joinCarPool")
                .header("jwt", jwt)
                .post(RequestBody.create(GSON.toJson(payload), MEDIA_TYPE))
                .build();

        executeRequest(request, callback);
    }

    private static final class JoinCarpoolRequest {
        private String departureTime;
        private OriginDestinationCoordinates rsOriginDestination;

        public void setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
        }

        public void setRsOriginDestination(OriginDestinationCoordinates rsOriginDestination) {
            this.rsOriginDestination = rsOriginDestination;
        }
    }
}
