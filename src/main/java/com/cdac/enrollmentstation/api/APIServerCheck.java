/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.security.CryptoAES256;
import com.cdac.enrollmentstation.security.HmacUtils;
import com.cdac.enrollmentstation.security.PKIUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static String getEnrollmentSaveURL() {
        return getMafisApiServer() + "/SaveEnrollment";
    }


    public static String getMafisApiServer() {
        return ServerAPI.getMafisApiUrl();
    }

}
    

