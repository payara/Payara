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

package org.glassfish.deployment.admin;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import javax.inject.Inject;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.v3.server.DomainXmlPersistence;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.beans.PropertyChangeEvent;
import org.glassfish.api.admin.AccessRequired;

@Service(name="add-library")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class, opType= RestEndpoint.OpType.POST, path="add-library", description="Install library")
})
@AccessRequired(resource=DeploymentCommandUtils.LIBRARY_SECURITY_RESOURCE_PREFIX + "$type", action="create")
public class AddLibraryCommand implements AdminCommand {

    @Param(primary=true, multiple=true)
    File[] files = null;

    @Param(optional=true, acceptableValues="common, ext, app")
    String type = "common";

    @Inject
    ServerEnvironment env;

    @Inject 
    DomainXmlPersistence dxp;

    @Inject 
    UnprocessedConfigListener ucl; 

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AddLibraryCommand.class);    

    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        File libDir = env.getLibPath();

        if (type.equals("ext")) {
            libDir = new File(libDir, "ext");
        } else if (type.equals("app")) {
            libDir = new File(libDir, "applibs");
        }

        // rename or copy the library file to the appropriate 
        // library directory
        try {
            List<UnprocessedChangeEvent> unprocessed = 
                new ArrayList<UnprocessedChangeEvent>();

            StringBuffer msg = new StringBuffer();

            for (File libraryFile : files) {
                if (libraryFile.exists()) {
                    DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(
                        libDir, libraryFile, logger, env);
                    PropertyChangeEvent pe = new PropertyChangeEvent(libDir, 
                        "add-library", null, libraryFile);
                    UnprocessedChangeEvent uce = new UnprocessedChangeEvent(
                        pe, "add-library");
                    unprocessed.add(uce);
                } else {
                    msg.append(localStrings.getLocalString("lfnf","Library file not found", libraryFile.getAbsolutePath()));
                }
            }
            if (msg.length() > 0) {
                logger.log(Level.WARNING, msg.toString());
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
                report.setMessage(msg.toString());
            }

            // set the restart required flag
            UnprocessedChangeEvents uces = new UnprocessedChangeEvents(
                unprocessed);
            List<UnprocessedChangeEvents> ucesList = 
                new ArrayList<UnprocessedChangeEvents>();
            ucesList.add(uces);
            ucl.unprocessedTransactedEvents(ucesList); 

            // touch the domain.xml so instances restart will synch 
            // over the libraries.
            dxp.touch();

        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
    }
}
