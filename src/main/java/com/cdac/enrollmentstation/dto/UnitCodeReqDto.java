package com.cdac.enrollmentstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * @author athisii, CDAC
 * Created on 07/02/23
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnitCodeReqDto {
    @JsonProperty("unitCode")
    String unitCode;
}
