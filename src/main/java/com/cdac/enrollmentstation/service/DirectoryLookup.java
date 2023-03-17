/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cdac.enrollmentstation.service;

/**
 * @author root
 */

import com.cdac.enrollmentstation.constant.ApplicationConstant;
import com.cdac.enrollmentstation.constant.PropertyName;
import com.cdac.enrollmentstation.exception.GenericException;
import com.cdac.enrollmentstation.logging.ApplicationLog;
import com.cdac.enrollmentstation.util.PropertyFile;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryLookup {

    private static final Logger LOGGER = ApplicationLog.getLogger(DirectoryLookup.class);

    public static void doLookup(String username, String password) {
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
        } catch (AuthenticationException e) {
            LOGGER.log(Level.SEVERE, () -> "Failed to authenticate user");
            throw new GenericException(ApplicationConstant.INVALID_CREDENTIALS);
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, e::getMessage);
            throw new GenericException(ApplicationConstant.GENERIC_ERR_MSG);
        }
    }

}