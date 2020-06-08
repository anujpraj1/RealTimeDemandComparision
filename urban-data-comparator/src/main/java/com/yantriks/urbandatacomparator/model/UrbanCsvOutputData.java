package com.yantriks.urbandatacomparator.model;

import com.datastax.driver.mapping.annotations.Transient;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
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
