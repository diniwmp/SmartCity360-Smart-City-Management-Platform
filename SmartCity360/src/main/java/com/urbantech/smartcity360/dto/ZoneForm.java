package com.urbantech.smartcity360.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ZoneForm {
    private String parentId;
    private String name;
    private String code;
    private String ratedLoadKw;
    private String maintenanceCost;
}