package com.cdac.enrollmentstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * @author root
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CRApiResDto {
    @JsonProperty("retval")
    int retVal;
}
