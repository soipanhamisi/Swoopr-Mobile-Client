package com.example.carpoolclient.dtos;

public class UserDto {
    private String fullName;
    private String email;
    private String role;
    private String messagingToken;

    public UserDto(String fullName, String email, String role, String messagingToken) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.messagingToken = messagingToken;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessagingToken() {
        return messagingToken;
    }

    public void setMessagingToken(String messagingToken) {
        this.messagingToken = messagingToken;
    }
}
