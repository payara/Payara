/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.lbplugin;

import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.util.FileTransferUtil;
import org.glassfish.paas.orchestrator.provisioning.util.RemoteCommandExecutor;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Service
public class GlassFishLBProvisioner implements LBProvisioner {


    @Inject
    private FileTransferUtil fileTransferUtil;

    @Inject
    private RemoteCommandExecutor remoteCommandExecutor;

    public static final String LB_APP_SERVER_LOCAL_KEYPAIR_LOCATION = "LB_APP_SERVER_LOCAL_KEYPAIR_LOCATION";
    public static final String LB_APP_SERVER_KEYPAIR_LOCATION = "LB_APP_SERVER_KEYPAIR_LOCATION";
    public static final String LB_APP_SERVER_INSTALL_LOCATION = "LB_APP_SERVER_INSTALL_LOCATION";
    public static final String LB_APP_SERVER_MACHINE_USER_NAME = "LB_APP_SERVER_MACHINE_USER_NAME";

    public static final String LB_PROVIDER = "LB_PROVIDER";
    public static final String GLASSFISH_LB = "GLASSFISH_LB";

    public static final String LB_IMAGE_ID = "LB_AWS_IMAGE_ID";
    public static final String LB_INSTALL_LOCATION = "LB_INSTALL_LOCATION";
    public static final String LB_INSTANCE_USER_NAME = "LB_INSTANCE_USER_NAME";
    public static final String LB_SCRIPTS_LOCATION = "LB_SCRIPTS_LOCATION";
    public static final String LB_LOCAL_KEYPAIR_LOCATION = "LB_LOCAL_KEYPAIR_LOCATION";


    public static final String LB_APACHE_INSTALL_LOCATION = "LB_APACHE_INSTALL_LOCATION";
    public static final String LB_APACHE_PORT_NUMBER = "LB_APACHE_PORT_NUMBER";

    private String lbInstallDir;
    private String lbImageId;
    private String userName;
    private String lbLocalKeyPairLocation;

    private String lbScriptsLocation;

    private String apacheInstallLocation;
    private String apachePortNumber;

    private String appServerLocalKeyPairLocation;
    private String appServerKeyPairLocation;
    private String appServerInstallDir;
    private String appServerMachineUserName;


    public boolean handles(Properties metaData) {
        String value = (String) metaData.get(LB_PROVIDER);
        if (value != null && value.equals(GLASSFISH_LB)) {
            return true;
        }
        return false;
    }

    public void initialize(Properties properties) {
        userName = (String) properties.get(LB_INSTANCE_USER_NAME);
        lbInstallDir = (String) properties.get(LB_INSTALL_LOCATION);
        lbLocalKeyPairLocation = (String) properties.get(LB_LOCAL_KEYPAIR_LOCATION);
        lbScriptsLocation = (String) properties.get(LB_SCRIPTS_LOCATION);
        apacheInstallLocation = (String) properties.get(LB_APACHE_INSTALL_LOCATION);
        appServerLocalKeyPairLocation = (String) properties.get(LB_APP_SERVER_LOCAL_KEYPAIR_LOCATION);
        appServerKeyPairLocation = (String) properties.get(LB_APP_SERVER_KEYPAIR_LOCATION);
        appServerInstallDir = (String) properties.get(LB_APP_SERVER_INSTALL_LOCATION);
        appServerMachineUserName = (String) properties.get(LB_APP_SERVER_MACHINE_USER_NAME);
        apachePortNumber = (String) properties.get(LB_APACHE_PORT_NUMBER);
        lbImageId = (String) properties.get(LB_IMAGE_ID);
    }

    public void startLB(String ipAddress) {
        String command = apacheInstallLocation + File.separator + "bin" + File.separator + "apachectl start";
        String args[] = new String[]{userName, ipAddress, lbLocalKeyPairLocation, command};
        System.out.println("startLB : " + Arrays.toString(args));
        remoteCommandExecutor.executeCommand(args);
    }

    public void configureLB(String ipAddress) {
        configureApache(ipAddress);
    }

    public void associateApplicationServerWithLB(String ipAddress, String dasIPAddress, String domainName) {
        generateAndTransferKey(dasIPAddress, ipAddress, domainName, appServerLocalKeyPairLocation,
                appServerMachineUserName, appServerKeyPairLocation);
        importDASCert(ipAddress);
    }

    private void importDASCert(String ipAddress) {
        String command = lbScriptsLocation + File.separator + "importDASCert.sh " +
                lbScriptsLocation + File.separator + "glassfish.crt";

        String args[] = new String[]{userName, ipAddress, lbLocalKeyPairLocation, command};
        System.out.println("importDASCert : " + Arrays.toString(args));
        remoteCommandExecutor.executeCommand(args);
    }

    private void generateAndTransferKey(String dasIPAddress, String lbIPAddress, String dasName, String localKeyPair,
                                        String appServerMachineUserName, String appServerKeyPair) {
        String certName = generateKey(dasIPAddress, dasName, localKeyPair, appServerMachineUserName);
        transferKeyFromAppServerToLB(dasIPAddress, lbIPAddress, appServerMachineUserName, appServerKeyPair, certName);

    }

    private void transferKeyFromAppServerToLB(String asIPAddress, String lbIPAddress, String appServerMachineUserName,
                                              String appServerKeyPair, String certFileName) {

        String certLocation = System.getProperty("java.io.tmpdir") + File.separator + certFileName;

        //clean-up before downloading the file from DAS.
        cleanupTmpFile(certLocation);
        {
            String args[] = new String[]{appServerMachineUserName + "@" + asIPAddress + ":" + certFileName, certLocation,
                    appServerLocalKeyPairLocation};
            System.out.println("File download from DAS to CAS: " + Arrays.toString(args));
            //FileDownload.main(args);
            fileTransferUtil.download(appServerMachineUserName, asIPAddress, appServerLocalKeyPairLocation,
                    certFileName, certLocation);

        }
        {
            String args[] = new String[]{certLocation,
                    userName + "@" + lbIPAddress + ":" + lbScriptsLocation + File.separator + certFileName,
                    lbLocalKeyPairLocation};
            System.out.println("File upload to LB from CAS: " + Arrays.toString(args));
            //FileUpload.main(args);
            fileTransferUtil.upload(userName, lbIPAddress, lbLocalKeyPairLocation,
                    lbScriptsLocation + File.separator + certFileName,
                    certLocation);
        }
        //clean-up after uploading the file to lb-machine.
        cleanupTmpFile(certLocation);
    }

    private void cleanupTmpFile(String fileLocation) {
        try {
            File file = new File(fileLocation);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            //ignore
        }
    }

    private String generateKey(String dasIPAddress, String dasName, String keyPairLocation,
                               String appServerMachineUserName) {
        String keyStore = appServerInstallDir + File.separator + "glassfish" + File.separator + "domains" + File.separator
                + dasName + File.separator + "config" + File.separator + "keystore.jks";
        String password = "changeit";
        String certFileName = "glassfish.crt";
        String aliasName = "s1as";

        String command = "keytool -export -rfc -keystore " + keyStore + " -alias " + aliasName +
                " -file " + certFileName + " -storepass " + password;

        String args[] = new String[]{appServerMachineUserName, dasIPAddress, keyPairLocation, command};
        remoteCommandExecutor.executeCommand(args);
        return certFileName;
    }

    private void createApacheCert(String ipAddress) {
        String command = lbScriptsLocation + File.separator + "createApacheCert.sh";
        String args[] = new String[]{userName, ipAddress, lbLocalKeyPairLocation, command};
        System.out.println("createApacheCert : " + Arrays.toString(args));
        remoteCommandExecutor.executeCommand(args);
    }

    private void configureApache(String ipAddress) {
        String command = lbScriptsLocation + File.separator + "configureApache.sh";
        String args[] = new String[]{userName, ipAddress, lbLocalKeyPairLocation, command};
        System.out.println("configureApache : " + Arrays.toString(args));
        remoteCommandExecutor.executeCommand(args);

        createApacheCert(ipAddress);
    }

    public void stopLB(String ipAddress) {
        String command = apacheInstallLocation + File.separator + "bin" + File.separator + "apachectl stop";
        String args[] = new String[]{userName, ipAddress, lbLocalKeyPairLocation, command};
        System.out.println("stopLB : " + Arrays.toString(args));
        remoteCommandExecutor.executeCommand(args);
    }

    public String getDefaultServiceName() {
        return "default-glassfish-lb-lbs";
    }

    public String getVendorName() {
        return GLASSFISH_LB;
    }

    public Properties getDefaultConnectionProperties() {
        Properties properties = new Properties();
        return properties;
    }
}
