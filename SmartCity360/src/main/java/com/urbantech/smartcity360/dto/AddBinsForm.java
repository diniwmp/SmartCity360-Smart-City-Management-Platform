package com.urbantech.smartcity360.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddBinsForm {
    private String template;
    private String zoneId;
    private String quantity;
}