package com.yantriks.urbandatacomparator.model.responses;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yantriks.urbandatacomparator.model.YantriksLineReservationDetailsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class YantriksAvailabilityErrorResponse {

    private String orgId;
    private String path;
    private List<YantriksErrorLineResponse> errorLines;
    private String error;
    private String message;
    private long status;
    private String timestamp;
    private String updateTime;
    private String updateUser;

    Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> otherFields() {
        return unknownFields;
    }

    @JsonAnySetter
    public void setOtherField(String name, Object value) {
        unknownFields.put(name, value);
    }

}
