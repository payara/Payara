/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import javax.inject.Inject;

@Service
@Singleton
public class ConnectionPoolUtil {

    @Inject
    private Applications applications;

    @Inject
    private Domain domain;

    @Inject
    private ServerEnvironment env;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ConnectionPoolUtil.class);

    public boolean isValidApplication(String applicationName, String poolName, ActionReport report) {

        boolean isValid = false;

        if(applicationName == null){
            setAppNameNeededErrorMessage(report);
            return isValid;
        }

        Application application = applications.getApplication(applicationName);
        if (application != null) {
            if (application.getEnabled().equalsIgnoreCase("true")) {
                Server server = domain.getServerNamed(env.getInstanceName());
                ApplicationRef appRef = server.getApplicationRef(applicationName);
                if (appRef != null) {
                        if (appRef.getRef().equals(applicationName)) {
                            if (appRef.getEnabled().equalsIgnoreCase("false")) {
                                setAppDisabledErrorMessage(report, applicationName, poolName);
                            } else {
                                isValid = true;
                            }
                        }
                } else {
                    setAppDisabledErrorMessage(report, applicationName, poolName);
                }
            } else {
                setAppDisabledErrorMessage(report, applicationName, poolName);
            }
        } else {
            setApplNotFoundErrorMessage(report, applicationName);
        }
        return isValid;
    }

    public boolean isValidModule(String applicationName, String moduleName, String poolName, ActionReport report) {
        boolean isValid = false;

        Application application = applications.getApplication(applicationName);

        if(!isValidApplication(applicationName, poolName, report)){
            return false;
        }

        Module module = application.getModule(moduleName);
        if(module != null){
            isValid = true;
        }else{
            setModuleNotFoundErrorMessage(report, moduleName, applicationName);
        }
        return isValid;
    }

    public boolean isValidPool(Resources resources, String poolName, String prefix, ActionReport report) {
        boolean isValid = false;
        if (resources != null) {
            if (ConnectorsUtil.getResourceByName(resources, ResourcePool.class, poolName) != null) {
                isValid = true;
            } else {
                setResourceNotFoundErrorMessage(report, poolName);
            }
        } else {
            setResourceNotFoundErrorMessage(report, poolName);
        }
        return isValid;
    }

    private void setAppNameNeededErrorMessage(ActionReport report) {
        report.setMessage(localStrings.getLocalString(
                "pool.util.app.name.needed",
                "--appname is needed when --modulename is specified"));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);

    }

    private void setAppDisabledErrorMessage(ActionReport report, String applicationName, String poolName) {
        report.setMessage(localStrings.getLocalString(
                "pool.util.app.is.not.enabled",
                "Application [ {0} ] in which the pool " +
                "[ {1} ] is defined, is not enabled", applicationName, poolName));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);

    }

    private void setApplNotFoundErrorMessage(ActionReport report, String applicationName){
        report.setMessage(localStrings.getLocalString(
                "pool.util.app.does.not.exist",
                "Application {0} does not exist.", applicationName));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }

    private void setModuleNotFoundErrorMessage(ActionReport report, String moduleName, String applicationName){
        report.setMessage(localStrings.getLocalString(
                "pool.util.module.does.not.exist",
                "Module {0} does not exist in application {1}.", moduleName, applicationName));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }

    private void setResourceNotFoundErrorMessage(ActionReport report, String poolName){
        report.setMessage(localStrings.getLocalString(
                "pool.util.pool.does.not-exist",
                "Pool {0} does not exist.", poolName));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
    }
}
