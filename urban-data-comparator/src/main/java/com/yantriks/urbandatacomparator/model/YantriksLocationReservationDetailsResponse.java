package com.yantriks.urbandatacomparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class YantriksLocationReservationDetailsResponse {
    private String locationId;
    private String locationType;
    private List<YantriksReservationDemandTypeResponse> demands;
}
