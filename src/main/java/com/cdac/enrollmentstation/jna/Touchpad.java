package com.cdac.enrollmentstation.jna;

import com.cdac.enrollmentstation.exception.GenericException;
import com.sun.jna.Pointer;

/**
 * @author athisii
 * @version 1.0
 * @since 7/14/24
 */

public class Touchpad {
    private Pointer touchpadInstance;

    public Touchpad(String devicePath) {
        this.touchpadInstance = TouchpadLib.INSTANCE.initDevice(devicePath);
        if (TouchpadLib.INSTANCE.isInitialized(touchpadInstance) != 0) {
            throw new GenericException("Touchpad initialization failed with code");
        }
    }

    public TouchpadEvent getNextEvent() {
        return TouchpadLib.INSTANCE.getNextEvent(touchpadInstance);
    }

    public void closeDevice() {
        TouchpadLib.INSTANCE.closeDevice(touchpadInstance);
    }

    public boolean isInitialized() {
        return TouchpadLib.INSTANCE.isInitialized(touchpadInstance) == 0;
    }

    public boolean isClosed() {
        return TouchpadLib.INSTANCE.isClosed(touchpadInstance) == 0;
    }

    public String getDeviceName() {
        return TouchpadLib.INSTANCE.getDeviceName(touchpadInstance);
    }

    public TouchpadInfo getDeviceInfo() {
        return TouchpadLib.INSTANCE.getDeviceInfo(touchpadInstance);
    }
}

