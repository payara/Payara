/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.store;

import com.sun.enterprise.util.SystemPropertyConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;


/**
 * This class implements an adapter for password manipulation a JCEKS.
 * <p>
 * This class delegates the work of actually opening the trust store to
 * AsadminSecurityUtil.
 *
 * @author Shing Wai Chan
 */
public class AsadminTruststore {
    private static final String ASADMIN_TRUSTSTORE = "truststore";
    private KeyStore _keyStore = null;
    private File _keyFile = null;        
    private char[] _password = null;
      
    public static File getAsadminTruststore()
    {
        String location = System.getProperty(SystemPropertyConstants.CLIENT_TRUSTSTORE_PROPERTY);
        if (location == null) {
            return new File(AsadminSecurityUtil.getDefaultClientDir(), ASADMIN_TRUSTSTORE);
        } else {
            return new File(location);
        }
    }

    public static AsadminTruststore newInstance()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        return AsadminSecurityUtil
                .getInstance(true /* isPromptable */)
                .getAsadminTruststore();
    }

    public static AsadminTruststore newInstance(final char[] password)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        return AsadminSecurityUtil
                .getInstance(password, true /* isPromptable */)
                .getAsadminTruststore();
    }
    
    AsadminTruststore(final char[] password) throws CertificateException, IOException,
        KeyStoreException, NoSuchAlgorithmException 
    {                 
        init(getAsadminTruststore(), password);
    }
    
    private void init(File keyfile, final char[] password)
        throws CertificateException, IOException,
        KeyStoreException, NoSuchAlgorithmException 
    {
        _keyFile = keyfile;
        _keyStore = KeyStore.getInstance("JKS"); 
        _password = password;
        BufferedInputStream bInput = null;        
        if (_keyFile.exists()) {
            bInput = new BufferedInputStream(new FileInputStream(_keyFile));
        }
        try {            
            //load must be called with null to initialize an empty keystore
            _keyStore.load(bInput, _password);
            if (bInput != null) {
                bInput.close();
                bInput = null;
            } 
        } finally {
             if (bInput != null) {
                 try {
                     bInput.close();
                 } catch(Exception ex) {
                     //ignore we are cleaning up
                 }
             }
        }        
    }   
    
    public boolean certificateExists(Certificate cert) throws KeyStoreException
    {
        return (_keyStore.getCertificateAlias(cert) == null ? false : true);
    }
    
    public void addCertificate(String alias, Certificate cert) throws KeyStoreException, IOException, 
        NoSuchAlgorithmException, CertificateException
    {
        _keyStore.setCertificateEntry(alias, cert);
        writeStore();
    }
    
    public void writeStore() throws KeyStoreException, IOException, 
        NoSuchAlgorithmException, CertificateException
    {
         BufferedOutputStream boutput = null;

         try {
             boutput = new BufferedOutputStream(
                     new FileOutputStream(_keyFile));
             _keyStore.store(boutput, _password);
             boutput.close();
             boutput = null;
         } finally {
             if (boutput != null) {
                 try {
                     boutput.close();
                 } catch(Exception ex) {
                     //ignore we are cleaning up
                 }
             }
         }
    }    
}
