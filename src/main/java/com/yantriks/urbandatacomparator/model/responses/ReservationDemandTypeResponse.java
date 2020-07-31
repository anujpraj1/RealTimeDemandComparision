package com.yantriks.urbandatacomparator.model.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"demandType", "reservationDate", "segment"})
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDemandTypeResponse {
    private String demandType;
    private int quantity;
    private String reservationDate;
    private String segment;

    @JsonIgnore
    public String getUniqueKey() {
        return demandType+":"+reservationDate;
    }
}
