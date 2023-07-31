package com.cdac.enrollmentstation.model;

import com.cdac.enrollmentstation.dto.ArcDetail;
import com.cdac.enrollmentstation.dto.SaveEnrollmentDetail;
import com.mantra.midirisenroll.MIDIrisEnroll;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * @author HP
 */
@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArcDetailsHolder {
    ArcDetail arcDetail;

    SaveEnrollmentDetail saveEnrollmentDetail;

    MIDIrisEnroll mIDIrisEnroll;

    private static final ArcDetailsHolder ARCHolder = new ArcDetailsHolder();

    public static ArcDetailsHolder getArcDetailsHolder() {
        return ARCHolder;
    }

}


