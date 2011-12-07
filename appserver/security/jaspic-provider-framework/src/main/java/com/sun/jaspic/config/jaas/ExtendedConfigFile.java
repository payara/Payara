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


package com.sun.jaspic.config.jaas;

import com.sun.jaspic.config.helper.JASPICLogManager;
import com.sun.security.auth.login.ConfigFile;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.AppConfigurationEntry;

/**
 *
 * @author Ron Monzillo
 */
public class ExtendedConfigFile extends ConfigFile {

    private static final Logger logger =
            Logger.getLogger(JASPICLogManager.JASPIC_LOGGER, JASPICLogManager.RES_BUNDLE);
    //may be more than one delegate for a given jaas config file

    public ExtendedConfigFile() {
    }

    /**
     *
     * @param uri
     */
    public ExtendedConfigFile(URI uri) {
        super(uri);
    }

    /**
     * The ExtendedConfigFile subclass was created because the
     * Configuration interface does not provide a way to do what this
     * method does; i.e. get all the app names from the config.
     * @param authModuleClass an Array of Class objects or null. When this
     * parameter is not null, the appnames are filtered by removing all names
     * that are not associated via an AppConfigurationEntry with at least
     * one LoginModule that implements an authModuleClass.
     * @return String[] containing all the AppNames appearing in the config file.
     * @throws SecurityException
     */
    public String[] getAppNames(final Class[] authModuleClass) {

        final Set<String> nameSet;
        try {
            nameSet = (Set<String>) AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {

                        @Override
                        public Object run() throws Exception {
                            HashMap map;
                            Field field = ConfigFile.class.getDeclaredField("configuration");
                            field.setAccessible(true);
                            map = (HashMap) field.get(ExtendedConfigFile.this);
                            return (Set<String>) map.keySet();
                        }
                    });

        } catch (PrivilegedActionException pae) {
            throw new SecurityException(pae.getCause());
        }

        // remove any modules that don't implement specified interface
        if (authModuleClass != null) {
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction() {

                    @Override
                    public Object run() throws Exception {
                        ClassLoader loader =
                                Thread.currentThread().getContextClassLoader();
                        String[] names = nameSet.toArray(new String[nameSet.size()]);
                        for (String id : names) {
                            boolean hasAuthModule = false;
                            AppConfigurationEntry[] entry = getAppConfigurationEntry(id);
                            for (int i = 0; i
                                    < entry.length && !hasAuthModule; i++) {
                                String clazz = entry[i].getLoginModuleName();
                                try {
                                    Class c = Class.forName(clazz, true, loader);
                                    for (Class required : authModuleClass) {
                                        if (required.isAssignableFrom(c)) {
                                            hasAuthModule = true;
                                            break;
                                        }
                                    }
                                } catch (Throwable t) {
                                    String msg = "skipping unloadable class: "
                                            + clazz + " of entry: " + id;
                                    logger.log(Level.WARNING, msg);
                                }
                            }
                            if (!hasAuthModule) {
                                nameSet.remove(id);
                            }
                        }
                        return null;
                    }
                });
            } catch (java.security.PrivilegedActionException pae) {
                throw new SecurityException(pae.getCause());
            }

        }
        return nameSet.toArray(new String[nameSet.size()]);
    }
}
