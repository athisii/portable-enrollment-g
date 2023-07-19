package com.cdac.enrollmentstation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * @author K. Karthikeyan
 */

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonPropertyOrder({"ErrorCode", "Desc", "arcDetails"})
public class ArcDetailsList {
    @JsonProperty("ErrorCode")
    int errorCode;

    @JsonProperty("Desc")
    String desc;

    @JsonProperty("arcDetails")
    List<ArcDetails> arcDetails;

}