package com.example.certbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QrPlacementDto {
    private int pageIndex;
    private String coordinateSpace;
    private double x;
    private double y;
    private double size;
}
