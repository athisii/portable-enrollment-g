/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.controller.BiometricCaptureCompleteController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class DeleteSavedJsonFile {

    TestProp prop = new TestProp();

    public void delSavedfile() {

        PrintWriter writer = null;
        try {
            String objFilePath = prop.getProp().getProperty("saveenrollment");
            File savedFile = new File(objFilePath);
            writer = new PrintWriter(savedFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DeleteSavedJsonFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        writer.print("");
        writer.close();

    }

}
