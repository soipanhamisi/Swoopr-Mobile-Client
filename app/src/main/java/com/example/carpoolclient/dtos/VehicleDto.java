package com.example.carpoolclient.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDto {
    private String regNo;
    private String desc;

    @Override
    public String toString() {
        return regNo + " - " + desc;
    }
}
