package com.cdac.enrollmentstation.api;


import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.Aes256Util;
import com.cdac.enrollmentstation.security.HmacUtil;
import com.cdac.enrollmentstation.security.PkiUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii, CDAC
 * Created on 29/03/23
 */

public class MafisServerApi {
    private static final String UNIQUE_KEY_HEADER = "UniqueKey";
    private static final String HASH_KEY_HEADER = "HashKey";

    private static final Logger LOGGER = ApplicationLog.getLogger(MafisServerApi.class);

    //Suppress default constructor for noninstantiability
    private MafisServerApi() {
        throw new AssertionError("The MafisServerApi methods must be accessed statically.");
    }

    /**
     * Fetches single ARCDetails based on e-ARC number.
     * Caller must handle the exception.
     *
     * @param arcNo unique id whose details are to be fetched
     * @return ARCDetails or null on connection timeout
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static ArcDetail fetchARCDetail(String arcNo) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(new ArcNoReqDto(arcNo));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createPostHttpRequest(getArcUrl(), jsonRequestData));
        ArcDetail arcDetail;
        try {
            arcDetail = Singleton.getObjectMapper().readValue(response.body(), ArcDetail.class);
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
    public static SaveEnrollmentResDto postEnrollment(String data) {
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

        HttpRequest postHttpRequest = HttpUtil.createPostHttpRequest(getSaveEnrollmentUrl(), base64EncodedEncryptedData, headersMap);
        HttpResponse<String> httpResponse = HttpUtil.sendHttpRequest(postHttpRequest);
        Optional<String> base64EncodedUniqueKeyOptional = httpResponse.headers().firstValue(UNIQUE_KEY_HEADER);

        if (base64EncodedUniqueKeyOptional.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Unique key header not found in http response");
            throw new GenericException("Unique Key not received from the server.");
        }
        // received base64 encoded encrypted secret key from server
        byte[] encryptedSecretKey = Base64.getDecoder().decode(base64EncodedUniqueKeyOptional.get());
        secret = PkiUtil.decrypt(encryptedSecretKey);
        key = Aes256Util.genKey(secret);

        // Received base64 encoded encrypted data
        byte[] encryptedResponseBody = Base64.getDecoder().decode(httpResponse.body());
        String receivedData = Aes256Util.decrypt(encryptedResponseBody, key);

        // response data from server
        SaveEnrollmentResDto saveEnrollmentResDto;
        try {
            saveEnrollmentResDto = Singleton.getObjectMapper().readValue(receivedData, SaveEnrollmentResDto.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        return saveEnrollmentResDto;
    }

    /**
     * Fetches all units.
     * Caller must handle the exception.
     *
     * @return List<Units> or null on connection timeout
     * @throws GenericException exception on error, json parsing exception etc.
     */
    public static List<Unit> fetchAllUnits() {
        LOGGER.log(Level.INFO, () -> "***Fetching all units from the server.");
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createGetHttpRequest(getUnitListURL()));
        // if this line is reached, response received with status code 200
        UnitsResDto unitsResDto;
        try {
            unitsResDto = Singleton.getObjectMapper().readValue(response.body(), UnitsResDto.class);
        } catch (JsonProcessingException ignored) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + unitsResDto.getErrorCode());
        if (unitsResDto.getErrorCode() != 0) {
            LOGGER.log(Level.SEVERE, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + unitsResDto.getDesc());
            throw new GenericException(unitsResDto.getDesc());
        }
        return unitsResDto.getUnits();
    }


    /**
     * Fetches list of e-ARC based on unitCode.
     * Caller must handle the exception.
     *
     * @return List<ARCDetails> or null on connection timeout
     * @throws GenericException exception on error, json parsing exception etc.
     */

    public static List<ArcDetail> fetchArcsByUnitCode(String unitCode) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(new UnitCodeReqDto(unitCode));
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***Fetching all e-ARCs for unitCode:" + unitCode);
        HttpRequest postHttpRequest = HttpUtil.createPostHttpRequest(getDemographicURL(), jsonRequestData);
        HttpResponse<String> httpResponse = HttpUtil.sendHttpRequest(postHttpRequest);
        ArcDetailsResDto arcDetailsResDto;
        try {
            arcDetailsResDto = Singleton.getObjectMapper().readValue(httpResponse.body(), ArcDetailsResDto.class);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_READ_ERR_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + arcDetailsResDto.getErrorCode());
        if (arcDetailsResDto.getErrorCode() != 0) {
            String desc = arcDetailsResDto.getDesc();
            desc = desc.replace("ARC", "e-ARC");
            LOGGER.log(Level.INFO, desc);
            throw new GenericException(desc);
        }
        return arcDetailsResDto.getArcDetails();
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
        if (mafisServerApi.isBlank()) {
            throw new GenericException("'" + PropertyName.MAFIS_API_URL + "' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }

        if (mafisServerApi.endsWith("/")) {
            return mafisServerApi + "api/EnrollmentStation";
        }
        return mafisServerApi + "/api/EnrollmentStation";
    }

    public static String getEnrollmentStationId() {
        String enrollmentStationId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID);
        if (enrollmentStationId.isBlank()) {
            throw new GenericException("'enrollment.station.id' not found or is empty in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
        }
        return enrollmentStationId;
    }

    public static String getEnrollmentStationUnitId() {
        String enrollmentStationUnitId = PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_UNIT_ID);
        if (enrollmentStationUnitId.isBlank()) {
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
