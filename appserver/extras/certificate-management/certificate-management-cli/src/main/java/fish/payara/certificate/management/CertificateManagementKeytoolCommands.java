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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.certificate.management;

import java.io.File;

/**
 * Class that contains the methods that construct the keytool commands.
 *
 * @author Andrew Pielage
 */
public class CertificateManagementKeytoolCommands {

    /**
     * Constructs the command to pass to keytool for creating a self-signed cert
     *
     * @param keystore The key store to add the certificate to
     * @param password The password for the key store
     * @param alias    The alias of the certificate
     * @param dname    The distinguished name of the certificate
     * @param altnames The alternative names of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructGenerateCertKeytoolCommand(File keystore, char[] password,
            String alias, String dname, String[] altnames) {
        String[] keytoolCmd = new String[]{"-genkeypair",
                "-keyalg", "RSA",
                "-keystore", keystore.getAbsolutePath(),
                "-alias", alias,
                "-dname", dname,
                "-validity", "365",
                "-keypass", new String(password),
                "-storepass", new String(password)};

        if (altnames != null && altnames.length != 0) {
            keytoolCmd = addSubjectAlternativeNames(keytoolCmd, altnames);
        }

        return keytoolCmd;
    }

    /**
     * Helper method that formats the String array of alternative names into the format that the keytool expects.
     *
     * @param keytoolCmd       The String array containing the keytool command before alternative names have been added.
     * @param alternativeNames The String array containing the alternatives names
     * @return A String array of the original keytool command with the alternative names added
     */
    public static String[] addSubjectAlternativeNames(String[] keytoolCmd, String[] alternativeNames) {
        // Create a new array to make room for the extra commands
        String[] expandedKeytoolCmd = new String[keytoolCmd.length + 2];
        System.arraycopy(keytoolCmd, 0, expandedKeytoolCmd, 0, keytoolCmd.length);

        int i = keytoolCmd.length;
        expandedKeytoolCmd[i] = "-ext";

        // Construct full SAN string
        String sanString = "SAN=";
        for (String altName : alternativeNames) {
            // Check if the altname was provided without any additional info, assuming it's DNS if so
            if (!altName.contains(",") && !altName.contains(":")) {
                sanString += "DNS:" + altName;
            } else {
                sanString += altName;
            }
            sanString += ",";
        }
        // Remove trailing comma and add to array
        sanString = sanString.substring(0, sanString.length() - 1);
        expandedKeytoolCmd[i + 1] = sanString;

        return expandedKeytoolCmd;
    }

    /**
     * Constructs the command to pass to keytool for adding a key store entry to another key store
     *
     * @param srcKeystore          The target key store that the certificate was added to
     * @param destKeystore         The target trust store to add the certificate to
     * @param srcKeystorePassword  The password for the source store
     * @param destKeystorePassword The password for the destination store
     * @param alias                The alias of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructImportKeystoreKeytoolCommand(File srcKeystore, File destKeystore,
            char[] srcKeystorePassword, char[] destKeystorePassword, String alias) {
        String[] keytoolCmd = new String[]{"-importkeystore",
                "-srckeystore", srcKeystore.getAbsolutePath(),
                "-destkeystore", destKeystore.getAbsolutePath(),
                "-srcalias", alias,
                "-destalias", alias,
                "-srcstorepass", new String(srcKeystorePassword),
                "-deststorepass", new String(destKeystorePassword),
                "-srckeypass", new String(srcKeystorePassword),
                "-destkeypass", new String(destKeystorePassword),
                "-noprompt"};

        return keytoolCmd;
    }

    /**
     * Constructs the command to pass to keytool for adding a certificate to a store
     *
     * @param certificate The certificate to add to the store
     * @param keystore    The target store to add the certificate to
     * @param password    The password for the key store and certificate
     * @param alias       The alias of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructImportCertKeytoolCommand(File certificate, File keystore,
            char[] password, String alias) {
        String[] keytoolCmd = new String[]{"-importcert",
                "-file", certificate.getAbsolutePath(),
                "-keypass", new String(password),
                "-keystore", keystore.getAbsolutePath(),
                "-storepass", new String(password),
                "-alias", alias,
                "-trustcacerts",
                "-noprompt"};

        return keytoolCmd;
    }

    /**
     * Constructs the command to pass to keytool for creating a CSR
     *
     * @param keystore   The target key store to get the certificate to ask to be signed from
     * @param password   The password for the key store
     * @param outputFile The ${@link File} representing the generated CSR
     * @param alias      The alias of the certificate to create the CSR for
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructGenerateCertRequestKeytoolCommand(File keystore, char[] password,
            File outputFile, String alias) {
        String[] keytoolCmd = new String[]{"-certreq",
                "-keystore", keystore.getAbsolutePath(),
                "-alias", alias,
                "-storepass", new String(password),
                "-keypass", new String(password),
                "-noprompt", "-file", outputFile.getAbsolutePath()};

        return keytoolCmd;
    }
}
