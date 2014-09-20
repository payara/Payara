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

package org.glassfish.security.common;

import java.io.*;
import java.util.*;
import java.security.*;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.universal.GFBase64Decoder;
import com.sun.enterprise.universal.GFBase64Encoder;

/**
 * Util class for salted SHA processing.
 *
 * <P>Salted SHA (aka SSHA) is computed as follows:
 * <br> result = {SSHA}BASE64(SHA(password,salt),salt)
 *
 * <P>Methods are also provided to return partial results, such as
 * SHA( password , salt) without Base64 encoding.
 *
 */
public class SSHA
{
    private static final String SSHA_TAG = "{SSHA}";
    private static final String SSHA_256_TAG = "{SSHA256}";
    private static final String algoSHA = "SHA";
    private static final String algoSHA256 = "SHA-256";
    public static final String defaultAlgo = algoSHA256;

    //TODO V3 need to check if second arg is correct
    private static StringManager sm =
        StringManager.getManager(SSHA.class);   

    
    /**
     * Compute a salted SHA hash.
     *
     * @param salt Salt bytes.
     * @param password Password bytes.
     * @return Byte array of length 20 bytes containing hash result.
     * @throws IllegalArgumentException Thrown if there is an error.
     *
     */
    public static byte[] compute(byte[] salt, byte[] password, String algo)
        throws IllegalArgumentException
    {
        
        byte[] buff = new byte[password.length + salt.length];
        System.arraycopy(password, 0, buff, 0, password.length);
        System.arraycopy(salt, 0, buff, password.length, salt.length);

        byte[] hash = null;

        boolean isSHA = false;
        if(algoSHA.equals(algo)) {
            isSHA = true;
        }
        
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algo);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        assert (md != null);
        md.reset();
        hash = md.digest(buff);

        if (!isSHA) {
            for (int i = 2; i <= 100; i++) {
                md.reset();
                md.update(hash);
                hash = md.digest();
            }
        }    
        if (isSHA) {
            assert (hash.length == 20); // SHA output is 20 bytes
        }
        else {
            assert (hash.length == 32); //SHA-256 output is 32 bytes
        }
        return hash;
    }


    /**
     * Compute a salted SHA hash.
     *
     * <P>Salt bytes are obtained using SecureRandom.
     *
     * @param saltBytes Number of bytes of random salt value to generate.
     * @param password Password bytes.
     * @return Byte array of length 20 bytes containing hash result.
     * @throws IASSecurityExeption Thrown if there is an error.
     *
     */
    //Deprecating this method as this is nt being used.To be removed later
  /*  public static byte[] compute(int saltBytes, byte[] password)
        throws IASSecurityException
    {
        SecureRandom rng=SharedSecureRandom.get();
        byte[] salt=new byte[saltBytes];
        rng.nextBytes(salt);

        return compute(salt, password);
    }*/

    
    /**
     * Perform encoding of salt and computed hash.
     *
     * @param salt Salt bytes.
     * @param hash Result of prior compute() operation.
     * @return String Encoded string, as described in class documentation.
     *
     */
    public static String encode(byte[] salt, byte[] hash, String algo)
    {       
        boolean isSHA = false;

        if (algoSHA.equals(algo)) {
            isSHA = true;
        }

        if (!isSHA) {
            assert (hash.length == 32);
        } else {
            assert (hash.length == 20);
        }

        int resultLength = 32;
        if (isSHA) {
            resultLength = 20;
        }

        byte[] res = new byte[resultLength+salt.length];
        System.arraycopy(hash, 0, res, 0, resultLength);
        System.arraycopy(salt, 0, res, resultLength, salt.length);

        GFBase64Encoder encoder = new GFBase64Encoder();
        String encoded = encoder.encode(res);

        String out = SSHA_256_TAG + encoded;
        if(isSHA) {
            out = SSHA_TAG + encoded;
        }
       
        return out;
    }


    /**
     * Compute a salted SHA hash and return the encoded result. This is
     * a convenience method combining compute() and encode().
     *
     * @param salt Salt bytes.
     * @param password Password bytes.
     * @return String Encoded string, as described in class documentation.
     * @throws IASSecurityExeption Thrown if there is an error.
     *
     */
    //Deprecating this method as this is nt being used.To be removed later
  /*  public static String computeAndEncode(byte[] salt, byte[] password)
        throws IASSecurityException
    {
        byte[] hash = compute(salt, password);
        return encode(salt, hash, false);
    }*/


    /**
     * Compute a salted SHA hash and return the encoded result. This is
     * a convenience method combining compute() and encode().
     *
     * @param saltBytes Number of bytes of random salt value to generate.
     * @param password Password bytes.
     * @return String Encoded string, as described in class documentation.
     * @throws IASSecurityExeption Thrown if there is an error.
     *
     */
    //Deprecating this method as this is nt being used.To be removed later
    /*public static String computeAndEncode(int saltBytes, byte[] password)
        throws IASSecurityException
    {
        SecureRandom rng=SharedSecureRandom.get();
        byte[] salt=new byte[saltBytes];
        rng.nextBytes(salt);

        byte[] hash = compute(salt, password);
        return encode(salt, hash, false);
    }*/


    /**
     * Verifies a password.
     *
     * <P>The given password is verified against the provided encoded SSHA
     * result string.
     *
     * @param encoded Encoded SSHA value (e.g. output of computeAndEncode())
     * @param password Password bytes of the password to verify.
     * @returns True if given password matches encoded SSHA.
     * @throws IllegalArgumentException Thrown if there is an error.
     *
     */
    public static boolean verify(String encoded, byte[] password)
        throws IllegalArgumentException
    {
        byte[] hash = new byte[20];
        String algo = algoSHA256;
        if (encoded.startsWith(SSHA_TAG)) {
            algo = algoSHA;
        }
        byte[] salt = decode(encoded, hash, algo);
        return verify(salt, hash, password, algo);
    }


    /**
     * Verifies a password.
     *
     * <P>The given password is verified against the provided salt and hash
     * buffers.
     *
     * @param salt Salt bytes used in the hash result.
     * @param hash Hash result to compare against.
     * @param password Password bytes of the password to verify.
     * @returns True if given password matches encoded SSHA.
     * @throws IllegalArgumentException Thrown if there is an error.
     *
     */
    public static boolean verify(byte[] salt, byte[] hash, byte[] password, String algo)
        throws IllegalArgumentException
    {
        byte[] newHash = compute(salt, password, algo);
        return Arrays.equals(hash, newHash);
    }


    /**
     * Decodes an encoded SSHA string.
     *
     * @param encoded Encoded SSHA value (e.g. output of computeAndEncode())
     * @param hashResult A byte array which must contain 20 elements. Upon
     *      succesful return from method, it will be filled by the hash
     *      value decoded from the given SSHA string. Existing values are
     *      not used and will be overwritten.
     * @returns Byte array containing the salt obtained from the encoded SSHA
     *      string.
     * @throws IllegalArgumentException Thrown if there is an error.
     *
     */
    public static byte[] decode(String encoded, byte[] hashResult, String algo)
        throws IllegalArgumentException
    {
         boolean isSHA = false;

        if(algoSHA.equals(algo)) {
            isSHA = true;
        }

        if(isSHA) {
            assert (hashResult.length==20);
        }
        else {
            assert (hashResult.length == 32);
        }

        if (!encoded.startsWith(SSHA_TAG) && !encoded.startsWith(SSHA_256_TAG)) {
            String msg = sm.getString("ssha.badformat", encoded);
            throw new IllegalArgumentException(msg);
        }

        String ssha = encoded.substring(SSHA_256_TAG.length());
        if (isSHA) {
            ssha = encoded.substring(SSHA_TAG.length());
        }
               
        GFBase64Decoder decoder = new GFBase64Decoder();
        byte[] result = null;
      
        try {
            result = decoder.decodeBuffer(ssha);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        int resultLength = 32;
        if(isSHA) {
            resultLength = 20;
        }
        assert (result.length > resultLength);
        
        byte[] salt = new byte[result.length - resultLength];

        System.arraycopy(result, 0, hashResult, 0, resultLength);
        System.arraycopy(result, resultLength, salt, 0, result.length-resultLength);

        return salt;
    }




}
