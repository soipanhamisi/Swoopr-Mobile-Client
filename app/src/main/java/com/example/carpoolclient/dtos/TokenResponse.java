package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponse {
    private String token;
    private long expiresIn;
    private String tokenType;
}
