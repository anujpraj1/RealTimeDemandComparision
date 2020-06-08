package com.yantriks.urbandatacomparator.model.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yantriks.urbandatacomparator.model.YantriksLineReservationDetailsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class YantriksAvailabilityErrorResponse {

    private String path;
    @JsonIgnore
    private List<YantriksErrorLineResponse> errorLines;
    private String error;
    private String message;
    private long status;
    private String timestamp;
}
