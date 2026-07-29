package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripData {
    private int capacity;
    private String departureTime;
    private VehicleDto vehicle;
    private OriginDestinationCoordinates originDestinationCoordinates;

    @Getter
    @Setter
    public static class OriginDestinationCoordinates {
        private double originLongitude;
        private double originLatitude;
        private double destinationLongitude;
        private double destinationLatitude;
    }
}
