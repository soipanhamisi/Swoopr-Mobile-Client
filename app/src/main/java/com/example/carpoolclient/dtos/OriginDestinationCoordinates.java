package com.example.carpoolclient.dtos;

public class OriginDestinationCoordinates {
    private Coordinates origin;
    private Coordinates destination;

    public Coordinates getOrigin() {
        return origin;
    }

    public void setOrigin(Coordinates origin) {
        this.origin = origin;
    }

    public Coordinates getDestination() {
        return destination;
    }

    public void setDestination(Coordinates destination) {
        this.destination = destination;
    }
}
