package com.cdac.enrollmentstation.jna;

import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface TouchpadLib extends Library {
    Logger LOGGER = ApplicationLog.getLogger(TouchpadLib.class);
    TouchpadLib INSTANCE = loadLibrary();

    static TouchpadLib loadLibrary() {
        try {
            NativeUtil.writeSoFromJarToSystem("touchpad.so");
            return (TouchpadLib) Native.loadLibrary("touchpad.so", TouchpadLib.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, () -> "Error loading touchpad so " + e.getMessage());
            throw new GenericException("Failed to load native library");
        }
    }

    int getDontCareCode();

    int getTouchCode();

    int getResetCode();

    int getXCode();

    int getYCode();

    int isTouched();

    int isInitialized(Pointer pointer);

    int isClosed(Pointer pointer);

    void closeDevice(Pointer pointer);

    String getDeviceName(Pointer pointer);

    Pointer initDevice(String devicePath);

    TouchpadInfo getDeviceInfo(Pointer pointer);

    TouchpadEvent getNextEvent(Pointer pointer);
}
