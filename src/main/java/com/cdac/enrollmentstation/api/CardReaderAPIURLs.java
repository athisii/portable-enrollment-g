/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class CardReaderAPIURLs {
    public static final Logger LOGGER = ApplicationLog.getLogger(CardReaderAPIURLs.class);

    public String getInitializeURL() {
        // return "http://localhost:8088/N_Initialize"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.INITIALIZE));
    }

    public String getWaitConnect() {
        // return "http://localhost:8088/N_Wait_for_Connect"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.WAIT_FOR_CONNECT));
    }

    public String getSelectApp() {
        //  return "http://localhost:8088/N_SelectApp"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.SELECT_APP));
    }

    public String readDataFromNaval() {
        // return "http://localhost:8088/N_readDatafromNaval"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.READ_DATA));
    }

    public String storeDataOnNaval() {
        // return "http://localhost:8088/N_storeDataonNaval"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.STORE_DATA));
    }

    public String verifyCertificate() {
        //  return "http://localhost:8088/N_verifyCertificate"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.VERIFY_CERT));
    }

    public String pkiAuth() {
        //  return "http://localhost:8088/N_PKIAuth"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.PKI_AUTH));
    }

    public String cardRemoval() {
        //  return "http://localhost:8088/N_Wait_for_Removal"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.WAIT_FOR_REMOVAL));
    }

    public String deInitialize() {
        //   return "http://localhost:8088/N_DeInitialize"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.DE_INITIALIZE));
    }

    public String getListOfReaders() {
        //   return "http://localhost:8088/listOfReaders"
        return requireNonBlank(PropertyFile.getProperty(PropertyName.LIST_OF_READERS));
    }

    public String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            LOGGER.log(Level.SEVERE, () -> "Property '" + value + "' is empty or not found in " + ApplicationConstant.DEFAULT_PROPERTY_FILE + ". Please add it");
            throw new GenericException("Property '" + value + "' is empty or not found in "+ ApplicationConstant.DEFAULT_PROPERTY_FILE + ". Please add it");
        }
        return value;
    }


}
