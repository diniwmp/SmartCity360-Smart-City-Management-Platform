package com.urbantech.smartcity360.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ComplaintForm {
    private String title;
    private String description;
    private String category;
    private String severity;
    private String zoneId;
}