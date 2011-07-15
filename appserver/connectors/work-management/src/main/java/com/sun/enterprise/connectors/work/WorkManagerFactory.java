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

package com.sun.enterprise.connectors.work;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Singleton;

import javax.resource.spi.work.WorkManager;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorkManagerFactory allows other customized WorkManager implementation
 * to be plugged into the server.  The name of the customized
 * implementation class for the WorkManager has to be specified as
 * a system property "workmanager.class".
 * <p/>
 * It is assumed that the implementation for WorkManager also provides
 * a public method called "getInstance" that returns a WorkManager object.
 * This frees the WorkManagerFactory from deciding whether WorkManager
 * is implemented as a Singleton in the server.
 * <p/>
 *
 * @author Qingqing Ouyang, Binod P.G.
 */

@Service
@Scoped(Singleton.class)
public final class WorkManagerFactory implements com.sun.appserv.connectors.internal.api.WorkManagerFactory {

    private static final String DEFAULT =
            "com.sun.enterprise.connectors.work.CommonWorkManager";

    private static final String WORK_MANAGER_CLASS = "workmanager.class";

    private static final Logger logger =
            LogDomains.getLogger(WorkManagerFactory.class, LogDomains.RSR_LOGGER);

    private static final StringManager localStrings =
            StringManager.getManager(WorkManagerFactory.class);

    protected static final Map<String, WorkManager> workManagers;

    @Inject
    private Habitat connectorRuntimeHabitat;

    private ConnectorRuntime runtime;

    static {
        workManagers = Collections.synchronizedMap(new HashMap<String, WorkManager>());
    }

    public WorkManagerFactory() {
    }

    private static Logger _logger = LogDomains.getLogger(WorkManagerFactory.class, LogDomains.RSR_LOGGER);

    /**
     * This is called by the constructor of BootstrapContextImpl
     *
     * @param poolName thread pool name
     * @return WorkManager work manager that can be used by resource-adapter
     */
    public WorkManager createWorkManager(String poolName, String raName, ClassLoader rarCL) {

        String className = null;
        String methodName = "getInstance";
        Class cls = null;
        WorkManager wm = null;

        try {
            className = System.getProperty(WORK_MANAGER_CLASS, DEFAULT);

            // Default work manager implementation is not a singleton.
            if (className.equals(DEFAULT)) {
                return new CommonWorkManager(poolName, getConnectorRuntime(), raName, rarCL);
            }

            cls = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (cls != null) {
                Method method = cls.getMethod(methodName, new Class[]{});
                wm = (WorkManager) method.invoke(cls, new Object[]{});
            }
        } catch (Exception e) {
            String msg = localStrings.getString("workmanager.instantiation_error", raName);
            logger.log(Level.SEVERE, msg, e);
        }

        return wm;
    }

    /**
     * add work manager to registry
     *
     * @param moduleName resource-adapter name
     * @param wm         WorkManager
     */
    static void addWorkManager(String moduleName, WorkManager wm) {
        workManagers.put(moduleName, wm);
    }

    /**
     * get the actual work manager from registry
     *
     * @param moduleName resource-adapter name
     * @return WorkManager
     */
    static WorkManager retrieveWorkManager(String moduleName) {
        return workManagers.get(moduleName);
    }

    /**
     * remove the actual work manager from registry
     *
     * @param moduleName resource-adapter name
     * @return boolean
     */
    public boolean removeWorkManager(String moduleName) {
        boolean result = true;
        WorkManager wm = workManagers.remove(moduleName);
        if (wm == null) {
            _logger.log(Level.FINE, "Failed to remove workManager for RAR [ " + moduleName + " ] from registry.");
            result = false;
        } else {
            _logger.log(Level.FINE, "Removed the workManager for RAR [ " + moduleName + " ] from registry.");

            if(wm instanceof CommonWorkManager){
                ((CommonWorkManager)wm).cleanUp();
            }
        }

        return result;
    }

    /**
     * provides work manager proxy that is Serializable
     *
     * @param poolId     ThreadPoolId
     * @param moduleName resource-adapter name
     * @return WorkManager
     * @throws ConnectorRuntimeException when unable to get work manager
     */
    public WorkManager getWorkManagerProxy(String poolId, String moduleName, ClassLoader rarCL)
            throws ConnectorRuntimeException {
        WorkManager wm = retrieveWorkManager(moduleName);
        if (wm == null) {
            wm = createWorkManager(poolId, moduleName, rarCL);
            addWorkManager(moduleName, wm);
        }
        return new WorkManagerProxy(wm, moduleName);
    }

    private ConnectorRuntime getConnectorRuntime() {
        if(runtime == null){
            runtime = connectorRuntimeHabitat.getComponent(ConnectorRuntime.class);
        }
        return runtime;
    }

}
