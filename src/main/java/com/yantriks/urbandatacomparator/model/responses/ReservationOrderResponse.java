package com.yantriks.urbandatacomparator.model.responses;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReservationOrderResponse {

    private String orgId;

    private LocalDateTime expirationTime;

    private TimeUnit expirationTimeUnit;

    private String orderId;

    private String orderType;

    private String sellingChannel;

    private List<ReservationOrderLineResponse> lineReservationDetails;

    private Set<ReservationLineErrorResponse> failedLines;

    Map<String, Object> unknownFields = new HashMap<>();

    private ZonedDateTime updateTime;
    private String updateUser;


    @JsonAnyGetter
    public Map<String, Object> otherFields() {
        return unknownFields;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        unknownFields.put(name, value);
    }
}
