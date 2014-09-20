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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.Cookie;

import org.apache.naming.Util;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.utils.Charsets;

/**
 * General purpose request parsing and encoding utility methods.
 *
 * @author Craig R. McClanahan
 * @author Tim Tye
 * @version $Revision: 1.4 $ $Date: 2006/12/12 20:43:07 $
 */

public final class RequestUtil {

    private static final String SESSION_VERSION_SEPARATOR = ":";


    /**
     * Encode a cookie as per RFC 2109.  The resulting string can be used
     * as the value for a <code>Set-Cookie</code> header.
     *
     * @param cookie The cookie to encode.
     * @return A string following RFC 2109.
     */
    public static String encodeCookie(Cookie cookie) {

        StringBuilder buf = new StringBuilder( cookie.getName() );
        buf.append("=");
        buf.append(cookie.getValue());

        if (cookie.getComment() != null) {
            buf.append("; Comment=\"");
            buf.append(cookie.getComment());
            buf.append("\"");
        }

        if (cookie.getDomain() != null) {
            buf.append("; Domain=\"");
            buf.append(cookie.getDomain());
            buf.append("\"");
        }

        if (cookie.getMaxAge() >= 0) {
            buf.append("; Max-Age=\"");
            buf.append(cookie.getMaxAge());
            buf.append("\"");
        }

        if (cookie.getPath() != null) {
            buf.append("; Path=\"");
            buf.append(cookie.getPath());
            buf.append("\"");
        }

        if (cookie.getSecure()) {
            buf.append("; Secure");
        }

        if (cookie.getVersion() > 0) {
            buf.append("; Version=\"");
            buf.append(cookie.getVersion());
            buf.append("\"");
        }

        return (buf.toString());
    }


    /**
     * Normalize a relative URI path that may have relative values ("/./",
     * "/../", and so on ) it it.  <strong>WARNING</strong> - This method is
     * useful only for normalizing application-generated paths.  It does not
     * try to perform security checks for malicious input.
     *
     * @param path Relative path to be normalized
     */
    public static String normalize(String path) {
        return normalize(path, true);
    }

    /**
     * Normalize a relative URI path that may have relative values ("/./",
     * "/../", and so on ) it it.  <strong>WARNING</strong> - This method is
     * useful only for normalizing application-generated paths.  It does not
     * try to perform security checks for malicious input.
     *
     * @param path Relative path to be normalized
     * @param replaceBackSlash Should '\\' be replaced with '/'
     */
    public static String normalize(String path, boolean replaceBackSlash) {
        // Implementation has been moved to org.apache.naming.Util
        // so that it may be accessed by code in web-naming
        return Util.normalize(path, replaceBackSlash);
    }


    /**
     * Parse the character encoding from the specified content type header.
     * If the content type is null, or there is no explicit character encoding,
     * <code>null</code> is returned.
     *
     * @param contentType a content type header
     */
    public static String parseCharacterEncoding(String contentType) {

        if (contentType == null)
            return (null);
        int start = contentType.indexOf("charset=");
        if (start < 0)
            return (null);
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0)
            encoding = encoding.substring(0, end);
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
            && (encoding.endsWith("\"")))
            encoding = encoding.substring(1, encoding.length() - 1);
        return (encoding.trim());

    }


    /**
     * Parse a cookie header into an array of cookies according to RFC 2109.
     *
     * @param header Value of an HTTP "Cookie" header
     */
    public static Cookie[] parseCookieHeader(String header) {

        if ((header == null) || (header.length() < 1))
            return (new Cookie[0]);

        ArrayList<Cookie> cookies = new ArrayList<Cookie>();
        while (header.length() > 0) {
            int semicolon = header.indexOf(';');
            if (semicolon < 0)
                semicolon = header.length();
            if (semicolon == 0)
                break;
            String token = header.substring(0, semicolon);
            if (semicolon < header.length())
                header = header.substring(semicolon + 1);
            else
                header = "";
            try {
                int equals = token.indexOf('=');
                if (equals > 0) {
                    String name = token.substring(0, equals).trim();
                    String value = token.substring(equals+1).trim();
                    cookies.add(new Cookie(name, value));
                }
            } catch (Throwable e) {
                // Ignore
            }
        }

        return cookies.toArray(new Cookie[cookies.size()]);

    }


    /**
     * Append request parameters from the specified String to the specified
     * Map.  It is presumed that the specified Map is not accessed from any
     * other thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>:  URL decoding is performed
     * individually on the parsed name and value elements, rather than on
     * the entire query string ahead of time, to properly deal with the case
     * where the name or value includes an encoded "=" or "&" character
     * that would otherwise be interpreted as a delimiter.
     *
     * @param map Map that accumulates the resulting parameters
     * @param data Input string containing request parameters
     * @param encoding The name of a supported charset used to encode
     *
     * @exception IllegalArgumentException if the data is malformed
     */
    public static void parseParameters(Map<String, String[]> map, String data,
            String encoding) throws UnsupportedEncodingException {

        if ((data != null) && (data.length() > 0)) {

            // use the specified encoding to extract bytes out of the
            // given string so that the encoding is not lost. If an
            // encoding is not specified, let it use platform default
            byte[] bytes = null;
            try {
                if (encoding == null) {
                    bytes = data.getBytes(Charset.defaultCharset());
                } else {
                    bytes = data.getBytes(Charsets.lookupCharset(encoding));
                }
            } catch (UnsupportedCharsetException uee) {
            }

            parseParameters(map, bytes, encoding);
        }

    }


    /**
     * Decode and return the specified URL-encoded String.
     * When the byte array is converted to a string, the system default
     * character encoding is used...  This may be different than some other
     * servers.
     *
     * @param str The url-encoded string
     *
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String urlDecode(String str) {
        // Implementation has been moved to org.apache.naming.Util
        // so that it may be accessed by code in war-util
        return Util.urlDecode(str);
    }


    /**
     * Decode and return the specified URL-encoded String.
     *
     * @param str The url-encoded string
     * @param enc The encoding to use; if null, the default encoding is used
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String urlDecode(String str, String enc) {
        // Implementation has been moved to org.apache.naming.Util
        // so that it may be accessed by code in war-util
        return Util.urlDecode(str, enc);
    }


    /**
     * Decode and return the specified URL-encoded byte array.
     *
     * @param bytes The url-encoded byte array
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String urlDecode(byte[] bytes) {
        // Implementation has been moved to org.apache.naming.Util
        // so that it may be accessed by code in war-util
        return Util.urlDecode(bytes);
    }


    /**
     * Decode and return the specified URL-encoded byte array.
     *
     * @param bytes The url-encoded byte array
     * @param enc The encoding to use; if null, the default encoding is used
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String urlDecode(byte[] bytes, String enc) {
        // Implementation has been moved to org.apache.naming.Util
        // so that it may be accessed by code in war-util
        return Util.urlDecode(bytes, enc);
    }


    /**
     * Decode (in place) the specified URL-encoded byte chunk, and optionally
     * return the decoded result as a String
     *
     * @param bc The URL-encoded byte chunk to be decoded in place
     * @param toString true if the decoded result is to be returned as a
     * String, false otherwise
     *
     * @return The decoded result in String form, if <code>toString</code>
     * is true, or null otherwise
     *
     * @exception IllegalArgumentException if a '%' character is not followed
     * by a valid 2-digit hexadecimal number
     */
    public static String urlDecode(ByteChunk bc, boolean toString) {

        if (bc == null) {
            return (null);
        }

        byte[] bytes = bc.getBytes();
        if (bytes == null) {
            return (null);
        }

        int ix = bc.getStart();
        int end = bc.getEnd();
        int ox = ix;
        while (ix < end) {
            byte b = bytes[ix++];     // Get byte to test
            if (b == '+') {
                b = (byte)' ';
            } else if (b == '%') {
                b = (byte) ((Util.convertHexDigit(bytes[ix++]) << 4)
                            + Util.convertHexDigit(bytes[ix++]));
            }
            bytes[ox++] = b;
        }
        bc.setEnd(ox);
        if (toString) {
            return bc.toString();
        } else {
            return null;
        }
    }


    /**
     * Put name value pair in map.
     *
     * Put name and value pair in map.  When name already exist, add value
     * to array of values.
     */
    private static void putMapEntry( Map<String, String[]> map, String name, String value) {
        String[] newValues = null;
        String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }


    /**
     * Append request parameters from the specified String to the specified
     * Map.  It is presumed that the specified Map is not accessed from any
     * other thread, so no synchronization is performed.
     * <p>
     * <strong>IMPLEMENTATION NOTE</strong>:  URL decoding is performed
     * individually on the parsed name and value elements, rather than on
     * the entire query string ahead of time, to properly deal with the case
     * where the name or value includes an encoded "=" or "&" character
     * that would otherwise be interpreted as a delimiter.
     *
     * NOTE: byte array data is modified by this method.  Caller beware.
     *
     * @param map Map that accumulates the resulting parameters
     * @param data Input string containing request parameters
     * @param encoding Encoding to use for converting hex
     *
     * @exception UnsupportedEncodingException if the data is malformed
     */
    public static void parseParameters(Map<String, String[]> map, byte[] data, String encoding)
        throws UnsupportedEncodingException {

        if (data != null && data.length > 0) {
            int    pos = 0;
            int    ix = 0;
            int    ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                case '&':
                    value = new String(data, 0, ox, Charsets.lookupCharset(encoding));
                    if (key != null) {
                        putMapEntry(map, key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case '=':
                    if (key == null) {
                        key = new String(data, 0, ox, Charsets.lookupCharset(encoding));
                        ox = 0;
                    } else {
                        data[ox++] = c;
                    }                   
                    break;  
                case '+':
                    data[ox++] = (byte)' ';
                    break;
                case '%':
                    data[ox++] = (byte)((Util.convertHexDigit(data[ix++]) << 4)
                                    + Util.convertHexDigit(data[ix++]));
                    break;
                default:
                    data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            if (key != null) {
                value = new String(data, 0, ox, Charsets.lookupCharset(encoding));
                putMapEntry(map, key, value);
            }
        }

    }


    /**
     * Parses the given session version string into its components.
     *
     * @param sessionVersion The session version string to parse
     *
     * @return The mappings from context paths to session version numbers
     * that were parsed from the given session version string
     */
    public static final HashMap<String, String> parseSessionVersionString(
                                                String sessionVersion) {
        if (sessionVersion == null) {
            return null;
        }

        StringTokenizer st = new StringTokenizer(sessionVersion,
                                                 SESSION_VERSION_SEPARATOR);
        HashMap<String, String> result =
            new HashMap<String, String>(st.countTokens());
        while (st.hasMoreTokens()) {
            String hexPath = st.nextToken();
            if (st.hasMoreTokens()) {
                try {
                    String contextPath = new String(
                            HexUtils.convert(hexPath), Charsets.UTF8_CHARSET);
                    result.put(contextPath, st.nextToken());
                } catch(UnsupportedCharsetException ex) {
                    //should not be here
                    throw new IllegalArgumentException(ex);
                }
            }
        }

        return result;
    }


    /**
     * Creates the string representation for the given context path to
     * session version mappings.
     *
     * <p>The returned string will be used as the value of a
     * JSESSIONIDVERSION cookie or jsessionidversion URI parameter, depending
     * on the configured session tracking mode.
     *
     * @param sessionVersions Context path to session version mappings
     *
     * @return The resulting string representation, to be used as the value
     * of a JSESSIONIDVERSION cookie or jsessionidversion URI parameter
     */
    public static String createSessionVersionString(Map<String, String> sessionVersions) {
        if (sessionVersions == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sessionVersions.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            String contextPath = e.getKey();
            // encode so that there is no / or %2F
            try {
                sb.append(new String(HexUtils.convert(contextPath.getBytes(Charsets.UTF8_CHARSET))));
            } catch(UnsupportedCharsetException ex) {
                //should not be here
                throw new IllegalArgumentException(ex);
            }
            sb.append(SESSION_VERSION_SEPARATOR);
            sb.append(e.getValue());
        }

        return sb.toString();
    }

    /**
     * This is a convenient API which wraps around the one in Grizzly and throws
     * checked java.io.UnsupportedEncodingException instead of
     * unchecked java.nio.charset.UnsupportedCharsetException.
     * cf. String.getBytes(String charset) throws UnsupportedEncodingException
     *
     * @exception UnsupportedEncodingException
     */
    public static Charset lookupCharset(String enc) throws UnsupportedEncodingException {
        Charset charset = null;
        Throwable throwable = null;
        try {
            charset = Charsets.lookupCharset(enc);
        } catch(Throwable t) {
            throwable = t;
        }

        if (charset == null) {
            UnsupportedEncodingException uee = new UnsupportedEncodingException();
            if (throwable != null) {
                uee.initCause(throwable);
            }
            throw uee;
        }

        return charset;
    }
}
