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
public class YantriksReservationRequest {

    private int expirationTime;
    private String expirationTimeUnit;
    private String orderId;
    private String orgId;
    private String updateTime;
    private String updateUser;
    private String orderType;
    private List<YantriksLineReservationDetailsRequest> lineReservationDetails;
}
