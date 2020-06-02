package fish.payara.certificate.management;

import java.io.File;

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
        String[] keytoolCmd = new String[]{"-genkeypair", "-keyalg", "RSA", "-keystore", keystore.getAbsolutePath(),
                "-alias", alias, "-dname", dname,
                "-validity", "365", "-keypass", new String(password), "-storepass", new String(password)};

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
        expandedKeytoolCmd[i + 1] = "SAN=";

        for (String altName : alternativeNames) {
            // Check if the altname was provided without any additional info, assuming it's DNS if so
            if (!altName.contains(",") && !altName.contains(":")) {
                expandedKeytoolCmd[i + 1] += "DNS:" + altName;
            } else {
                expandedKeytoolCmd[i + 1] += altName;
            }
            expandedKeytoolCmd[i + 1] += ",";
        }

        // Remove trailing comma
        expandedKeytoolCmd[i + 1] = expandedKeytoolCmd[i + 1].substring(0, expandedKeytoolCmd[i + 1].length() - 1);

        return expandedKeytoolCmd;
    }

    /**
     * Constructs the command to pass to keytool for adding the self-signed cert to the trust store
     *
     * @param keystore           The target key store that the certificate was added to
     * @param truststore         The target trust store to add the certificate to
     * @param keystorePassword   The password for the key store
     * @param truststorePassword The password for the trust store
     * @param alias              The alias of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructImportCertKeytoolCommand(File keystore, File truststore, char[] keystorePassword,
            char[] truststorePassword, String alias) {
        String[] keytoolCmd = new String[]{"-importkeystore", "-srckeystore", keystore.getAbsolutePath(),
                "-destkeystore", truststore.getAbsolutePath(), "-srcalias", alias, "-destalias", alias,
                "-srcstorepass", new String(keystorePassword), "-deststorepass", new String(truststorePassword),
                "-srckeypass", new String(keystorePassword), "-destkeypass", new String(truststorePassword),
                "-noprompt"};

        return keytoolCmd;
    }

    public static String[] constructGenerateCertRequestKeytoolCommand(File keystore, char[] password,
            File outputFile, String alias) {
        String[] keytoolCmd = new String[]{"-certreq", "-keystore", keystore.getAbsolutePath(),
                "-alias", alias,
                "-storepass", new String(password),
                "-keypass", new String(password),
                "-noprompt", "-file", outputFile.getAbsolutePath()};

        return keytoolCmd;
    }
}
