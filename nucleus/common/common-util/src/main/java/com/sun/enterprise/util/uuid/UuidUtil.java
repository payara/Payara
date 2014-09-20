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

/*
 * UuidUtil.java
 *
 * Created on October 15, 2002, 9:39 AM
 */

package com.sun.enterprise.util.uuid;

import java.net.InetAddress;

import java.rmi.server.UID;

import java.security.SecureRandom;

/**
 * Class UuidUtil
 *
 *
 */
public class UuidUtil
{

    static final String _inetAddr = initInetAddr();

    //first method (from MarketMaker Guid)
    public static String generateUuidMM() {
        return new StringBuffer(new UID().toString()).reverse().append(':').append(_inetAddr).toString();
    }

    //second method
    public static String generateUuid() {
        return generateUuid(new Object());
    }

    //this method can take in the session object
    //and insure better uniqueness guarantees
    public static String generateUuid(Object obj) {

        //low order time bits
        long presentTime = System.currentTimeMillis();
        int presentTimeLow = (int) presentTime;
        String presentTimeStringLow = formatHexString(presentTimeLow);

        StringBuilder sb = new StringBuilder(50);
        sb.append(presentTimeStringLow);
        //sb.append(":");
        sb.append(getIdentityHashCode(obj));
        //sb.append(":");
        //sb.append(_inetAddr);
        sb.append(addRandomTo(_inetAddr));
        //sb.append(":");
        sb.append(getNextRandomString());
        return sb.toString();
    }

    /**
     * Method initInetAddr
     *
     *
     * @return
     *
     * @audience
     */
    private static String initInetAddr() {

        try {
            byte[] bytes = InetAddress.getLocalHost().getAddress();
            StringBuilder b = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                String s = Integer.toHexString(bytes[i]);

                if (bytes[i] < 0) {
                    b.append(s.substring(s.length() - 2));
                } else {
                    b.append(s);
                }
            }

            return b.toString();
        } catch (Exception ex) {
            //must return a value
            return "a48eb993";
            //return null;
        }
    }

    private static String addRandomTo(String hexString)
    {
        long hexAsLong = convertToLong(hexString);
        int nextRandom = getNextInt();
        long resultInt = hexAsLong + nextRandom;
        String result = Long.toHexString(resultInt);
        // START PWC 6425338
        // Always return a length of 7
        int len = result.length();
        if (len < 7) {
            result = padto7(result);
        } else {
            result = result.substring(len - 7, len);
        }
        //  END PWC 6425338
        return result;
    }

    /**
     * Method getIdentityHashCode
     *
     *
     * @return
     *
     * @audience
     */
    private static String getIdentityHashCode(Object obj) {

        String result = null;
        try {
            int hc = System.identityHashCode(obj);
            return formatHexString(hc);

        } catch (Exception ex) {
            //must return a value
            //return null;
            return "8AF5182";
        }
    }

    private static String formatHexString(int inputInt)
    {
        String result;
        String s = Integer.toHexString(inputInt);
        /* PWC 6425338
        if(s.length() < 8)
        {
            result = s;
        } else {
            result = s.substring(0, 7);
        }
        */
        // START PWC 6425338
        // Always return a length of 7
        int len = s.length();
        if (len < 7) {
            result = padto7(s);
        } else {
            result = s.substring(len - 7, len);
        }
        //  END PWC 6425338
        return result;
    }

    private static synchronized int getNextInt() {
        return _seeder.nextInt();
    }

    private static String getNextRandomString() {
        int nextInt = getNextInt();
        return formatHexString(nextInt);
    }

    private static long convertToLong(String hexString)
    {
        long result = 0;
        try
        {
            result = (Long.valueOf(hexString, 16)).longValue();
        } catch (NumberFormatException ex) {
        }
        return result;
    }

    private static SecureRandom _seeder = new SecureRandom();

    /**
     * Method main
     *
     *
     * @param args
     *
     * @audience
     */
    public static void main(String[] args) {
        System.out.println(UuidUtil.generateUuidMM());
        System.out.println(UuidUtil.generateUuid());
        System.out.println(UuidUtil.generateUuid(new Object()));
    }
    // START PWC 6425338
    /*
    * Pads the given string to a length of 7.
    */
   private static String padto7(String s) {

       int i = 0;
       char[] chars = new char[7];
       int len = s.length();
       while (i < len) {
           chars[i] = s.charAt(i);
           i++;
       }
       while (i < 7) {
           chars[i++] = '0';
       }
       return new String(chars);
   }
    // END PWC 6425338
}



