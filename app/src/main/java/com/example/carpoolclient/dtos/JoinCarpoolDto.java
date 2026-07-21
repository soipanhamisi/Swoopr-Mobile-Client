package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinCarpoolDto {
    private String departureTime;
    private RsOriginDestination rsOriginDestination;

    @Getter
    @Setter
    public static class RsOriginDestination {
        private double originLongitude;
        private double originLatitude;
        private double destinationLongitude;
        private double destinationLatitude;
    }
}
