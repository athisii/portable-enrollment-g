package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardReaderDeInitialize;
import com.cdac.enrollmentstation.model.CardReaderInitialize;
import com.cdac.enrollmentstation.model.CardReaderWaitForConnect;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

/**
 * @author athisii, CDAC
 * Created on 28/03/23
 */
public class LocalCardReaderApi {
    private static final Logger LOGGER = ApplicationLog.getLogger(LocalCardReaderApi.class);


    //Suppress default constructor for noninstantiability
    private LocalCardReaderApi() {
        throw new AssertionError("The LocalCardReaderApi methods should be accessed statically.");
    }

    // throws GenericException
    // Caller must handle the exception
    public static CardReaderDeInitialize getDeInitialize() {
        HttpRequest httpRequest = HttpUtil.createGetHttpRequest(LocalCardReaderApiUrl.getDeInitialize());
        HttpResponse<String> response = HttpUtil.sendHttpRequest(httpRequest);
        // connection timeout
        if (response == null) {
            return null;
        }
        try {
            return Singleton.getObjectMapper().readValue(response.body(), CardReaderDeInitialize.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }

    // throws GenericException
    // Caller must handle the exception
    public static CardReaderInitialize getInitialize() {
        HttpRequest httpRequest = HttpUtil.createGetHttpRequest(LocalCardReaderApiUrl.getInitialize());
        HttpResponse<String> response = HttpUtil.sendHttpRequest(httpRequest);
        // connection timeout
        if (response == null) {
            return null;
        }
        try {
            return Singleton.getObjectMapper().readValue(response.body(), CardReaderInitialize.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }

    // throws GenericException
    // Caller must handle the exception
    public static CardReaderWaitForConnect postWaitForConnect(String data) {
        HttpRequest httpRequest = HttpUtil.createPostHttpRequest(LocalCardReaderApiUrl.getWaitForConnect(), data);
        HttpResponse<String> response = HttpUtil.sendHttpRequest(httpRequest);
        // connection timeout
        if (response == null) {
            return null;
        }
        try {
            return Singleton.getObjectMapper().readValue(response.body(), CardReaderWaitForConnect.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(GENERIC_ERR_MSG);
        }
    }
}
