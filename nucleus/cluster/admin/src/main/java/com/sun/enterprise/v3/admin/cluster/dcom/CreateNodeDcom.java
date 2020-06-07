/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.cluster.dcom;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.cluster.RemoteType;
import org.glassfish.cluster.ssh.util.DcomUtils;
import java.util.List;
import com.sun.enterprise.v3.admin.cluster.CreateRemoteNodeCommand;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import static com.sun.enterprise.util.StringUtils.ok;
/**
 * Remote AdminCommand to create a DCOM node
 *
 * @author Byron Nevins
 */
@Service(name = "create-node-dcom")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Nodes.class,
        opType=RestEndpoint.OpType.POST,
        path="create-node-dcom",
        description="Create Node DCOM")
})
public class CreateNodeDcom extends CreateRemoteNodeCommand {
    @Param(name = "windowsuser", shortName = "w", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_REMOTE_USER)
    private String windowsuser;
    @Param(name = "windowspassword", optional = true, password = true)
    private String windowspassword;
    @Param(name = "windowsdomain", shortName = "d", optional = true)
    private String windowsdomain;

    @Override
    protected void initialize() {
        // check windows domain
        if(!ok(windowsdomain))
            windowsdomain = nodehost;
    }

    @Override
    public final void execute(AdminCommandContext context) {
        executeInternal(context);
    }

    @Override
    protected void validate() throws CommandValidationException {
        if (!ok(windowspassword))
            throw new CommandValidationException(Strings.get("update.node.dcom.no.password"));
    }

    @Override
    protected RemoteType getType() {
        return RemoteType.DCOM;
    }

    /**
     * Sometimes the console passes an empty string for a parameter. This
     * makes sure those are defaulted correctly.
     */
    @Override
    protected final void checkDefaults() {
        super.checkDefaults();

        // The default is automatically set to 22 -- which is certainly a mistake!
        if (remotePort == null || remotePort.isEmpty() || remotePort.equals(NodeUtils.NODE_DEFAULT_SSH_PORT)) {
            remotePort = NodeUtils.NODE_DEFAULT_DCOM_PORT;
        }
    }

    /**
     * We can't put these values into the base class simply to get the names that
     * the user sees correct.  I.e. "ssh" versus "dcom" versus future types...
     *
     */
    @Override
    protected final void populateBaseClass() {
        remotePort = "135";
        remoteUser = windowsuser;
        remotePassword = windowspassword;
    }

    @Override
    protected final void populateParameters(ParameterMap pmap) {
        pmap.add(NodeUtils.PARAM_WINDOWS_DOMAIN, windowsdomain);
    }

    @Override
    protected final void populateCommandArgs(List<String> args) {
        args.add("--windowsuser");
        args.add(remoteUser);
        args.add("--windowsdomain");
        args.add(windowsdomain);
    }

    /**
     * Get list of password file entries
     * @return List
     */
    @Override
    protected List<String> getPasswords() {
        return DcomUtils.resolvePasswordToList(windowspassword);
    }

    @Override
    protected String getInstallNodeCommandName() {
        return "install-node-dcom";
    }
}
