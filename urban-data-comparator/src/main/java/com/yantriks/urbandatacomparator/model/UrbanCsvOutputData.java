package com.yantriks.urbandatacomparator.model;

import com.datastax.driver.mapping.annotations.Transient;
import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
public class UrbanCsvOutputData {

    private String extnReservationId;

    private String orderId;

    private String enterpriseCode;

    @Transient
    private boolean isCompareAndGenerate;

    private String reservationStatus;

    private long reservationResponseCode;

    private String error;

    private String message;
}
