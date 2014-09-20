/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Util {

    private static Logger logger = LogFacade.BOOTSTRAP_LOGGER;

    private Util(){}

    public static Logger getLogger() {
        return logger;
    }
    
    /**
     * Gets a property value from the supplied properties object, if not defined there from system properties.
     *
     * @param properties Properties to be searched ahead of system properties.
     * @param propertyName name of the property
     * @param defaultValue default property value
     * @return value of the property
     */
    static String getPropertyOrSystemProperty(Properties properties, String propertyName, String defaultValue) {
        String value = properties.getProperty(propertyName);
        return value != null ? value : System.getProperty(propertyName, defaultValue);
    }

    /**
     * @see #getPropertyOrSystemProperty(java.util.Properties, String, String)
     */
    static String getPropertyOrSystemProperty(Properties properties, String propertyName) {
        return getPropertyOrSystemProperty(properties, propertyName, null);
    }

    static boolean deleteRecursive(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                f.delete();
            } else {
                deleteRecursive(f);
            }
        }
        return dir.delete();
    }

    static long getLastModified(File directory, long current) {

        for (File file : directory.listFiles()) {
            long lastModified;
            if (file.isDirectory()) {
                lastModified = getLastModified(file, current);
            } else {
                lastModified = file.lastModified();
            }
            if (lastModified > current) {
                current = lastModified;
            }
        }
        return current;
    }

    /**
     * This method is used to copy a given file to another file
     * using the buffer sixe specified
     *
     * @param fin  the source file
     * @param fout the destination file
     */
    static void copyFile(File fin, File fout) throws IOException {

        InputStream inStream = new BufferedInputStream(new FileInputStream(fin));
        FileOutputStream fos = new FileOutputStream(fout);
        copy(inStream, fos, fin.length());
    }

    static void copyWithoutClose(InputStream in, FileOutputStream out, long size) throws IOException {

        ReadableByteChannel inChannel = Channels.newChannel(in);
        FileChannel outChannel = out.getChannel();
        outChannel.transferFrom(inChannel, 0, size);

    }

    static void copy(InputStream in, FileOutputStream out, long size) throws IOException {

        try {
            copyWithoutClose(in, out, size);
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    static URI whichJar(Class clazz) {
        URL url = clazz.getClassLoader().getResource(
                clazz.getName().replace(".", "/") + ".class");
        if (url != null) {
            URLConnection con = null;
            try {
                con = url.openConnection();
                if (con instanceof JarURLConnection) {
                    return JarURLConnection.class.cast(con).getJarFileURL().toURI();
                }
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return null;
    }

    static File getJDKToolsJar() {
        File javaHome = new File(System.getProperty("java.home"));
        File jdktools = null;
        if (javaHome.getParent() != null) {
            jdktools = new File(javaHome.getParent(),
                    "lib" + File.separator + "tools.jar");
        }
        return jdktools;
    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP  = "}";

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     */
    private static String substVars(String val, String currentKey,
        Map cycleMap, Properties configProps)
        throws IllegalArgumentException
    {
        /*
         * THIS METHOD HAS BEEN COPIED FROM FELIX
         */
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null)
        {
            cycleMap = new HashMap();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = -1;
        int startDelim = -1;

        do
        {
            stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
            // If there is no stopping delimiter, then just return
            // the value since there is no variable declared.
            if (stopDelim < 0)
            {
                return val;
            }
            // Try to find the matching start delimiter by
            // looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            startDelim = val.indexOf(DELIM_START);
            // If there is no starting delimiter, then just return
            // the value since there is no variable declared.
            if (startDelim < 0)
            {
                return val;
            }
            while (stopDelim >= 0)
            {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim))
                {
                    break;
                }
                else if (idx < stopDelim)
                {
                    startDelim = idx;
                }
            }
        }
        while ((startDelim > stopDelim) && (stopDelim >= 0));

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
            val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException(
                "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
            ? configProps.getProperty(variable, null)
            : null;
        if (substValue == null)
        {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
            + substValue
            + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }


    public static void substVars(Properties props) {
        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            props.setProperty(name,
                    substVars(props.getProperty(name), name, null, props));
        }
    }

    /**
     * Override property values in the given properties object by values set in corresponding property names in
     * System properties object.
     *
     * @param platformConf which will be updated by corresponding values in System properties.
     * @param excluding property names that should not be overridden
     */
    public static void overrideBySystemProps(Properties platformConf, Collection<String> excluding) {
        Properties sysProps = System.getProperties();
        for (Map.Entry<Object, Object> entry: platformConf.entrySet()) {
            if (excluding.contains(entry.getKey())) {
                continue;
            }
            Object systemPropValue = sysProps.get(entry.getKey());
            if (systemPropValue != null && !systemPropValue.equals(entry.getValue())) {
                platformConf.put(entry.getKey(), systemPropValue);
            }
        }
    }
}
