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

package org.glassfish.connectors.admin.cli;

import org.glassfish.admin.cli.resources.ResourceUtil;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.resource.common.ResourceConstants;
import org.glassfish.resource.common.ResourceStatus;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create Java Mail Resource
 *
 */
@TargetType(value={CommandTarget.DAS,CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@org.glassfish.api.admin.ExecuteOn(RuntimeType.ALL)
@Service(name="create-javamail-resource")
@Scoped(PerLookup.class)
@I18n("create.javamail.resource")
public class CreateJavaMailResource implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateJavaMailResource.class);

    @Param(name="mailhost", alias="host")
    private String mailHost;

    @Param(name="mailuser", alias="user")
    private String mailUser;

    @Param(name="fromaddress",alias="from")
    private String fromAddress;

    @Param(name="jndi_name", primary=true)
    private String jndiName;

    @Param(name="storeprotocol", optional=true, defaultValue="imap", alias="storeProtocol")
    private String storeProtocol;

    @Param(name="storeprotocolclass", optional=true, defaultValue="com.sun.mail.imap.IMAPStore", alias="storeProtocolClass")
    private String storeProtocolClass;

    @Param(name="transprotocol", optional=true, defaultValue="smtp", alias="transportProtocol")
    private String transportProtocol;

    @Param(name="transprotocolclass", optional=true, defaultValue="com.sun.mail.smtp.SMTPTransport", alias="transportProtocolClass")
    private String transportProtocolClass;

    @Param(optional=true, defaultValue="true")
    private Boolean enabled;

    @Param(optional=true, defaultValue="false")
    private Boolean debug;

    @Param(name="property", optional=true, separator=':')
    private Properties properties;

    @Param(optional=true,
    defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(optional=true)
    private String description;


    @Inject
    private Domain domain;

    @Inject
    private JavaMailResourceManager mailResMgr;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        HashMap attributes = new HashMap();
        attributes.put(ResourceConstants.JNDI_NAME, jndiName);
        attributes.put(ResourceConstants.MAIL_HOST, mailHost);
        attributes.put(ResourceConstants.MAIL_USER, mailUser);
        attributes.put(ResourceConstants.MAIL_FROM_ADDRESS, fromAddress);
        attributes.put(ResourceConstants.MAIL_STORE_PROTO, storeProtocol);
        attributes.put(ResourceConstants.MAIL_STORE_PROTO_CLASS, storeProtocolClass);
        attributes.put(ResourceConstants.MAIL_TRANS_PROTO, transportProtocol);
        attributes.put(ResourceConstants.MAIL_TRANS_PROTO_CLASS, transportProtocolClass);
        attributes.put(ResourceConstants.MAIL_DEBUG, debug.toString());
        attributes.put(ResourceConstants.ENABLED, enabled.toString());
        attributes.put(ServerTags.DESCRIPTION, description);

        ResourceStatus rs;

        try {
            rs = mailResMgr.create(domain.getResources(), attributes, properties, target);
        } catch(Exception e) {
            Logger.getLogger(CreateJavaMailResource.class.getName()).log(Level.SEVERE,
                    "Unable to create Mail Resource " + jndiName, e);
            String def = "Mail resource: {0} could not be created";
            report.setMessage(localStrings.getLocalString("create.mail.resource.fail",
                    def, jndiName) + " " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        if (rs.getStatus() == ResourceStatus.FAILURE) {
            ec = ActionReport.ExitCode.FAILURE;
            if (rs.getMessage() == null) {
                 report.setMessage(localStrings.getLocalString("create.mail.resource.fail",
                    "Unable to create Mail Resource {0}.", jndiName));
                
            }
            if (rs.getException() != null)
                report.setFailureCause(rs.getException());
        }
        if(rs.getMessage() != null){
            report.setMessage(rs.getMessage());
        }
        report.setActionExitCode(ec);
    }
}
