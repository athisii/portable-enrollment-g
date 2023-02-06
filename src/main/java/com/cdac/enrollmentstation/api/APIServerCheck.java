/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.CryptoAES256;
import com.cdac.enrollmentstation.security.HmacUtils;
import com.cdac.enrollmentstation.security.PKIUtil;
import com.cdac.enrollmentstation.util.PropertyFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author root
 */
public class APIServerCheck {
    String sessionkey;
    private static final Logger LOGGER = ApplicationLog.getLogger(APIServerCheck.class);


    public static String checkGetARCNoAPI(String url, String arcNo) {

        String result = "";
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            String jsonInputString = "{\"ARCNo\": \"" + arcNo + "\"}";
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result = "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "Exception: " + e.getMessage());


        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
        return result;
    }


    public String getDemoGraphicDetailsAPI(String url, String unitid) {

        System.out.println("inside demographic");

        //String unitid1="123";
        String unitid1 = getUnitID();
        //String jsonInputString = "{\"unitId\": \""+unitid1+"\"}";
        //String jsonInputString = "{\"unitId\": \""+unitid+"\"}";
        //String jsonInputString = "{\"unitCode\": \""+unitid1+"\"}";
        String jsonInputString = "{\"unitCode\": \"" + unitid + "\"}";
        System.out.println("josn string : " + jsonInputString);
//        String encString = "";
//        try {
//            encString = aes256.encryptString(jsonInputString, strKey);
//        } catch (Exception ex) {
//            Logger.getLogger(APIServerCheck.class.getName()).log(Level.SEVERE, null, ex);
//        }
        String result = "";
        StringBuilder response = new StringBuilder();
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {

            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            // con.setRequestProperty("SessionKey", getuuid);
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";

            try (OutputStream os = con.getOutputStream()) {
                //byte[] input = encString.getBytes("utf-8");
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            //  sessionkey = con.getHeaderField("SessionKey");

        } catch (Exception e) {
            result = "Exception: " + e.getMessage() + "Try Again";
            //System.out.println(url + "\t\tStatus:" + result);
            LOGGER.log(Level.INFO, "\t\tStatus:" + result + url);
            return result;
        }

        return response.toString();
    }

    public String getStatusTokenUpdate(String url, String testjson) {
        String result = "";
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";

            //String jsonInputString = "{\"ContractorID\": \"CONTRACT001\" ,\"CardSerialNo\": \"1234567890\"}";
            System.out.println("josn string : " + testjson);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = testjson.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result =json "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "Exception: " + e.getMessage());

        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
        return result;
    }

    public String getStatusContractListAPI(String url) {

        String result = "";
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";

            String jsonInputString = "{\"ContractorID\": \"CONTRACT001\" ,\"CardSerialNo\": \"1234567890\"}";
            System.out.println("josn string : " + jsonInputString);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result =json "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "Exception: " + e.getMessage());

        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + result + url);
        return result;
    }

    public String getContractListAPI(String url, String contractorID, String cardSerialNo) {

        System.out.println("inside contract list");
        CryptoAES256 aes256 = new CryptoAES256();
        String getuuid = aes256.generateRandomUUID();
        getuuid = getuuid.replace("-", "");
        System.out.println("guid : " + getuuid.length());
        Key strKey = aes256.generateKey32(getuuid);
        String jsonInputString = "{\"ContractorID\": \"" + contractorID + "\" ,\"CardSerialNo\": \"" + cardSerialNo + "\"}";
        //System.out.println("josn string : "+ jsonInputString);
        LOGGER.log(Level.INFO, "josn string : " + jsonInputString);
        String encString = "";
        try {
            encString = aes256.encryptString(jsonInputString, strKey);
        } catch (Exception ex) {
            Logger.getLogger(APIServerCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        String result = "";
        StringBuilder response = new StringBuilder();
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {

            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("SessionKey", getuuid);
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = encString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {

                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            sessionkey = con.getHeaderField("SessionKey");
            LOGGER.log(Level.INFO, "Session Key in contraclist::" + sessionkey);

        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            //System.out.println(url + "\t\tStatus:" + result);
            LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
            return result;
        }

        return response.toString();
    }

    public String getStatusLabourListAPI(String url) {

        String result = "";
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";

            String jsonInputString = "{\"ContractorID\": \"CONTRACT001\" ,\"ContractID\": \"1234567890\"}";
            System.out.println("josn string : " + jsonInputString);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result =json "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "get Labour list" + result);

        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
        return result;
    }

    public String getStatusUnitListAPI(String url) {

        String result = "";
        int code = 200;
        int noOfRetries = 1;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result =json "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);

        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
        return result;
    }


    public String getLabourListAPI(String url, String contractorID, String contractID) {

        System.out.println("inside contract list");
        CryptoAES256 aes256 = new CryptoAES256();
        String getuuid = aes256.generateRandomUUID();
        getuuid = getuuid.replace("-", "");
        System.out.println("guid : " + getuuid.length());
        Key strKey = aes256.generateKey32(getuuid);
        String jsonInputString = "{\"ContractorID\": \"" + contractorID + "\" ,\"ContractID\": \"" + contractID + "\"}";
        System.out.println("josn string : " + jsonInputString);
        String encString = "";
        try {
            encString = aes256.encryptString(jsonInputString, strKey);
        } catch (Exception ex) {
            Logger.getLogger(APIServerCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        String result = "";
        StringBuilder response = new StringBuilder();
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {

            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("SessionKey", getuuid);
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = encString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {

                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            sessionkey = con.getHeaderField("SessionKey");
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            System.out.println(url + "\t\tStatus:" + result);
            return result;
        }

        return response.toString();
    }

    public String getUnitListAPI(String url) {

        System.out.println("inside unit list");

        String result = "";
        StringBuilder response = new StringBuilder();
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";


            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {

                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            sessionkey = con.getHeaderField("SessionKey");
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            //System.out.println(url + "\t\tStatus:" + result);
            LOGGER.log(Level.INFO, "Exception: " + e.getMessage());
            //con.close()
            return result;
        }

        return response.toString();
    }

    public String getARCNoAPI(String url, String arcNo) {


        //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";
        String jsonInputString = "{\"ARCNo\": \"" + arcNo + "\"}";

        String result = "";

        String response = null;
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {

            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            //con.setRequestProperty("UniqueKey", getuuid);
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);
            //String arcNo = "123abc";
            //String jsonInputString = "{\"ContractorID\": \""+contractorID+"\" ,\"CardSerialNo\": \""+cardSerialNo+"\"}";

            try (OutputStream os = con.getOutputStream()) {
                //Uncomment Afterwards
                //byte[] input = encString.getBytes("utf-8");
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {

                response = br.lines().collect(Collectors.joining());
            }

        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            //System.out.println(url + "\t\tStatus:" + result);
            LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
            return result;
        }
        return response;
    }


    public String checkGetEnrollmentSaveAPI(String url, String postJson) {

        String result = "";
        int code = 200;
        int noOfRetries = 3;
        String status = "";
        try {
            URL siteURL = new URL(url);

            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(20000);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = postJson.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            while (noOfRetries > 0) {
                con.connect();

                code = con.getResponseCode();
                if (code == 200) {
                    //result = "-> Green <-\t" + "Code: " + code;
                    result = "connected";
                    break;
                } else {
                    result = "notreachable";
                }
                noOfRetries--;
            }
        } catch (Exception e) {
            result = "Exception: " + e.getMessage();
            LOGGER.log(Level.INFO, "Get Enrollment Save" + result);

        }
        //System.out.println(url + "\t\tStatus:" + result);
        LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
        return result;
    }

    public String getEnrollmentSaveAPI(String url, String postJson) {

        String result = "";
        StringBuilder response = new StringBuilder();
        String decResponse = "";
        CryptoAES256 aes256 = new CryptoAES256();
        //skey = aes256.getAESKey();
        String getuuid = aes256.generateRandomUUID();
        getuuid = getuuid.replace("-", "");
        System.out.println("guid : " + getuuid.length());
        Key strKey = aes256.generateKey32(getuuid);
        String encstr;
        try {
            //encstr = aes256.encryptString("test", strKey);
            //String dec = aes256.decryptStringSK(encstr, strKey);
            //System.out.println("dec string :"+ dec);
            postJson = postJson.replace("\n", "");
            String encryptedJson = "";
            encryptedJson = aes256.encryptString(postJson, strKey);
            System.out.println("URL::" + url);
            System.out.println("Post JSON::" + postJson);
            System.out.println("Encrypted JSON::" + encryptedJson);
            // FileUtils.writeStringToFile(new File("/home/enadmin/saveBio.txt"), encryptedJson);
            // FileUtils.writeStringToFile(new File("/home/enadmin/saveBiojson.txt"), postJson);

            // TO be uncommented later
            byte[] pkigetuuid = null;
            PKIUtil pki = new PKIUtil();
            pkigetuuid = pki.encrypt_test(getuuid);
            System.out.println("PKI Get UUid" + pkigetuuid);
            String encodedBase64getuuid = Base64.getEncoder().encodeToString(pkigetuuid);
            System.out.println("getuuid:" + getuuid);
            System.out.println("getuuidpkiencryptbase64 Bytes" + encodedBase64getuuid);

            //Hashvalue for JSON
            HmacUtils hm = new HmacUtils();
            String messageDigestJson = hm.generateHmac256(encryptedJson, getuuid.getBytes());
            System.out.println("messageDigestJson::" + messageDigestJson);

            URL siteURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            //con.setRequestProperty("SessionKey", getuuid);
            //con.setRequestProperty("UniqueKey", getuuid);
            con.setRequestProperty("UniqueKey", encodedBase64getuuid);
            con.setRequestProperty("HashKey", messageDigestJson);
            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(30000);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = encryptedJson.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                    System.out.println("Response:::" + response);
                }
                br.close();

            }

            Map<String, List<String>> map = con.getHeaderFields();
            Boolean isSessionKeyPresent = false;
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getKey() == null)
                    continue;
                System.out.println("Key : " + entry.getKey() +
                        " ,Value : " + entry.getValue());
                //if(entry.getKey().contains("SessionKey")){
                if (entry.getKey().contains("UniqueKey")) {
                    isSessionKeyPresent = true;
                }
            }

            String secKey = "";
            if (isSessionKeyPresent) {
                //secKey = con.getHeaderField("SessionKey");
                secKey = con.getHeaderField("UniqueKey");
                System.out.println("Unique key :" + secKey);
            } else {
                result = "Exception: " + "Unique Key From Server is Empty";
                return result;
            }

            //PKI Decrypt Session Key
            byte[] base64decodesessionkey = Base64.getDecoder().decode(secKey);
            String decodedString = new String(base64decodesessionkey);
            System.out.println("decodedString:::" + decodedString);

            String sessKey = "";
            sessKey = pki.decrypt(base64decodesessionkey);
            System.out.println("Decrypted PKI Session Key:::" + sessKey);

            //Pass Decrypted Session Key to AES Algo
            CryptoAES256 aesdec = new CryptoAES256(sessKey);
            decResponse = aesdec.decryptString(response.toString());


        } catch (Exception ex) {
            //Logger.getLogger(APIServerCheck.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println("In GET Enrollment Save API Exception"+ex);
            result = "Exception: " + ex.getMessage();
            LOGGER.log(Level.INFO, "\t\tStatus:" + url + result);
            return result;
        }


        //return result;
        //System.out.println("In GET Enrollment Save API"+decResponse);
        LOGGER.log(Level.INFO, "In GET Enrollment Save API:" + decResponse);
        return decResponse;
    }


    public static String getArcUrl() {
        return getMafisApiServer() + "/GetDetailsByARCNo";
    }

    public static String getContractListURL() {
        return getMafisApiServer() + "/GetContractList";
    }

    public static String getLabourListURL() {
        return getMafisApiServer() + "/GetLabourList";
    }

    public static String getUnitListURL() {
        return getMafisApiServer() + "/GetAllUnits";
    }

    public static String getTokenUpdateURL() {
        return getMafisApiServer() + "/UpdateTokenStatus";
    }

    public static String getEnrollmentSaveURL() {
        return getMafisApiServer() + "/SaveEnrollment";
    }

    public static String getDemographicURL() {
        return getMafisApiServer() + "/GetDemographicDetails";
    }


    public static String getMafisApiServer() {
        String mafisServerApi = "";
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

    public static String getUnitID() {
        String unitID = "";
        try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
            String line = " ";
            String input = " ";
            while ((line = file.readLine()) != null) {
                String[] tokens = line.split(",");
                unitID = tokens[0];

            }
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Problem reading file.");
            LOGGER.log(Level.INFO, "Problem reading file /etc/data.txt");
        }
        return unitID;
    }

    public String getStationID() {
        String stationID = "";
        try (BufferedReader file = new BufferedReader(new FileReader("/etc/data.txt"))) {
            String line = " ";
            String input = " ";
            while ((line = file.readLine()) != null) {
                String[] tokens = line.split(",");
                stationID = tokens[2];

            }
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Problem reading file.");
            LOGGER.log(Level.INFO, "Problem reading file /etc/data.txt");
        }
        return stationID;
    }

}
    

