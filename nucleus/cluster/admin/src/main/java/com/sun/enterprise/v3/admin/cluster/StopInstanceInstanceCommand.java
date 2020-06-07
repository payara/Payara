/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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


import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.v3.admin.StopServer;
import org.glassfish.api.Async;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * AdminCommand to stop the instance
 * server.
 * Shutdown of an instance.
 * This is the Async command running on the instance.
 *
 * note: This command is asynchronous.  We can't return anything so we just
 * log errors and return

 * @author Byron Nevins
 */
@Service(name = "_stop-instance")
@Async
@PerLookup
@CommandLock(CommandLock.LockType.NONE) // allow stop-instance always
@ExecuteOn(RuntimeType.INSTANCE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_stop-instance", 
        description="_stop-instance")
})
public class StopInstanceInstanceCommand extends StopServer implements AdminCommand {

    @Inject
    private ServerEnvironment env;
    @Inject
    private ServiceLocator habitat;
    @Param(optional = true, defaultValue = "true")
    private Boolean force = true;

    @Override
    public void execute(AdminCommandContext context) {

        if (!env.isInstance()) {
            String msg = Strings.get("stop.instance.notInstance",
                    env.getRuntimeType().toString());

            context.getLogger().warning(msg);
            return;
        }

        doExecute(habitat, env, force);
    }
}
