/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.service;

/**
 * @author root
 */

import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryLookup {

    private static final Logger LOGGER = ApplicationLog.getLogger(DirectoryLookup.class);

    public static String doLookup(String username, String password) {
        String result;
        String domain = PropertyFile.getProperty(PropertyName.DOMAIN);
        String ldapUrl = PropertyFile.getProperty(PropertyName.LDAP_URL);
        // domain = "CDACAD"
        // ldapUrl = "ldap://10.184.36.14"
        String securityPrincipal = domain + "\\" + username;
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        properties.put(Context.PROVIDER_URL, ldapUrl);
        properties.put(Context.SECURITY_AUTHENTICATION, "simple");
        properties.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        properties.put(Context.SECURITY_CREDENTIALS, password);
        try {
            new InitialDirContext(properties);
            result = "true";
        } catch (CommunicationException e) {
            LOGGER.log(Level.SEVERE, () -> "Failed to connect with ldap server");
            result = "Failed to connect with ldap server";
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, () -> "Failed to authenticate user");
            result = "Failed to authenticate user";
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, () -> "Naming exception occurred");
            result = "Invalid credentials";
        }
        return result;
    }

    public static void main(String[] args) {
        DirectoryLookup sample = new DirectoryLookup();
        //String result = sample.doLookup("r101", "boss");
        //String result = sample.doLookup("CDACAD\\sudhakar", "Root1234#$");
        String result = sample.doLookup("sudhakar", "Root1234#$");
        System.out.println("Ldap Result::" + result);
    }

}