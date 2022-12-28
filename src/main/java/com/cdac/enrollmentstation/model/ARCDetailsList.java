/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author K. Karthikeyan
 */

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonPropertyOrder({"ErrorCode", "Desc", "arcDetails"})
public class ARCDetailsList {
    @JsonProperty("ErrorCode")
    Integer errorCode;

    @JsonProperty("Desc")
    String desc;

    @JsonProperty("arcDetails")
    List<ARCDetails> arcDetails;

    @JsonIgnore
    Map<String, Object> additionalProperties = new HashMap<>();

}