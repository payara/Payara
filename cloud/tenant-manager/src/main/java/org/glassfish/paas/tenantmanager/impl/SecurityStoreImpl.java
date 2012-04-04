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
package org.glassfish.paas.tenantmanager.impl;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.jvnet.hk2.annotations.Service;

/**
 * Default implementation for file realm.
 * 
 * @author Andriy Zhdanov
 *
 */
@Service
public class SecurityStoreImpl implements SecurityStore {

    /**
     * Creates file-user in default realm.
     */
    @Override
    public void create(String name, char[] password) {

        CommandInvocation cmd = commandRunner.getCommandInvocation("create-file-user", actionReport);
        ParameterMap map = new ParameterMap();
        map.add("userpassword", password.toString());
        map.add("username", name);
        // TODO: map.add("authrealmname", "file"); if default-realm differs from 'file'? 
        cmd.parameters(map);
        cmd.execute();
        if (actionReport.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            Throwable cause = actionReport.getFailureCause();
            // minimize end error message
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } else if (actionReport.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            logger.fine(actionReport.getMessage());
        }
    }

    @Inject
    private CommandRunner commandRunner;

    @Inject
    @Named("plain")
    private ActionReport actionReport;

    @Inject
    private Logger logger;
}
