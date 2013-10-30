/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.config.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ssl.SSLSupport;

/* JSSESupport

   Concrete implementation class for JSSE
   Support classes.

   This will only work with JDK 1.2 and up since it
   depends on JDK 1.2's certificate support

   @author EKR
   @author Craig R. McClanahan
   Parts cribbed from JSSECertCompat       
   Parts cribbed from CertificatesValve
*/

class JSSESupport implements SSLSupport {
    /**
     * Default Logger.
     */
    private final static Logger logger = Grizzly.logger(JSSESupport.class);

    /**
     * A mapping table to determine the number of effective bits in the key
     * when using a cipher suite containing the specified cipher name.  The
     * underlying data came from the TLS Specification (RFC 2246), Appendix C.
     */
    private static final CipherData ciphers[] = {
        new CipherData("_WITH_NULL_", 0),
        new CipherData("_WITH_IDEA_CBC_", 128),
        new CipherData("_WITH_RC2_CBC_40_", 40),
        new CipherData("_WITH_RC4_40_", 40),
        new CipherData("_WITH_RC4_128_", 128),
        new CipherData("_WITH_DES40_CBC_", 40),
        new CipherData("_WITH_DES_CBC_", 56),
        new CipherData("_WITH_3DES_EDE_CBC_", 168),
        new CipherData("_WITH_AES_128_", 128),
        new CipherData("_WITH_AES_256_", 256)
    };
    
    protected SSLSocket ssl;
    
    // START SJSAS 6439313
    /**
     * The SSLEngine used to support SSL over NIO.
     */
    protected SSLEngine sslEngine;
    

    /**
     * The SSLSession contains SSL information.
     */
    protected SSLSession session;
    // END SJSAS 6439313
    
    JSSESupport(SSLSocket sock){
       ssl=sock;
        // START SJSAS 6439313
       session = ssl.getSession();
        // END SJSAS 6439313
    }
    
    // START SJSAS 6439313
    JSSESupport(SSLEngine sslEngine){
        this.sslEngine = sslEngine;
        session = sslEngine.getSession();
    }
    // END SJSAS 6439313

    public String getCipherSuite() throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        if (session == null)
            return null;
        return session.getCipherSuite();
    }

    public Object[] getPeerCertificateChain() 
        throws IOException {
        return getPeerCertificateChain(false);
    }

    protected java.security.cert.X509Certificate [] 
	getX509Certificates(SSLSession session) throws IOException {
        X509Certificate jsseCerts[] = null;
    try{
	    jsseCerts = session.getPeerCertificateChain();
    } catch (Throwable ex){
       // Get rid of the warning in the logs when no Client-Cert is
       // available
    }

	if(jsseCerts == null)
	    jsseCerts = new X509Certificate[0];
	java.security.cert.X509Certificate [] x509Certs =
	    new java.security.cert.X509Certificate[jsseCerts.length];
	for (int i = 0; i < x509Certs.length; i++) {
	    try {
		byte buffer[] = jsseCerts[i].getEncoded();
		CertificateFactory cf =
		    CertificateFactory.getInstance("X.509");
		ByteArrayInputStream stream =
		    new ByteArrayInputStream(buffer);
		x509Certs[i] = (java.security.cert.X509Certificate)
		    cf.generateCertificate(stream);
		if(logger.isLoggable(Level.FINEST))
		    logger.log(Level.FINE,"Cert #" + i + " = " + x509Certs[i]);
	    } catch(Exception ex) {
		logger.log(Level.SEVERE,"Error translating " + jsseCerts[i], ex);
		return null;
	    }
	}
	
	if ( x509Certs.length < 1 )
	    return null;
	return x509Certs;
    }
    public Object[] getPeerCertificateChain(boolean force)
        throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        if (session == null)
            return null;

        // Convert JSSE's certificate format to the ones we need
	X509Certificate [] jsseCerts = null;
	try {
	    jsseCerts = session.getPeerCertificateChain();
	} catch(Exception bex) {
	    // ignore.
	}
	if (jsseCerts == null)
	    jsseCerts = new X509Certificate[0];
	if(jsseCerts.length <= 0 && force) {
	    session.invalidate();
	    handShake();
            /* SJSAS 6439313
            session = ssl.getSession();
            */     
            // START SJSAS 6439313
            if ( ssl == null)
                session = sslEngine.getSession();
            else
                session = ssl.getSession();
            // END SJSAS 6439313
	}
        return getX509Certificates(session);
    }

    protected void handShake() throws IOException {
        ssl.setNeedClientAuth(true);
        ssl.startHandshake();        
    }
    /**
     * Copied from <code>org.apache.catalina.valves.CertificateValve</code>
     */
    public Integer getKeySize() 
        throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        SSLSupport.CipherData c_aux[]=ciphers;
        if (session == null)
            return null;
        Integer keySize = (Integer) session.getValue(KEY_SIZE_KEY);
        if (keySize == null) {
            int size = 0;
            String cipherSuite = session.getCipherSuite();

            for (int i = 0; i < c_aux.length; i++) {
                if (cipherSuite.indexOf(c_aux[i].phrase) >= 0) {
                    size = c_aux[i].keySize;
                    break;
                }
            }
            keySize = size;
            session.putValue(KEY_SIZE_KEY, keySize);
        }
        return keySize;
    }

    public String getSessionId()
        throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        if (session == null)
            return null;
        // Expose ssl_session (getId)
        byte [] ssl_session = session.getId();
        if ( ssl_session == null) 
            return null;
        StringBuilder buf=new StringBuilder("");
        for(int x=0; x<ssl_session.length; x++) {
            String digit=Integer.toHexString((int)ssl_session[x]);
            if (digit.length()<2) buf.append('0');
            if (digit.length()>2) digit=digit.substring(digit.length()-2);
            buf.append(digit);
        }
        return buf.toString();
    }


}

