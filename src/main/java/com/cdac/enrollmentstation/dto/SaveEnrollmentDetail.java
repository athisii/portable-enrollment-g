package com.cdac.enrollmentstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.Set;

/**
 * @author root
 */
@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveEnrollmentDetail {
    @JsonProperty("ARCNo")
    String arcNo;

    @JsonProperty("Photo")
    String photo;

    @JsonProperty("PhotoCompressed")
    String photoCompressed;

    @JsonProperty("EnrollmentStationID")
    String enrollmentStationId;

    @JsonProperty("EnrollmentStationUnitID")
    String enrollmentStationUnitId;

    @JsonProperty("EnrollmentStatus")
    String enrollmentStatus;

    @JsonProperty("EnrollmentDate")
    String enrollmentDate;

    @JsonProperty("FP")
    Set<Fp> fp;

    @JsonProperty("IRIS")
    Set<Iris> iris;

    @JsonProperty("IRISScannerSerailNo") // incorrect name from API
    String irisScannerSerialNo;

    @JsonProperty("LeftFPScannerSerailNo")
    String leftFrScannerSerialNo;

    @JsonProperty("RightFPScannerSerailNo")
    String rightFpScannerSerialNo;

    @JsonProperty("UniqueID")
    String uniqueId;

    @JsonProperty("ARCStatus")
    String arcStatus;

    @JsonProperty("BiometricOptions")
    String biometricOptions;
}
