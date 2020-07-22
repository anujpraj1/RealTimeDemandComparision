package com.yantriks.urbandatacomparator.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationIdAndType {
    private String locationId;
    private String locationType;
}