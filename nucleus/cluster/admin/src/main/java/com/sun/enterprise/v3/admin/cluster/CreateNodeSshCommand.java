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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Nodes;
import java.util.List;
import java.util.ArrayList;
import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;


import org.jvnet.hk2.annotations.Service;

import org.glassfish.cluster.ssh.util.SSHUtil;
import org.glassfish.hk2.api.PerLookup;

/**
 * Remote AdminCommand to create and ssh node.  This command is run only on DAS.
 * Register the node with SSH info on DAS
 *
 * @author Carla Mott
 */
@Service(name = "create-node-ssh")
@I18n("create.node.ssh")
@PerLookup
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Nodes.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-node-ssh", 
        description="Create Node SSH")
})
public class CreateNodeSshCommand extends CreateRemoteNodeCommand {
    @Param(name = "sshport", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_SSH_PORT)
    private String sshport;
    @Param(name = "sshuser", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_REMOTE_USER)
    private String sshuser;
    @Param(name = "sshpassword", optional = true, password = true)
    private String sshpassword;
    @Param(name = "sshkeyfile", optional = true)
    private String sshkeyfile;
    @Param(name = "sshkeypassphrase", optional = true, password = true)
    private String sshkeypassphrase;

    @Override
    public final void execute(AdminCommandContext context) {
        populateBaseClass();
        executeInternal(context);
    }

    @Override
    protected void initialize() {
        // nothing to do...
    }

    @Override
    protected void validate() throws CommandValidationException {
        // nothing to do
    }

    /**
     * Sometimes the console passes an empty string for a parameter. This
     * makes sure those are defaulted correctly.
     */
    @Override
    protected final void checkDefaults() {
        super.checkDefaults();

        if (!StringUtils.ok(remotePort)) {
            remotePort = NodeUtils.NODE_DEFAULT_SSH_PORT;
        }
    }

    @Override
    protected final RemoteType getType() {
        return RemoteType.SSH;
    }

    /**
     * We can't put these values into the base class simply to get the names that
     * the user sees correct.  I.e. "ssh" versus "dcom" versus future types...
     *
     */
    @Override
    protected void populateBaseClass() {
        remotePort = sshport;
        remoteUser = sshuser;
        remotePassword = sshpassword;
    }

    @Override
    protected final void populateParameters(ParameterMap pmap) {
        pmap.add(NodeUtils.PARAM_SSHKEYFILE, sshkeyfile);
        pmap.add(NodeUtils.PARAM_SSHKEYPASSPHRASE, sshkeypassphrase);
    }

    @Override
    protected final void populateCommandArgs(List<String> args) {
        if (sshkeyfile == null) {
            sshkeyfile = SSHUtil.getExistingKeyFile();
        }

        if (sshkeyfile != null) {
            args.add("--sshkeyfile");
            args.add(sshkeyfile);
        }

        args.add("--sshuser");
        args.add(remoteUser);
        args.add("--sshport");
        args.add(remotePort);
    }

    /**
     * Get list of password file entries
     * @return List
     */
    @Override
    protected List<String> getPasswords() {
        List list = new ArrayList<String>();
        NodeUtils nUtils = new NodeUtils(habitat, logger);
        list.add("AS_ADMIN_SSHPASSWORD=" + nUtils.sshL.expandPasswordAlias(remotePassword));

        if (sshkeypassphrase != null) {
            list.add("AS_ADMIN_SSHKEYPASSPHRASE=" + nUtils.sshL.expandPasswordAlias(sshkeypassphrase));
        }
        return list;
    }

    @Override
    protected String getInstallNodeCommandName() {
        return "install-node-ssh";
    }
}
