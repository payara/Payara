/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import org.glassfish.admin.rest.provider.ProviderUtil;

/**
 * @author rajeshwar patil
 */
public abstract class InputObject extends ProviderUtil {

    /**
     * Get the map of key-value pairs represented by this object.
     *
     * @return The map of key-value pairs.
     */
    public abstract Map initializeMap() throws InputException;


    /**
     * Get the value object associated with a key.
     *
     * @param key   A key string.
     * @return      The object associated with the key.
     * @throws   InputException if the key is not found.
     */
    public Object get(String key) throws InputException {
        Object o = getValue(key);
        if (o == null) {
            throw new InputException("InputObject[" + quote(key) + "] not found.");
        }
        return o;
    }


    /**
     * Get the boolean value associated with a key.
     *
     * @param key   A key string.
     * @return      The boolean value associated with the key.
     * @throws   InputException
     *  if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) throws InputException {
        Object o = getValue(key);
        if (o.equals(Boolean.FALSE) ||
                (o instanceof String &&
                ((String)o).equalsIgnoreCase("false"))) {
            return false;
        } else if (o.equals(Boolean.TRUE) ||
                (o instanceof String &&
                ((String)o).equalsIgnoreCase("true"))) {
            return true;
        }
        throw new InputException("InputObject[" + quote(key) +
                "] is not a Boolean.");
    }


    /**
     * Get the double value associated with a key.
     * @param key   A key string.
     * @return      The numeric value.
     * @throws InputException if the key is not found or
     *  if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(String key) throws InputException {
        Object o = getValue(key);
        try {
            return o instanceof Number ?
                ((Number)o).doubleValue() :
                Double.valueOf((String)o).doubleValue();
        } catch (Exception e) {
            throw new InputException("InputObject[" + quote(key) +
                "] is not a number.");
        }
    }


    /**
     * Get the int value associated with a key. If the number value is too
     * large for an int, it will be clipped.
     *
     * @param key   A key string.
     * @return      The integer value.
     * @throws   InputException if the key is not found or if the value cannot
     *  be converted to an integer.
     */
    public int getInt(String key) throws InputException {
        Object o = getValue(key);
        return o instanceof Number ?
                ((Number)o).intValue() : (int)getDouble(key);
    }


    /**
     * Get the long value associated with a key. If the number value is too
     * long for a long, it will be clipped.
     *
     * @param key   A key string.
     * @return      The long value.
     * @throws   InputException if the key is not found or if the value cannot
     *  be converted to a long.
     */
    public long getLong(String key) throws InputException {
        Object o = getValue(key);
        return o instanceof Number ?
                ((Number)o).longValue() : (long)getDouble(key);
    }

    /**
     * Get value associated with a key.
     * @param key   A key string.
     * @return      An object which is the value, or null if there is no value.
     */
    private Object getValue(String key) {
        return key == null ? null : map.get(key);
    }


    /**
     * Get the string associated with a key.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     * @throws   InputException if the key is not found.
     */
    public String getString(String key) throws InputException {
        return get(key).toString();
    }


    /**
     * Determine if the InputObject contains a specific key.
     * @param key   A key string.
     * @return      true if the key exists in the InputObject.
     */
    public boolean has(String key) {
        return map.containsKey(key);
    }


    /**
     * Determine if the value associated with the key is null or if there is
     *  no value.
     * @param key   A key string.
     * @return      true if there is no value associated with the key or if
     *  the value is null.
     */
    public boolean isNull(String key) {
        Object value = getValue(key);
        if (value == null) return true;
        return false;
    }


    /**
     * Get an enumeration of the keys of the InputObject.
     *
     * @return An iterator of the keys.
     */
    public Iterator keys() {
        return map.keySet().iterator();
    }


    /**
     * Get the number of keys stored in the InputObject.
     *
     * @return The number of keys in the InputObject.
     */
    public int length() {
        return map.size();
    }


    /*static protected final String readAsString(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in);
        StringBuilder sb = new StringBuilder();
        char[] c = new char[1024];
        int l;
        while ((l = reader.read(c)) != -1) {
            sb.append(c, 0, l);
        }
        return sb.toString();
    }*/


    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     * @param s A String.
     * @return A simple JSON value.
     */
    static public Object stringToValue(String s) {
        if (s.equals("")) {
            return s;
        }
        if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (s.equalsIgnoreCase("null")) {
            return null;
        }

        /*
         * If it might be a number, try converting it. We support the 0- and 0x-
         * conventions. If a number cannot be produced, then the value will just
         * be a string. Note that the 0-, 0x-, plus, and implied string
         * conventions are non-standard. A JSON parser is free to accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */

        char b = s.charAt(0);
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
            if (b == '0') {
                if (s.length() > 2 &&
                        (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                    try {
                        return Integer.parseInt(s.substring(2),16);
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                } else {
                    try {
                        return Integer.parseInt(s, 8);
                    } catch (Exception e) {
                        /* Ignore the error */
                    }
                }
            }
            try {
                if (s.indexOf('.') > -1 || s.indexOf('e') > -1 || s.indexOf('E') > -1) {
                    return Double.valueOf(s);
                } else {
                    Long myLong = new Long(s);
                    if (myLong.longValue() == myLong.intValue()) {
                        return myLong.intValue();
                    } else {
                        return myLong;
                    }
                }
            }  catch (Exception f) {
                /* Ignore the error */
            }
        }
        return s;
    }


    /**
     * Put a key/value pair in this object, but only if the key and the
     * value are both non-null, and only if there is not already a member
     * with that name. If the value is null, then the key will be removed
     * from this object if it is present. 
     * @param key
     * @param value. It should be of one  of these types: Boolean, Double,
     * Integer, Long, String, or null.
     * @return this.
     * @throws InputException if the key is a duplicate
     */
    public InputObject put(String key, Object value) throws InputException {
        if (key != null && value != null) {
            if (this.map.get(key) != null) {
                throw new InputException("Duplicate key \"" + key + "\"");
            }
            verify(value);
            this.map.put(key, value);
        }

        if (value == null) {
            this.map.remove(key);
        }
        return this;
    }


    public InputObject putMap(String key, Map value) {
        // This method is called in case of xml input
        //We can safely ignor key input value - we know the object we are modifying
        //from the input url.
        //We do not need to check for duplicate enteries - put/post of a resource
        //modifies only the resource and not any of its child resources.
        //Duplicate entries are possible when we have same attribute on a resource
        //and its child/children
        this.map.putAll(value); 
        return this;
    }


    /**
     * Throw an exception if the object is an NaN or infinite number.
     * @param o The object to verify.
     * @throws InputException If o is a non-finite number.
     */
    static void verify(Object o) throws InputException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new InputException(
                        "Non-finite numbers not allowed");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new InputException(
                        "Non-finite numbers not allowed");
                }
            }
        }
    }


    protected Map map;
}
