package com.yantriks.urbandatacomparator.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"demandType", "reservationDate", "segment"})
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDemandTypeRequest {
    private String demandType;
    private int quantity;
    private String reservationDate;
    private String segment;

    @JsonIgnore
    public String getUniqueKey() {
        return demandType+":"+reservationDate;
    }
}
