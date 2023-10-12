package com.cdac.enrollmentstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class CRVerifyPinReqDto {
    @JsonProperty("handle")
    int handle;
    @JsonProperty("pin")
    String pin;
    @JsonProperty("pinlen")
    int pinLen;
}
