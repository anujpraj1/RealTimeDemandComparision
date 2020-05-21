package com.yantriks.urbandatacomparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class YantriksReservationDemandTypeResponse {
    private String demandType;
    private String quantity;
    private String reservationDate;
    private String segment;
}
