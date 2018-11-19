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

import com.sun.enterprise.module.ModulesRegistry;
import org.glassfish.api.*;
import org.glassfish.api.admin.*;
import javax.inject.Inject;
import com.sun.enterprise.v3.admin.RestartServer;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 *
 * @author bnevins
 */
@Service(name = "_restart-instance")
@PerLookup
@CommandLock(CommandLock.LockType.NONE) // allow restart always
@Async
@I18n("restart.instance.command")
@ExecuteOn(RuntimeType.INSTANCE)
public class RestartInstanceInstanceCommand extends RestartServer implements AdminCommand {

    @Inject
    ModulesRegistry registry;
    @Inject
    private ServerEnvironment env;
    // no default value!  We use the Boolean as a tri-state.
    @Param(name = "debug", optional = true)
    private Boolean debug;

    @Override
    public void execute(AdminCommandContext context) {
        if (!env.isInstance()) {
            String msg = Strings.get("restart.instance.notInstance",
                    env.getRuntimeType().toString());

            context.getLogger().warning(msg);
            return;
        }
        setRegistry(registry);
        setServerName(env.getInstanceName());

        if (debug != null)
            setDebug(debug);

        doExecute(context);
    }
}
