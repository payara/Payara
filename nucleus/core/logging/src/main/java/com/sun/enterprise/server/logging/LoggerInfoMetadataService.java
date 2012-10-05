/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import javax.inject.Inject;
import javax.inject.Singleton;

//import org.glassfish.admin.mbeanserver.LogMessagesResourceBundle;
//import org.glassfish.admin.mbeanserver.LoggerInfo;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleChangeListener;

@Service
@Singleton
public class LoggerInfoMetadataService implements LoggerInfoMetadata, ModuleChangeListener {

    private static final String RBNAME = "META-INF/loggerinfo/LoggerInfoMetadata";
    private static final Locale BASE_LOCALE = Locale.ROOT;
        
    @Inject 
    ModulesRegistry modulesRegistry;
    
    private Map<Locale, LoggersInfoMap> metadataMaps;
    private Set<String> moduleNames;
    private boolean valid;
    
    // Reset valid flag if teh set of modules changes, so meta-data will be recomputed
    private Set<String> currentModuleNames() {
        Set<String> currentNames = new HashSet<String>();
        for (Module module : modulesRegistry.getModules()) {
            currentNames.add(module.getName());
        }
        // If new modules set changes, force recomputation of logger meta-datas
        if (moduleNames != null) {
            for (String name : moduleNames) {
                if (!currentNames.contains(name)) {
                    valid = false;
                }
            }
            for (String name : currentNames) {
                if (!moduleNames.contains(name)) {
                    valid = false;
                }
            }
        } else {
            valid = false;
        }
        return currentNames;
    }
    
    private boolean isValid() {
        return (valid && metadataMaps != null);
    }
    
    private synchronized LoggersInfoMap getLoggersInfoMap(Locale locale) {
        moduleNames = currentModuleNames();
        if (!isValid()) {
            metadataMaps = new HashMap<Locale, LoggersInfoMap>();
        }
        LoggersInfoMap infos = metadataMaps.get(locale);
        if (infos == null) {
            infos = new LoggersInfoMap(locale);
            metadataMaps.put(locale, infos);
        }
        valid = true;
        return infos;
    }
    
    @Override
    public String getDescription(String logger) {
        LoggersInfoMap infos = getLoggersInfoMap(BASE_LOCALE);
        return infos.getDescription(logger);    
    }
    
    @Override
    public String getDescription(String logger, Locale locale) {
        LoggersInfoMap infos = getLoggersInfoMap(locale);
        return infos.getDescription(logger);   
    }

    @Override
    public Set<String> getLoggerNames() {
        LoggersInfoMap infos = getLoggersInfoMap(BASE_LOCALE);
        return infos.getLoggerNames();
    }

    @Override
    public String getSubsystem(String logger) {
        LoggersInfoMap infos = getLoggersInfoMap(BASE_LOCALE);
        return infos.getSubsystem(logger);
    }

    @Override
    public boolean isPublished(String logger) {
        LoggersInfoMap infos = getLoggersInfoMap(BASE_LOCALE);
        return infos.isPublished(logger);
    }
    
    // If a module changed in any way, reset the valid flag so meta-data will be
    // recomputed when subsequently requested.
    public synchronized void changed(Module sender)  {
        valid = false;
    }
    
    private class LoggersInfoMap {
        private Locale locale;
        private Map<String, LoggerInfoData> map;
        
        LoggersInfoMap(Locale locale) {
            this.locale = locale;
            this.map = new HashMap<String, LoggerInfoData>();
            initialize();
        }

        public Set<String> getLoggerNames() {
            return map.keySet();
        }

        public String getDescription(String logger) {
            LoggerInfoData info = map.get(logger);
            return (info != null ? info.getDescription() : null);
        }

        public String getSubsystem(String logger) {
            LoggerInfoData info = map.get(logger);
            return (info != null ? info.getSubsystem() : null);
        }

        public boolean isPublished(String logger) {
            LoggerInfoData info = map.get(logger);
            return (info != null ? info.isPublished() : false);
        }
        
        private void initialize() {
            ClassLoader nullClassLoader = new NullClassLoader();
            for (Module module : modulesRegistry.getModules()) {
                ModuleDefinition moduleDef = module.getModuleDefinition();
                // FIXME: We may optimize this by creating a manifest entry in the 
                // jar file(s) to indicate that the jar contains logger infos. Jar files
                // need not be opened if they don't contain logger infos.
                URI uris[] = moduleDef.getLocations();
                int size = (uris != null ? uris.length : 0);
                if (size == 0) {
                    continue;
                }
                URL urls[] = new URL[size];
                try {
                    for (int i=0; i < size; i++) {
                        urls[i] = uris[i].toURL();
                    }
                    ClassLoader loader = new URLClassLoader(urls, nullClassLoader);
                    ResourceBundle rb = ResourceBundle.getBundle(RBNAME, locale, loader);
                    for (String key : rb.keySet()) {
                        int index = key.lastIndexOf('.');
                        String loggerName = key.substring(0, index);
                        String attribute = key.substring(index+1);
                        String value = rb.getString(key);
                        LoggerInfoData li = findOrCreateLoggerInfoMetadata(loggerName);
                        if (attribute.equals("description")) {
                            li.setDescription(value);
                        } else if (attribute.equals("publish")) {
                            li.setPublished(Boolean.parseBoolean(value));
                        } else if (attribute.equals("subsystem")) {
                            li.setSubsystem(value);
                        } else {
                            continue;
                        } 
                    }
                } catch (MalformedURLException mfe) {
                    //FIXME: log message
                    mfe.printStackTrace();
                } catch (MissingResourceException mre) {
                    // Ignore
                }
            }
        }
        
        private LoggerInfoData findOrCreateLoggerInfoMetadata(String loggerName) {
            LoggerInfoData loggerInfoData = null;
            if (map.containsKey(loggerName)) {
                loggerInfoData = map.get(loggerName);
            } else {
                loggerInfoData = new LoggerInfoData();
                map.put(loggerName, loggerInfoData);
            }
            return loggerInfoData;
        }
    }
    
    // Null classloader to avoid delegation to parent classloader(s)
    private static class NullClassLoader extends ClassLoader {
        protected URL findResource(String name) {
            return null;
        }
        protected Enumeration findResources(java.lang.String name) throws IOException {
            return null;
        }
        public URL getResource(String name) {
            return null;
        }
        protected Class findClass(java.lang.String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("Class not found: " + name);
        }
        protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            throw new ClassNotFoundException("Class not found: " + name);
        }
    }
}
