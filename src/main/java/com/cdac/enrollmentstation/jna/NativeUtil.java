package com.cdac.enrollmentstation.jna;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author athisii
 * @version 1.0
 * @since 7/12/24
 */

public interface NativeUtil {

    // temporarily loading .so from jar not supported in Boss10, but works in Ubuntu.
    // so writing .so file instead to /usr/lib/ on each startup. (just override)
    static void writeSoFromJarToSystem(String libName) throws IOException {
        // Load the .so file from the JAR into a temporary directory
        try (InputStream inputStream = NativeUtil.class.getResourceAsStream("/" + libName)) {
            if (inputStream == null) {
                throw new IOException("Native library not found in JAR: " + libName);
            }
            Path path = Paths.get("/usr/lib/" + libName);
            try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    }
}
