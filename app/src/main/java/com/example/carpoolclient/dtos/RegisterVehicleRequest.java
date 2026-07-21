package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVehicleRequest {
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private int seatingCapacity;
    private String color;
}
