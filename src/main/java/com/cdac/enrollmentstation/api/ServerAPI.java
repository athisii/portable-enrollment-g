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
import com.cdac.enrollmentstation.model.Unit;
import com.cdac.enrollmentstation.model.UnitListDetails;
import com.cdac.enrollmentstation.util.PropertyFile;
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
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        // throws GenericException
        String jsonResponse = sendHttpRequest(createPostHttpRequest(url, jsonRequestData));
        ARCDetails arcDetail;
        try {
            arcDetail = Singleton.getObjectMapper().readValue(jsonResponse, ARCDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
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
    public static List<Unit> fetchAllUnits() {
        String jsonResponse = sendHttpRequest(createGetHttpRequest(getUnitListURL()));
        // if this line is reached, response received with status code 200
        UnitListDetails unitListDetails;
        try {
            unitListDetails = Singleton.getObjectMapper().readValue(jsonResponse, UnitListDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
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
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        String jsonResponse = sendHttpRequest(createPostHttpRequest(getDemographicURL(), jsonRequestData));
        ARCDetailsList arcDetailsList;
        try {
            arcDetailsList = Singleton.getObjectMapper().readValue(jsonResponse, ARCDetailsList.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
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
     * @return String - MAFIS API home url
     */
    public static String getMafisApiUrl() {
        String mafisServerApi = PropertyFile.getProperty(PropertyName.MAFIS_API_URL);
        if (mafisServerApi == null || mafisServerApi.isBlank()) {
            throw new GenericException("'mafis.api.url' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }

        if (mafisServerApi.endsWith("/")) {
            mafisServerApi = mafisServerApi.substring(0, mafisServerApi.lastIndexOf("/"));
        }
        return mafisServerApi + "/api/EnrollmentStation";
    }

    public static String getEnrollmentStationId() {
        String enrollmentStationId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID);
        if (enrollmentStationId == null || enrollmentStationId.isBlank()) {
            throw new GenericException("'enrollment.station.id' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        return enrollmentStationId;
    }

    public static String getEnrollmentStationUnitId() {
        String enrollmentStationUnitId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID);
        if (enrollmentStationUnitId == null || enrollmentStationUnitId.isBlank()) {
            throw new GenericException("'enrollment.station.unit.id' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        return enrollmentStationUnitId;
    }

    public static String getArcUrl() {
        return getMafisApiUrl() + "/GetDetailsByARCNo";
    }

}
