/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.admin.cli;

import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.PortUnification;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ProtocolFinder;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.web.admin.LogFacade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

@Service(name = "create-protocol-finder")
@PerLookup
@I18n("create.protocol.finder")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Protocol.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-protocol-finder", 
        description="Create",
        params={
            @RestParam(name="protocol", value="$parent")
        })
})
public class CreateProtocolFinder implements AdminCommand {
    @Param(name = "name", primary = true)
    String name;
    @Param(name = "protocol", optional = false)
    String protocolName;
    @Param(name = "targetprotocol", optional = false)
    String targetName;
    @Param(name = "classname", optional = false)
    String classname;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    private ActionReport report;
    @Inject
    ServiceLocator services;
    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Override
    public void execute(AdminCommandContext context) {
        Target targetUtil = services.getService(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        report = context.getActionReport();
        final Protocols protocols = config.getNetworkConfig().getProtocols();
        final Protocol protocol = protocols.findProtocol(protocolName);
        final Protocol target = protocols.findProtocol(targetName);
        try {
            validate(protocol, LogFacade.CREATE_HTTP_FAIL_PROTOCOL_NOT_FOUND, protocolName);
            validate(target, LogFacade.CREATE_HTTP_FAIL_PROTOCOL_NOT_FOUND, targetName);
            final Class<?> finderClass = Thread.currentThread().getContextClassLoader().loadClass(classname);
            if(!org.glassfish.grizzly.portunif.ProtocolFinder.class.isAssignableFrom(finderClass)) {
                report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_PORTUNIF_FAIL_NOTFINDER), name, classname));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            PortUnification unif = (PortUnification)ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                @Override
                public Object run(Protocol param) throws PropertyVetoException, TransactionFailure {
					     PortUnification pu = param.getPortUnification();
						  if(pu == null) {
						      pu = param.createChild(PortUnification .class);
								param.setPortUnification(pu);
						  }
						  return pu;
                }
            }, protocol);
            ConfigSupport.apply(new SingleConfigCode<PortUnification>() {
                @Override
                public Object run(PortUnification param) throws PropertyVetoException, TransactionFailure {
                    final List<ProtocolFinder> list = param.getProtocolFinder();
                    for (ProtocolFinder finder : list) {
                        if (name.equals(finder.getName())) {
                            throw new TransactionFailure(
                                String.format("A protocol finder named %s already exists.", name));
                        }
                    }
                    final ProtocolFinder finder = param.createChild(ProtocolFinder.class);
                    finder.setName(name);
                    finder.setProtocol(targetName);
                    finder.setClassname(classname);
                    list.add(finder);
                    return null;
                }
            }, unif);
        } catch (ValidationFailureException e) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
            report.setMessage(MessageFormat.format(
                    rb.getString(LogFacade.CREATE_PORTUNIF_FAIL),
                    name,
                    e.getMessage() == null ? "No reason given" : e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
    }

    private void validate(ConfigBeanProxy check, String key, String... arguments)
        throws ValidationFailureException {
        if ((check == null) && (report != null)) {
            report.setMessage(MessageFormat.format(rb.getString(key), arguments));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            throw new ValidationFailureException();
        }
    }

    private static class ValidationFailureException extends Exception {
    }
}
