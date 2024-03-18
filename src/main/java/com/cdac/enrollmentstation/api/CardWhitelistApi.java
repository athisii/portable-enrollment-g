package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.CardWhitelistResDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.dto.CardWhitelistDetail;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 18/05/23
 */
public class CardWhitelistApi {

    private static final Logger LOGGER = ApplicationLog.getLogger(CardWhitelistApi.class);

    //Suppress default constructor for noninstantiability
    private CardWhitelistApi() {
        throw new AssertionError("The CardWhitelistApi methods must be accessed statically.");
    }

    /**
     * Fetches all whitelisted card details.
     * Caller must handle the exception.
     *
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static List<CardWhitelistDetail> fetchWhitelistedCard() {
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createGetHttpRequest(whitelistedCardApiUrl()));
        try {
            return Singleton.getObjectMapper().readValue(response.body(), CardWhitelistResDto.class).getCardWhitelistDetails();
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }

    public static String whitelistedCardApiUrl() {
        String whitelistedUrl = PropertyFile.getProperty(PropertyName.CARD_API_WHITELISTED_URL);
        if (whitelistedUrl.isBlank()) {
            throw new GenericException("'" + PropertyName.CARD_API_WHITELISTED_URL + "' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }

        if (whitelistedUrl.endsWith("/")) {
            return whitelistedUrl + "AFSACSERVICE/service/cardWhitelistDetails";
        }
        return whitelistedUrl + "/AFSACSERVICE/service/cardWhitelistDetails";
    }
}
