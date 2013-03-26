/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.interfaces.MailResourceIntf;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.repository.ResourceProperty;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * This class represent the configuration information of the JavaMail
 * Session object within J2EE.
 */
public class MailConfiguration implements Serializable {
    /* IASRI 4629057
    private String username = "";
    private String mailFrom = "";
    private String mailHost = "";
    private static String MAIL_FROM = "mail.from";
    private static String MAIL_USER = "mail.user";
    private static String MAIL_HOST = "mail.host";
    */

    // START OF IASRI 4629057
    private static String PROTOCOL_TYPE_IMAP = "imap";
    private static String PROTOCOL_TYPE_POP3 = "pop3";
    private static String PROTOCOL_TYPE_SMTP = "smtp";

    private static String PROP_NAME_PREFIX_LEGACY = "mail-";
    private static char PROP_NAME_DELIM_LEGACY = '-';
    
    private static String DEF_VAL_STORE_PROTOCOL = PROTOCOL_TYPE_IMAP;
    private static String DEF_VAL_STORE_PROTOCOL_CLASS =
        "com.sun.mail.imap.IMAPStore";
    private static String DEF_VAL_TRANSPORT_PROTOCOL = PROTOCOL_TYPE_SMTP;
    private static String DEF_VAL_TRANSPORT_PROTOCOL_CLASS =
        "com.sun.mail.smtp.SMTPTransport";
    private static String DEF_VAL_HOST = "localhost";
    private static String DEF_VAL_USER = "user.name";
    private static String DEF_VAL_FROM = "username@host";
    private static boolean DEF_VAL_DEBUG = false;

    private static String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    private static String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static String MAIL_HOST = "mail.host";
    private static String MAIL_USER = "mail.user";
    private static String MAIL_FROM = "mail.from";
    private static String MAIL_DEBUG = "mail.debug";

    private static String MAIL_PREFIX = "mail.";
    private static String MAIL_SUFFIX_CLASS = ".class";
    private static String MAIL_SUFFIX_HOST = ".host";
    private static String MAIL_SUFFIX_USER = ".user";
    private static char MAIL_DELIM = '.';

    /**
     * Mail resource attributes
     */
    private String description = "";
    private String jndiName = "";
    private boolean enabled = false;
    private String storeProtocol = DEF_VAL_STORE_PROTOCOL;
    private String storeProtocolClass = DEF_VAL_STORE_PROTOCOL_CLASS;
    private String transportProtocol = DEF_VAL_TRANSPORT_PROTOCOL;
    private String transportProtocolClass = DEF_VAL_TRANSPORT_PROTOCOL_CLASS;
    private String mailHost = DEF_VAL_HOST;
    private String username = DEF_VAL_USER;
    private String mailFrom = DEF_VAL_FROM;
    private boolean debug = DEF_VAL_DEBUG;

    private Properties mailProperties = new Properties();
    // END OF IASRI 4629057

    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static Logger _logger = DOLUtils.getDefaultLogger();
    
    /** 
     * This constructs the mail configuration based on the username and
     * the from address. This constructor is deprecated.
     * @param the username
     * @param the from address
     * @deprecated 
     */
    public MailConfiguration(String username, String mailFrom) {
	this.username = username;
	this.mailFrom = mailFrom;
	this.mailHost = "";

        mailProperties.put(MAIL_FROM, this.getMailFrom());
        mailProperties.put(MAIL_USER, this.getUsername());
        mailProperties.put(MAIL_HOST, this.getMailHost());
    }
    
    /** 
     * Construct a specification of mail configuration with the given username,
     * Mail From Address and mail hostname. 
     * @param the username.
     * @param the from address.
     * @param the mail hostname.
     */
    public MailConfiguration(String username, 
			     String mailFrom, 
			     String mailHost) {
	this.username = username;
	this.mailFrom = mailFrom;
	this.mailHost = mailHost;

        mailProperties.put(MAIL_FROM, this.getMailFrom());
        mailProperties.put(MAIL_USER, this.getUsername());
        mailProperties.put(MAIL_HOST, this.getMailHost());
    }

    // START OF IASRI 4629057
    // START OF IASRI 4650786
    /** 
     * Construct a specification of mail configuration.
     */
    public MailConfiguration(MailResourceIntf mailRes) {
        try {
            loadMailResources(mailRes);
        }
        catch (Exception ce) {
            _logger.log(Level.INFO,"enterprise.deployment_mail_cfgexcp",ce);

        }
    }

    /** 
     * Load all configuration information from the mail resource node in
     * server.xml for the JavaMail Session object within J2EE.
     */
    private void loadMailResources(MailResourceIntf mailResource) throws Exception {

        if (mailResource == null) {
            _logger.log(Level.FINE,
                        "MailConfiguration: no MailResource object. mailResource=null");
            return;
        }
        
        jndiName = mailResource.getName();
        description = mailResource.getDescription();
        enabled = mailResource.isEnabled();

        storeProtocol = mailResource.getStoreProtocol();
        storeProtocolClass = mailResource.getStoreProtocolClass();
        transportProtocol = mailResource.getTransportProtocol();
        transportProtocolClass = mailResource.getTransportProtocolClass();
        mailHost = mailResource.getMailHost();
        username = mailResource.getUsername();
        mailFrom = mailResource.getMailFrom();
        debug = mailResource.isDebug();

        // Save to Property list
        String storeProtocolClassName = MAIL_PREFIX + storeProtocol +
            MAIL_SUFFIX_CLASS;
        String transportProtocolClassName = MAIL_PREFIX + transportProtocol +
            MAIL_SUFFIX_CLASS;

        mailProperties.put(MAIL_STORE_PROTOCOL, storeProtocol);
        mailProperties.put(MAIL_TRANSPORT_PROTOCOL, transportProtocol);
        mailProperties.put(storeProtocolClassName, storeProtocolClass);
        mailProperties.put(transportProtocolClassName, transportProtocolClass);
        mailProperties.put(MAIL_FROM, mailFrom);
        mailProperties.put(MAIL_DEBUG, (debug ? "true" : "false"));

        // Get the properties and save to Property list
        Set properties = mailResource.getProperties();
        ResourceProperty property = null;
        String name = null;
        String value = null;

        String protRelatedHostName = MAIL_PREFIX + storeProtocol +
                        MAIL_SUFFIX_HOST;
        String protRelatedUserName = MAIL_PREFIX + storeProtocol +
                        MAIL_SUFFIX_USER;

        for (Iterator it = properties.iterator(); it.hasNext();) {
            property = (ResourceProperty)it.next();
            name = property.getName();
            value = (String)property.getValue();

            if(name.startsWith(PROP_NAME_PREFIX_LEGACY)) {
                name = name.replace(PROP_NAME_DELIM_LEGACY, MAIL_DELIM);
            }

            if(name.startsWith(MAIL_PREFIX)) {
                if(name.equals(protRelatedHostName)) {
                    mailHost = value;
                } else if(name.equals(protRelatedUserName)) {
                    username = value;
                }
                mailProperties.put(name, value);
            }
        }

        // Save mail.host and mail.user to Property list
        mailProperties.put(MAIL_HOST, mailHost);
        mailProperties.put(MAIL_USER, username);
    }
    // END OF IASRI 4650786
    // END OF IASRI 4629057
    
    /** 
     * Get the username for the mail session the server will provide.
     * @return the username.
     */
    public String getUsername() {
	return this.username;
    }
    
    /** 
     * Get the mail from address for the mail session the server will provide.
     * @return the from address.
     */
    public String getMailFrom() {
	return this.mailFrom;
    }
    
    /** 
     * Get the mail hostname for the mail session the server will provide.
     * @return the hostname of the mail server.
     */
    public String getMailHost() {
	return this.mailHost;
    }

    // START OF IASRI 4629057
    /** 
     * Get the default Message Access Protocol for the mail session the  server
     * will provide.
     * @return the store protocol of the mail server.
     */
    public String getMailStoreProtocol() {
        return this.storeProtocol;
    }

    /** 
     * Get the default Transport Protocol for the mail session the server will
     * provide.
     * @return the transport protocol of the mail server.
     */
    public String getMailTransportProtocol() {
        return this.transportProtocol;
    }

    /** 
     * Get the default Message Access Protocol class for the mail session the
     * server will provide.
     * @return the store protocol of the mail server.
     */
    public String getMailStoreProtocolClass() {
        return this.storeProtocolClass;
    }

    /** 
     * Get the default Transport Protocol class for the mail session the server
     * will provide.
     * @return the transport protocol of the mail server.
     */
    public String getMailTransportProtocolClass() {
        return this.transportProtocolClass;
    }

    /** 
     * Get the mail debug flag for the mail session the server will provide.
     * @return the debug flag of the mail server.
     */
    public boolean getMailDebug() {
        return this.debug;
    }

    /** 
     * Get the mail description for the mail session the server will provide.
     * @return the description of the mail server.
     */
    public String getDescription() {
        return this.description;
    }

    /** 
     * Get the mail JNDI name for the mail session the server will provide.
     * @return the JNDI name of the mail server.
     */
    public String getJndiName() {
        return this.jndiName;
    }

    /** 
     * Get the mail enable flag for the mail session the server will provide.
     * @return the enable flag of the mail server.
     */
    public boolean getEnabled() {
        return this.enabled;
    }
    // END OF IASRI 4629057

    /** 
     * Get the mail session properties as per JavaMail.
     * @return the mail session properties.
     */
    public Properties getMailProperties() {
        /* IASRI 4629057
        Properties mailProperties = new Properties();
        mailProperties.put(MAIL_FROM, this.getMailFrom());
        mailProperties.put(MAIL_USER, this.getUsername());
        mailProperties.put(MAIL_HOST, this.getMailHost());
        */

        return mailProperties;
    }
    
    /** 
     * A formatted representation of my state.
     */
    public void print(StringBuffer toStringBuffer) {
	/* IASRI 4629057
	return "MailConfiguration: [" + username + "," + mailFrom + "," + 
		mailHost + "]";
	*/

        // START OF IASRI 4629057
        toStringBuffer.append("MailConfiguration: [");
        toStringBuffer.append("description=").append(description);
        toStringBuffer.append( ", jndiName=").append(jndiName);
        toStringBuffer.append( ", enabled=").append(enabled);

        toStringBuffer.append( ", storeProtocol=").append(storeProtocol);
        toStringBuffer.append( ", transportProtocol=").append(transportProtocol);
        toStringBuffer.append( ", storeProtocolClass=").append(storeProtocolClass);
        toStringBuffer.append( ", transportProtocolClass=").append(transportProtocolClass);
        toStringBuffer.append( ", mailHost=").append(mailHost);
        toStringBuffer.append( ", username=").append(username);
        toStringBuffer.append( ", mailFrom=").append(mailFrom);
        toStringBuffer.append( ", debug=").append(debug);
        toStringBuffer.append( ", mailProperties: [");

        Enumeration e = mailProperties.propertyNames();
        String name;
        String value;
        boolean isFirst = true;

        while (e.hasMoreElements()) {
            name = (String)e.nextElement();
            value = mailProperties.getProperty(name);
            if (isFirst) {
                toStringBuffer.append(name).append("=").append(value);
                isFirst = false;
            }
            else {
                toStringBuffer.append( ", ").append(name).append("=").append(value);
            }
        }
        toStringBuffer.append( "]]");

        // END OF IASRI 4629057
    }
}

