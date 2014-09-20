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

package org.glassfish.web.admin.cli;

import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.config.dom.Transports;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Command to create transport element within network-config
 *
 * Sample Usage : create-transport [--acceptorThreads no_of_acceptor_threads] [--bufferSizeBytes buff_size_bytes]
 * [--classname class_name] [--enableSnoop true|false][--selectionKeyHandler true|false] [--displayConfiguration
 * true|false][--maxConnectionsCount count] [--idleKeyTimeoutSeconds idle_key_timeout] [--tcpNoDelay true|false]
 * [--readTimeoutMillis read_timeout][--writeTimeoutMillis write_timeout] [--byteBufferType buff_type]
 * [--selectorPollTimeoutMillis true|false] transport_name
 *
 * domain.xml element example <transports> <transport name="tcp" /> </transports>
 *
 * @author Nandini Ektare
 */
@Service(name = "create-transport")
@PerLookup
@I18n("create.transport")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateTransport implements AdminCommand {

    private static final ResourceBundle rb = HttpServiceStatsProviderBootstrap.rb;

    @LogMessageInfo(
            message = "{0} transport already exists. Cannot add duplicate transport.",
            level = "INFO")
    protected static final String CREATE_TRANSPORT_FAIL_DUPLICATE = "AS-WEB-ADMIN-00022";

    @LogMessageInfo(
            message = "Failed to create transport {0}.",
            level = "INFO")
    protected static final String CREATE_TRANSPORT_FAIL = "AS-WEB-ADMIN-00023";

    @Param(name = "transportname", primary = true)
    String transportName;
    @Param(name = "acceptorthreads", alias="acceptorThreads", optional = true, defaultValue = "-1")
    String acceptorThreads;
    @Param(name = "buffersizebytes", alias="bufferSizeBytes", optional = true, defaultValue = "8192")
    String bufferSizeBytes;
    @Param(name = "bytebuffertype", alias="byteBufferType", optional = true, defaultValue = "HEAP")
    String byteBufferType;
    @Param(name = "classname", optional = true,
        defaultValue = "org.glassfish.grizzly.TCPSelectorHandler")
    String className;
    @Param(name = "displayconfiguration", alias="displayConfiguration", optional = true, defaultValue = "false")
    Boolean displayConfiguration;
    @Param(name = "enablesnoop", alias="enableSnoop", optional = true, defaultValue = "false")
    Boolean enableSnoop;
    @Param(name = "idlekeytimeoutseconds", alias="idleKeyTimeoutSeconds", optional = true, defaultValue = "30")
    String idleKeyTimeoutSeconds;
    @Param(name = "maxconnectionscount", alias="maxConnectionsCount", optional = true, defaultValue = "4096")
    String maxConnectionsCount;
    @Param(name = "readtimeoutmillis", alias="readTimeoutMillis", optional = true, defaultValue = "30000")
    String readTimeoutMillis;
    @Param(name = "writetimeoutmillis", alias="writeTimeoutMillis", optional = true, defaultValue = "30000")
    String writeTimeoutMillis;
    @Param(name = "selectionkeyhandler", alias="selectionKeyHandler", optional = true)
    String selectionKeyHandler;
    @Param(name = "selectorpolltimeoutmillis", alias="selectorPollTimeoutMillis", optional = true, defaultValue = "1000")
    String selectorPollTimeoutMillis;
    @Param(name = "tcpnodelay", alias="tcpNoDelay", optional = true, defaultValue = "false")
    Boolean tcpNoDelay;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    @Inject
    ServiceLocator services;

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        Target targetUtil = services.getService(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        final ActionReport report = context.getActionReport();
        // check for duplicates
        NetworkConfig networkConfig = config.getNetworkConfig();
        Transports transports = networkConfig.getTransports();
        for (Transport transport : transports.getTransport()) {
            if (transportName != null &&
                transportName.equalsIgnoreCase(transport.getName())) {
                report.setMessage(MessageFormat.format(rb.getString(CREATE_TRANSPORT_FAIL_DUPLICATE), transportName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        // Add to the <network-config>
        try {
            ConfigSupport.apply(new SingleConfigCode<Transports>() {
                public Object run(Transports param)
                    throws PropertyVetoException, TransactionFailure {
                    boolean docrootAdded = false;
                    boolean accessLogAdded = false;
                    Transport newTransport = param.createChild(Transport.class);
                    newTransport.setName(transportName);
                    newTransport.setAcceptorThreads(acceptorThreads);
                    newTransport.setBufferSizeBytes(bufferSizeBytes);
                    newTransport.setByteBufferType(byteBufferType);
                    newTransport.setClassname(className);
                    newTransport.setDisplayConfiguration(displayConfiguration.toString());
                    newTransport.setEnableSnoop(enableSnoop.toString());
                    newTransport.setIdleKeyTimeoutSeconds(idleKeyTimeoutSeconds);
                    newTransport.setMaxConnectionsCount(maxConnectionsCount);
                    newTransport.setName(transportName);
                    newTransport.setReadTimeoutMillis(readTimeoutMillis);
                    newTransport.setSelectionKeyHandler(selectionKeyHandler);
                    newTransport.setSelectorPollTimeoutMillis(
                        selectorPollTimeoutMillis);
                    newTransport.setWriteTimeoutMillis(writeTimeoutMillis);
                    newTransport.setTcpNoDelay(tcpNoDelay.toString());
                    param.getTransport().add(newTransport);
                    return newTransport;
                }
            }, transports);
        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(CREATE_TRANSPORT_FAIL), transportName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
