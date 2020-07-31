package com.yantriks.urbandatacomparator.model.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class YantriksLocation {

    private YantriksLocationKey locationKey;
    private List<YantriksError> errors;
}
