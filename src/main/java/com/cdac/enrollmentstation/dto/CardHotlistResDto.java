package com.cdac.enrollmentstation.dto;

import com.cdac.enrollmentstation.model.CardHotlistDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
public class CardHotlistResDto {
    @JsonProperty("cardHotlistDetails")
    List<CardHotlistDetail> cardHotlistDetails;
}
