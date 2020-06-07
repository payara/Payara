/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.hk2.api.PerLookup;

/**
 * Dumps the currently configured HK2 modules and their contents.
 *
 * <p>
 * Useful for debugging classloader related issues.
 *
 * @author Kohsuke Kawaguchi
 */
@PerLookup
@Service(name="_dump-hk2")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_dump-hk2", 
        description="_dump-hk2")
})
@AccessRequired(resource="domain", action="dump")
public class DumpHK2Command implements AdminCommand {

    @Inject
    ModulesRegistry modulesRegistry;
    
    public void execute(AdminCommandContext context) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        modulesRegistry.dumpState(new PrintStream(baos));

        ActionReport report = context.getActionReport();
        report.setActionExitCode(ExitCode.SUCCESS);
        String msg = baos.toString();
        
        // the proper way to do this is to check the user-agent of the caller,
        // but I can't access that -- so I'll just check the type of the 
        // ActionReport.  If we are sending back to CLI then linefeeds will 
        // cause problems.  Manifest.write() is OK but Manifest.read() explodes!
        if(report instanceof PropsFileActionReporter) {
            msg = ManifestUtils.encode(msg);
        }
        report.setMessage(msg);
    }
}
