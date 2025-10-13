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
 *
 * Portions Copyright 2017-2025 Payara Foundation and/or affiliates
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
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import jakarta.inject.Inject;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.loader.CurrentBeforeParentClassLoader;
import com.sun.enterprise.v3.server.CommonClassLoaderServiceImpl;
import com.sun.enterprise.v3.server.DomainXmlPersistence;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.beans.PropertyChangeEvent;
import org.glassfish.api.admin.AccessRequired;

/**
 * An asadmin command to add a new library to the libs directory of the domain by uploading a file.
 * <p>
 * It will then attempt to load the new classes dynamically. If there is a file with that name already existing,
 * then it will do nothing, and the new library will not be used until the library is reloaded. Similarly, if there
 * is a class in the new library that has the same full name as a class already existing then it won't be loaded.
 * 
 * @since 3.1.2
 * @version 4.1.2.173
 */
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

    @Param(optional=true, acceptableValues="common, app, war")
    String type = "common";

    @Inject
    ServerEnvironment env;

    @Inject 
    DomainXmlPersistence dxp;

    @Inject 
    UnprocessedConfigListener ucl; 
    
    @Inject
    CommonClassLoaderServiceImpl commonClsLdr;
    
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AddLibraryCommand.class);    

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();
        final Logger logger = Logger.getLogger("org.glassfish.deployment.admin");

        File libDir = env.getLibPath();

        if (type.equals("app")) {
            libDir = new File(libDir, "applibs");
        }
        else if (type.equals("war")) {
            libDir = new File(libDir, "warlibs");
        }

        // rename or copy the library file to the appropriate 
        // library directory
        try {
            List<UnprocessedChangeEvent> unprocessed = 
                new ArrayList<UnprocessedChangeEvent>();

            StringBuilder msg = new StringBuilder();
            
            ClassLoader commonLoader = commonClsLdr.getCommonClassLoader();
            CurrentBeforeParentClassLoader loader = null;
            if (commonLoader instanceof CurrentBeforeParentClassLoader){
                loader = (CurrentBeforeParentClassLoader) commonLoader;
            }

            for (File libraryFile : files) {
                if (libraryFile.exists()) {
                    logger.log(Level.FINER, "ready to add new library");
                    File result = DeploymentCommandUtils.renameUploadedFileOrCopyInPlaceFile(
                        libDir, libraryFile, logger, env);
                    
                    //Applib is its own classloader which does not have a method to load files,
                    if (loader != null && !type.equals("applibs")){
                        loader.addURL(result.toURI().toURL());
                        logger.log(Level.FINE, "added library to classloader",loader);  
                    } else {
                        PropertyChangeEvent pe = new PropertyChangeEvent(libDir,
                                "add-library", null, libraryFile);
                        UnprocessedChangeEvent uce = new UnprocessedChangeEvent(
                                pe, "add-library");
                        unprocessed.add(uce);
                        logger.log(Level.FINER, "library not added to classloader");
                    }
                } else {
                    msg.append(localStrings.getLocalString("lfnf","Library file not found", libraryFile.getAbsolutePath()));
                }
            }
            if (msg.length() > 0) {
                logger.log(Level.WARNING, msg.toString());
                report.setActionExitCode(ActionReport.ExitCode.WARNING);
                report.setMessage(msg.toString());
            }

            if (!unprocessed.isEmpty()) {
                // set the restart required flag
                UnprocessedChangeEvents uces = new UnprocessedChangeEvents(
                        unprocessed);
                List<UnprocessedChangeEvents> ucesList
                        = new ArrayList<UnprocessedChangeEvents>();
                ucesList.add(uces);
                ucl.unprocessedTransactedEvents(ucesList);
            }
            // touch the domain.xml so instances restart will synch 
            // over the libraries.
            dxp.touch();

        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }
    }
}
