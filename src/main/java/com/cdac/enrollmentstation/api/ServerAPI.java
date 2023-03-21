package com.cdac.enrollmentstation.api;


import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.HttpHeader;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.ARCNoReqDto;
import com.cdac.enrollmentstation.dto.SaveEnrollmentResponse;
import com.cdac.enrollmentstation.dto.UnitCodeReqDto;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.ARCDetails;
import com.cdac.enrollmentstation.model.ARCDetailsList;
import com.cdac.enrollmentstation.model.Unit;
import com.cdac.enrollmentstation.model.UnitListDetails;
import com.cdac.enrollmentstation.security.Aes256Util;
import com.cdac.enrollmentstation.security.HmacUtil;
import com.cdac.enrollmentstation.security.PkiUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

public class ServerAPI {
    private static final int NO_OF_RETRIES = 1;
    private static final int CONNECTION_TIMEOUT = 10;
    private static final int WRITE_TIMEOUT = 30;
    private static final String UNIQUE_KEY_HEADER = "UniqueKey";
    private static final String HASH_KEY_HEADER = "HashKey";


    private static final Logger LOGGER = ApplicationLog.getLogger(ServerAPI.class);
    private static final HttpClient httpClient;

    static {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT)).build();
    }

    //Suppress default constructor for noninstantiability
    private ServerAPI() {
        throw new AssertionError("The ServerAPI methods should be accessed statically.");
    }

    /**
     * Fetches single ARCDetails based on ARC number.
     * Caller must handle the exception.
     *
     * @param url   url of the API.
     * @param arcNo unique id whose details are to be fetched
     * @return ARCDetails or null on connection timeout
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
        HttpResponse<String> response = sendHttpRequest(createPostHttpRequest(url, jsonRequestData, null));
        if (response == null) {
            return null;
        }
        ARCDetails arcDetail;
        try {
            arcDetail = Singleton.getObjectMapper().readValue(response.body(), ARCDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        return arcDetail;
    }

    /**
     * Sends http post request.
     * Caller must handle the exception.
     *
     * @param data request payload
     * @return SaveEnrollmentResponse or null on connection timeout
     * @throws GenericException exception on error, json parsing exception etc.
     */
    public static SaveEnrollmentResponse postEnrollment(String data) {
        // to avoid encrypt/decrypt problems
        data = data.replace("\n", "");
        // assigns random secret key at each call
        String secret = Aes256Util.genUuid();

        // for sending base64 encoded encrypted SECRET KEY to server in HEADER
        byte[] pkiEncryptedUniqueKey = PkiUtil.encrypt(secret);
        String base64EncodedPkiEncryptedUniqueKey = Base64.getEncoder().encodeToString(pkiEncryptedUniqueKey);

        // encrypts the actual data passed from the method's argument
        Key key = Aes256Util.genKey(secret);
        byte[] encryptedData = Aes256Util.encrypt(data, key);
        String base64EncodedEncryptedData = Base64.getEncoder().encodeToString(encryptedData);

        // hashKey header
        String messageDigest = HmacUtil.genHmacSha256(base64EncodedEncryptedData, secret);

        // need to add unique-key, hash value in request header
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(UNIQUE_KEY_HEADER, base64EncodedPkiEncryptedUniqueKey);
        headersMap.put(HASH_KEY_HEADER, messageDigest);

        HttpRequest postHttpRequest = createPostHttpRequest(getSaveEnrollmentUrl(), base64EncodedEncryptedData, headersMap);
        HttpResponse<String> httpResponse = sendHttpRequest(postHttpRequest);
        // connection timeout
        if (httpResponse == null) {
            return null;
        }
        Optional<String> base64EncodedUniqueKeyOptional = httpResponse.headers().firstValue(UNIQUE_KEY_HEADER);

        if (base64EncodedUniqueKeyOptional.isEmpty()) {
            throw new GenericException("Unique key header not found in http response");
        }

        String base64EncodedUniqueKeyFromServer = base64EncodedUniqueKeyOptional.get();

        if (base64EncodedUniqueKeyFromServer.isBlank()) {
            throw new GenericException("Unique Key From Server is Empty");
        }
        // received base64 encoded encrypted secret key from server
        byte[] encryptedSecretKey = Base64.getDecoder().decode(base64EncodedUniqueKeyFromServer);
        secret = PkiUtil.decrypt(encryptedSecretKey);
        key = Aes256Util.genKey(secret);

        // Received base64 encoded encrypted data
        byte[] encryptedResponseBody = Base64.getDecoder().decode(httpResponse.body());
        String receivedData = Aes256Util.decrypt(encryptedResponseBody, key);

        // response data from server
        SaveEnrollmentResponse saveEnrollmentResponse;
        try {
            saveEnrollmentResponse = Singleton.getObjectMapper().readValue(receivedData, SaveEnrollmentResponse.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        return saveEnrollmentResponse;
    }

    /**
     * Sends Http request.
     * Caller must handle the exception.
     *
     * @param httpRequest request payload
     * @return HttpResponse<String>  or null if timeout exception occurred
     */

    private static HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) {
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
        // connection timeout
        if (response == null || noOfRetries == 0) {
            LOGGER.log(Level.INFO, "Connection timeout");
            return null;
        }
        return response;
    }

    /**
     * Fetches all units.
     * Caller must handle the exception.
     *
     * @return List<Units> or null on connection timeout
     * @throws GenericException exception on error, json parsing exception etc.
     */
    public static List<Unit> fetchAllUnits() {
        HttpResponse<String> response = sendHttpRequest(createGetHttpRequest(getUnitListURL()));
        if (response == null) {
            return null;
        }
        // if this line is reached, response received with status code 200
        UnitListDetails unitListDetails;
        try {
            unitListDetails = Singleton.getObjectMapper().readValue(response.body(), UnitListDetails.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        if (unitListDetails.getErrorCode() != 0) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.GENERIC_SERVER_ERR_MSG + unitListDetails.getDesc());
            throw new GenericException(unitListDetails.getDesc());
        }
        return unitListDetails.getUnits();
    }


    /**
     * Fetches list of ARC based on unitCode.
     * Caller must handle the exception.
     *
     * @return List<ARCDetails> or null on connection timeout
     * @throws GenericException exception on error, json parsing exception etc.
     */

    public static List<ARCDetails> fetchArcListByUnitCode(String unitCode) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(new UnitCodeReqDto(unitCode));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        HttpRequest postHttpRequest = createPostHttpRequest(getDemographicURL(), jsonRequestData, null);
        HttpResponse<String> httpResponse = sendHttpRequest(postHttpRequest);
        if (httpResponse == null) {
            return null;
        }
        ARCDetailsList arcDetailsList;
        try {
            arcDetailsList = Singleton.getObjectMapper().readValue(httpResponse.body(), ARCDetailsList.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        if (arcDetailsList.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, ApplicationConstant.GENERIC_SERVER_ERR_MSG + arcDetailsList.getDesc());
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

    private static HttpRequest createPostHttpRequest(String url, String data, Map<String, String> extraHeaders) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        if (extraHeaders != null) {
            extraHeaders.forEach(builder::header);
        }
        return builder.uri(URI.create(url))
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

    public static String getSaveEnrollmentUrl() {
        return getMafisApiUrl() + "/SaveEnrollment";
    }

}
