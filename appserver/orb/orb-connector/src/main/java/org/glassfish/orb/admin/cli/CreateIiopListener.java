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

package org.glassfish.orb.admin.cli;

import java.beans.PropertyVetoException;
import java.util.Properties;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;

import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Create IioP Listener Command
 *
 */
@Service(name="create-iiop-listener")
@PerLookup
@I18n("create.iiop.listener")
@ExecuteOn(value={RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.CLUSTER,CommandTarget.CONFIG,
    CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE }
)
public class CreateIiopListener implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new
            LocalStringManagerImpl(CreateIiopListener.class);

    @Param( name="target", optional=true,
        defaultValue=SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target ;

    @Param(name="listeneraddress", alias="address")
    String listeneraddress;

    @Param(name="iiopport", optional=true, alias="port", defaultValue="1072")
    String iiopport;

    @Param(optional=true, defaultValue="true")
    Boolean enabled;

    @Param(name="securityenabled", optional=true, defaultValue="false", alias="security-enabled")
    Boolean securityenabled;

    @Param(name="property", optional=true, separator=':')
    Properties properties;

    @Param(name="listener_id", primary=true, alias="id")
    String listener_id;

    @Inject
    Configs configs;

    @Inject
    Servers servers;

    @Inject
    ServiceLocator services ;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */

    @Override
    public void execute(AdminCommandContext context) {
        final Target targetUtil = services.getService(Target.class ) ;
        final Config config = targetUtil.getConfig(target ) ;
        final ActionReport report = context.getActionReport();

        IiopService iiopService = config.getExtensionByType(IiopService.class);

        // ensure we don't already have one of this name
        // check port uniqueness, only for same address
        for (IiopListener listener : iiopService.getIiopListener()) {
            if (listener.getId().equals(listener_id)) {
                String ls = localStrings.getLocalString(
                    "create.iiop.listener.duplicate",
                    "IIOP Listener named {0} already exists.", listener_id);
                report.setMessage(ls);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (listener.getAddress().trim().equals(listeneraddress) &&
                    listener.getPort().trim().equals((iiopport))) {
                String def = "Port [{0}] is already taken by another listener: "
                    + "[{1}] for address [{2}], choose another port.";
                String ls = localStrings.getLocalString(
                    "create.iiop.listener.port.occupied",
                    def, iiopport, listener.getId(), listeneraddress);
                report.setMessage(ls);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        try {
            ConfigSupport.apply(
                new SingleConfigCode<IiopService>() {
                    @Override
                    public Object run(IiopService param)
                        throws PropertyVetoException, TransactionFailure {

                        IiopListener newListener = param.createChild(
                            IiopListener.class);

                        newListener.setId(listener_id);
                        newListener.setAddress(listeneraddress);
                        newListener.setPort(iiopport);
                        newListener.setSecurityEnabled(securityenabled.toString());
                        newListener.setEnabled(enabled.toString());

                        //add properties
                        if (properties != null) {
                            for ( java.util.Map.Entry entry : properties.entrySet()) {
                                Property property =
                                    newListener.createChild(Property.class);
                                property.setName((String)entry.getKey());
                                property.setValue((String)entry.getValue());
                                newListener.getProperty().add(property);
                            }
                        }

                        param.getIiopListener().add(newListener);
                        return newListener;
                    }
                }, iiopService);

            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch(TransactionFailure e) {
            String actual = e.getMessage();
            String def = "Creation of: " + listener_id + "failed because of: "
                + actual;
            String msg = localStrings.getLocalString(
                "create.iiop.listener.fail", def, listener_id, actual);
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}

