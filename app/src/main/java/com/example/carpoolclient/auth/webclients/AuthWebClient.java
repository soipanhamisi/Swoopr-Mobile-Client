package com.example.carpoolclient.auth.webclients;

import okhttp3.OkHttpClient;

public class AuthWebClient {
    private final String BASE_URL = "https://swooprserver-373496068484.europe-west1.run.app/auth";
    private final OkHttpClient httpClient = new OkHttpClient();

    public void sendEmailVerification(String email) {
        String uri = "/getOtp";
        String url = BASE_URL + uri;



    }
}
