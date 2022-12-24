/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.api;

import com.cdac.enrollmentstation.util.TestProp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class CardReaderAPIURLs {

    TestProp prop = new TestProp();

    public String getInitializeURL() {

        String initialize = null;
        try {
            initialize = prop.getProp().getProperty("initialize");
            if (initialize.isBlank() || initialize.isEmpty() || initialize == null) {
                System.out.println("The property 'inputfile' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        // return "http://localhost:8088/N_Initialize";
        // return "http://localhost:8088/N_Initialize";

        return initialize;
    }

    public String getWaitConnect() {
        String waitforconnect = null;
        try {
            waitforconnect = prop.getProp().getProperty("waitforconnect");
            if (waitforconnect.isBlank() || waitforconnect.isEmpty() || waitforconnect == null) {
                System.out.println("The property 'waitforconnect' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        // return "http://localhost:8088/N_Wait_for_Connect";
        // return "http://localhost:8088/N_Wait_for_Connect";

        return waitforconnect;

    }

    public String getSelectApp() {
        String selectapp = null;
        try {
            selectapp = prop.getProp().getProperty("selectapp");
            if (selectapp.isBlank() || selectapp.isEmpty() || selectapp == null) {
                System.out.println("The property 'selectapp' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //  return "http://localhost:8088/N_SelectApp";
        //  return "http://localhost:8088/N_SelectApp";

        return selectapp;

    }

    public String readDataFromNaval() {
        String readdata = null;
        try {
            readdata = prop.getProp().getProperty("readdata");
            if (readdata.isBlank() || readdata.isEmpty() || readdata == null) {
                System.out.println("The property 'readdata' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //   return "http://localhost:8088/N_readDatafromNaval";
        //   return "http://localhost:8088/N_readDatafromNaval";

        return readdata;


    }

    public String storeDataOnNaval() {
        String storedata = null;
        try {
            storedata = prop.getProp().getProperty("storedata");
            if (storedata.isBlank() || storedata.isEmpty() || storedata == null) {
                System.out.println("The property 'storedata' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        // return "http://localhost:8088/N_storeDataonNaval";
        // return "http://localhost:8088/N_storeDataonNaval";

        return storedata;

    }

    public String verifyCertificate() {
        String verifycert = null;
        try {
            verifycert = prop.getProp().getProperty("verifycert");
            if (verifycert.isBlank() || verifycert.isEmpty() || verifycert == null) {
                System.out.println("The property 'verifycert' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //  return "http://localhost:8088/N_verifyCertificate";
        //  return "http://localhost:8088/N_verifyCertificate";

        return verifycert;

    }

    public String PKIAuth() {
        String pkiauth = null;
        try {
            pkiauth = prop.getProp().getProperty("pkiauth");
            if (pkiauth.isBlank() || pkiauth.isEmpty() || pkiauth == null) {
                System.out.println("The property 'pkiauth' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //  return "http://localhost:8088/N_PKIAuth";
        //  return "http://localhost:8088/N_PKIAuth";

        return pkiauth;


    }

    public String cardRemoval() {
        String waitforremoval = null;
        try {
            waitforremoval = prop.getProp().getProperty("waitforremoval");
            if (waitforremoval.isBlank() || waitforremoval.isEmpty() || waitforremoval == null) {
                System.out.println("The property 'waitforremoval' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //  return "http://localhost:8088/N_Wait_for_Removal";
        //  return "http://localhost:8088/N_Wait_for_Removal";

        return waitforremoval;

    }

    public String deInitialize() {
        String deinitialize = null;
        try {
            deinitialize = prop.getProp().getProperty("deinitialize");
            if (deinitialize.isBlank() || deinitialize.isEmpty() || deinitialize == null) {
                System.out.println("The property 'deinitialize' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //   return "http://localhost:8088/N_DeInitialize";
        //   return "http://localhost:8088/N_DeInitialize";

        return deinitialize;


    }

    public String getListofReaders() {
        String listofreaders = null;
        try {
            listofreaders = prop.getProp().getProperty("listofreaders");
            if (listofreaders.isBlank() || listofreaders.isEmpty() || listofreaders == null) {
                System.out.println("The property 'listofreaders' is empty, Please add it in properties");
                return null;
            }


        } catch (IOException ex) {
            Logger.getLogger(CardReaderAPIURLs.class.getName()).log(Level.SEVERE, "Could not retireive Property", ex);
        }
        //   return "http://localhost:8088/listOfReaders";
        //   return "http://localhost:8088/listOfReaders";

        return listofreaders;

    }


}
