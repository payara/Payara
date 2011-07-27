/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.digest.impl;

import com.sun.enterprise.security.auth.digest.*;
import com.sun.enterprise.security.auth.digest.api.Key;
import com.sun.enterprise.security.auth.digest.api.Password;
import com.sun.enterprise.security.auth.digest.impl.KeyDigestAlgoParamImpl;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.api.NestedDigestAlgoParam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.sun.enterprise.security.auth.digest.api.Constants.*;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * supports creation and validation of digest.
 * @author K.Venugopal@sun.com
 */
public abstract class DigestProcessor {

   
    
    public DigestProcessor() {
    }
    private Password passwd;
    
    private Logger _logger = LogDomains.getLogger(DigestProcessor.class, LogDomains.SECURITY_LOGGER);
    private static final StringManager sm = StringManager.getManager(DigestProcessor.class);
    private static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private final MD5Encoder md5Encoder = new MD5Encoder();
    /**
     *
     *
     * @param passwd password to be used for digest calculation.
     * @param params digest parameter
     * @throws java.security.NoSuchAlgorithmException
     * @return
     */
    
    public String createDigest(Password passwd, DigestAlgorithmParameter[] params) throws NoSuchAlgorithmException {
        try {
            com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter data = null;
            com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter clientResponse = null;
            com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter key = null;
            this.passwd = passwd;
            for (int i = 0; i < params.length; i++) {
                com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter dap = params[i];
                if (A1.equals(dap.getName()) && (dap instanceof com.sun.enterprise.security.auth.digest.api.Key)) {
                    key = dap;
                } else {
                    data = dap;
                }
            }
            byte[] p1 = valueOf(key);
            byte[] p2 = valueOf(data);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bos.write(p1);
            bos.write(":".getBytes());
            bos.write(p2);
            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance(key.getAlgorithm());
            byte[] derivedKey = null;
            byte[] dk = md.digest(bos.toByteArray());
            java.lang.String tmp =getMd5Encoder().encode(dk);
                    //new MD5Encoder().encode(dk);
            return tmp;
        } catch (IOException ex) {
            Object [] parm = new String [1];
            parm[1] = ex.getMessage();
            _logger.log(Level.SEVERE, "create.digest.error",parm);
            _logger.log(Level.FINE,"",ex);
        }
        return null;
    }
    
    
    /**
     *
     * @param passwd
     * @param params
     * @throws java.security.NoSuchAlgorithmException
     * @return
     */
    protected abstract boolean validate(Password passwd, DigestAlgorithmParameter[] params) throws NoSuchAlgorithmException;
    
    /**
     *
     * @param param
     * @throws java.security.NoSuchAlgorithmException
     * @return
     */
    protected final byte[] valueOf(DigestAlgorithmParameter param) throws NoSuchAlgorithmException {
        if (param instanceof KeyDigestAlgoParamImpl) {
            return valueOf((KeyDigestAlgoParamImpl) param);
        } else if (param instanceof NestedDigestAlgoParam) {
            return valueOf((NestedDigestAlgoParam) param);
        }
        if (param.getAlgorithm() == null || param.getAlgorithm().length() == 0) {
            return param.getValue();
        } else {
            MessageDigest md = MessageDigest.getInstance(param.getAlgorithm());
            md.update(param.getValue());
            byte[] dk = md.digest();
            String tmp = getMd5Encoder().encode(dk);
                    //new MD5Encoder().encode(dk);
            return tmp.getBytes();
        }
    }
    
    /**
     *
     * @param passwd
     */
    protected void setPassword(Password passwd) {
        this.passwd = passwd;
    }
    
    private byte[] valueOf(KeyDigestAlgoParamImpl param) throws  java.security.NoSuchAlgorithmException {
        
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        
        
        if (passwd.getType() == Password.PLAIN_TEXT) {
            try {
                bos.write(param.getUsername().getBytes());
                bos.write(param.getDelimiter());
                bos.write(param.getRealmName().getBytes());
                bos.write(param.getDelimiter());
                bos.write(passwd.getValue());
                MessageDigest md = MessageDigest.getInstance(param.getAlgorithm());
                byte[] dk = md.digest(bos.toByteArray());
                String tmp =getMd5Encoder().encode(dk);
                        //new MD5Encoder().encode(dk);
                return tmp.getBytes();
            } catch (IOException ex) {
                _logger.log(java.util.logging.Level.SEVERE, "digest.param.error", ex);
            }
        } else {
            return passwd.getValue();
        }
        
        
        return null;
    }
    
    private byte[] valueOf(NestedDigestAlgoParam param) throws NoSuchAlgorithmException {
        
        ByteArrayOutputStream bos = null;
        AlgorithmParameterSpec[] datastore = param.getNestedParams();
        bos = new ByteArrayOutputStream();
        for (int i = 0; i < datastore.length; i++) {
            DigestAlgorithmParameter dataP = (DigestAlgorithmParameter) datastore[i];
            byte[] tmpData = valueOf(dataP);
            bos.write(tmpData, 0, tmpData.length);
            if (param.getDelimiter() != null && (i + 1 < datastore.length)) {
                bos.write(param.getDelimiter(), 0, param.getDelimiter().length);
            }
        }
        if (hasAlgorithm(param)) {
            MessageDigest md = MessageDigest.getInstance(param.getAlgorithm());
            byte[] dk = md.digest(bos.toByteArray());
            String tmp = getMd5Encoder().encode(dk);
                    //new MD5Encoder().encode(dk);
            return tmp.getBytes();
        } else {
            return bos.toByteArray();
        }
    }
    
    public MD5Encoder getMd5Encoder() {
        return md5Encoder;
    }
    
    public String encode(byte[] dk){
        return getMd5Encoder().encode(dk);
    }
    private boolean hasAlgorithm(DigestAlgorithmParameter param) {
        if (param.getAlgorithm() == null || param.getAlgorithm().length() == 0) {
            return false;
        }
        return true;
    }


    static class MD5Encoder {



	    /**
	     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
	     *
	     * @param binaryData Array containing the digest
	     * @return Encoded MD5, or null if encoding failed
	     */
	    public String encode( byte[] binaryData ) {

       	         if (binaryData.length != 16)
                 return null;

                 char[] buffer = new char[32];

                 for (int i=0; i<16; i++) {
                     int low = (int) (binaryData[i] & 0x0f);
                     int high = (int) ((binaryData[i] & 0xf0) >> 4);
                     buffer[i*2] = hexadecimal[high];
                     buffer[i*2 + 1] = hexadecimal[low];
                 }

                 return new String(buffer);

           }
    }
}
