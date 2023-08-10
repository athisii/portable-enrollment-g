package com.cdac.enrollmentstation.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * @author root
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class CardFp {
    String position;
    byte[] image;
}
