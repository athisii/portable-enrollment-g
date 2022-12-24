/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.model;

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
public class ARCDetailsHolder {
    ARCDetails arcDetails;

    SaveEnrollmentDetails saveEnrollmentDetails;

    MIDIrisEnroll mIDIrisEnroll;

    private static final ARCDetailsHolder ARCHolder = new ARCDetailsHolder();

    public static ARCDetailsHolder getArcDetailsHolder() {
        return ARCHolder;
    }

}


