/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The change-master-password command.
 * This is a command which can operate on both the DAS and the node
 * The master password is the password that is used to encrypt the DAS (and instances) keystore. Therefore the DAS and associated server instances need the password to open the keystore at startup.
 * The master password is the same for the DAS and all instances in the domain
 * The default master password is "changeit"and can be saved in a master-password file:

 * 1. DAS: domains/domainname/master-password
 * 2. Instance: nodes/node-name/master-password
 * The master-password may be changed on the DAS by running change-master-password.
 * The DAS must be down to run this command. change-master-password supports the --savemasterpassword option.
 * To change the master-password file on a node you run change-master-password with --nodedir
 * and the node name.  The instances must be down to run this command on a node
 * 
 * If --nodedir is not specified it will look in the default location of nodes folder and find
 * the node
 *
 * If the domain and node have the same name it will execute the command for the domain. Incase
 * you want the command to be executed for a node when the domain and node name is same
 * you will need to specify the --nodedir option
 *
 * @author Bhakti Mehta
 */
@Service(name = "change-master-password")
@PerLookup
public class ChangeMasterPasswordCommand extends CLICommand {

    @Inject
    private ServiceLocator habitat;

    @Param(name = "savemasterpassword", optional = true, defaultValue = "false")
    private boolean savemp;

    @Param(name = "domain_name_or_node_name", primary = true, optional = true)
    private String domainNameOrNodeName;

    @Param(name = "nodedir", optional = true)
    protected String nodeDir;

    @Param(name = "domaindir", optional = true)
    protected String domainDirParam = null;

    private final String CHANGE_MASTER_PASSWORD_DAS =
            "_change-master-password-das";

    private final String CHANGE_MASTER_PASSWORD_NODE =
            "_change-master-password-node";

    private static final LocalStringsImpl strings =
       new LocalStringsImpl(ChangeMasterPasswordCommand.class);



    @Override
    protected int executeCommand() throws CommandException {
        CLICommand command = null;

        if (domainDirParam != null && nodeDir != null) {
            throw new CommandException(strings.get("both.domaindir.nodedir.not.allowed"));
        }
        try {
            if (isDomain()) {  // is it domain
                command = CLICommand.getCommand(habitat,
                        CHANGE_MASTER_PASSWORD_DAS);
                return command.execute(argv);
            }

            if (nodeDir != null) {
                command = CLICommand.getCommand(habitat,
                        CHANGE_MASTER_PASSWORD_NODE);
                return command.execute(argv);
            } else {

                // nodeDir is not specified and domainNameOrNodeName is not a domain.
                // It could be a node
                // We add defaultNodeDir parameter to args
                ArrayList arguments = new ArrayList<String>(Arrays.asList(argv));
                arguments.remove(argv.length -1);
                arguments.add("--nodedir");
                arguments.add(getDefaultNodesDirs().getAbsolutePath());
                arguments.add(domainNameOrNodeName);
                String[] newargs = (String[]) arguments.toArray(new String[arguments.size()]);

                command = CLICommand.getCommand(habitat,
                        CHANGE_MASTER_PASSWORD_NODE);
                return command.execute(newargs);
            }
        } catch (IOException e) {
            throw new CommandException(e.getMessage(),e);
        }
    }

    @Override
    public int execute(String... args) throws CommandException {  
        
        //This will parse the args and call executeCommand
        super.execute(args);
        return 0;
       
    }

    private boolean isDomain() throws IOException {
        DomainDirs domainDirs = null;
        //if both domainDir and domainNameOrNodeName are null get default domaindir
        if (domainDirParam == null && domainNameOrNodeName == null ) {
            domainDirs = new DomainDirs(DomainDirs.getDefaultDomainsDir());
        } else  {
            if (domainDirParam != null) {
                domainDirs = new DomainDirs(new File(domainDirParam),domainNameOrNodeName);
                return domainDirs.isValid();
            }
            if (domainNameOrNodeName != null) {
                return new File(DomainDirs.getDefaultDomainsDir(),domainNameOrNodeName).isDirectory();
            }
        }
        //It can be null in the case when this is not a domain but a node
        if (domainDirs != null) {
            return domainDirs.getDomainsDir().isDirectory();
        }
        return false;

    }

    private File getDefaultNodesDirs() throws IOException {
        return new File(DomainDirs.getDefaultDomainsDir().getParent(),
                "nodes");
    }


}
