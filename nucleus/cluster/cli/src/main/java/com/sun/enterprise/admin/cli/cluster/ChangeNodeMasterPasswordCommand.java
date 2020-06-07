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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.cli.cluster;

import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.MASTERPASSWORD_FILE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.HostAndPort;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.common.FileProtectionUtility;
import org.jvnet.hk2.annotations.Service;

import javax.xml.stream.XMLStreamException;

/**
 * The change-master-password command for a node. It takes in a nodeDir and node
 * name
 *
 * @author Bhakti Mehta
 * @author Matt Gill
 */
@Service(name = "_change-master-password-node")
@PerLookup
public class ChangeNodeMasterPasswordCommand extends LocalInstanceCommand {

    private static final Logger LOGGER = Logger.getLogger(ChangeNodeMasterPasswordCommand.class.getName());

    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(ChangeNodeMasterPasswordCommand.class);

    protected static final String OLD_PASSWORD_ALIAS = "AS_ADMIN_MASTERPASSWORD";
    protected static final String NEW_PASSWORD_ALIAS = "AS_ADMIN_NEWMASTERPASSWORD";

    @Param(name = "node", primary = true)
    protected String node;

    @Param(name = "savemasterpassword", optional = true)
    private boolean saveMasterPassword;

    protected File selectedNodeDir;

    private String newPassword;

    @Override
    protected void inject() throws CommandException {
        super.inject();

        selectedNodeDir = new File(nodeDir, node);
    }

    @Override
    protected void validate() throws CommandException, CommandValidationException {
        super.validate();

        if (saveMasterPassword) {
            LOGGER.warning(STRINGS.get("savemasterpassword.unused"));
        }

        // Check node exists
        if (!selectedNodeDir.isDirectory() || !getServerDirs().getAgentDir().exists()) {
            throw new CommandException(STRINGS.get("bad.node.dir", selectedNodeDir));
        }

        // Check instances aren't running
        for (File instanceDir : getInstanceDirectories()) {
            if (isRunning(instanceDir)) {
                throw new CommandException(STRINGS.get("instance.is.running", instanceDir.getName()));
            }
        }

        // Find and verify old password
        String oldPassword = findOldPassword();
        String currentPassword = ofNullable(readFromMasterPasswordFile()).orElse(DEFAULT_MASTER_PASSWORD);
        if (!oldPassword.equals(currentPassword)) {
            throw new CommandException(STRINGS.get("incorrect.old.mp"));
        }

        // Find and set new password
        setNewPassword();

        try {
            if (dataGridEncryptionEnabled()) {
                LOGGER.warning("Data grid encryption is enabled - " +
                        "you will need to regenerate the encryption key");
            }
        } catch (IOException | XMLStreamException exception) {
            LOGGER.warning("Could not determine if data grid encryption is enabled - " +
                    "you will need to regenerate the encryption key if it is");
        }
    }

    @Override
    protected int executeCommand() throws CommandException {
        // Find the master password file
        final File pwdFile = new File(this.getServerDirs().getAgentDir(), MASTERPASSWORD_FILE);
        try {
            // Write the master password file
            PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(), MASTERPASSWORD_FILE.toCharArray());
            p.setPasswordForAlias(MASTERPASSWORD_FILE, newPassword.getBytes());
            FileProtectionUtility.chmod0600(pwdFile);
            return 0;
        } catch (Exception ex) {
            throw new CommandException(STRINGS.get("masterPasswordFileNotCreated", pwdFile), ex);
        }
    }

    @Override
    public int execute(String... argv) throws CommandException {
        // We iterate through all the instances and so it should relax this requirement
        // that there is only 1 instance in a node .
        checkOneAndOnly = false;
        return super.execute(argv);
    }

    /**
     * Find the old password from the property in the password file with the name
     * {@link #OLD_PASSWORD_ALIAS} if it exists, or by prompting the user otherwise.
     * 
     * @throws CommandException if the password is null
     */
    protected String findOldPassword() throws CommandException {
        // Fetch from master password file
        String oldPassword = super.readFromMasterPasswordFile();

        // Fetch from provided password file
        if (oldPassword == null) {
            oldPassword = passwords.get(OLD_PASSWORD_ALIAS);
        }

        // Prompt user
        if (oldPassword == null) {
            char[] opArr = super.readPassword(STRINGS.get("old.mp"));
            oldPassword = opArr != null ? new String(opArr) : null;
        }

        // Check if password was collected
        if (oldPassword == null) {
            throw new CommandException(STRINGS.get("no.console"));
        }

        return oldPassword;
    }

    /**
     * Set the {@link #newPassword} field from the property in the password file
     * with the name {@link #OLD_PASSWORD_ALIAS} if it exists, or by prompting the
     * user twice otherwise.
     * 
     * @throws CommandException if the passwords don't match or are null
     */
    protected void setNewPassword() throws CommandException {
        ParamModelData nmpo = new ParamModelData(NEW_PASSWORD_ALIAS, String.class, false, null);
        nmpo.prompt = STRINGS.get("new.mp");
        nmpo.promptAgain = STRINGS.get("new.mp.again");
        nmpo.param._password = true;
        char[] npArr = super.getPassword(nmpo, null, true);
        newPassword = npArr != null ? new String(npArr) : null;

        // Check if password was collected
        if (newPassword == null) {
            throw new CommandException(STRINGS.get("no.console"));
        }
    }

    /**
     * This will get the directory of all instances for the selected node.
     * 
     * @return The list of instances for the selected node
     * @throws CommandException if there are no instances
     */
    private List<File> getInstanceDirectories() throws CommandException {

        File[] instanceDirectories = selectedNodeDir.listFiles(f -> f.isDirectory() && !f.getName().equals("agent"));

        if (instanceDirectories == null || instanceDirectories.length == 0) {
            throw new CommandException(STRINGS.get("Instance.noInstanceDirs", selectedNodeDir));
        }

        return asList(instanceDirectories);
    }

    /**
     * @param instanceDir the directory of the instance to check
     * @return if the instance is currently running
     */
    private boolean isRunning(File instanceDir) throws CommandException {
        try {
            File configDir = new File(instanceDir, "config");
            File domainXml = new File(configDir, "domain.xml");
            if (!domainXml.exists()) {
                return false;
            }
            MiniXmlParser parser = new MiniXmlParser(domainXml, instanceDir.getName());
            List<HostAndPort> addrSet = parser.getAdminAddresses();
            if (addrSet.isEmpty()) {
                throw new CommandException(STRINGS.get("NoAdminPort"));
            }
            HostAndPort addr = addrSet.get(0);
            return isRunning(addr.getHost(), addr.getPort());
        } catch (MiniXmlParserException ex) {
            throw new CommandException(STRINGS.get("NoAdminPortEx", ex), ex);
        }
    }

}
