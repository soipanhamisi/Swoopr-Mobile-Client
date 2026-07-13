package com.example.carpoolclient.dtos;

public class JoinCarpoolDto {
    private String departureTime;
    private RsOriginDestination rsOriginDestination;

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public RsOriginDestination getRsOriginDestination() {
        return rsOriginDestination;
    }

    public void setRsOriginDestination(RsOriginDestination rsOriginDestination) {
        this.rsOriginDestination = rsOriginDestination;
    }

    public static class RsOriginDestination {
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
