package com.yantriks.urbandatacomparator.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ReservationOrderLineRequest {

    private String lineId;

    private String fulfillmentService;

    private String fulfillmentType;

    private String orderLineRef;

    private String productId;

    private String uom;

    private List<ReservationProductLocationRequest> locationReservationDetails;

    private List<ReservationGtinOrderLineRequest> gtinReservationDetails;

}
