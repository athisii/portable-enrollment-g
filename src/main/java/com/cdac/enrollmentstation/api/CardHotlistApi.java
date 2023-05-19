package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.CardHotlistResDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardHotlistDetail;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 18/05/23
 */
public class CardHotlistApi {

    private static final Logger LOGGER = ApplicationLog.getLogger(CardHotlistApi.class);

    //Suppress default constructor for noninstantiability
    private CardHotlistApi() {
        throw new AssertionError("The CardHotlistApi methods must be accessed statically.");
    }

    /**
     * Fetches all hotlisted card details.
     * Caller must handle the exception.
     *
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static List<CardHotlistDetail> fetchHotlistedCard() {
        String cardHotlistUrl = PropertyFile.getProperty(PropertyName.CARD_API_HOTLISTED_URL);
        if (cardHotlistUrl == null || cardHotlistUrl.isBlank()) {
            throw new GenericException("'" + PropertyName.CARD_API_HOTLISTED_URL + "' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createGetHttpRequest(cardHotlistUrl));
        // connection timeout
        if (response == null) {
            return null;
        }

        CardHotlistResDto cardHotlistResDto;
        try {
            cardHotlistResDto = Singleton.getObjectMapper().readValue(response.body(), CardHotlistResDto.class);
            return Singleton.getObjectMapper().readValue(cardHotlistResDto.getCardHotlistDetails(), new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }
}
