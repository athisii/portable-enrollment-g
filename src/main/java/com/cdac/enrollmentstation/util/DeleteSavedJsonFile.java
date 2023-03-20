/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.util;

import com.cdac.enrollmentstation.constant.PropertyName;
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

    public void delSavedfile() {

        PrintWriter writer = null;
        try {
            String objFilePath = PropertyFile.getProperty(PropertyName.SAVE_ENROLLMENT);
            File savedFile = new File(objFilePath);
            writer = new PrintWriter(savedFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BiometricCaptureCompleteController.class.getName()).log(Level.SEVERE, null, ex);
        }
        writer.print("");
        writer.close();

    }

}
