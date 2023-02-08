package com.cdac.enrollmentstation.api;


import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.HttpHeader;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.ARCNoReqDto;
import com.cdac.enrollmentstation.dto.UnitCodeReqDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsList;
import com.cdac.enrollmentstation.model.UnitListDetails;
import com.cdac.enrollmentstation.model.Units;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

public class ServerAPI {
    private static final int NO_OF_RETRIES = 1;
    private static final int CONNECTION_TIMEOUT = 10;
    private static final int WRITE_TIMEOUT = 30;


    private static final Logger LOGGER = ApplicationLog.getLogger(ServerAPI.class);
    private static final HttpClient httpClient;

    static {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT)).build();
    }

    //disables instantiation of this class.
    private ServerAPI() {
    }

    /**
     * Fetches single ARCDetails based on ARC number.
     * Caller must handle the exception.
     *
     * @param url   url of the API.
     * @param arcNo unique id whose details are to be fetched
     * @return ARCDetails
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static ARCDetails fetchARCDetails(String url, String arcNo) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(new ARCNoReqDto(arcNo));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ERROR_MESSAGE);
            throw new GenericException(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
        String jsonResponse = sendHttpRequest(createPostHttpRequest(url, jsonRequestData));
        ARCDetails arcDetail;
        try {
            arcDetail = Singleton.getObjectMapper().readValue(jsonResponse, ARCDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERROR_MESSAGE);
            throw new GenericException(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
        if (!"0".equals(arcDetail.getErrorCode())) {
            throw new GenericException(arcDetail.getDesc());
        }
        return arcDetail;
    }

    /**
     * Sends Http request.
     * Caller must handle the exception.
     *
     * @param httpRequest request payload
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
     * Caller must handle the exception.
     *
     * @return List<Units>
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */
    public static List<Units> fetchAllUnits() {
        String jsonResponse = sendHttpRequest(createGetHttpRequest(getUnitListURL()));
        // if this line is reached, response received with status code 200
        UnitListDetails unitListDetails;
        try {
            unitListDetails = Singleton.getObjectMapper().readValue(jsonResponse, UnitListDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERROR_MESSAGE);
            throw new GenericException(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
        if (unitListDetails.getErrorCode() != 0) {
            throw new GenericException(unitListDetails.getDesc());
        }
        return unitListDetails.getUnits();
    }


    /**
     * Fetches list of ARC based on unitCode.
     * Caller must handle the exception.
     *
     * @return List<ARCDetails>
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static List<ARCDetails> fetchArcListByUnitCode(String unitCode) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(new UnitCodeReqDto(unitCode));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ERROR_MESSAGE);
            throw new GenericException(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
        String jsonResponse = sendHttpRequest(createPostHttpRequest(getDemographicURL(), jsonRequestData));
        ARCDetailsList arcDetailsList;
        try {
            arcDetailsList = Singleton.getObjectMapper().readValue(jsonResponse, ARCDetailsList.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERROR_MESSAGE);
            throw new GenericException(ApplicationConstant.GENERIC_ERROR_MESSAGE);
        }
        if (arcDetailsList.getErrorCode() != 0) {
            throw new GenericException(arcDetailsList.getDesc());
        }
        return arcDetailsList.getArcDetails();
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


    public static String getUnitListURL() {
        return getMafisApiUrl() + "/GetAllUnits";
    }

    public static String getDemographicURL() {
        return getMafisApiUrl() + "/GetDemographicDetails";
    }

    /**
     * Returns MAFIS API home url from /etc/file.properties
     * Caller must handle the exception.
     *
     * @return String - MAFIS API home url
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */
    public static String getMafisApiUrl() {
        String mafisServerApi;
        try {
            List<String> lines = Files.readAllLines(Paths.get(PropertyFile.getProperty(PropertyName.URL_DATA)));
            if (lines.isEmpty() || lines.get(0).isBlank()) {
                throw new GenericException(PropertyFile.getProperty(PropertyName.URL_DATA) + " is empty");
            }
            String line = lines.get(0);
            // /etc/data.txt -> U1,http://X.X.X.X:X,XX
            String[] tokens = line.split(",");
            if (tokens.length < 3) {
                throw new GenericException("Malformed values. Values should be separated by ','. Example- U1,http://X.X.X.X:X,XX");
            }
            mafisServerApi = tokens[1];
        } catch (IOException e) {
            LOGGER.log(Level.INFO, () -> "Problem reading file: " + PropertyFile.getProperty(PropertyName.URL_DATA));
            e.printStackTrace();
            throw new GenericException("Errored occurred reading " + PropertyFile.getProperty(PropertyName.URL_DATA));
        }
        if (mafisServerApi.endsWith("/")) {
            mafisServerApi = mafisServerApi.substring(0, mafisServerApi.lastIndexOf("/"));
        }
        return mafisServerApi + "/api/EnrollmentStation";
    }
}
