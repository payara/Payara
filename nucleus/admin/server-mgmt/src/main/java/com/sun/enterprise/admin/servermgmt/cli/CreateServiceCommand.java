/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2021] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.util.ServerDirsSelector;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.util.logging.*;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.admin.servermgmt.services.ServiceFactory;
import com.sun.enterprise.admin.servermgmt.services.Service;
import com.sun.enterprise.admin.servermgmt.services.AppserverServiceType;
import com.sun.enterprise.admin.servermgmt.services.PlatformServicesInfo;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.ServerDirs;

/**
 * Create a "service" in the operating system to start this domain
 * automatically.
 */
@org.jvnet.hk2.annotations.Service(name = "create-service")
@PerLookup
public final class CreateServiceCommand extends CLICommand {
    public static final String SYSTEM_TYPE_SOLARIS = "solaris";
    public static final String SYSTEM_TYPE_SYSTEMD = "systemd";
    public static final String SYSTEM_TYPE_SYSTEMV = "systemv";
    public static final String SYSTEM_TYPE_WINDOWS = "windows";

    @Param(name = "name", optional = true)
    private String serviceName;
    @Param(name = "serviceproperties", optional = true)
    private String serviceProperties;
    @Param(name = "dry-run", shortName = "n", optional = true,
            defaultValue = "false")
    private boolean dry_run;
    @Param(name = "force", optional = true, defaultValue = "false")
    private boolean force;
    @Param(name = "domaindir", optional = true)
    private File userSpecifiedDomainDirParent;
    @Param(name = "serviceuser", optional = true)
    private String serviceUser;
    @Param(name = "system-type", optional = true)
    private String systemType;

    /*
     * The following parameters allow an unattended start-up any number of
     * ways to tell where the domain.xml file is that should be read for
     * client/instance-side security confir.
     */
    @Param(name = "domain_or_instance_name", primary = true, optional = true, alias = "domain_name")
    private String userSpecifiedServerName;
    @Param(name = "nodedir", optional = true, alias = "agentdir")
    private String userSpecifiedNodeDir;           // nodeDirRoot
    @Param(name = "node", optional = true, alias = "nodeagent")
    private String userSpecifiedNode;
    private File asadminScript;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(CreateServiceCommand.class);
    private ServerDirs dirs;
    private ServerDirsSelector selector = null;

    @Override
    protected void validate() throws CommandException {
        try {
            super.validate(); // pointless empty method but who knows what the future holds?


            if (ok(serviceUser) && !OS.isLinux()) {
                // serviceUser is only supported on Linux
                throw new CommandException(strings.get("serviceUser_wrong_os"));
            }
            // The order that you make these calls matters!!

            selector = ServerDirsSelector.getInstance(
                    userSpecifiedDomainDirParent,
                    userSpecifiedServerName,
                    userSpecifiedNodeDir,
                    userSpecifiedNode);
            dirs = selector.dirs();

            validateServiceName();
            validateSystemType();
            validateAsadmin();
        }
        catch (CommandException e) {
            throw e;
        }
        catch (Exception e) {
            // plenty of RuntimeException possibilities!
            throw new CommandException(e.getMessage(), e);
        }
    }

    @Override
    protected int executeCommand() throws CommandException {
        try {
            final Service service = ServiceFactory.getService(dirs, getType(), systemType);
            PlatformServicesInfo info = service.getInfo();
            info.setTrace(logger.isLoggable(Level.FINER));
            info.setDryRun(dry_run);
            info.setForce(force);
            info.setAppServerUser(getProgramOptions().getUser());
            if (ok(serviceName)) {
                info.setServiceName(serviceName);
            }

            if (ok(serviceUser)) {
                info.setServiceUser(serviceUser);
            }

            if (programOpts.getPasswordFile() != null) {
                info.setPasswordFile(SmartFile.sanitize(
                        new File(programOpts.getPasswordFile())));
            }

            service.setServiceProperties(serviceProperties);
            service.createService();

            // Why the messiness?  We don't want to talk about the help
            // file inside the help file thus the complications below...
            String help = service.getSuccessMessage();
            String tellUserAboutHelp = strings.get("create.service.runtimeHelp", help,
                    new File(dirs.getServerDir(), "PlatformServices.log"));
            logger.info(tellUserAboutHelp);
            service.writeReadmeFile(help);
        } catch (Exception e) {
            // We only want to wrap the string -- not the Exception.
            // Otherwise the message that is printed out to the user will be like this:
            // java.lang.IllegalArgumentException: The passwordfile blah blah blah
            // What we want is:
            // The passwordfile blah blah blah
            // IT 8882
            String msg = e.getMessage();

            if (ok(msg)) {
                throw new CommandException(msg);
            } else {
                throw new CommandException(e);
            }
        }
        return 0;
    }

    private void validateServiceName() throws CommandException {
        if (!ok(serviceName))
            serviceName = dirs.getServerDir().getName();

        // On Windows we need a legal filename for the service name.
        if(OS.isWindowsForSure() && !FileUtils.isFriendlyFilename(serviceName)) {
            throw new CommandException(strings.get("create.service.badServiceName", serviceName));
        }

        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "service name = {0}", serviceName);
    }

    private void validateAsadmin() throws CommandException {
        String s = SystemPropertyConstants.getAsAdminScriptLocation();

        if (!ok(s))
            throw new CommandException(
                    strings.get("internal.error",
                    "Can't get Asadmin script location"));

        asadminScript = SmartFile.sanitize(new File(s));

        if (!asadminScript.isFile()) {
            throw new CommandException(
                    strings.get("create.service.noAsadminScript", asadminScript));
        }
    }

    private void validateSystemType() throws CommandException {
        if (ok(systemType)
                && !(SYSTEM_TYPE_SOLARIS.equals(systemType)
                || SYSTEM_TYPE_SYSTEMD.equals(systemType)
                || SYSTEM_TYPE_SYSTEMV.equals(systemType)
                || SYSTEM_TYPE_WINDOWS.equals(systemType))) {
            throw new CommandException(
                    strings.get("create.service.invalidSystemType", systemType));
        }
    }

    private AppserverServiceType getType() {
        if (selector.isInstance())
            return AppserverServiceType.Instance;
        else
            return AppserverServiceType.Domain;
    }
}
