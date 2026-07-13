package com.example.carpoolclient.dtos;

public class RegisterVehicleRequest {
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private int seatingCapacity;
    private String color;

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public int getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(int seatingCapacity) { this.seatingCapacity = seatingCapacity; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
