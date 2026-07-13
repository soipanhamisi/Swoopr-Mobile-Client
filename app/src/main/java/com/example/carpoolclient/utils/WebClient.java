package com.example.carpoolclient.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * A generic WebClient for interacting with the Swoopr API.
 * Handles authentication headers, JSON serialization/deserialization, and standard response formats.
 */
public class WebClient {
    private static final String BASE_URL = "https://swooprserver-373496068484.europe-west1.run.app";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    private final SecureTokenStore tokenStore;
    private final Handler mainHandler;

    public WebClient(Context context) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.tokenStore = SecureTokenStore.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for API requests.
     * @param <T> The type of the 'data' field in the ApiResponse.
     */
    public interface WebCallback<T> {
        void onResult(boolean success, String message, T data);
    }

    /**
     * Sends a POST request and expects a specific data type in response.
     * Can handle both standard ApiResponse wrapped data and raw JSON (like Lists).
     */
    public <T> void post(String endpoint, Object requestData, Type responseType, boolean expectApiResponse, WebCallback<T> callback) {
        String jsonPayload = (requestData != null) ? gson.toJson(requestData) : "{}";

        RequestBody body = RequestBody.create(jsonPayload, JSON);

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body);

        addAuthHeader(builder);
        execute(builder.build(), responseType, expectApiResponse, callback);
    }

    public <T> void post(String endpoint, Object requestData, Class<T> responseDataType, WebCallback<T> callback) {
        post(endpoint, requestData, (Type) responseDataType, true, callback);
    }

    public <T> void post(String endpoint, Class<T> responseDataType, WebCallback<T> callback) {
        post(endpoint, null, (Type) responseDataType, true, callback);
    }

    /**
     * Sends a GET request to the specified endpoint.
     */
    public <T> void get(String endpoint, Class<T> responseDataType, WebCallback<T> callback) {
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get();

        addAuthHeader(builder);
        execute(builder.build(), responseDataType, true, callback);
    }

    private void addAuthHeader(Request.Builder builder) {
        String token = tokenStore.getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private <T> void execute(Request request, Type responseType, boolean expectApiResponse, WebCallback<T> callback) {
        execute(request, responseType, expectApiResponse, callback, false);
    }

    private <T> void execute(Request request, Type responseType, boolean expectApiResponse, WebCallback<T> callback, boolean isRetry) {
        // Log Outbound Request with Headers
        try {
            StringBuilder outLog = new StringBuilder();
            outLog.append("--> ").append(request.method()).append(" ").append(request.url()).append("\n");
            for (String name : request.headers().names()) {
                outLog.append(name).append(": ").append(request.header(name)).append("\n");
            }
            if (request.body() != null) {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String body = buffer.readUtf8();
                if (!body.isEmpty()) {
                    try {
                        String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(
                                JsonParser.parseString(body)
                        );
                        outLog.append("\n").append(prettyJson);
                    } catch (Exception e) {
                        outLog.append("\n").append(body);
                    }
                }
            }
            android.util.Log.d("outbound_json", outLog.toString());
        } catch (Exception e) {
            android.util.Log.e("outbound_json", "Logging failed", e);
        }

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                android.util.Log.e("WebClient", "Request failed: " + e.getMessage());
                notifyResult(callback, false, "Network error: " + e.getMessage(), null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    // Check for new JWT in Authorization header
                    String authHeader = res.header("Authorization");
                    if (authHeader != null) {
                        String newToken = null;
                        if (authHeader.startsWith("Bearer ")) {
                            newToken = authHeader.substring(7);
                        } else if (!authHeader.isEmpty()) {
                            newToken = authHeader;
                        }

                        if (newToken != null) {
                            tokenStore.saveJwtToken(newToken);
                        }
                    }

                    String bodyString = res.body() != null ? res.body().string() : "";
                    
                    // Log Inbound Response with Headers
                    try {
                        StringBuilder inLog = new StringBuilder();
                        inLog.append("<-- ").append(res.code()).append(" ").append(request.method()).append(" ").append(request.url().encodedPath()).append("\n");
                        for (String name : res.headers().names()) {
                            inLog.append(name).append(": ").append(res.header(name)).append("\n");
                        }
                        if (!bodyString.isEmpty()) {
                            try {
                                String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(
                                        JsonParser.parseString(bodyString)
                                );
                                inLog.append("\n").append(prettyJson);
                            } catch (Exception e) {
                                inLog.append("\n").append(bodyString);
                            }
                        }
                        android.util.Log.d("inbound_json", inLog.toString());
                    } catch (Exception e) {
                        android.util.Log.e("inbound_json", "Logging failed", e);
                    }

                    // Automatic token refresh on 403 Forbidden with token-related errors
                    if (res.code() == 403 && !isRetry && !request.url().encodedPath().contains("/auth/refreshToken")) {
                        if (isTokenError(bodyString)) {
                            android.util.Log.d("WebClient", "Token expired/invalid. Attempting transparent refresh...");
                            performTokenRefreshAndRetry(request, responseType, expectApiResponse, callback);
                            return;
                        }
                    }

                    if (bodyString.isEmpty()) {
                        notifyResult(callback, res.isSuccessful(), res.isSuccessful() ? "Success" : "Server error: " + res.code(), null);
                        return;
                    }

                    try {
                        if (expectApiResponse) {
                            Type apiResponseType = TypeToken.getParameterized(ApiResponse.class, responseType).getType();
                            ApiResponse<T> apiResponse = gson.fromJson(bodyString, apiResponseType);

                            if (apiResponse != null) {
                                notifyResult(callback, apiResponse.isSuccess(),
                                        apiResponse.getMessage() != null ? apiResponse.getMessage() : (apiResponse.isSuccess() ? "Success" : "Error"),
                                        apiResponse.getData());
                            } else {
                                notifyResult(callback, false, "Empty API response", null);
                            }
                        } else {
                            // Raw response (e.g., a List)
                            T data = gson.fromJson(bodyString, responseType);
                            notifyResult(callback, res.isSuccessful(), res.isSuccessful() ? "Success" : "Error", data);
                        }
                    } catch (Exception e) {
                        if (!res.isSuccessful()) {
                            notifyResult(callback, false, bodyString, null);
                        } else {
                            notifyResult(callback, false, "Parsing error: " + e.getMessage(), null);
                        }
                    }
                }
            }
        });
    }

    private boolean isTokenError(String bodyString) {
        try {
            ApiResponse<?> apiResponse = gson.fromJson(bodyString, ApiResponse.class);
            if (apiResponse != null && !apiResponse.isSuccess() && apiResponse.getMessage() != null) {
                String msg = apiResponse.getMessage().toLowerCase();
                return msg.contains("token") || msg.contains("expired") || msg.contains("unauthorized");
            }
        } catch (Exception ignored) {}
        return false;
    }

    private <T> void performTokenRefreshAndRetry(Request originalRequest, Type responseType, boolean expectApiResponse, WebCallback<T> callback) {
        post("/auth/refreshToken", null, Void.class, (success, message, data) -> {
            if (success) {
                android.util.Log.d("WebClient", "Token refreshed successfully. Retrying original request...");
                Request.Builder builder = originalRequest.newBuilder();
                addAuthHeader(builder);
                execute(builder.build(), responseType, expectApiResponse, callback, true);
            } else {
                android.util.Log.e("WebClient", "Transparent token refresh failed: " + message);
                notifyResult(callback, false, "Your session has expired. Please log in again.", null);
            }
        });
    }


    private <T> void notifyResult(WebCallback<T> callback, boolean success, String message, T data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(success, message, data));
        }
    }
}
