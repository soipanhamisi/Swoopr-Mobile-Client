package com.example.carpoolclient.auth.webclients;

import com.example.carpoolclient.auth.dtos.*;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthWebClient {
    private static final String BASE_URL = "https://swooprserver-373496068484.europe-west1.run.app/auth";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

    public void getOtp(String email, AuthCallback callback) {
        EmailDto emailDto = new EmailDto();
        emailDto.setEmail(email);
        String json = gson.toJson(emailDto);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/getOtp")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void authenticateUser(String otp, String email, AuthCallback callback) {
        AuthenticateRequest authRequest = new AuthenticateRequest(otp, email);
        String json = gson.toJson(authRequest);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/authenticateUser")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void registerUser(RegisterRequest registerRequest, AuthCallback callback) {
        String json = gson.toJson(registerRequest);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/registerUser")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void login(String email, String password, AuthCallback callback) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        String json = gson.toJson(loginRequest);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    public void testEndpoint(String jwt, String message, AuthCallback callback) {
        TestRequest testRequest = new TestRequest(jwt, message);
        String json = gson.toJson(testRequest);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/testEndpoint")
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    private void executeRequest(Request request, AuthCallback callback) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onResult(false, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response res = response) {
                    String responseBody = res.body() != null ? res.body().string() : "";
                    if (res.isSuccessful()) {
                        callback.onResult(true, responseBody);
                    } else {
                        callback.onResult(false, responseBody.isEmpty() ? "Error: " + res.code() : responseBody);
                    }
                }
            }
        });
    }
}
