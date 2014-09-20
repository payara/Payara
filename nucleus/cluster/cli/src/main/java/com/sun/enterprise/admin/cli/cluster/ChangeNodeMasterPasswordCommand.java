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


package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.servermgmt.NodeKeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryConfig;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.HostAndPort;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * The change-master-password command for a node.
 * It takes in a nodeDir and node name
 *
 * @author Bhakti Mehta
 */
@Service(name = "_change-master-password-node")
@PerLookup

public  class ChangeNodeMasterPasswordCommand extends LocalInstanceCommand {

    @Param(name = "nodedir", optional = true)
    protected String nodeDir0;           // nodeDirRoot

    @Param(name = "node", primary = true)
    protected String node0;

    @Param(name = "savemasterpassword", optional = true, defaultValue = "false")
    protected boolean savemp;

    private static final String MASTER_PASSWORD_ALIAS="master-password";

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(ChangeNodeMasterPasswordCommand.class);

    private String newPassword;

    private String oldPassword;


    @Override
    protected int executeCommand() throws CommandException {

        try {
            nodeDir = nodeDir0;
            node = node0;
            File serverDir = new File(nodeDir,node);

            if (!serverDir.isDirectory()) {
                throw new CommandException(strings.get("bad.node.dir",serverDir));
            }

            ArrayList<String> serverNames = getInstanceDirs(serverDir);
            for (String serverName: serverNames) 
                if (isRunning(serverDir, serverName))
                    throw new CommandException(strings.get("instance.is.running",
                            serverName));

            oldPassword = passwords.get("AS_ADMIN_MASTERPASSWORD");
            if (oldPassword == null) {
                oldPassword = super.readPassword(strings.get("old.mp"));
            }
            if (oldPassword == null)
                throw new CommandException(strings.get("no.console"));

            // for each instance iterate through the instances first,
            // read each keystore with the old password,
            // only then should it save the new master password.
            boolean valid = true;
            for(String instanceDir0: getInstanceDirs(nodeDirChild)) {
               valid &= verifyInstancePassword(new File(nodeDirChild,instanceDir0));
           }
           if (!valid) {
               throw new CommandException(strings.get("incorrect.old.mp"));
           }
            ParamModelData nmpo = new ParamModelData("AS_ADMIN_NEWMASTERPASSWORD",
                    String.class, false, null);
            nmpo.prompt = strings.get("new.mp");
            nmpo.promptAgain = strings.get("new.mp.again");
            nmpo.param._password = true;
            newPassword = super.getPassword(nmpo, null, true);

            // for each instance encrypt the keystore
            for(String instanceDir2: getInstanceDirs(nodeDirChild)) {
               encryptKeystore(instanceDir2);
           }
            if (savemp) {
                createMasterPasswordFile();
            }
            return 0;
        } catch(Exception e) {
            throw new CommandException(e.getMessage(),e);
        }
    }

    /**
     * This will load and verify the keystore for each of the instances
     * in a node
     * @param instanceDir0 The instance directory
     * @return  if the password is valid for the instance keystore
     */
    private boolean verifyInstancePassword(File instanceDir) {

        File mp = new File(new File(instanceDir, "config"), "cacerts.jks");
        return loadAndVerifyKeystore(mp,oldPassword);
    }



    @Override
    public int execute(String... argv) throws CommandException {
        // We iterate through all the instances and so it should relax this requirement
        // that there is only 1 instance in a node .
        checkOneAndOnly = false;
        return super.execute(argv);
    }

    /**
     * Create the master password keystore. This routine can also modify the master password
     * if the keystore already exists
     *
     * @throws CommandException
     */
    protected void createMasterPasswordFile() throws CommandException {
        final File pwdFile = new File(this.getServerDirs().getAgentDir(), MASTER_PASSWORD_ALIAS);
        try {
            PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(),
                MASTER_PASSWORD_ALIAS.toCharArray());
            p.setPasswordForAlias(MASTER_PASSWORD_ALIAS, newPassword.getBytes());
            pwdFile.setReadable(true);
            pwdFile.setWritable(true);
        } catch (Exception ex) {
            throw new CommandException(strings.get("masterPasswordFileNotCreated", pwdFile),
                ex);
        }
    }


    /*
     * This will encrypt the keystore
     */
    public void encryptKeystore(String f) throws CommandException {

        RepositoryConfig nodeConfig = new RepositoryConfig(f,
                new File(nodeDir, node).toString(), f);
        NodeKeystoreManager km = new NodeKeystoreManager();
        try {
            km.encryptKeystore(nodeConfig,oldPassword,newPassword);

        } catch (Exception e) {
             throw new CommandException(strings.get("Keystore.not.encrypted"),
                e);
        }

    }

    /**
     * This will get all the instances for a given node
     * @param parent  node
     * @return   The list of instances for a node
     * @throws CommandException
     */
    private ArrayList<String> getInstanceDirs(File parent) throws CommandException {

         ArrayList<String> instancesList = new ArrayList<String>();
         File[] files = parent.listFiles(new FileFilter() {
             public boolean accept(File f) {
                 return f.isDirectory();
             }
         });

         if (files == null || files.length == 0) {
             throw new CommandException(
                     strings.get("Instance.noInstanceDirs", parent));
         }

         for (File f : files) {
             if (!f.getName().equals("agent"))
                 instancesList.add(f.getName());
         }
         return instancesList;

     }

    private boolean isRunning(File nodeDirChild, String serverName)
            throws CommandException {
        try {
            File serverDir = new File(nodeDirChild, serverName);
            File configDir = new File(serverDir, "config");
            File domainXml = new File(configDir, "domain.xml");
            if (!domainXml.exists())
                return false;
            MiniXmlParser parser = new MiniXmlParser(domainXml, serverName);
            List<HostAndPort> addrSet = parser.getAdminAddresses();
            if (addrSet.size() <= 0)
                throw new CommandException(strings.get("NoAdminPort"));
            HostAndPort addr = addrSet.get(0);
            return isRunning(addr.getHost(), addr.getPort());
        } catch (MiniXmlParserException ex) {
            throw new CommandException(strings.get("NoAdminPortEx", ex), ex);
        }
    }
    
}
