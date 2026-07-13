package com.example.carpoolclient.dtos;

public class TestRequest {
    private String jwt;
    private String message;

    public TestRequest(String jwt, String message) {
        this.jwt = jwt;
        this.message = message;
    }

    public String getJwt() { return jwt; }
    public void setJwt(String jwt) { this.jwt = jwt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
