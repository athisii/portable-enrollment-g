package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.SaveEnrollmentDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 20/03/23
 */
public class SaveEnrollmentDetailsUtil {
    private static final Logger LOGGER = ApplicationLog.getLogger(SaveEnrollmentDetailsUtil.class);

    //Suppress default constructor for noninstantiability
    private SaveEnrollmentDetailsUtil() {
        throw new AssertionError("The SaveEnrollmentUtil methods should be accessed statically");
    }

    public static void writeToFile(SaveEnrollmentDetails saveEnrollmentDetails) {
        Path path = getFilePath();
        try {
            String saveEnrollmentDetailsString = Singleton.getObjectMapper().writeValueAsString(saveEnrollmentDetails);
            Files.writeString(path, saveEnrollmentDetailsString, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new GenericException(ex.getMessage());
        }

    }

    private static Path getFilePath() {
        String saveEnrollmentFileString = PropertyFile.getProperty(PropertyName.SAVE_ENROLLMENT);
        if (saveEnrollmentFileString == null || saveEnrollmentFileString.isBlank()) {
            throw new GenericException("'save.enrollment' entry not found or is empty in" + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        return Path.of(saveEnrollmentFileString);
    }

    public static SaveEnrollmentDetails readFromFile() {
        Path path = getFilePath();
        if (!Files.exists(path)) {
            throw new GenericException("saveEnrollment.txt file not found.");
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return Singleton.getObjectMapper().readValue(content, SaveEnrollmentDetails.class);
        } catch (IOException ex) {
            throw new GenericException(ex.getMessage());
        }
    }

    public static void delete() {
        Path path = getFilePath();
        try {
            Files.delete(path);
        } catch (IOException ex) {
            throw new GenericException((ex.getMessage()));
        }
    }

}
