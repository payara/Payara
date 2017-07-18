/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.util;

import org.apache.catalina.LogFacade;

import java.util.ResourceBundle;

/**
 * Library of utility methods useful in dealing with converting byte arrays
 * to and from strings of hexadecimal digits.
 *
 * @author Craig R. McClanahan
 */

public final class HexUtils {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    // Code from Ajp11, from Apache's JServ

    // Table for HEX to DEC byte translation
    static final int[] DEC = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        00, 01, 02, 03, 04, 05, 06, 07,  8,  9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };



    /**
     * Convert a String of hexadecimal digits into the corresponding
     * byte array by encoding each two hexadecimal digits as a byte.
     *
     * @param digits Hexadecimal digits representation
     *
     * @exception IllegalArgumentException if an invalid hexadecimal digit
     *  is found, or the input string contains an odd number of hexadecimal
     *  digits
     */
    public static byte[] convert(String digits) {

        int length = digits.length();
        if (length % 2 != 0) {
            throw new IllegalArgumentException
                    (rb.getString(LogFacade.ODD_NUMBER_HEX_DIGITS_EXCEPTION));
        }

        int bLength = length / 2;
        byte[] bytes = new byte[bLength];
        for (int i = 0; i < bLength; i++) {
            char c1 = digits.charAt(2*i);
            char c2 = digits.charAt(2*i + 1);
            byte b = 0;
            if ((c1 >= '0') && (c1 <= '9'))
                b += ((c1 - '0') * 16);
            else if ((c1 >= 'a') && (c1 <= 'f'))
                b += ((c1 - 'a' + 10) * 16);
            else if ((c1 >= 'A') && (c1 <= 'F'))
                b += ((c1 - 'A' + 10) * 16);
            else
                throw new IllegalArgumentException
                    (rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
            if ((c2 >= '0') && (c2 <= '9'))
                b += (c2 - '0');
            else if ((c2 >= 'a') && (c2 <= 'f'))
                b += (c2 - 'a' + 10);
            else if ((c2 >= 'A') && (c2 <= 'F'))
                b += (c2 - 'A' + 10);
            else
                throw new IllegalArgumentException
                    (rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
            bytes[i] = b;
        }
        return bytes;

    }


    /**
     * Convert a byte array into a printable format containing a
     * char[] of hexadecimal digit characters (two per byte).
     *
     * @param bytes Byte array representation
     */
    public static char[] convert(byte bytes[]) {

        char[] arr = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            arr[2*i] = convertDigit((int) (bytes[i] >> 4));
            arr[2*i + 1] = convertDigit((int) (bytes[i] & 0x0f));
        }
        return arr;

    }

    /**
     * Convert 4 hex digits to an int, and return the number of converted
     * bytes.
     *
     * @param hex Byte array containing exactly four hexadecimal digits
     *
     * @exception IllegalArgumentException if an invalid hexadecimal digit
     *  is included
     */
    public static int convert2Int( byte[] hex ) {
        // Code from Ajp11, from Apache's JServ

        // assert b.length==4
        // assert valid data
        int len;
        if(hex.length < 4 ) return 0;
        if( DEC[hex[0]]<0 )
            throw new IllegalArgumentException(rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
        len = DEC[hex[0]];
        len = len << 4;
        if( DEC[hex[1]]<0 )
            throw new IllegalArgumentException(rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
        len += DEC[hex[1]];
        len = len << 4;
        if( DEC[hex[2]]<0 )
            throw new IllegalArgumentException(rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
        len += DEC[hex[2]];
        len = len << 4;
        if( DEC[hex[3]]<0 )
            throw new IllegalArgumentException(rb.getString(LogFacade.BAD_HEX_DIGIT_EXCEPTION));
        len += DEC[hex[3]];
        return len;
    }



    /**
     * [Private] Convert the specified value (0 .. 15) to the corresponding
     * hexadecimal digit.
     *
     * @param value Value to be converted
     */
    private static char convertDigit(int value) {

        value &= 0x0f;
        if (value >= 10)
            return ((char) (value - 10 + 'a'));
        else
            return ((char) (value + '0'));

    }


}
