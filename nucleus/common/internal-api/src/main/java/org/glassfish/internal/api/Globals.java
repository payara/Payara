/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.internal.api;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Global class for storing the service locator for all hk2 services
 * <p>
 * Very sensitive class, anything stored here cannot be garbage collected
 *
 * @author Jerome Dochez
 */
@Service(name = "globals")
@Singleton
public class Globals {

    private static final Logger LOG = Logger.getLogger(Globals.class.getName());
    private static volatile ServiceLocator defaultHabitat;

    private static Object staticLock = new Object();

    // dochez : remove this once we can get rid of ConfigBeanUtilities class
    @SuppressWarnings("unused")
    @Inject
    private ConfigBeansUtilities utilities;

    @Inject
    private Globals(ServiceLocator habitat) {
        LOG.fine(() -> "Globals(" + habitat + ")");
        defaultHabitat = habitat;
    }

    /**
     * Gets the default service locator
     * <p>
     * This method is identical to {@link #getDefaultBaseServiceLocator}
     * @return
     */
    public static ServiceLocator getDefaultBaseServiceLocator() {
        LOG.finest("getDefaultBaseServiceLocator()");
    	return getDefaultHabitat();
    }

    
    /**
     * Gets the default service locator
     * <p>
     * This method is identical to {@link #getDefaultBaseServiceLocator}
     * @return
     */
    @Deprecated
    public static ServiceLocator getDefaultHabitat() {
        LOG.finest(() -> "getDefaultHabitat() returns " + defaultHabitat);
        return defaultHabitat;
    }

    /**
     * Gets the service from the default service locator with the specified type
     * <p>
     * See {@link ServiceLocator#getService(java.lang.Class, java.lang.annotation.Annotation...) }
     * @param <T>
     * @param type
     * @return
     */
    public static <T> T get(Class<T> type) {
        LOG.fine(() -> "get(type=" + type + ")");
        return defaultHabitat.getService(type);
    }

    /**
     * Sets the default service locator to a different one
     * @param habitat
     */
    public static void setDefaultHabitat(final ServiceLocator habitat) {
        LOG.config(() -> "setDefaultHabitat(habitat=" + habitat + ")");
        defaultHabitat = habitat;
    }

    /**
     * Returns the default service locator. If it does not exist, one will be created.
     * <p>
     * See {@link #getStaticHabitat()}
     * @return
     */
    public static ServiceLocator getStaticBaseServiceLocator() {
    	return getStaticHabitat();
    }

    /**
     * Returns the default service locator. If it does not exist, one will be created.
     * @return
     */
    public static ServiceLocator getStaticHabitat() {
        if (defaultHabitat == null) {
            synchronized (staticLock) {
                if (defaultHabitat == null) {
                    final ModulesRegistry modulesRegistry = new StaticModulesRegistry(Globals.class.getClassLoader());
                    defaultHabitat = modulesRegistry.createServiceLocator("default");
                    LOG.config(() -> "getStaticHabitat() done lazy initialization to: " + defaultHabitat);
                }
            }
        }

        return defaultHabitat;
    }

    /**
     * The point of this service is to ensure that the Globals
     * service is properly initialized by the RunLevelService
     * at the InitRunLevel.  However, Globals itself must be
     * of scope Singleton because it us used in contexts where
     * the RunLevelService is not there
     *
     * @author jwells
     *
     */
    @Service
    @RunLevel(value=(InitRunLevel.VAL - 1), mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
    public static class GlobalsInitializer {
        private static final Logger LOGGI = Logger.getLogger(Globals.GlobalsInitializer.class.getName());

        @SuppressWarnings("unused")
        private Globals globals;

        @Inject
        private void setGlobals(final Globals globals) {
            LOGGI.finest(() -> String.format("setGlobals(globals=%s)", globals));
            this.globals = globals;
        }
    }
}
