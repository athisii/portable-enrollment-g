package com.cdac.enrollmentstation.jna;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author athisii
 * @version 1.0
 * @since 7/12/24
 */

public interface NativeUtil {

    static Path loadNativeLibraryFromJar(String libName) throws IOException {
        // Load the .so file from the JAR into a temporary directory
        try (InputStream inputStream = NativeUtil.class.getResourceAsStream("/" + libName)) {
            if (inputStream == null) {
                throw new IOException("Native library not found in JAR: " + libName);
            }
            Path tempFile = Files.createTempFile(libName, "");
            tempFile.toFile().deleteOnExit();

            try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return tempFile;
        }
    }
}
