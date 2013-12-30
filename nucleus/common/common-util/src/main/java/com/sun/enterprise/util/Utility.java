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

package com.sun.enterprise.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.*;
import javax.naming.*;
import javax.rmi.*;

/**
 *  Handy class full of static functions.
 */
public final class Utility {

    static final Logger _logger = CULoggerInfo.getLogger();

    private static LocalStringManagerImpl localStrings =
    new LocalStringManagerImpl(Utility.class);

    public static void checkJVMVersion()
    {
        // do not perform any JVM version checking
    }


    public static Properties getPropertiesFromFile(String file)
            throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(file);
        InputStream is2 = null;
        try {
            if (is != null) {
                Properties config = new Properties();
                config.load(is);
                return config;
            }
            else {
                String remoteclient = "/" + file;
                is2 = Utility.class.getResourceAsStream(remoteclient);
                Properties config = new Properties();
                config.load(is2);
                return config;
            }
        }
        finally {
            try {
                if (is2 != null)
                    is2.close();
            }
            catch (Exception e) {
                // nothing can be done about it.
            }
        }
    }


    /**
     * Return the hostname of the local machine.
     */
    public static String getLocalHost()  {
	String hostname = null;
	try {
	    InetAddress ia = InetAddress.getLocalHost();
	    hostname = ia.getHostName();
	} catch(UnknownHostException e) {
	    return "localhost";
	}
	return hostname;
    }

    /**
     * Return the hostname of the local machine.
     */
    public static String getLocalAddress()  {
	String address = null;
	try {
	    InetAddress ia = InetAddress.getLocalHost();
	    address = ia.getHostAddress();
	} catch(UnknownHostException e) {
	    return "127.0.0.1";
	}
	return address;
    }

    /**
     * This is a convenience method to lookup a remote object by name within
     * the naming context.
     * @exception javax.naming.NamingException if the object with that
     * name could not be found.
     */
    public static java.rmi.Remote lookupObject(String publishedName,
					       java.lang.Class anInterface)
	    throws javax.naming.NamingException {

	Context ic = new InitialContext();
	java.lang.Object objRef = ic.lookup(publishedName);
	return (java.rmi.Remote)
	    PortableRemoteObject.narrow(objRef, anInterface);
    }

    
    /**
     * Returns a character array for the valid characters in a CharBuffer.
     * @param cb
     * @return 
     */
    public static char[] toCharArray(final CharBuffer cb) {
        return cb.toString().toCharArray();
    }
    
    /**
     * Returns a byte array for the valid bytes in a ByteBuffer.
     * @param bb
     * @return 
     */
    public static byte[] toByteArray(final ByteBuffer bb) {
        final byte[] result = new byte[bb.limit() - bb.position()];
        bb.get(result);
        return result;
    }

    /** Unmarshal a byte array to an integer.
	Assume the bytes are in BIGENDIAN order.
	i.e. array[offset] is the most-significant-byte
	and  array[offset+3] is the least-significant-byte.
	@param array The array of bytes.
	@param offset The offset from which to start unmarshalling.
    */
    public static int bytesToInt(byte[] array, int offset)
    {
	int b1, b2, b3, b4;

        b1 = (array[offset++] << 24) & 0xFF000000;
        b2 = (array[offset++] << 16) & 0x00FF0000;
        b3 = (array[offset++] << 8)  & 0x0000FF00;
        b4 = (array[offset++] << 0)  & 0x000000FF;

	return (b1 | b2 | b3 | b4);
    }

    /** Marshal an integer to a byte array.
	The bytes are in BIGENDIAN order.
	i.e. array[offset] is the most-significant-byte
	and  array[offset+3] is the least-significant-byte.
	@param array The array of bytes.
	@param offset The offset from which to start marshalling.
    */
    public static void intToBytes(int value, byte[] array, int offset)
    {
        array[offset++] = (byte)((value >>> 24) & 0xFF);
        array[offset++] = (byte)((value >>> 16) & 0xFF);
        array[offset++] = (byte)((value >>> 8) & 0xFF);
        array[offset++] = (byte)((value >>> 0) & 0xFF);
    }

    /** Unmarshal a byte array to an long.
	Assume the bytes are in BIGENDIAN order.
	i.e. array[offset] is the most-significant-byte
	and  array[offset+7] is the least-significant-byte.
	@param array The array of bytes.
	@param offset The offset from which to start unmarshalling.
    */
    public static long bytesToLong(byte[] array, int offset)
    {
	long l1, l2;

	l1 = (long)bytesToInt(array, offset) << 32;
	l2 = (long)bytesToInt(array, offset+4) & 0xFFFFFFFFL;

	return (l1 | l2);
    }

    /** Marshal an long to a byte array.
	The bytes are in BIGENDIAN order.
	i.e. array[offset] is the most-significant-byte
	and  array[offset+7] is the least-significant-byte.
	@param array The array of bytes.
	@param offset The offset from which to start marshalling.
    */
    public static void longToBytes(long value, byte[] array, int offset)
    {
        array[offset++] = (byte)((value >>> 56) & 0xFF);
        array[offset++] = (byte)((value >>> 48) & 0xFF);
        array[offset++] = (byte)((value >>> 40) & 0xFF);
        array[offset++] = (byte)((value >>> 32) & 0xFF);
        array[offset++] = (byte)((value >>> 24) & 0xFF);
        array[offset++] = (byte)((value >>> 16) & 0xFF);
        array[offset++] = (byte)((value >>> 8) & 0xFF);
        array[offset++] = (byte)((value >>> 0) & 0xFF);
    }

    /**
     * Verify and invoke main if present in the specified class.
     */
    public static void invokeApplicationMain(Class mainClass, String[] args)
	throws InvocationTargetException, IllegalAccessException,
		ClassNotFoundException
    {
	    String err = localStrings.getLocalString ("utility.no.main", "",
                     new Object[] {mainClass});

	    // determine the main method using reflection
	    // verify that it is public static void and takes
	    // String[] as the only argument
	    Method mainMethod = null;
	    try {
	        mainMethod = mainClass.getMethod("main",
		    new Class[] { String[].class } );
	    } catch(NoSuchMethodException msme) {
		_logger.log(Level.SEVERE, CULoggerInfo.exceptionInUtility, msme);
		throw new ClassNotFoundException(err);
	    }

	    // check modifiers: public static
            // check return type and exceptions
	    int modifiers = mainMethod.getModifiers ();
	    if (!Modifier.isPublic (modifiers) ||
		!Modifier.isStatic (modifiers) ||
                !mainMethod.getReturnType().equals (Void.TYPE))  {
		    err = localStrings.getLocalString(
			"utility.main.invalid",
			"The main method signature is invalid");
		    _logger.log(Level.SEVERE, CULoggerInfo.mainNotValid);
	    	    throw new ClassNotFoundException(err);
            }

	    // build args to the main and call it
	    Object params [] = new Object [1];
	    params[0] = args;
	    mainMethod.invoke(null, params);

    }

    public static void invokeSetMethod(Object obj, String prop, String value)
        throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException
    {
        Class cl = obj.getClass();
        // change first letter to uppercase
        String setMeth = "set" + prop.substring(0,1).toUpperCase(Locale.US) +
            prop.substring(1);

        // try string method
        try {
            Class[] cldef = {String.class};
            Method meth = cl.getMethod(setMeth, cldef);
            Object[] params = {value};
            meth.invoke(obj, params);
            return;
        } catch (NoSuchMethodException ex) {
            try {
                // try int method
                Class[] cldef = {Integer.TYPE};
                Method meth = cl.getMethod(setMeth, cldef);
                Object[] params = {Integer.valueOf(value)};
                meth.invoke(obj, params);
                return;
            } catch(NoSuchMethodException nsmex) {
                // try boolean method
                Class[] cldef = {Boolean.TYPE};
                Method meth = cl.getMethod(setMeth, cldef);
                Object[] params = {Boolean.valueOf(value)};
                meth.invoke(obj, params);
                return;
            }
        }
    }


    public static void invokeSetMethodCaseInsensitive(Object obj, String prop, String value)
        throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException
    {
            String alternateMethodName = null;
            Class cl = obj.getClass();

            String setMeth = "set" + prop;


            Method[] methodsList = cl.getMethods();
            boolean methodFound = false;
            int i=0;
            for (i =0; i<methodsList.length; ++i)
            {
                if(methodsList[i].getName().equalsIgnoreCase(setMeth) == true)
                {
                  Class[] parameterTypes = methodsList[i].getParameterTypes();
                  if(parameterTypes.length == 1 )
                  {
                       if(parameterTypes[0].getName().equals("java.lang.String"))
                       {
                           methodFound = true;
                           break;
                       }
                       else
                           alternateMethodName = methodsList[i].getName();
                   }

                }
            }
            if(methodFound == true)
            {
                Object[] params = {value};
                methodsList[i].invoke(obj, params);
                return;
            }
            if(alternateMethodName != null)
            {
                 try
                 {
                // try int method
                    Class[] cldef = {Integer.TYPE};
                    Method meth = cl.getMethod(alternateMethodName, cldef);
                    Object[] params = {Integer.valueOf(value)};
                    meth.invoke(obj, params);
                    return;
                 }
                 catch(NoSuchMethodException nsmex)
                 {
                    // try boolean method
                    Class[] cldef = {Boolean.TYPE};
                    Method meth = cl.getMethod(alternateMethodName, cldef);
                    Object[] params = {Boolean.valueOf(value)};
                    meth.invoke(obj, params);
                    return;
                 }

            }
            else
                 throw new NoSuchMethodException(setMeth);
    }


  // Ports are marshalled as shorts on the wire.  The IDL
    // type is unsigned short, which lacks a convenient representation
    // in Java in the 32768-65536 range.  So, we treat ports as
    // ints throught this code, except that marshalling requires a
    // scaling conversion.  intToShort and shortToInt are provided
    // for this purpose.

    public static short intToShort(int value)
    {
	if (value > 32767)
            return (short)(value - 65536) ;
	return (short)value ;
    }

    public static int shortToInt(short value)
    {
    	if (value < 0)
            return value + 65536 ;
	return value ;
    }

    /**
     * Get the current thread's context class loader which is set to
     *	the CommonClassLoader by ApplicationServer
     * @return the thread's context classloader if it exists;
     *	else the system class loader.
     */
    public static ClassLoader getClassLoader() {
	if (Thread.currentThread().getContextClassLoader() != null) {
	    return Thread.currentThread().getContextClassLoader();
	} else {
	    return ClassLoader.getSystemClassLoader();
	}
    }

    /**
     * Loads the class with the common class loader.
     * @param className the class name
     * @return the loaded class
     * @exception if the class is not found.
     */
    public static Class loadClass(String className) throws ClassNotFoundException {
	return getClassLoader().loadClass(className);
    }

    /**
     * Utility routine for setting the context class loader.
     * Returns previous class loader.
     */
    public static ClassLoader setContextClassLoader(ClassLoader newClassLoader) {

        // Can only reference final local variables from dopriveleged block
        final ClassLoader classLoaderToSet = newClassLoader;

        final Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();

        if (classLoaderToSet != originalClassLoader) {
            if (System.getSecurityManager() == null) {
                currentThread.setContextClassLoader(classLoaderToSet);
            } else {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public java.lang.Object run() {
                                currentThread.setContextClassLoader(classLoaderToSet);
                                return null;
                            }
                        }
                );
            }
        }
        return originalClassLoader;
    }

    public static void setEnvironment() {
        Environment.obtain().activateEnvironment();
    }
/**
 * Return the value for a given name from the System Properties or the
 * Environmental Variables.  The former overrides the latter.
 * @param name - the name of the System Property or Environmental Variable
 * @return the value of the variable or null if it was not found
 */
    public static String getEnvOrProp(String name) {
        // System properties override env. variables
        String envVal = System.getenv(name);
        String sysPropVal = System.getProperty(name);

        if(sysPropVal != null)
            return sysPropVal;

        return envVal;
    }

    /**
     * Convert the byte array to char array with respect to given charset.
     *
     * @param byteArray
     * @param charset  null or "" means default charset
     * @exception CharacterCodingException
     */
    public static char[] convertByteArrayToCharArray(byte[] byteArray, String charset)
            throws CharacterCodingException {

        if (byteArray == null) {
            return null;
        }

        byte[] bArray = (byte[])byteArray.clone();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bArray);
        Charset charSet;
        if (charset == null || "".equals(charset)) {
            charSet = Charset.defaultCharset();
        } else if (Charset.isSupported(charset)) {
            charSet = Charset.forName(charset);
        } else {
            CharacterCodingException e = new CharacterCodingException();
            e.initCause(new UnsupportedCharsetException(charset));
            throw e;
        }

        CharsetDecoder decoder = charSet.newDecoder();
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(byteBuffer);
        } catch(CharacterCodingException cce) {
            throw cce;
        } catch(Throwable t) {
            CharacterCodingException e = new CharacterCodingException();
            e.initCause(t);
            throw e;
        }
        char[] result = toCharArray(charBuffer);
        clear(byteBuffer);
        clear(charBuffer);

        return result;
    }

    /**
     * Convert the char array to byte array with respect to given charset.
     *
     * @param charArray
     * @param strCharset  null or "" means default charset
     * @exception CharacterCodingException
     */
    public static byte[] convertCharArrayToByteArray(char[] charArray, String strCharset)
            throws CharacterCodingException {

        if (charArray == null) {
            return null;
        }

        char[] cArray = (char[])charArray.clone();
        CharBuffer charBuffer = CharBuffer.wrap(cArray);
        Charset charSet;
        if (strCharset == null || "".equals(strCharset)) {
            charSet = Charset.defaultCharset();
        } else if (Charset.isSupported(strCharset)) {
            charSet = Charset.forName(strCharset);
        } else {
            CharacterCodingException e = new CharacterCodingException();
            e.initCause(new UnsupportedCharsetException(strCharset));
            throw e;
        }

        CharsetEncoder encoder = charSet.newEncoder();
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = encoder.encode(charBuffer);
        } catch(CharacterCodingException cce) {
            throw cce;
        } catch(Throwable t) {
            CharacterCodingException e = new CharacterCodingException();
            e.initCause(t);
            throw e;
        }

        byte[] result = new byte[byteBuffer.remaining()];
        byteBuffer.get(result);
        clear(byteBuffer);
        clear(charBuffer);

        return result.clone();
    }

    private static void clear(ByteBuffer byteBuffer) {
        byte[] bytes = byteBuffer.array();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }

    private static void clear(CharBuffer charBuffer) {
        char[] chars = charBuffer.array();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = '0';
        }
    }
}
