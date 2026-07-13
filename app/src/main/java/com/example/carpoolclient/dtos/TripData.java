package com.example.carpoolclient.dtos;

public class TripData {
    private int capacity;
    private String departureTime;
    private OriginDestinationCoordinates originDestinationCoordinates;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public OriginDestinationCoordinates getOriginDestinationCoordinates() {
        return originDestinationCoordinates;
    }

    public void setOriginDestinationCoordinates(OriginDestinationCoordinates originDestinationCoordinates) {
        this.originDestinationCoordinates = originDestinationCoordinates;
    }

    public static class OriginDestinationCoordinates {
        private double originLongitude;
        private double originLatitude;
        private double destinationLongitude;
        private double destinationLatitude;

        public double getOriginLongitude() {
            return originLongitude;
        }

        public void setOriginLongitude(double originLongitude) {
            this.originLongitude = originLongitude;
        }

        public double getOriginLatitude() {
            return originLatitude;
        }

        public void setOriginLatitude(double originLatitude) {
            this.originLatitude = originLatitude;
        }

        public double getDestinationLongitude() {
            return destinationLongitude;
        }

        public void setDestinationLongitude(double destinationLongitude) {
            this.destinationLongitude = destinationLongitude;
        }

        public double getDestinationLatitude() {
            return destinationLatitude;
        }

        public void setDestinationLatitude(double destinationLatitude) {
            this.destinationLatitude = destinationLatitude;
        }
    }
}
