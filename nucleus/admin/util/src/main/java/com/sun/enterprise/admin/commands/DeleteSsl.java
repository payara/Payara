/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.commands;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkListeners;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.inject.Named;
import java.beans.PropertyVetoException;

/**
 * Delete Ssl command
 * 
 * Usage: delete-ssl --type [http-listener|iiop-listener|iiop-service|protocol]
 *        [--terse=false] [--echo=false] [--interactive=true] [--host localhost] 
 *        [--port 4848|4849] [--secure | -s] [--user admin_user] 
 *        [--passwordfile file_name] [--target target(Default server)] [listener_id|protocol_id]
 *
 * @author Nandini Ektare
 */
@Service(name="delete-ssl")
@PerLookup
@I18n("delete.ssl")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteSsl implements AdminCommand {
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteSsl.class);

    @Param(name="type", acceptableValues="network-listener, http-listener, iiop-listener, iiop-service, jmx-connector, protocol")
    public String type;
    
    @Param(name="listener_id", primary=true, optional=true)
    public String listenerId;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    public String target;

    @Inject
    NetworkListeners networkListeners;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    public Config config;
    
    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator habitat;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Target targetUtil = habitat.getService(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }

        if (!type.equals("iiop-service")) {
            if (listenerId == null) {
                report.setMessage(
                    localStrings.getLocalString(
                        "create.ssl.listenerid.missing",
                        "Listener id needs to be specified"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        
        try {
            SslConfigHandler sslConfigHandler = habitat.getService(SslConfigHandler.class, type);
            if (sslConfigHandler!=null) {
                sslConfigHandler.delete(this, report);
            } else if ("jmx-connector".equals(type)) {
                JmxConnector jmxConnector = null;
                for (JmxConnector listener : config.getAdminService().getJmxConnector()) {
                    if (listener.getName().equals(listenerId)) {
                        jmxConnector = listener;
                    }
                }

                if (jmxConnector == null) {
                    report.setMessage(localStrings.getLocalString(
                        "delete.ssl.jmx.connector.notfound",
                        "Iiop Listener named {0} not found", listenerId));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

                if (jmxConnector.getSsl() == null) {
                    report.setMessage(localStrings.getLocalString(
                        "delete.ssl.element.doesnotexist", "Ssl element does " +
                        "not exist for Listener named {0}", listenerId));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }

                ConfigSupport.apply(new SingleConfigCode<JmxConnector>() {
                    public Object run(JmxConnector param)
                    throws PropertyVetoException {
                        param.setSsl(null);
                        return null;
                    }
                }, jmxConnector);

                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        } catch(TransactionFailure e) {
            reportError(report, e);
        }
    }

    public void reportError(ActionReport report, Exception e) {
        report.setMessage(localStrings.getLocalString("delete.ssl.fail", "Deletion of Ssl in {0} failed", listenerId));
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setFailureCause(e);
    }
}
