package com.cdac.enrollmentstation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * @author HP
 */

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ARCDetails {
    @JsonProperty("arcNo")
    String arcNo;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Rank")
    String rank;

    @JsonProperty("ApplicantID")
    String applicantID;

    @JsonProperty("Unit")
    String unit;

    @JsonProperty("Fingers")
    List<String> fingers;

    @JsonProperty("Iris")
    List<String> iris;

    @JsonProperty("DetailLink")
    String detailLink;

    @JsonProperty("ArcStatus")
    String arcStatus;

    @JsonProperty("ErrorCode")
    String errorCode;

    @JsonProperty("Desc")
    String desc;

    @JsonProperty("EmailID")
    String emailId;

    //Added For Photo/Biometric
    @JsonProperty("BiometricOptions")
    String biometricOptions;

}
