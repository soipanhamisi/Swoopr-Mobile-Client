package com.example.carpoolclient.dtos;

public class CreateTripRequest {
    private double originLatitude;
    private double originLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private String departureTime;
    private String vehicleId;
    private int costPerPassenger;
    private String notes;

    public double getOriginLatitude() { return originLatitude; }
    public void setOriginLatitude(double originLatitude) { this.originLatitude = originLatitude; }
    public double getOriginLongitude() { return originLongitude; }
    public void setOriginLongitude(double originLongitude) { this.originLongitude = originLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(double destinationLatitude) { this.destinationLatitude = destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public int getCostPerPassenger() { return costPerPassenger; }
    public void setCostPerPassenger(int costPerPassenger) { this.costPerPassenger = costPerPassenger; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
