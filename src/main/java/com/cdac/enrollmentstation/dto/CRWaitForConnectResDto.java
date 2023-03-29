/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * @author root
 */
@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CRWaitForConnectResDto {
    @JsonProperty("retval")
    int retVal;

    @JsonProperty("handle")
    int handle;

    @JsonProperty("csn")
    String csn;

    @JsonProperty("csnLength")
    int csnLength;

}
