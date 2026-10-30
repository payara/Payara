/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.servermgmt.cli.ChangeMasterPasswordCommandDAS;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.common.FileProtectionUtility;
import org.jvnet.hk2.annotations.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Random;

@Service(name = "generate-encryption-key")
@PerLookup
public class GenerateEncryptionKey extends LocalDomainCommand {

    @Param(name = "domain_name", primary = true, optional = true)
    private String domainName;

    @Param(name = "dontcheckifrunning", optional = true)
    private boolean dontChedkIfDomainRunning;

    private static final String DATAGRID_KEY_FILE = "datagrid-key";
    private static final LocalStringsImpl SERVERMGMT_CLI_STRINGS =
            new LocalStringsImpl(ChangeMasterPasswordCommandDAS.class);
    private static final Random random = new SecureRandom();
    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATION_COUNT = 65556;
    private static final int KEYSIZE = 256;
    private static final String AES = "AES";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    @Override
    protected void validate() throws CommandException {
        setDomainName(domainName);
        super.validate();
        if (!dontChedkIfDomainRunning) {
            checkDomainIsNotRunning();
        }
    }

    @Override
    protected int executeCommand() throws CommandException {
        char[] masterPasswordChars = verifyMasterPassword();

        File datagridKeyFile = new File(getServerDirs().getConfigDir(), DATAGRID_KEY_FILE);
        if (!datagridKeyFile.exists()) {
            createDatagridEncryptionKeyFile(datagridKeyFile);
        }

        byte[] encodedKey = generateAndEncryptKey(masterPasswordChars);

        try {
            Files.write(datagridKeyFile.toPath(), encodedKey);
        } catch (IOException ioe) {
            throw new CommandException("Error writing encoded key to file", ioe);
        }

        return 0;
    }

    private void checkDomainIsNotRunning() throws CommandException {
        HostAndPort adminAddress = getAdminAddress();
        if (NetUtils.isRunning(adminAddress.getHost(), adminAddress.getPort())) {
            throw new CommandException(SERVERMGMT_CLI_STRINGS.get("domain.is.running",
                    getDomainName(), getDomainRootDir()));
        }
    }

    private char[] verifyMasterPassword() throws CommandException {
        String masterpassword = super.readFromMasterPasswordFile();
        if (masterpassword == null) {
            masterpassword = passwords.get("AS_ADMIN_MASTERPASSWORD");
            if (masterpassword == null) {
                char[] masterpasswordChars = super.readPassword(SERVERMGMT_CLI_STRINGS.get("current.mp"));
                masterpassword = masterpasswordChars != null ? new String(masterpasswordChars) : null;
            }
        }
        if (masterpassword == null) {
            throw new CommandException(SERVERMGMT_CLI_STRINGS.get("no.console"));
        }
        if (!super.verifyMasterPassword(masterpassword)) {
            throw new CommandException(SERVERMGMT_CLI_STRINGS.get("incorrect.mp"));
        }

        return masterpassword.toCharArray();
    }

    private void createDatagridEncryptionKeyFile(File datagridKeyFile) throws CommandException {
        try {
            // Windows defaults to essentially "7" for current user, Admins, and System
            Files.createFile(datagridKeyFile.toPath());
            FileProtectionUtility.chmod0600(datagridKeyFile);
        } catch (IOException ioe) {
            throw new CommandException(ioe.getMessage(), ioe);
        }
    }

    private byte[] generateAndEncryptKey(char[] masterpasswordChars) throws CommandException {
        byte[] saltBytes = new byte[20];
        random.nextBytes(saltBytes);

        try {
            // Derive the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(masterpasswordChars, saltBytes, ITERATION_COUNT, KEYSIZE);
            SecretKeySpec secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);

            // Encrypting the data
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();

            byte[] keyBytes = new byte[KEYSIZE / 8];  // Key length is in bits !
            random.nextBytes(keyBytes);

            byte[] encryptedTextBytes = cipher.doFinal(keyBytes);

            // Prepend salt and IV
            byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
            System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
            System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
            System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);
            return buffer;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException
                | NoSuchPaddingException | InvalidParameterSpecException | IllegalBlockSizeException exception) {
            throw new CommandException(exception.getMessage(), exception);
        }
    }
}
