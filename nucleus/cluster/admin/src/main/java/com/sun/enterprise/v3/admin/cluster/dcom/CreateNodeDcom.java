/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin.cluster.dcom;

import com.sun.enterprise.v3.admin.cluster.CreateRemoteNodeCommand;
import com.sun.enterprise.v3.admin.cluster.NodeUtils;
import java.util.List;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.PerLookup;
import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Remote AdminCommand to create a DCOM node
 *
 * @author Byron Nevins
 */
@Service(name = "create-node-dcom")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
public class CreateNodeDcom extends CreateRemoteNodeCommand {
    //@Param(name = "dcomport", optional = true, defaultValue = "135")
    //private String dcomport;
    @Param(name = "dcomuser", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_REMOTE_USER)
    private String dcomuser;
    @Param(name = "dcompassword", optional = true, password = true)
    private String dcompassword;

    @Override
    public final void execute(AdminCommandContext context) {
        executeInternal(context);
    }

    @Override
    protected void validate() throws CommandValidationException {
        if (!ok(dcompassword))
            throw new CommandValidationException(Strings.get("update.node.dcom.no.password"));
    }

    @Override
    protected NodeUtils.RemoteType getType() {
        return NodeUtils.RemoteType.DCOM;
    }

    /**
     * Sometimes the console passes an empty string for a parameter. This
     * makes sure those are defaulted correctly.
     */
    @Override
    protected final void checkDefaults() {
        super.checkDefaults();

        // The default is automatically set to 22 -- which is certainly a mistake!
        if (!ok(remotePort) || remotePort.equals(NodeUtils.NODE_DEFAULT_SSH_PORT)) {
            remotePort = NodeUtils.NODE_DEFAULT_DCOM_PORT;
        }
    }

    /**
     * We can't put these values into the base class simply to get the names that
     * the user sees correct.  I.e. "ssh" versus "dcom" versus future types...
     *
     */
    @Override
    final protected void populateBaseClass() {
        remotePort = "135";
        remoteUser = dcomuser;
        remotePassword = dcompassword;
    }

    @Override
    protected final void populateParameters(ParameterMap pmap) {
    }

    @Override
    protected final void populateCommandArgs(List<String> args) {
        args.add("--dcom=true");
        args.add("--dcomuser");
        args.add(remoteUser);
        //args.add("--dcomport");
        //args.add(remotePort);
    }

    @Override
    protected String getPasswordsForFile() {
        return "AS_ADMIN_DCOMPASSWORD=" + dcompassword + "\n";
    }
}
