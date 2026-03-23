package com.finder.letscheck.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private String type; // Always "Point"
    private double[] coordinates; // [longitude, latitude]
}