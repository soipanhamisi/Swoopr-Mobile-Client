package com.example.carpoolclient.dtos;

public class VehicleDto {
    private String regNo;
    private String desc;

    public VehicleDto() {}

    public VehicleDto(String regNo, String desc) {
        this.regNo = regNo;
        this.desc = desc;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return regNo + " - " + desc;
    }
}
