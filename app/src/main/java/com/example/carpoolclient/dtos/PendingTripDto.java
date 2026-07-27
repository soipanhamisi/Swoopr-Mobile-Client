package com.example.carpoolclient.dtos;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingTripDto {
    private List<String> carpoolMemberNames;
    private TripData tripData;
}
