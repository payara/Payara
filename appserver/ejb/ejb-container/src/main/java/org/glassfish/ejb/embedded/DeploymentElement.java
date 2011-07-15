/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.embedded;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.ejb.EJBException;

import org.glassfish.embeddable.archive.ScatteredArchive;
import org.glassfish.deployment.common.ModuleExploder;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import com.sun.ejb.containers.EjbContainerUtilImpl;

/**
 * Wrapper that allows to distinguish between an EJB module and a library reference.
 *
 * @author Marina Vatkina
 */
public class DeploymentElement {

    // Use Bundle from another package
    private static final Logger _logger =
            LogDomains.getLogger(EjbContainerUtilImpl.class, LogDomains.EJB_LOGGER);

    private File element;
    private boolean isEJBModule;
    private boolean isWebApp = false;
    private String mname = null;

    DeploymentElement (File element, boolean isEJBModule, String mname) {
        this.element  = element;
        this.isEJBModule  = isEJBModule;
        this.mname  = mname;
        if (element.isFile()) {
            isWebApp = element.getName().endsWith(".war");
        } else {
            List files = Arrays.asList(element.list());
            isWebApp = files.contains("WEB-INF");
        }
    }

    File getElement() {
        return element;
    }

    boolean isEJBModule() {
        return isEJBModule;
    }

    boolean isWebApp() {
        return isWebApp;
    }

    public static boolean hasEJBModule(Set<DeploymentElement> modules) {
        for (DeploymentElement module : modules) {
            if (module.isEJBModule) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasWar(Set<DeploymentElement> modules) {
        for (DeploymentElement module : modules) {
            if (module.isWebApp) {
                return true;
            }
        }
        return false;
    }

    public static DeploymentElement getWar(Set<DeploymentElement> modules) {
        for (DeploymentElement module : modules) {
            if (module.isWebApp) {
                return module;
            }
        }
        return null;
    }

    public static boolean hasLibrary(Set<DeploymentElement> modules) {
        for (DeploymentElement module : modules) {
            if (!module.isEJBModule) {
                return true;
            }
        }
        return false;
    }

    public static int countEJBModules(Set<DeploymentElement> modules) {
        int result = 0;
        for (DeploymentElement module : modules) {
            if (module.isEJBModule) {
                ++result;
            }
        }
        return result;
    }


    /**
     * Create deployable application from a Set of DeploymentElements.
     * @param modules the Set of DeploymentElements.
     * @return deployable application.
     */
    public static ResultApplication getOrCreateApplication(Set<DeploymentElement> modules, String appName)
            throws EJBException, IOException {
        Object result = null;
        boolean deleteOnExit = false;
        if (modules == null || modules.size() == 0 || !DeploymentElement.hasEJBModule(modules)) {
            _logger.severe("[DeploymentElement] No modules found");
        } else if (appName == null && DeploymentElement.countEJBModules(modules) == 1) {
            // Use the single component as-is
            if (modules.size() == 1) {
                // Single EJB module
                result = modules.iterator().next().getElement();
            } else if(DeploymentElement.countEJBModules(modules) == 1 && DeploymentElement.hasWar(modules)) {
                // A WAR file with an EJB
                result = DeploymentElement.getWar(modules).getElement();
            } else {
                // EJB molule with libraries - create ScatteredArchive
                ScatteredArchive sa = null;
                for (DeploymentElement m : modules) {
                    if (m.isEJBModule) {
                        // XXX Work around GLASSFISH-16618
                        // The name was already calculated when DeploymentElement was created
                        sa = new ScatteredArchive(m.mname, ScatteredArchive.Type.JAR);
                        if (_logger.isLoggable(Level.INFO)) {
                            _logger.info("[DeploymentElement] adding EJB module to ScatteredArchive " + m.mname);
                        }
                        sa.addClassPath(m.element);
                        break;
                    }
                }

                if (sa != null) {
                    for (DeploymentElement m : modules) {
                        if (!m.isEJBModule) {
                            if (_logger.isLoggable(Level.INFO)) {
                                _logger.info("[DeploymentElement] adding library to ScatteredArchive " + m.element.getName());
                            }
        
                            sa.addClassPath(m.element);
                        }
                    }
                    result = sa;
                }
            }
        } else {
            // Create an ear if appName is set or if there is more than 1 EJB module

            // Create a temp dir by creating a temp file first, then
            // delete the file and create a directory in its place.
            File resultFile = File.createTempFile("ejb-app", "");
            File lib = null;
            if (resultFile.delete() && resultFile.mkdirs()) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("[DeploymentElement] temp dir created at " + resultFile.getAbsolutePath());
                }

                // Create lib dir if there are library entries
                if (DeploymentElement.hasLibrary(modules)) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("[DeploymentElement] lib dir added ... ");
                    }
                    lib = new File(resultFile, "lib");
                }

            } else {
                throw new EJBException("Not able to create temp dir " + resultFile.getAbsolutePath ());
            }

            // Copy module directories and explode module jars
            int duplicate_dir_counter = 0;
            for (DeploymentElement m : modules) {
                File f = m.element;

                if (_logger.isLoggable(Level.INFO)) {
                    _logger.info("[DeploymentElement] adding " + f.getName() + " to exploded ear " +
                            " isEJBModule? " + m.isEJBModule + " isWebApp? " + m.isWebApp);
                }

                String filename = f.toURI().getSchemeSpecificPart();
                if (filename.endsWith(File.separator) || filename.endsWith("/")) {
                    int length = filename.length();
                    filename = filename.substring(0, length - 1);
                }

                int lastpart = filename.lastIndexOf(File.separatorChar);
                if (lastpart == -1) {
                    lastpart = filename.lastIndexOf('/');
                }
                String name = filename.substring(lastpart + 1);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("[DeploymentElement] Converted file name: " + filename + " to " + name);
                }

                File base = (m.isEJBModule)? resultFile : lib;
                if (!f.isDirectory() && m.isEJBModule) { 
                    File out = new File(base, FileUtils.makeFriendlyFilename(name));
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("[DeploymentElement] Exploding jar to: " + out);
                    }
                    ModuleExploder.explodeJar(f, out);
                } else {
                    if (f.isDirectory()) { 
                        name = name + (m.isWebApp? "_war" : (m.isEJBModule? "_jar" : ".jar"));
                    }
                    File out = new File(base, name);
                    if (out.exists()) {
                        out = new File(base, "d__" + ++duplicate_dir_counter + "__" + name);
                    }
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("[DeploymentElement] Copying element to: " + out);
                    }
                    FileUtils.copy(f, out);
                }

            }
            // Check if the archive should not be deleted at the end
            deleteOnExit = !Boolean.getBoolean(EJBContainerProviderImpl.KEEP_TEMPORARY_FILES);

            if (appName == null) {
                appName = "ejb-app";
            }
            result = resultFile;
        }
        return new ResultApplication(result, appName, deleteOnExit);
    }

    protected static class ResultApplication {
        private boolean deleteOnExit = false;
        private Object app = null;
        private String appName = null;

        ResultApplication (Object app) {
            this.app = app;
        }

        ResultApplication (Object app, String appName, boolean deleteOnExit) {
            this.app = app;
            this.appName = appName;
            this.deleteOnExit = deleteOnExit;
        }

        Object getApplication() {
            return app;
        }

        String getAppName() {
            return appName;
        }

        boolean deleteOnExit() {
            return deleteOnExit;
        }
    }

}
