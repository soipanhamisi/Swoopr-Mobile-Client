package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarpoolRequestDto {
    private String destinationZone;
    private String requestMadeAt;
}
