package com.cdac.enrollmentstation.constant;/*
 * @author athisii, CDAC
 * Created on 02/12/22
 */

public class ApplicationConstant {
    //Suppress default constructor for noninstantiability
    private ApplicationConstant() {
        throw new AssertionError("The ApplicationConstant fields should be accessed statically");
    }

    public static final String DEFAULT_PROPERTY_FILE = "/etc/file.properties";
    public static final String INTERNAL = "Internal";
    public static final String EXTERNAL = "External";
    public static final String GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again.";
    public static final String JSON_READ_ERROR_MESSAGE = "Error occurred while reading json data.";
    public static final String JSON_WRITE_ERROR_MESSAGE = "Error occurred while writing as JSON string.";
    public static final String INVALID_CREDENTIALS = "Invalid credentials.";

}
