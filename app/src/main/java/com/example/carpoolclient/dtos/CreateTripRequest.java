package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTripRequest {
    private double originLatitude;
    private double originLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private String departureTime;
    private String vehicleId;
    private int costPerPassenger;
    private String notes;
}
