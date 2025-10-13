/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEPORT;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_REMOTEUSER;
import static com.sun.enterprise.v3.admin.cluster.NodeUtils.PARAM_SSHKEYFILE;
import static org.glassfish.api.admin.RestEndpoint.OpType.DELETE;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Nodes;


/**
 * Remote AdminCommand to create a config node.  This command is run only on DAS.
 *  Register the config node on DAS
 *
 * @author Carla Mott
 */
@Service(name = "delete-node-ssh")
@I18n("delete.node.ssh")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Nodes.class,
        opType = DELETE,
        path = "delete-node-ssh",
        description = "Delete Node SSH")
})
public class DeleteNodeSshCommand extends DeleteNodeRemoteCommand {
    
    @Override
    public final void execute(AdminCommandContext context) {
        executeInternal(context);
    }

    /**
     * Get list of password file entries
     * 
     * @return List
     */
    @Override
    protected final List<String> getPasswords() {
        List<String> passwords = new ArrayList<>();
        NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
        
        passwords.add("AS_ADMIN_SSHPASSWORD=" + nodeUtils.sshL.expandPasswordAlias(remotepassword));

        if (sshkeypassphrase != null) {
            passwords.add("AS_ADMIN_SSHKEYPASSPHRASE=" + nodeUtils.sshL.expandPasswordAlias(sshkeypassphrase));
        }
        
        return passwords;
    }

    @Override
    protected String getUninstallCommandName() {
        return "uninstall-node-ssh";
    }

    @Override
    final protected void setTypeSpecificOperands(List<String> command, ParameterMap commandParameters) {
        command.add("--sshport");
        command.add(commandParameters.getOne(PARAM_REMOTEPORT));

        command.add("--sshuser");
        command.add(commandParameters.getOne(PARAM_REMOTEUSER));

        String key = commandParameters.getOne(PARAM_SSHKEYFILE);

        if (key != null) {
            command.add("--sshkeyfile");
            command.add(key);
        }
    }
}
