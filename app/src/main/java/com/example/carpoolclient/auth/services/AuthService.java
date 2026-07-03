package com.example.carpoolclient.auth.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.carpoolclient.auth.dtos.*;
import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthService {
    private static final String BASE_URL = "https://swooprserver-373496068484.europe-west1.run.app/auth";
    private static final String DEFAULT_ROLE = "NORMAL_USER";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final long JWT_WAIT_TIMEOUT_MS = 5_000L;
    private static final long JWT_WAIT_INTERVAL_MS = 100L;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS)
            .build();
    private final Gson gson = new Gson();
    private final SecureTokenStore tokenStore;
    private volatile String currentJwtToken;

    public AuthService(Context context) {
        this.tokenStore = SecureTokenStore.getInstance(context);
        this.currentJwtToken = tokenStore.getJwtToken();
    }

    public void refreshToken(String email, AuthCallback callback) {
        Request request = postRequest("/refreshToken", RequestBody.create(email, JSON));
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    String responseBody = res.body() != null ? res.body().string().trim() : "";
                    if (!res.isSuccessful()) {
                        callback.onResult(false, responseBody.isEmpty() ? "Error: " + res.code() : responseBody);
                        return;
                    }

                    String token = responseBody;
                    if (token.isEmpty()) {
                        String authorization = res.header("Authorization");
                        token = authorization != null ? authorization.trim() : "";
                    }

                    if (token.isEmpty()) {
                        callback.onResult(false, "Token not returned by server");
                        return;
                    }

                    currentJwtToken = token;
                    tokenStore.saveJwtToken(token);
                    callback.onResult(true, token);
                }
            }
        });
    }

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

    private interface TokenCallback {
        void onToken(String token);
    }

    public void getOtp(String email, AuthCallback callback) {
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(email);
        String json = gson.toJson(emailDto);
        executeRequest(postRequest("/getOtp", RequestBody.create(json, JSON)), callback);
    }

    public void authenticateUser(String otp, String email, AuthCallback callback) {
        AuthenticateRequest authRequest = new AuthenticateRequest(otp, email);
        String json = gson.toJson(authRequest);
        executeRequest(postRequest("/authenticateUser", RequestBody.create(json, JSON)), callback);
    }

    public void registerUser(RegisterRequest registerRequest, AuthCallback callback) {
        fetchMessagingToken(token -> {
            String fullName = buildFullName(registerRequest);
            saveUser(fullName, registerRequest.getEmail(), DEFAULT_ROLE, callback);
        });
    }

    public void saveUser(String fullName, String email, String role, AuthCallback callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("fullName", fullName);
        payload.addProperty("email", email);
        payload.addProperty("role", role);
        executeRequest(postRequest("/saveUser", RequestBody.create(gson.toJson(payload), JSON)), callback);
    }

    public void submitMessagingToken(String jwt, String messagingToken, AuthCallback callback){
        Request request = new Request.Builder()
                .url(BASE_URL + "/submitMessagingToken")
                .header("Authorization", jwt)
                .post(RequestBody.create(messagingToken, JSON))
                .build();
        executeRequest(request, callback);
    }
    public void getNewToken(String email, String otp, AuthCallback callback) {
        AuthenticateRequest requestBody = new AuthenticateRequest(otp, email);
        String json = gson.toJson(requestBody);
        executeRequest(postRequest("/getNewToken", RequestBody.create(json, JSON)), callback);
    }

    public void sendMessagingToken(AuthCallback callback){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()){
                        Exception exception = task.getException();
                        callback.onResult(false, exception != null ? exception.getMessage() : "Unable to fetch messaging token");
                        return;
                    }
                    String token = task.getResult();

                    waitForJwtToken(jwt -> {
                        if (jwt == null || jwt.trim().isEmpty()) {
                            callback.onResult(false, "JWT token not available");
                            return;
                        }

                        submitMessagingToken(jwt, token, callback);
                    });
                });
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void login(String email, String password, AuthCallback callback) {
        getNewToken(email, password, callback);
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void testEndpoint(String jwt, String message, AuthCallback callback) {
        testEndpoint(jwt, callback);
    }

    public void testEndpoint(String jwt, AuthCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/testEndpoint")
                .header("Authorization", jwt)
                .post(RequestBody.create("", JSON))
                .build();

        executeRequest(request, callback);
    }

    private Request postRequest(String path, RequestBody body) {
        return new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();
    }

    private String buildFullName(RegisterRequest registerRequest) {
        String firstName = registerRequest.getFirstName() != null ? registerRequest.getFirstName().trim() : "";
        String lastName = registerRequest.getLastName() != null ? registerRequest.getLastName().trim() : "";

        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return firstName.isEmpty() ? lastName : firstName;
    }

    private void fetchMessagingToken(TokenCallback tokenCallback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        tokenStore.saveFcmToken(token);
                        tokenCallback.onToken(token);
                        return;
                    }

                    tokenCallback.onToken(null);
                });
    }

    private void waitForJwtToken(TokenCallback tokenCallback) {
        AtomicBoolean delivered = new AtomicBoolean(false);
        long deadline = System.currentTimeMillis() + JWT_WAIT_TIMEOUT_MS;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] poller = new Runnable[1];

        poller[0] = new Runnable() {
            @Override
            public void run() {
                String token = currentJwtToken;
                if (token != null && !token.trim().isEmpty() && delivered.compareAndSet(false, true)) {
                    tokenCallback.onToken(token);
                    return;
                }

                if (System.currentTimeMillis() >= deadline && delivered.compareAndSet(false, true)) {
                    tokenCallback.onToken(currentJwtToken);
                    return;
                }

                handler.postDelayed(this, JWT_WAIT_INTERVAL_MS);
            }
        };

        handler.post(poller[0]);
    }

    private void executeRequest(Request request, AuthCallback callback) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    String responseBody = res.body() != null ? res.body().string() : "";
                    if (res.isSuccessful()) {
                        String authorization = res.header("Authorization");
                        if (authorization != null && !authorization.trim().isEmpty()) {
                            currentJwtToken = authorization;
                            tokenStore.saveJwtToken(authorization);
                        }
                        callback.onResult(true, responseBody);
                    } else {
                        callback.onResult(false, responseBody.isEmpty() ? "Error: " + res.code() : responseBody);
                    }
                }
            }
        });
    }
}
