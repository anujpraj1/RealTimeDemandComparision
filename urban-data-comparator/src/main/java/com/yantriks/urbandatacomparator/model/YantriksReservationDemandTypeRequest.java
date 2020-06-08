package com.yantriks.urbandatacomparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class YantriksReservationDemandTypeRequest {
    private String demandType;
    private int quantity;
    private String reservationDate;
    private String segment;

    public String getUniqueKey() {
        return demandType+":"+reservationDate;
    }
}
