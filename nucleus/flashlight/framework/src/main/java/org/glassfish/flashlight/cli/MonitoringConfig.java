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

package org.glassfish.flashlight.cli;

import com.sun.enterprise.config.serverbeans.ModuleMonitoringLevels;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.beans.PropertyVetoException;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import org.glassfish.api.monitoring.ContainerMonitoring;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.Dom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sreenivas Munnangi
 */
@Service(name="monitoring-config")
public class MonitoringConfig {

    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(MonitoringConfig.class);

    private static AtomicBoolean valueUpdated = new AtomicBoolean(false);

    static void setMonitoringEnabled(MonitoringService ms, 
        final String enabled, final ActionReport report) {

        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                public Object run(MonitoringService param)
                throws PropertyVetoException, TransactionFailure {
                    param.setMonitoringEnabled(enabled);
                    return param;
                }
            }, ms);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("disable.monitoring.exception",
                "Encountered exception while setting monitoring-enabled to false {0}", tfe.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }


    static void setMBeanEnabled(MonitoringService ms, 
        final String enabled, final ActionReport report) {

        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                public Object run(MonitoringService param)
                throws PropertyVetoException, TransactionFailure {
                    param.setMbeanEnabled(enabled);
                    return param;
                }
            }, ms);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("disable.monitoring.exception",
                "Encountered exception while setting mbean-enabled to false {0}", tfe.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }


    static void setDTraceEnabled(MonitoringService ms, 
        final String enabled, final ActionReport report) {

        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                public Object run(MonitoringService param)
                throws PropertyVetoException, TransactionFailure {
                    param.setDtraceEnabled(enabled);
                    return param;
                }
            }, ms);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("disable.monitoring.exception",
                "Encountered exception while setting dtrace-enabled to false {0}", tfe.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    static void setMonitoringLevel(MonitoringService ms, 
        final String moduleName, final String level, final ActionReport report) {

        if (ms.getMonitoringLevel(moduleName) == null) {
            report.setMessage(localStrings.getLocalString("invalid.module",
                    "Invalid module name {0}",
                    moduleName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {

                public Object run(MonitoringService param)
                        throws PropertyVetoException, TransactionFailure {
                    param.setMonitoringLevel(moduleName, level);
                    return null;
                }
            }, ms);
        } catch (TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("monitoring.config.exception",
                    "Encountered exception {0} while setting monitoring level to {1} for {2}",
                    tfe.getMessage(), level, moduleName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    static void setMonitoringLevelX(MonitoringService ms, 
        final String moduleName, final String level, final ActionReport report) {

        ModuleMonitoringLevels mmls = ms.getModuleMonitoringLevels();
        //TODO: synchronize
        try {
            ConfigSupport.apply(new SingleConfigCode<ModuleMonitoringLevels>() {
                public Object run(ModuleMonitoringLevels param)
                throws PropertyVetoException, TransactionFailure {
                    Dom dom = Dom.unwrap(param);
                    String currentVal = dom.attribute(moduleName);
                    if (currentVal == null) {
                        valueUpdated.set(false);
                        return null;
                    } else {
                        dom.attribute(moduleName, level);
                    }
                    return param;
                }
            }, mmls);
        } catch(TransactionFailure tfe) {
            valueUpdated.set(false);
            report.setMessage(localStrings.getLocalString("disable.monitoring.level",
                "Encountered exception {0} while setting monitoring level to OFF for {1}", 
                tfe.getMessage(), moduleName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        if (!valueUpdated.get()) {
            setContainerMonitoringLevel(ms, moduleName, level, report);
        }
    }


    static boolean setContainerMonitoringLevel(MonitoringService ms,
        final String moduleName, final String level, final ActionReport report) {

        ContainerMonitoring cm = ms.getContainerMonitoring(moduleName);
        if (cm == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(localStrings.getLocalString("invalid.module",
                "Invalid module name {0}", moduleName));
            return false;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<ContainerMonitoring>() {
                public Object run(ContainerMonitoring param)
                throws PropertyVetoException, TransactionFailure {
                    param.setLevel(level);
                    return param;
                }
            }, cm);
        } catch(TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("disable.monitoring.level",
                "Encountered exception {0} while setting monitoring level to OFF for {1}", 
                tfe.getMessage(), moduleName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        return true;
    }
}
