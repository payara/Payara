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
package org.glassfish.ha.commands;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.*;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import javax.validation.constraints.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.*;
import org.glassfish.ha.store.spi.BackingStoreFactoryRegistry;

/**
 * The list-persistence-types command lists different kinds of persistence options for session data
 * when high availability is enabled for an application deployed to a cluster.
 */
@Service(name="list-persistence-types")
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.persistence.types.command")
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-persistence-types", 
        description="list-persistence-types")
})
public class ListPersistenceTypesCommand implements AdminCommand {

    @Param(name="type", optional=false, primary=false)
    @I18n("list.persistence.types.container")
    @Pattern(regexp = "(ejb|web)")
    private String containerType = "";

    private Logger logger;
    private static final String EOL = "\n";
    private static final String SEPARATOR=EOL;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        logger = context.getLogger();
        if (!checkEnvAndParams(report)) {
            return;
        }
        if (logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, Strings.get("list.persistence.types.called", containerType));
        }

        Set<String> allPersistenceTypes = BackingStoreFactoryRegistry.getRegisteredTypes();
        allPersistenceTypes.remove("noop"); // implementation detail.  do not expose to users.
                                            // "noop" is functionally equivalent to "memory".
        if (containerType.equals("ejb") ) {
            allPersistenceTypes.remove("memory");  // ejb did not have "memory" in glassfish v2.x.
        }
        
        StringBuilder sb = new StringBuilder("");
        boolean removeTrailingSeparator = false;
        for (String type : allPersistenceTypes) {
            sb.append(type).append(SEPARATOR);
            removeTrailingSeparator = true;
        }
        String output = sb.toString();
        if (removeTrailingSeparator) {
            output = output.substring(0, output.length()-1);
        }
        Properties extraProperties = new Properties();
        extraProperties.put("types", new ArrayList<String>(allPersistenceTypes));
        
        report.setExtraProperties(extraProperties);        
        report.setMessage(output);
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    // return false for any failures
    private boolean checkEnvAndParams(ActionReport report) {
        if (containerType == null) {
            return fail(report, Strings.get("list.persistence.types.null.parameter"));

        }
        if (!containerType.equals("ejb") && !containerType.equals("web")) {
            return fail(report, Strings.get("list.persistence.types.invalid.parameter", containerType));
        }

        // ok to go
        return true;
    }

    private boolean fail(ActionReport report, String s) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(s);
        return false;
    }

}
