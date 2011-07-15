/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.common.database;

import org.glassfish.persistence.common.I18NHelper;

import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import com.sun.logging.LogDomains;

/** 
 * @author Mitesh Meswani
 * This class provides helper to load reources into property object.
 *
 */
public class PropertyHelper {

    /** The logger */
    private final static Logger logger = LogDomains.getLogger(
            PropertyHelper.class, LogDomains.JDO_LOGGER);

    /** I18N message handler */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
        "org.glassfish.persistence.common.LogStrings", //NOI18N
         PropertyHelper.class.getClassLoader());

    /**
     * Loads properties list from the specified resource into specified Properties object.
     * @param properties    Properties object to load
     * @param resourceName  Name of resource.
     * @param classLoader   The class loader that should be used to load the resource. If null,primordial
     *                      class loader is used.
     */
    public static void loadFromResource(Properties properties, String resourceName, ClassLoader classLoader)
            throws IOException {
        load(properties, resourceName, false, classLoader);
    }


    /**
     * Loads properties list from the specified file into specified Properties object.
     * @param properties    Properties object to load
     * @param fileName      Fully qualified path name to the file.
     */
    public static void loadFromFile(Properties properties, String fileName)
            throws IOException {
        load(properties, fileName, true, null);
    }

    /**
     * Loads properties list from the specified resource
     * into specified Properties object.
     * @param resourceName	Name of resource. 
     *                      If loadFromFile  is true, this is fully qualified path name to a file.
     *                      param classLoader is ignored.
     *                      If loadFromFile  is false,this is resource name. 
     * @param classLoader   The class loader that should be used to load the resource. If null,primordial
     *                      class loader is used.
     * @param properties	Properties object to load
     * @param loadFromFile  true if resourcename is to be treated as file name.
     */
    private static void load(Properties properties, final String resourceName,
                            final boolean loadFromFile, final ClassLoader classLoader)
                            throws IOException {

        InputStream bin = null;
        InputStream in = null;
        boolean debug = logger.isLoggable(Level.FINE);

        if (debug) {
            Object[] items = new Object[] {resourceName,Boolean.valueOf(loadFromFile)};
            logger.log(Level.FINE, I18NHelper.getMessage(
                    messages, "database.PropertyHelper.load",items)); // NOI18N
        }

        in =  loadFromFile ? openFileInputStream(resourceName) : 
                                openResourceInputStream(resourceName,classLoader);
        if (in == null) {
            throw new IOException(I18NHelper.getMessage(messages,
                    "database.PropertyHelper.failedToLoadResource", resourceName));// NOI18N
        }
        bin = new BufferedInputStream(in);
        try {
            properties.load(bin);
        } finally {
            try {
                bin.close();
            } catch (Exception e) {
                // no action
            }
        }
    }
    
    /**
     * Open fileName as input stream inside doPriviledged block
     */
    private static InputStream openFileInputStream(final String fileName) throws java.io.FileNotFoundException  {
        try {
            return (InputStream) AccessController.doPrivileged(
                new PrivilegedExceptionAction() {
                    public Object run() throws FileNotFoundException {
                            return new FileInputStream(fileName);
                    }
                }
           );
        } catch (PrivilegedActionException e) {
            // e.getException() should be an instance of FileNotFoundException,
            // as only "checked" exceptions will be "wrapped" in a
            // PrivilegedActionException.
            throw (FileNotFoundException) e.getException();
        }
    
    }

    /**
     * Open resourcenName as input stream inside doPriviledged block
     */
    private static InputStream openResourceInputStream(final String resourceName, final ClassLoader classLoader) 
                                                                        throws java.io.FileNotFoundException  {
        return (InputStream) AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    if (classLoader != null) {
                        return classLoader.getResourceAsStream(resourceName);
                    } else {
                        return ClassLoader.getSystemResourceAsStream(resourceName);
                    }
                }
            }
        );
    }

}
