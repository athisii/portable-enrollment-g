package com.cdac.enrollmentstation.api;


import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.Aes256Util;
import com.cdac.enrollmentstation.security.DHUtil;
import com.cdac.enrollmentstation.security.HmacUtil;
import com.cdac.enrollmentstation.util.PropertyFile;
import com.cdac.enrollmentstation.util.Singleton;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
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
    private static final String PUBLIC_KEY_HEADER = "PublicKey";
    private static final String ENROLLMENT_STATION_ID_HEADER = "EnrollmentStationId";

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
        // Generates key pair
        KeyPair keyPair = DHUtil.generateKeyPair("EC", "secp256r1");
        // Exchanges public keys with the server.
        byte[] mafisPublicKeyBytes = MafisServerApi.exchangePublicKey(PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID), Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        // Obtains Mafis' public key
        PublicKey mafisPublicKey = DHUtil.generatePublicKey(mafisPublicKeyBytes, "EC");
        // Generates shared secret
        byte[] sharedSecretBytes = DHUtil.generateSharedSecretBytes(keyPair.getPrivate(), mafisPublicKey, "ECDH");
        // only uses first 32 bytes
        byte[] secret = Arrays.copyOfRange(sharedSecretBytes, 0, 32);
        Key key = Aes256Util.genKey(secret);

        // to avoid encrypt/decrypt problems
        data = data.replace("\n", "");

        // encrypts the actual data passed from the method's argument
        byte[] encryptedData = Aes256Util.encrypt(data, key);
        String base64EncodedEncryptedData = Base64.getEncoder().encodeToString(encryptedData);

        // hashKey header
        String messageDigest = HmacUtil.genHmacSha256(base64EncodedEncryptedData, secret);

        // need to add enrollment-station-id, hash value in request header
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(HASH_KEY_HEADER, messageDigest);
        headersMap.put(ENROLLMENT_STATION_ID_HEADER, PropertyFile.getProperty(PropertyName.ENROLLMENT_STATION_ID));

        HttpRequest postHttpRequest = HttpUtil.createPostHttpRequest(getSaveEnrollmentUrl(), base64EncodedEncryptedData, headersMap);
        HttpResponse<String> httpResponse = HttpUtil.sendHttpRequest(postHttpRequest);

        // response data from server
        SaveEnrollmentResDto saveEnrollmentResDto;
        try {
            saveEnrollmentResDto = Singleton.getObjectMapper().readValue(httpResponse.body(), SaveEnrollmentResDto.class);
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

    /**
     * Fetches all whitelisted card details.
     * Caller must handle the exception.
     *
     * @throws GenericException exception on connection timeout, error, json parsing exception etc.
     */

    public static List<CardWhitelistDetail> fetchWhitelistedCard() {
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createGetHttpRequest(getWhitelistedCardApiUrl()));
        CardWhitelistResDto cardWhitelistResDto;
        try {
            cardWhitelistResDto = Singleton.getObjectMapper().readValue(response.body(), CardWhitelistResDto.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + cardWhitelistResDto.getErrorCode());
        if (cardWhitelistResDto.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + cardWhitelistResDto.getDesc());
            throw new GenericException(cardWhitelistResDto.getDesc());
        }
        return cardWhitelistResDto.getCardWhitelistDetails();
    }

    public static String getArcUrl() {
        return getMafisApiUrl() + "/GetDetailsByARCNo";
    }

    public static String getSaveEnrollmentUrl() {
        return getMafisApiUrl() + "/SaveEnrollment";
    }

    public static String getWhitelistedCardApiUrl() {
        return getMafisApiUrl() + "/GetCardWhitelistDetails";
    }

    public static void validateUserCategory(UserReqDto userReqDto) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(userReqDto);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createPostHttpRequest(getUserUrl(), jsonRequestData));
        CommonResDto commonResDto;
        try {
            commonResDto = Singleton.getObjectMapper().readValue(response.body(), CommonResDto.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + commonResDto.getErrorCode());
        if (commonResDto.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + commonResDto.getDesc());
            throw new GenericException(commonResDto.getDesc());
        }
    }

    public static String getUserUrl() {
        return getMafisApiUrl() + "/GetDetailValidFesPesUser";
    }

    public static OnboardingResDto fetchOnboardingDetails(OnboardingReqDto onboardingReqDto) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(onboardingReqDto);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createPostHttpRequest(getOnboardingDetailsUrl(), jsonRequestData));
        OnboardingResDto onboardingResDto;
        try {
            onboardingResDto = Singleton.getObjectMapper().readValue(response.body(), OnboardingResDto.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + onboardingResDto.getErrorCode());
        if (onboardingResDto.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + onboardingResDto.getDesc());
            throw new GenericException(onboardingResDto.getDesc());
        }
        if (onboardingResDto.getOnboardingUnitDetails() == null || onboardingResDto.getOnboardingUnitDetails().isEmpty()) {
            LOGGER.log(Level.INFO, () -> "***ServerErrorCode: Null or received an empty list of device.");
            throw new GenericException("No PES available for the selected unit.");
        }
        return onboardingResDto;
    }

    public static String getOnboardingDetailsUrl() {
        return getMafisApiUrl() + "/GetListOfVacantFPDevice";
    }

    public static void updateOnboarding(UpdateOnboardingReqDto updateOnboardingReqDto) {
        String jsonRequestData;
        try {
            jsonRequestData = Singleton.getObjectMapper().writeValueAsString(updateOnboardingReqDto);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, ApplicationConstant.JSON_WRITE_ER_MSG);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }

        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createPostHttpRequest(getUpdateOnboardingUrl(), jsonRequestData));
        CommonResDto commonResDto;
        try {
            commonResDto = Singleton.getObjectMapper().readValue(response.body(), CommonResDto.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + commonResDto.getErrorCode());
        if (commonResDto.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + commonResDto.getDesc());
            throw new GenericException(commonResDto.getDesc());
        }
    }

    public static String getUpdateOnboardingUrl() {
        return getMafisApiUrl() + "/GetOnBoardFPDevice";
    }

    public static byte[] exchangePublicKey(String enrollmentStationId, String base64EncodedPublicKey) {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(ENROLLMENT_STATION_ID_HEADER, enrollmentStationId);
        headersMap.put(PUBLIC_KEY_HEADER, base64EncodedPublicKey);
        HttpResponse<String> response = HttpUtil.sendHttpRequest(HttpUtil.createHttpRequest(HttpUtil.MethodType.GET, getExchangePublicKeyUrl(), null, headersMap));
        CommonResDto commonResDto;
        try {
            commonResDto = Singleton.getObjectMapper().readValue(response.body(), CommonResDto.class);
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
        LOGGER.log(Level.INFO, () -> "***ServerResponseErrorCode: " + commonResDto.getErrorCode());
        if (commonResDto.getErrorCode() != 0) {
            LOGGER.log(Level.INFO, () -> ApplicationConstant.GENERIC_SERVER_ERR_MSG + commonResDto.getDesc());
            throw new GenericException(commonResDto.getDesc());
        }
        String base64EncodedMafisPublicKey = response.headers().firstValue(PUBLIC_KEY_HEADER).orElseThrow(() -> {
            LOGGER.log(Level.INFO, "ServerResponseError:: Public key header not found.");
            return new GenericException("Error occurred while exchanging keys. Please try again.");
        });

        try {
            return Base64.getDecoder().decode(base64EncodedMafisPublicKey);
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "ServerResponseError:: Received invalid base64 public key from the server.");
            throw new GenericException("Error occurred while decoding base64 public key. Please try again.");
        }
    }

    public static String getExchangePublicKeyUrl() {
        return getMafisApiUrl() + "/ExchangePublicKey";
    }
}
