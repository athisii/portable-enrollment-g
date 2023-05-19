package com.cdac.enrollmentstation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * @author athisii, CDAC
 * Created on 18/05/23
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class CardHotlistDetail {
    @JsonProperty("PNo")
    String pNo;
    @JsonProperty("CardNo")
    String cardNo;
}
