package com.cdac.enrollmentstation.jna;

import com.sun.jna.Structure;

import java.util.List;

/**
 * @author athisii
 * @version 1.0
 * @since 7/14/24
 */

public class TouchpadInfo extends Structure implements Structure.ByReference {
    public int width = -1;
    public int height = -1;

    @Override
    protected List<String> getFieldOrder() {
        return List.of("width", "height");
    }
}
