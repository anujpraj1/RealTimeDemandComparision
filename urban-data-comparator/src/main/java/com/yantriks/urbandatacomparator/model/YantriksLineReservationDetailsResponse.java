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
public class YantriksLineReservationDetailsResponse {

    private String fulfillmentService;
    private String fulfillmentType;
    private String lineId;
    private String productId;
    private String uom;
    private List<YantriksLocationReservationDetailsResponse> locationReservationDetails;

}
