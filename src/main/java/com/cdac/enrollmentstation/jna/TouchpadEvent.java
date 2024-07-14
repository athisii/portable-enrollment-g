package com.cdac.enrollmentstation.jna;

import com.sun.jna.Structure;

import java.util.List;

/**
 * @author athisii
 * @version 1.0
 * @since 7/14/24
 */

public class TouchpadEvent extends Structure implements Structure.ByReference {
    public int x = -1;
    public int y = -1;
    public int code = -1;
    public int touch = -1;

    @Override
    protected List<String> getFieldOrder() {
        return List.of("x", "y", "code", "touch");
    }
}
