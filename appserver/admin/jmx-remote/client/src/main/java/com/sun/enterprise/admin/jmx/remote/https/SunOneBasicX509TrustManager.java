/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* SunOneBasicX509TrustManager.java
 * $Id: SunOneBasicX509TrustManager.java,v 1.4 2005/12/25 04:26:33 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:33 $
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 */

package com.sun.enterprise.admin.jmx.remote.https;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import com.sun.enterprise.admin.jmx.remote.https.AsadminTruststore;
import com.sun.enterprise.admin.jmx.remote.IStringManager;
import com.sun.enterprise.admin.jmx.remote.StringManagerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Date;
import java.util.Map;
import java.text.DateFormat;

/**
 * An implementation of {@link X509TrustManager} that provides basic support for Trust Management.
 * As of $Revision: 1.4 $ it checks if the server is trusted and displays the certificate chain
 * that was received from the server. The user is then prompted to confirm the certificate. If 
 * confirmed the certificate is entered into the client side asadmintruststore (whose default
 * name is ~/.asadmintruststore). Once in the truststore, the user is never prompted to confirm 
 * a second time.
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.4 $
 */
public class SunOneBasicX509TrustManager implements X509TrustManager {
		
    private final Object _alias;    
    private boolean _alreadyInvoked;
    private CertificateException _lastCertException;
    private RuntimeException _lastRuntimeException;
    
    private static IStringManager _strMgr = null;
        
    /**
     * Creates an instance of the SunOneBasicX509TrustManager
     * @param alias The toString() of the alias object concatenated with a date/time stamp is used as 
     * the alias of the trusted server certificate in the client side .asadmintruststore. When null
     * only a date / timestamp is used as an alias.
     */    
    public SunOneBasicX509TrustManager (Object alias, Map env) {		
        if (_strMgr == null) 
            _strMgr = StringManagerFactory.getClientStringManager(SunOneBasicX509TrustManager.class, env);
        _alias = alias;
        _alreadyInvoked = false;
        _lastCertException = null;
        _lastRuntimeException = null;
    }
	
    /**
     * Creates an instance of the SunOneBasicX509TrustManager
     * A date/time stamp is used of the trusted server certificate in the client side 
     *.asadmintruststore
     */
    public SunOneBasicX509TrustManager () {
        this (null, null);
    }
    
	/** Checks if client is trusted given the certificate chain and authorization type string,
	 * e.g. "RSA".
	 * @throws {@link CertificateException}
	 * @throws {@link UnsupportedOperationException}
	 */
	public void checkClientTrusted(X509Certificate[] x509Certificate, String authType) 
        throws CertificateException 
    {
		throw new UnsupportedOperationException("Not Implemented for Client Trust Management");
	}
	
    /**
     * Checs if the server is trusted.
     * @param chain The server certificate to be  validated.
     * @param authType
     * @throws CertificateException
     */    
	public void checkServerTrusted(X509Certificate[] chain, String authType) 
        throws CertificateException 
    {
        //The alreadyInvoked flag keeps track of whether we have already prompted the 
        //user. Unfortunately, checkServerTrusted is called 2x and we want to avoid
        //prompting the user twice. I'm not sure of the root cause of this problem (i.e. 
        //why it is called twice. In addition, we keep track of any exception that occurred
        //on the first invocation and propagate that back.
        if (!_alreadyInvoked) {
            _alreadyInvoked = true;            
            try {
                checkCertificate(chain);                
            } catch (RuntimeException ex) {
                _lastRuntimeException = ex;
                throw ex;
            } catch (CertificateException ex) {
                _lastCertException = ex;
                throw ex;
            } 
        } else {
            if (_lastRuntimeException != null) {
                throw _lastRuntimeException;
            } else if (_lastCertException != null) {
                throw _lastCertException;
            }
        }
	}
	
    public X509Certificate[] getAcceptedIssuers() 
    {
        return ( new X509Certificate[0] );
    }

    /**     
     * @return true if the cert should be displayed and the user asked to confirm it. A 
     * return valie of false indicates that the cert will be implicitly trusted and 
     * added to the asadmin truststore.
     */    
    protected boolean promptForConfirmation()
    {
        return true;
    }
    
    /**
     * If we fail to open the client database using the default password (changeit) or 
     * the password found in  "javax.net.ssl.trustStorePassword" system property,
     * then the fallback behavior is to prompt the user for the password by calling
     * this method.
     * @return the password to the client side truststore
     */    
    protected String promptForPassword() throws IOException
    {
        if (promptForConfirmation()) {
            System.out.print(_strMgr.getString("certificateDbPrompt"));
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            return r.readLine();
        } else {
            return null;
        }
    }

    /**
     * Displays the certificate and prompts the user whether or 
     * not it is trusted.
     * @param c
     * @throws IOException
     * @return true if the user trusts the certificate
     */    
    protected boolean isItOKToAddCertToTrustStore(X509Certificate c) throws IOException
    {	                  
        if (promptForConfirmation()) {
            System.out.println(c.toString());
            System.out.print(_strMgr.getString("certificateTrustPrompt"));
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String result = r.readLine();
            if (result != null && result.equalsIgnoreCase("y")) {
                return true;
            } else {
                return false;
            }   
        } else {
            return true;
        }        
    }
    
    private String getAliasName()
    {
        String aliasName = _alias != null ? _alias.toString() : "";
        //We append a timestamp to the alias to ensure that it is unqiue.    
        DateFormat f = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);                    
        aliasName += ":" + f.format(new Date());        
        return aliasName;
    }
   

    /**
     * This function validates the cert and ensures that it is trusted.
     * @param chain
     * @throws RuntimeException
     * @throws CertificateException
     */    
	protected void checkCertificate(X509Certificate[] chain) throws RuntimeException,
        CertificateException, IllegalArgumentException
    {        
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException (_strMgr.getString(
                "emptyServerCertificate"));
        } 
        //First ensure that the certificate is valid.
        for (int i = 0 ; i < chain.length ; i ++) {
            chain[i].checkValidity();   
        }
        try {            
            AsadminTruststore truststore = null;
            try {
                truststore = new AsadminTruststore(); 
            } catch (IOException ex) {                    
                //An IOException is thrown when an invalid keystore password is entered.
                //In this case, we prompt the user for the truststore password.                    
                String password = promptForPassword();
                if (password != null) {
                    truststore = new AsadminTruststore(password);
                } else {
                    throw ex;
                }                    
            }
            //if the certificate already exists in the truststore, it is implicitly trusted
            if (!truststore.certificateExists(chain[0])) {
                //if the certificate does not exist in the truststore, then we prompt the
                //user. Upon confirmation from the user, the certificate is added to the 
                //truststore.
                if (isItOKToAddCertToTrustStore(chain[0])) {                            
                    truststore.addCertificate(getAliasName(), chain[0]);
                } else {
                    throw new CertificateException(_strMgr.getString(
                        "serverCertificateNotTrusted"));
                }
            }     
        } catch (CertificateException ex) {
            throw ex;
        } catch (Exception e) {        
            throw new RuntimeException(e);
        }        
	}
}
