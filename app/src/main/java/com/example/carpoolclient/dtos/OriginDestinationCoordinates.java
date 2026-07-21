package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OriginDestinationCoordinates {
    private Coordinates origin;
    private Coordinates destination;
}
