/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;

import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.internal.api.Globals;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.util.SSHUtil;

import com.sun.enterprise.util.io.FileUtils;

/**
 *  This is a local command that distributes the SSH public key to remote node(s)
 *
 */
@Service(name = "setup-ssh")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
public final class SetupSshKey extends NativeRemoteCommandsBase {
    private static final String NL = System.getProperty("line.separator");
    @Param(name = "sshuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(optional = true, defaultValue = "22", name = "sshport")
    int port;
    @Param(optional = true)
    String sshkeyfile;
    @Param(optional = true)
    private String sshpublickeyfile;
    @Param(optional = true, defaultValue = "false")
    private boolean generatekey;
    @Inject
    private Habitat habitat;

    public SetupSshKey() {
    }

    /**
     */
    @Override
    protected void validate()
            throws CommandException {
        super.validate();
        Globals.setDefaultHabitat(habitat);

        if (sshkeyfile == null) {
            //if user hasn't specified a key file and there is no key file at default
            //location, then generate one
            String existingKey = SSHUtil.getExistingKeyFile();
            if (existingKey == null) {
                sshkeyfile = SSHUtil.getDefaultKeyFile();
                if (promptForKeyGeneration()) {
                    generatekey = true;
                }
            }
            else {
                //there is a key that requires to be distributed, hence need password
                promptPass = true;
                sshkeyfile = existingKey;

                if (isEncryptedKey()) {
                    sshkeypassphrase = getSSHPassphrase(false);
                }
            }
        }
        else {
            validateKeyFile(sshkeyfile);
            if (isEncryptedKey()) {
                sshkeypassphrase = getSSHPassphrase(false);
            }
        }

        if (sshpublickeyfile != null) {
            validateKeyFile(sshpublickeyfile);
        }

    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException {

        SSHLauncher sshL = habitat.getComponent(SSHLauncher.class);

        String previousPassword = null;
        boolean status = false;
        for (String node : hosts) {
            sshL.init(getRemoteUser(), node, getRemotePort(), sshpassword, sshkeyfile, sshkeypassphrase, logger);
            if (generatekey || promptPass) {
                //prompt for password iff required
                if (sshkeyfile != null || SSHUtil.getExistingKeyFile() != null) {
                    if (sshL.checkConnection()) {
                        logger.info(Strings.get("SSHAlreadySetup", getRemoteUser(), node));
                        continue;
                    }
                }
                if (previousPassword != null) {
                    status = sshL.checkPasswordAuth();
                }
                if (!status) {
                    sshpassword = getSSHPassword(node);
                    previousPassword = sshpassword;
                }
            }

            try {
                sshL.setupKey(node, sshpublickeyfile, generatekey, sshpassword);
            }
            catch (IOException ce) {
                //logger.fine("SSH key setup failed: " + ce.getMessage());
                throw new CommandException(Strings.get("KeySetupFailed", ce.getMessage()));
            }
            catch (Exception e) {
                //handle KeyStoreException
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Keystore error: ", e);
                }
            }

            if (!sshL.checkConnection()) {
                throw new CommandException(Strings.get("ConnFailed"));
            }
        }
        return SUCCESS;
    }

    /**
     * Method that sets the prompt flag only if key file exists
     * @param file the key file
     */
    private void validateKeyFile(String file) throws CommandException {
        //if key exists, set prompt flag
        File f = new File(file);
        if (f.exists()) {
            if (!f.getName().endsWith(".pub")) {
                String key = null;
                try {
                    key = FileUtils.readSmallFile(file);
                }
                catch (IOException ioe) {
                    throw new CommandException(Strings.get("UnableToReadKey", file, ioe.getMessage()));
                }
                if (!key.startsWith("-----BEGIN ") && !key.endsWith(" PRIVATE KEY-----" + NL)) {
                    throw new CommandException(Strings.get("InvalidKeyFile", file));
                }
            }
            promptPass = true;
        }
        else {
            throw new CommandException(Strings.get("KeyDoesNotExist", file));
        }
    }

    /**
     * Prompt for key generation
     */
    private boolean promptForKeyGeneration() {
        if (generatekey)
            return true;

        if (!programOpts.isInteractive())
            return false;

        Console cons = System.console();

        if (cons != null) {
            String val = null;
            do {
                cons.printf("%s", Strings.get("GenerateKeyPairPrompt", getRemoteUser(), Arrays.toString(hosts)));
                val = cons.readLine();
                if (val != null && (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("y"))) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer("Generate key!");
                    }
                    return true;
                }
                else if (val != null && (val.equalsIgnoreCase("no") || val.equalsIgnoreCase("n"))) {
                    break;
                }
            }
            while (val != null && !isValidAnswer(val));
        }
        return false;
    }

    @Override
    final String getRawRemoteUser() {
        return user;
    }

    @Override
    int getRawRemotePort() {
        return port;
    }

    @Override
    String getSshKeyFile() {
        return sshkeyfile;
    }
}
