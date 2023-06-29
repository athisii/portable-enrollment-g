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
public class CardHotlistDetail {
    @JsonProperty("pno")
    String pNo;
    @JsonProperty("cardStatus")
    boolean cardStatus;
    @JsonProperty("cardNo")
    String cardNo;
    @JsonProperty("roleId")
    String roleId;
    @JsonProperty("roleName")
    String roleName;
}
