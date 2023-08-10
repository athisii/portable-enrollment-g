package com.cdac.enrollmentstation.model;

import com.cdac.enrollmentstation.dto.SaveEnrollmentDetail;
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
public class EnrollmentDetailsHolder {
    SaveEnrollmentDetail saveEnrollmentDetail;
    private static final EnrollmentDetailsHolder enrollmentDetailsHolder = new EnrollmentDetailsHolder();

    public static EnrollmentDetailsHolder getEnrollmentDetails() {
        return enrollmentDetailsHolder;
    }

}
