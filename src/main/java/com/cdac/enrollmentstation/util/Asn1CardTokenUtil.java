
package com.cdac.enrollmentstation.util;


import com.cdac.enrollmentstation.api.LocalNavalWebServiceApi;
import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.dto.*;
import com.cdac.enrollmentstation.exception.ConnectionTimeoutException;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.exception.NoReaderOrCardException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.model.CardFp;
import org.bouncycastle.asn1.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cdac.enrollmentstation.constant.ApplicationConstant.GENERIC_ERR_MSG;

public class Asn1CardTokenUtil {
    private static final Logger LOGGER = ApplicationLog.getLogger(Asn1CardTokenUtil.class);

     /*
      -------------------------------------------------------------------------------------------------------------------------
      -                                                  ASN1 DATA TYPES                                                      -
      -------------------------------------------------------------------------------------------------------------------------
      -  Sr.No   Data Type           Class   Form       Tag Number(binary)  Tag Number(dec)    Byte(binary)    Byte(decimal)  -
      -------------------------------------------------------------- ----------------------------------------------------------
      -   1       EOC(marker)          00     0             00000                  0             00000000            0        -
      -   2       BOOLEAN              00     0             00001                  1             00000001            1        -
      -   3       INTEGER              00     0             00010                  2             00000010            2        -
      -   4       BIT STRING           00     0             00011                  3             00000011            3        -
      -   5       OCTET STRING         00     0             00100                  4             00000100            4        -
      -   6       NULL                 00     0             00101                  5             00000101            5        -
      -   7       OBJECT IDENTIFIER    00     0             00110                  6             00000110            6        -
      -   8       REAL                 00     0             00111                  7             00000111            7        -
      -   9       SEQUENCE             00     1             10000                  16            00110000            48       -
      -   10      SET                  00     1             10001                  17            00110001            49       -
      -   11      PrintableString      00     0             10011                  19            00010011            19       -
      -   12      IA5String            00     0             10110                  22            00010110            22       -
      -   -        -                    -     -               -                    -                 -               -        -
      -   -        -                    -     -               -                    -                 -               -        -
      -   -        -                    -     -               -                    -                 -               -        -
      -----------------------------------------------------------------------------------------------------------------------------


                                NOIDA'S API CONTRACT
      .............................................................................
      . Input Type           Acronym                         Value     Max Byte   .
      .............................................................................
      . card-type            Naval ID Card                     4                  .
      .                      Token                             5                  .
      .............................................................................
      . which-data           Static File                       21       500       .
      .                      Default Access Validity           22       28        .
      .                      Special Access Permission File    24       15000     .
      .                      Fingerprint File                  25       5164      .
      .                      System Certificate                32       2806      .
      .                      Signature File1                   33       1655      .
      .                      Signature File2                   34                 .
      .                      Signature File3                   35       1655      .
      .                      Dynamic File                      36       500       .
      .                      CSN                               42                 .
      .                      Photo File                        43       10250     .
      .............................................................................
      . which-certificate    System certificate                14                 .
      .............................................................................
      . which-trust          AFSAC                             11                 .
      .............................................................................
      .                                                           Total: 37,358   .
      .............................................................................
     */

    private static int jniErrorCode;
    public static final int SLEEP_TIME_BEFORE_WAIT_FOR_CONNECT_CALL_IN_MIL_SEC = 500;

    public static final int CARD_TYPE_NUMBER = 4; // Naval ID/Contractor Card
    public static final int TOKEN_TYPE_NUMBER = 5; // Token 5
    public static final int WHICH_TRUST = 11; // AFSAC
    public static final int WHICH_CERTIFICATE = 14; // System certificate
    public static final int MAX_BUFFER_SIZE = 1024; // Max bytes card can handle
    public static final int MAX_DYNAMIC_FILE_SIZE = 500;
    public static final int MAX_DEFAULT_ACCESS_VALIDITY_SIZE = 28;
    public static final int MAX_SPECIAL_ACCESS_PERMISSION_FILE_SIZE = 15000;
    public static final int MAX_SYSTEM_CERTIFICATE_SIZE = 2806;
    public static final int MAX_SIGNATURE_FILE_SIZE = 1655;
    public static final int MAX_FINGERPRINT_FILE_SIZE = 5164;
    public static final int MAX_PHOTO_FILE_SIZE = 10250;
    public static final String MANTRA_CARD_READER_NAME;
    public static final String CARD_API_SERVICE_RESTART_COMMAND;

    static {
        try {
            MANTRA_CARD_READER_NAME = PropertyFile.getProperty(PropertyName.CARD_API_CARD_READER_NAME).trim();
            CARD_API_SERVICE_RESTART_COMMAND = PropertyFile.getProperty(PropertyName.CARD_API_SERVICE_RESTART_COMMAND).trim();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, () -> "No entry for '" + PropertyName.CARD_API_CARD_READER_NAME + "/" + PropertyName.CARD_API_SERVICE_RESTART_COMMAND + "' in " + ApplicationConstant.DEFAULT_PROPERTY_FILE);
            throw new GenericException(ex.getMessage());
        }
    }

    public enum CardTokenFileType {
        STATIC(21),
        DEFAULT_ACCESS_VALIDITY(22),
        SPECIAL_ACCESS_PERMISSION_FILE(24),
        FINGERPRINT_FILE(25),
        SYSTEM_CERTIFICATE(32),
        SIGNATURE_FILE_1(33),
        SIGNATURE_FILE_2(34),
        SIGNATURE_FILE_3(35),
        DYNAMIC_FILE(36),
        CSN(42),
        PHOTO_FILE(43);
        private final int value;

        CardTokenFileType(int val) {
            value = val;
        }

        public int getValue() {
            return value;
        }
    }


    public enum CardStaticDataIndex {
        CHIP_SERIAL_NUMBER(0),
        CARD_NUMBER(1),
        CARD_TYPE_ID(2),
        USER_CATEGORY_ID(3),
        NAME(4),
        SERVICE(5),
        PN(6),
        UNIQUE_ID(7),
        RANK(8),
        DESIGNATION(9),
        GROUP(10),
        DATE_OF_BIRTH(11),
        UNIT(12),
        ZONE_ACCESS(13),
        DATE_ISSUED(14),
        PLACE_ISSUED(15),
        BLOOD_GROUP(16),
        NATIONALITY(17),
        ISSUED_BY(18),
        FIRMS_NAME(19),
        GENDER(20),
        SPONSOR_PHONE_NUMBER(21),
        SPONSOR_NAME(22),
        SPONSOR_RANK(23),
        SPONSORS_UNIT(24),
        RELATION(25);
        private final int value;

        CardStaticDataIndex(int val) {
            value = val;
        }

        public int getValue() {
            return value;
        }
    }

    //Suppress default constructor for noninstantiability
    private Asn1CardTokenUtil() {
        throw new AssertionError("The Asn1CardTokenUtil methods must be accessed statically.");
    }

    /**
     * Utility to decode ASN1 encoded static data. Caller must handle the exception
     *
     * @param bytes - byte array of ASN1 encoded data.
     * @param index - index of a component in ASN1Sequence
     * @return the string representation of data.
     * @throws GenericException - on null, io
     */
    public static byte[] extractFromAsn1EncodedStaticData(byte[] bytes, int index) {
        try {
            ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(bytes));
            ASN1Primitive asn1Primitive = asn1InputStream.readObject();
            if (asn1Primitive instanceof ASN1Sequence) {
                ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(asn1Primitive);
                ASN1Encodable asn1SequenceObject = asn1Sequence.getObjectAt(index);
                if (asn1SequenceObject instanceof ASN1OctetString) {
                    LOGGER.log(Level.INFO, "****ExtractFromAsn1EncodedStaticData: OctetString type parsed.");
                    return ((ASN1OctetString) asn1SequenceObject).getOctets(); // encoded in hex
                }
                return asn1SequenceObject.toString().getBytes();
            }
            if (asn1Primitive instanceof ASN1OctetString) {
                LOGGER.log(Level.INFO, "****ExtractFromAsn1EncodedStaticData: OctetString type parsed.");
                return ((ASN1OctetString) asn1Primitive).getOctets();
            }
            return asn1Primitive.toString().getBytes();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }

    /**
     * Utility to decode ASN1 encoded fingerprint. Caller must handle the exception
     *
     * @param bytes - byte array of ASN1 encoded data.
     * @return the string representation of data.
     * @throws GenericException - on null, io
     */
    public static List<CardFp> extractFromAsn1EncodedFingerprintData(byte[] bytes) {
        // TODO: to be tested and improved.
        try {
            ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(bytes));
            ASN1Primitive asn1Primitive = asn1InputStream.readObject();
            Enumeration<ASN1Sequence> sequences = DERSet.getInstance(asn1Primitive).getObjects();
            List<CardFp> fps = new ArrayList<>();
            while (sequences.hasMoreElements()) {
                ASN1Sequence sequence = sequences.nextElement();
                String fpPosition = DERIA5String.getInstance(sequence.getObjectAt(0)).toString();
                byte[] image = DEROctetString.getInstance(sequence.getObjectAt(1)).getOctets();
                fps.add(new CardFp(fpPosition, image));
            }
            return fps;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }

    /**
     * Disconnects the initialized card
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void deInitialize() {
        CRApiResDto deInitializeResDto = LocalNavalWebServiceApi.getDeInitialize();
        jniErrorCode = deInitializeResDto.getRetVal();
        // -1409286131 -> prerequisites failed error
        if (jniErrorCode != 0 && jniErrorCode != -1409286131) {
            LOGGER.log(Level.SEVERE, () -> "****DeInitializeErrorCode: " + jniErrorCode);
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * prerequisite of all other APIs
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void initialize() {
        CRApiResDto crInitializeResDto = LocalNavalWebServiceApi.getInitialize();
        jniErrorCode = crInitializeResDto.getRetVal();
        if (jniErrorCode != 0) {
            LOGGER.log(Level.SEVERE, () -> "****InitializeErrorCode: " + jniErrorCode);
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * Gets the handle id, csn
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static CRWaitForConnectResDto waitForConnect(String readerName) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRWaitForConnectReqDto(readerName));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRWaitForConnectResDto crWaitForConnectResDto = LocalNavalWebServiceApi.postWaitForConnect(reqData);
        jniErrorCode = crWaitForConnectResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****WaitForConnectErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            if (jniErrorCode == -1090519029) { // custom message when press 'login' without reader connected
                LOGGER.log(Level.SEVERE, () -> "****WaitForConnectErrorCode: No card reader detected or unsupported reader name.");
                throw new NoReaderOrCardException("No card reader detected or unsupported reader name.");
            }
            if (jniErrorCode == -1090514932) { // custom message when press 'login' without reader connected
                LOGGER.log(Level.SEVERE, () -> "****WaitForConnectErrorCode: No card/token detected.");
                throw new NoReaderOrCardException("No card/token detected. Kindly place it on the reader.");
            }
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
        return crWaitForConnectResDto;
    }

    /**
     * select card/token application
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void selectApp(int cardType, int handle) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRSelectAppReqDto(cardType, handle));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crSelectAppResDto = LocalNavalWebServiceApi.postSelectApp(reqData);
        jniErrorCode = crSelectAppResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****SelectAppErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * returns Object[], must be type cast by caller
     * array[0] -> int (ASN1 Tag),
     * array[1] --> byte[] (ASN1 encoded bytes)
     * reads all data and saves it in buffer
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static byte[] readBufferedData(int handle, CardTokenFileType cardTokenFileType) {
        //  4000
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // read 1024 bytes for the first time as we don't know the exact size to be read.
            byte[] base64DecodedBytes = readData(handle, cardTokenFileType.getValue(), 0, MAX_BUFFER_SIZE);
            // check first byte (TAG)
            // the first byte specifies ASN1 data type.
            byte asn1TypeByte = base64DecodedBytes[0];
            if (asn1TypeByte <= 0) {
                LOGGER.log(Level.SEVERE, () -> "****Error: Read unknown ASN1 type. Tag value: " + asn1TypeByte);
                throw new GenericException("Read unknown ASN1 type.");
            }
            // check second byte (LENGTH)
            // the second byte specifies data length or how many bytes are used
            // to encode LENGTH based on MSB.
            byte asn1LengthByte = base64DecodedBytes[1];
            if (asn1LengthByte == 0) {
                LOGGER.log(Level.SEVERE, () -> "****Error: No data is available on the card/token.");
                throw new GenericException("No data is available on the card/token.");
            }
            int totalByteCount;
            // check if only one byte is used to encode actual data length
            // if second byte's MSB is 0, then it a positive number,
            // and only a single byte is used to encode data length
            if (asn1LengthByte > 0) {
                // now data length is less than 128 bytes.
                // 01111111 --> 127
                totalByteCount = asn1LengthByte + 2; //first byte + second byte
                if (base64DecodedBytes.length < totalByteCount) {
                    LOGGER.log(Level.SEVERE, () -> "****Error: Read corrupted data.");
                    throw new GenericException("Read corrupted data from the card/token.");
                }
                return Arrays.copyOfRange(base64DecodedBytes, 0, totalByteCount);
            }

            /*
                 now LENGTH(second byte) has 1xxxxxxx form.
                 the MSB is 1, so, rest 7 bits are used to find the number of bytes used to encode LENGTH
                 Example:
                     byte[] =      [00010110, 10000001, 11111111, 01000001, 01000001, 01000001 .... 01000001]
                            Index ->   0,         1,        2,       3,         4,        5,    ....   256

                          TAG(IA5String=22) byte                               = 00010110 (first byte)
                          LENGTH byte                                          = 10000001 (second byte)
                          LENGTH byte MSB                                      = 1
                          LENGTH byte 7 LS bits(LENGTH Encoding Byte Number)   = 0000001
                          LENGTH Encoding Byte Number in decimal               = 1
                          LENGTH Encoding Byte array                           = [11111111]
                          Decimal value of LENGTH Encoding Byte array          = 255 (binary string converted to dec)
                          VALUE Array                                          = [01000001, 01000001 ....]255 times
                          Binary String of character A                         = 01000001

                LENGTH Encoding Byte Number in decimal:
                          10000001 (LENGTH)
                        & 01111111 (0x7f) masking MSB
                        ------------------------
                          00000001 -> 1 (decimal)

                Decimal value of LENGTH Encoding Byte array:
                            1. array = [11111111] -> 255
                            // if array.size > 1, then Big Endian is used for converting binary string to decimal.
                            2. e.g. array = [000000001, 11111111] -> 511 [0000000111111111] binary string concatenated
            */
            // get number of bytes for encoding length
            int byteCountForEncodingLength = asn1LengthByte & 0x7F; // removes the MSB
            int metaDataByteCount = 2 + byteCountForEncodingLength; // number of bytes used by ASN1 information.
            // now LENGTH encoding byte starts from 3rd byte (index 2) + number of bytes used to encode LENGTH
            byte[] bytesForEncodingLength = Arrays.copyOfRange(base64DecodedBytes, 2, metaDataByteCount);
            int dataByteCount = calculateDataLength(bytesForEncodingLength); // number of bytes for actual data
            totalByteCount = metaDataByteCount + dataByteCount;
            if (totalByteCount <= base64DecodedBytes.length) {
                return Arrays.copyOfRange(base64DecodedBytes, 0, totalByteCount);
            }
            /*
                 more bytes need to be read.
                 dataByteCount > (base64DecodedBytes.length - metaDataByteCount)
                     e.g. 1200 > (1024 - 4)
                 remaining required bytes = 180 ( 1200 - ( 1024 - 4 ))
             */

            int moreByteToReadSize = dataByteCount - (base64DecodedBytes.length - metaDataByteCount);
            byteArrayOutputStream.write(base64DecodedBytes);
            int offset = base64DecodedBytes.length;
            while (moreByteToReadSize > 0) {
                if (moreByteToReadSize < MAX_BUFFER_SIZE) {
                    base64DecodedBytes = readData(handle, cardTokenFileType.getValue(), offset, moreByteToReadSize);
                    if (moreByteToReadSize != base64DecodedBytes.length) {
                        LOGGER.log(Level.SEVERE, () -> "****Error: Failed to read required bytes.");
                        throw new GenericException("Failed to read required bytes from card/token.");
                    }
                    byteArrayOutputStream.write(base64DecodedBytes);
                    break;
                }
                base64DecodedBytes = readData(handle, cardTokenFileType.getValue(), offset, MAX_BUFFER_SIZE);
                byteArrayOutputStream.write(base64DecodedBytes);
                moreByteToReadSize -= base64DecodedBytes.length;
                offset += base64DecodedBytes.length;
            }
            byte[] allReadBytes = byteArrayOutputStream.toByteArray();
            if (allReadBytes.length != totalByteCount) {
                LOGGER.log(Level.SEVERE, () -> "****Error: Required byte length and read byte length are not matched.");
                throw new GenericException("The required byte length and read byte length are not matched.");
            }
            return allReadBytes;
        } catch (Exception ex) {
            throw new GenericException(ex.getMessage());
        }
    }

    public static int calculateDataLength(byte[] byteArray) {
        StringBuilder binaryString = new StringBuilder(byteArray.length * 8);
        for (byte b : byteArray) {
            for (int i = 7; i >= 0; i--) {
                binaryString.append((b >> i) & 1);
            }
        }
        return Integer.parseInt(binaryString.toString(), 2);
    }


    /**
     * decodes base64 encoded data and give actual data.
     * read max 1024 bytes
     * Caller must handle the exception.
     *
     * @return actual data in byte[]
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    private static byte[] readData(int handle, int whichData, int offset, int requestLength) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRReadDataReqDto(handle, whichData, offset, requestLength));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRReadDataResDto crReadDataResDto = LocalNavalWebServiceApi.postReadData(reqData);
        jniErrorCode = crReadDataResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****ReadDataErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
        return Base64.getDecoder().decode(crReadDataResDto.getResponse());
    }

    /**
     * stores buffered data
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void storeBufferedData(int handle, CardTokenFileType cardTokenFileType, byte[] bytes) {
        int offset = 0;
        // for writing multiple times
        int times = bytes.length / MAX_BUFFER_SIZE;
        int extraBytes = bytes.length % MAX_BUFFER_SIZE;
        if (times < 1) {
            storeData(handle, cardTokenFileType.getValue(), offset, bytes);
            return;
        }
        for (int i = 0; i < times; i++) {
            byte[] temp = Arrays.copyOfRange(bytes, offset, offset + MAX_BUFFER_SIZE);
            storeData(handle, cardTokenFileType.getValue(), offset, temp);
            offset += MAX_BUFFER_SIZE;
        }
        if (extraBytes > 0) {
            byte[] temp = Arrays.copyOfRange(bytes, offset, offset + extraBytes);
            storeData(handle, cardTokenFileType.getValue(), offset, temp);
        }
    }


    /**
     * private API.
     * Should not be used by client.
     * store max 1024-byte
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    private static void storeData(int handle, int whichData, int offset, byte[] bytes) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRStoreDataReqDto(handle, whichData, offset, Base64.getEncoder().encodeToString(bytes), bytes.length));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crApiResDto = LocalNavalWebServiceApi.postStoreData(reqData);
        jniErrorCode = crApiResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****StoreDataErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * verifies Certificate chain
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */

    public static void verifyCertificate(int handle, int whichTrust, int whichCertificate, byte[] bytes) {
        if (bytes.length > MAX_SYSTEM_CERTIFICATE_SIZE) {
            LOGGER.log(Level.SEVERE, () -> "****VerifyCertificateError: Certificate size exceeded the allowed limit. Length: " + bytes.length);
            throw new GenericException("Certificate size exceeded the allowed limit.");
        }
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRVerifyCertificateReqDto(handle, whichTrust, whichCertificate, Base64.getEncoder().encodeToString(bytes), bytes.length));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crApiResDto = LocalNavalWebServiceApi.postVerifyCertificate(reqData);
        jniErrorCode = crApiResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****VerifyCertificateErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * authenticate two cards
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void pkiAuth(int handle1, int handle2) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRPkiAuthReqDto(handle1, handle2));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crApiResDto = LocalNavalWebServiceApi.postPkiAuth(reqData);
        jniErrorCode = crApiResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****PkiAuthErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * disconnect the incoming card
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void waitForRemoval(int handle) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRWaitForRemovalReqDto(handle));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crApiResDto = LocalNavalWebServiceApi.postWaitForRemoval(reqData);
        jniErrorCode = crApiResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****WaitForRemovalErrorCode: " + jniErrorCode);
        if (jniErrorCode != 0) {
            throw new GenericException(LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        }
    }

    /**
     * verifies pin
     * Caller must handle the exception.
     *
     * @throws ConnectionTimeoutException - on timeout or response status code not 200
     * @throws GenericException           - on Exception
     */
    public static void verifyPin(int handle, String pin) {
        String reqData;
        try {
            reqData = Singleton.getObjectMapper().writeValueAsString(new CRVerifyPinReqDto(handle, Base64.getEncoder().encodeToString(pin.getBytes()), pin.length()));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex::getMessage);
            throw new GenericException(GENERIC_ERR_MSG);
        }
        CRApiResDto crApiResDto = LocalNavalWebServiceApi.postVerifyPin(reqData);
        jniErrorCode = crApiResDto.getRetVal();
        LOGGER.log(Level.SEVERE, () -> "****VerifyPinErrorCode: " + jniErrorCode + " Message: " + LocalCardReaderErrMsgUtil.getMessage(jniErrorCode));
        if (jniErrorCode != 0) {
            if (-1310891072 == jniErrorCode) {
                throw new GenericException("PIN attempts exhausted for the card.");
            }
            throw new GenericException("Wrong pin.");
        }
    }
}
