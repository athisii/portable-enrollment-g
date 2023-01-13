/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.model;

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
//@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Units {
    @JsonProperty("Caption")
    String caption;

    @JsonProperty("Value")
    String value;

    @Override
    public String toString() {
        return caption;
    }
}
