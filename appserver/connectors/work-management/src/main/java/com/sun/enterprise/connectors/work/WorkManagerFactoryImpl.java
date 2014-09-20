/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.appserv.connectors.internal.api.WorkManagerFactory;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.resource.spi.work.WorkManager;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorkManagerFactoryImpl allows other customized WorkManager implementation
 * to be plugged into the server.  The name of the customized
 * implementation class for the WorkManager has to be specified as
 * a system property "workmanager.class".
 * <p/>
 * It is assumed that the implementation for WorkManager also provides
 * a public method called "getInstance" that returns a WorkManager object.
 * This frees the WorkManagerFactoryImpl from deciding whether WorkManager
 * is implemented as a Singleton in the server.
 * <p/>
 *
 * @author Qingqing Ouyang, Binod P.G.
 */

@Service
@Singleton
public final class WorkManagerFactoryImpl implements WorkManagerFactory {

    private static final String DEFAULT =
            "com.sun.enterprise.connectors.work.CommonWorkManager";

    private static final String WORK_MANAGER_CLASS = "workmanager.class";

    private static final Logger logger = LogFacade.getLogger();
    
    private static final Map<String, WorkManager> workManagers;

    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;

    private ConnectorRuntime runtime;

    static {
        workManagers = Collections.synchronizedMap(new HashMap<String, WorkManager>());
    }

    public WorkManagerFactoryImpl() {
    }

    @LogMessageInfo(
            message = "An error occurred during instantiation of the Work Manager class [ {0} ] for resource adapter [ {1} ].",
            comment = "Failed to create Work Manager instance.",
            level = "SEVERE",
            cause = "Can not initiate the Work Manager class.",
            action = "Check the Work Manager class type.",
            publish = true)
    private static final String RAR_INIT_WORK_MANAGER_ERR = "AS-RAR-05003";

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
            logger.log(Level.SEVERE, RAR_INIT_WORK_MANAGER_ERR, new Object[]{className, raName, e});
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
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "Failed to remove workManager for RAR [ " + moduleName + " ] from registry.");
            }
            result = false;
        } else {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "Removed the workManager for RAR [ " + moduleName + " ] from registry.");
            }

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
            runtime = connectorRuntimeProvider.get();
        }
        return runtime;
    }

}
