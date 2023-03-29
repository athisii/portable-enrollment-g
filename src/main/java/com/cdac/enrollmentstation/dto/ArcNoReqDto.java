package com.cdac.enrollmentstation.dto;/*
 * @author athisii, CDAC
 * Created on 02/12/22
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArcNoReqDto {
    @JsonProperty("ARCNo")
    String arcNo;
}
