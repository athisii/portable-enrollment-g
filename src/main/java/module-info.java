module com.cdac.enrollmentstation {
//    requires java.base;
    requires java.net.http;
    requires java.logging;
    requires java.naming;


    //javafx
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;
    requires javafx.web;

    // device sdks
    requires MantraUtility;
    requires MIDIris.Enroll;
    requires MIDFingerAuth;
    requires iengine.ansi.iso.main;
    requires sdk.commons.main;

    // transitively import ans1.runtime
    requires asn1.converter;
    // for ans1 encoded hex decoding
    requires org.bouncycastle.provider;

    // python
    requires opencv;
    requires jython;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    // xml
    requires java.xml.bind;

    requires org.apache.commons.io;
    requires json.io;

    opens com.cdac.enrollmentstation to javafx.fxml;
    opens RealScan to javafx.fxml;
    exports com.cdac.enrollmentstation;
    exports RealScan;
    opens com.cdac.enrollmentstation.controller to javafx.fxml;
    exports com.cdac.enrollmentstation.controller;
    opens com.cdac.enrollmentstation.model to javafx.fxml;
    exports com.cdac.enrollmentstation.model;
    exports com.cdac.enrollmentstation.dto;
    opens com.cdac.enrollmentstation.dto to javafx.fxml;
    exports com.cdac.enrollmentstation.security;
    opens com.cdac.enrollmentstation.security to javafx.fxml;
    exports com.cdac.enrollmentstation.api;
    opens com.cdac.enrollmentstation.api to javafx.fxml;
    exports com.cdac.enrollmentstation.logging;
    opens com.cdac.enrollmentstation.logging to javafx.fxml;
    exports com.cdac.enrollmentstation.util;
    opens com.cdac.enrollmentstation.util to javafx.fxml;


    requires json;

    requires static lombok;
    requires MFS;


}
