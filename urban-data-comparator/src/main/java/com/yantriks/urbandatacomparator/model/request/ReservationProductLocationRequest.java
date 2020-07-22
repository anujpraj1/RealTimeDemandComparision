package com.yantriks.urbandatacomparator.model.request;

import com.yantriks.urbandatacomparator.model.request.ReservationDemandTypeRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ReservationProductLocationRequest {
    private String locationId;
    private String locationType;
    private List<ReservationDemandTypeRequest> demands;
}
