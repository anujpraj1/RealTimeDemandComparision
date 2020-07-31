package com.yantriks.urbandatacomparator.model.responses;

import com.yantriks.urbandatacomparator.model.responses.ReservationDemandTypeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ReservationProductLocationResponse {
    private String locationId;
    private String locationType;
    private List<ReservationDemandTypeResponse> demands;
}
