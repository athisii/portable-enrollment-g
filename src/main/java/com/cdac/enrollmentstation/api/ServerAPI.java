package com.cdac.enrollmentstation.api;


import com.cdac.enrollmentstation.constant.HttpHeader;
import com.cdac.enrollmentstation.dto.ARCNoReqDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.UnitListDetails;
import com.cdac.enrollmentstation.model.Units;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

public class ServerAPI {
    private static final int NO_OF_RETRIES = 1;
    private static final int CONNECTION_TIMEOUT = 5;
    private static final int WRITE_TIMEOUT = 30;
    private static final String ERROR_MESSAGE = "Something went wrong. Please try again.";


    private static final Logger LOGGER = ApplicationLog.getLogger(ServerAPI.class);
    private static final HttpClient httpClient;

    static {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT)).build();
    }

    //disables instantiation of this class.
    private ServerAPI() {
    }

    /**
     * Fetches single ARCDetails based on ARC unique number.
     * Caller must handle exceptions
     *
     * @param url   url of the API.
     * @param arcNo unique id whose details are to be fetched
     * @return ARCDetails
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static ARCDetails fetchARCDetails(String url, String arcNo) {
        String jsonRequest;
        try {
            jsonRequest = Singleton.getObjectMapper().writeValueAsString(new ARCNoReqDto(arcNo));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Unable to write as JSON string.");
            throw new GenericException(ERROR_MESSAGE);
        }
        String jsonResponse = sendHttpRequest(createPostHttpRequest(url, jsonRequest));
        ARCDetails arcDetail;
        try {
            arcDetail = Singleton.getObjectMapper().readValue(jsonResponse, ARCDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, "Error occurred while parsing json data.");
            throw new GenericException(ERROR_MESSAGE);
        }
        if (!"0".equals(arcDetail.getErrorCode())) {
            throw new GenericException(arcDetail.getDesc());
        }
        return arcDetail;
    }

    /**
     * Sends request. Caller must handle exceptions
     *
     * @param httpRequest reqeust payload
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    private static String sendHttpRequest(HttpRequest httpRequest) {
        int noOfRetries = NO_OF_RETRIES;
        HttpResponse<String> response = null;
        while (noOfRetries > 0) {
            try {
                response = httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    break;
                }
            } catch (IOException ignored) {
                LOGGER.log(Level.INFO, String.format("%s", (NO_OF_RETRIES + 1 - noOfRetries) + " Retrying connection."));
                noOfRetries--;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (response == null || noOfRetries == 0) {
            throw new GenericException("Connection timeout. Failed to connect to server. Please try again.");
        }
        return response.body();
    }

    /**
     * Fetches all units.
     * Caller must handle exceptions.
     *
     * @return List<Units>
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */
    public static List<Units> fetchAllUnits() {
        var getRequest = createGetHttpRequest(APIServerCheck.getUnitListURL());
        String response = sendHttpRequest(getRequest);
        // if this line is reached, response received with status code 200
        UnitListDetails unitListDetails;
        try {
            unitListDetails = Singleton.getObjectMapper().readValue(response, UnitListDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, "Error occurred while parsing json data.");
            throw new GenericException(ERROR_MESSAGE);
        }
        if (!"0".equals(unitListDetails.getErrorCode())) {
            throw new GenericException(unitListDetails.getDesc());
        }
        return unitListDetails.getUnits();
    }

    private static HttpRequest createGetHttpRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(HttpHeader.CONTENT_TYPE, "application/json; utf-8")
                .header(HttpHeader.ACCEPT, "application/json")
                .timeout(Duration.ofSeconds(WRITE_TIMEOUT))
                .build();
    }

    private static HttpRequest createPostHttpRequest(String url, String data) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(data))
                .header(HttpHeader.CONTENT_TYPE, "application/json; utf-8")
                .header(HttpHeader.ACCEPT, "application/json")
                .timeout(Duration.ofSeconds(WRITE_TIMEOUT))
                .build();
    }


    //Test Code
    public static void main(String[] args) {
        ARCDetails arcDetail;
        try {
            arcDetail = fetchARCDetails("http://localhost:8080/arc-details", "1111-AAAA");
            LOGGER.log(Level.INFO, arcDetail::toString);
        } catch (GenericException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }
}
