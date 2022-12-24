/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.util;

/**
 * @author root
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class TestProp {
    public static void main(String[] args) throws Exception {
        FileReader reader = new FileReader("/etc/file.properties");

        Properties p = new Properties();
        p.load(reader);

        // System.out.println(p.getProperty("user"));
        // System.out.println(p.getProperty("password"));
    }

    public Properties getProp() throws FileNotFoundException, IOException {
        FileReader reader = new FileReader("/etc/file.properties");

        Properties p = new Properties();
        p.load(reader);
        return p;
    }

}
