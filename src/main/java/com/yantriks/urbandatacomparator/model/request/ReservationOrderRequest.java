package com.yantriks.urbandatacomparator.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ReservationOrderRequest {

    private String orgId;

    private Long expirationTime;

    private TimeUnit expirationTimeUnit;


    private String orderId;

    private String orderType;


    private ZonedDateTime updateTime;
    private String updateUser;


    private List<ReservationOrderLineRequest> lineReservationDetails;
}
