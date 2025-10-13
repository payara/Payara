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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018-2025] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.DomainsManager;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryManager;
import com.sun.enterprise.admin.servermgmt.domain.DomainBuilder;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.net.NetUtils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.common.FileRealmStorageManager;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.admin.servermgmt.DomainConfig.KEYTOOLOPTIONS;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_ADMIN_CERT_DN;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_ADMIN_PORT;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_INITIAL_ADMIN_USER_GROUPS;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_INSTANCE_CERT_DN;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_MASTER_PASSWORD_LOCATION;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_PORTBASE;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_SECURE_ADMIN_IDENTIFIER;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_TEMPLATE_NAME;
import static com.sun.enterprise.admin.servermgmt.DomainConfig.K_VALIDATE_PORTS;
import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.MASTERPASSWORD_FILE;
import static com.sun.enterprise.config.util.PortConstants.DEFAULT_HAZELCAST_DAS_PORT;
import static com.sun.enterprise.config.util.PortConstants.DEFAULT_HAZELCAST_START_PORT;
import static com.sun.enterprise.config.util.PortConstants.DEFAULT_INSTANCE_PORT;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_ADMINPORT_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_DEBUG_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_HAZELCAST_DAS_PORT_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_HAZELCAST_START_PORT_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_HTTPSSL_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_IIOPMUTUALAUTH_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_IIOPSSL_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_IIOP_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_INSTANCE_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_JMS_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_JMX_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORTBASE_OSGI_SUFFIX;
import static com.sun.enterprise.config.util.PortConstants.PORT_MAX_VAL;
import static com.sun.enterprise.util.SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD;
import static com.sun.enterprise.util.SystemPropertyConstants.DEFAULT_ADMIN_USER;
import static com.sun.enterprise.util.net.NetUtils.checkPort;
import static com.sun.enterprise.util.net.NetUtils.isPortValid;
import static java.util.logging.Level.FINER;

/**
 * This is a local command that creates a domain.
 */
@Service(name = "create-domain")
@PerLookup
public final class CreateDomainCommand extends CLICommand {

    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(CreateDomainCommand.class);

    // Constants for create-domain options
    private static final String ADMIN_PORT = "adminport";
    private static final String ADMIN_PASSWORD = "password";
    private static final String MASTER_PASSWORD = "masterpassword";
    private static final String DEFAULT_MASTER_PASSWORD = RepositoryManager.DEFAULT_MASTER_PASSWORD;
    private static final String SAVE_MASTER_PASSWORD = "savemasterpassword";
    private static final String INSTANCE_PORT = "instanceport";
    private static final String DOMAIN_PROPERTIES = "domainproperties";
    private static final String PORTBASE_OPTION = "portbase";

    private static final String HAZELCAST_DAS_PORT = "hazelcastdasport";
    private static final String HAZELCAST_START_PORT = "hazelcaststartport";
    private static final String HAZELCAST_AUTO_INCREMENT ="hazelcastautoincrement";

    @Param(name = ADMIN_PORT, optional = true)
    private String adminPort;

    @Param(name = PORTBASE_OPTION, optional = true)
    private String portBase;

    @Param(obsolete = true, name = "profile", optional = true)
    private String profile;

    @Param(name = "template", optional = true)
    private String template;

    @Param(name = "domaindir", optional = true)
    private String domainDir;

    @Param(name = INSTANCE_PORT, optional = true)
    private String instancePort;

    @Param(name = SAVE_MASTER_PASSWORD, optional = true, defaultValue = "false")
    private boolean saveMasterPassword = false;

    @Param(name = "masterpasswordlocation", optional = true)
    private String masterPasswordLocation;

    @Param(name = "usemasterpassword", optional = true, defaultValue = "false")
    private boolean useMasterPassword = false;

    @Param(name = DOMAIN_PROPERTIES, optional = true, separator = ':')
    private Properties domainProperties;

    @Param(name = "keytooloptions", optional = true)
    private String keytoolOptions;

    @Param(name = "savelogin", optional = true, defaultValue = "false")
    private boolean saveLoginOpt = false;

    @Param(name = "nopassword", optional = true, defaultValue = "false")
    private boolean noPassword = false;

    @Param(name = ADMIN_PASSWORD, optional = true, password = true)
    private String adminPassword = null;
    
    @Param(name = MASTER_PASSWORD, optional = true, password = true)
    private String masterPassword = null;

    @Param(name = "checkports", optional = true, defaultValue = "true")
    private boolean checkPorts = true;

    @Param(name = HAZELCAST_DAS_PORT, optional = true)
    private String hazelcastDasPort;

    @Param(name = HAZELCAST_START_PORT, optional = true)
    private String hazelcastStartPort;

    @Param(name = HAZELCAST_AUTO_INCREMENT, optional = true)
    private String hazelcastAutoIncrement;

    @Param(name = "domain_name", primary = true)
    private String domainName;

    private String adminUser;

    /**
     * Add options with port with proper default values. (Can't set default values above because it
     * conflicts with --portbase option processing.)
     */
    @Override
    protected Collection<ParamModel> usageOptions() {
        Collection<ParamModel> opts = commandModel.getParameters();
        Set<ParamModel> uopts = new LinkedHashSet<>();
        ParamModel adminPort = new ParamModelData(ADMIN_PORT, String.class, true, Integer.toString(CLIConstants.DEFAULT_ADMIN_PORT));
        ParamModel instancePort = new ParamModelData(INSTANCE_PORT, String.class, true, Integer.toString(DEFAULT_INSTANCE_PORT));
        ParamModel hazelcastDasPort = new ParamModelData(HAZELCAST_DAS_PORT, String.class, true, Integer.toString(DEFAULT_HAZELCAST_DAS_PORT));
        ParamModel hazelcastStartPort = new ParamModelData(HAZELCAST_START_PORT, String.class, true, Integer.toString(DEFAULT_HAZELCAST_START_PORT));

        for (ParamModel paramModel : opts) {
            switch (paramModel.getName()) {
                case ADMIN_PORT:
                    uopts.add(adminPort);
                    break;
                case INSTANCE_PORT:
                    uopts.add(instancePort);
                    break;
                case HAZELCAST_DAS_PORT:
                    uopts.add(hazelcastDasPort);
                    break;
                case HAZELCAST_START_PORT:
                    uopts.add(hazelcastStartPort);
                default:
                    uopts.add(paramModel);
                    break;
            }
        }

        return uopts;
    }

    /**
     */
    @Override
    protected void validate() throws CommandException, CommandValidationException {
        if (domainDir == null) {
            domainDir = getSystemProperty(SystemPropertyConstants.DOMAINS_ROOT_PROPERTY);
        }

        if (domainDir == null) {
            throw new CommandValidationException(STRINGS.get("InvalidDomainPath", domainDir));
        }

        /*
         * The only required value is the domain_name operand, which might have been prompted for before we get here.
         *
         * If --user wasn't specified as a program option, we treat it as a required option and prompt for it if possible,
         * unless --nopassword was specified in which case we default the user name.
         *
         * The next prompted-for value will be the admin password, if required.
         */
        if (programOpts.getUser() == null && !noPassword) {
            // prompt for it (if interactive)

            try {
                buildTerminal();
                buildLineReader();
                if (lineReader != null && programOpts.isInteractive()) {
                    String val = lineReader.readLine(STRINGS.get("AdminUserRequiredPrompt", SystemPropertyConstants.DEFAULT_ADMIN_USER));

                    if (ok(val)) {
                        programOpts.setUser(val);
                        if (adminPassword == null) {
                            char[] pwdArr = getAdminPassword();
                            adminPassword = pwdArr != null ? new String(pwdArr) : null;
                        }
                    }
                } else {
                    throw new CommandValidationException(STRINGS.get("AdminUserRequired"));
                }
            } catch (UserInterruptException | EndOfFileException e) {
                // Ignore
            } finally {
                closeTerminal();
            }
        }

        if (programOpts.getUser() != null) {
            try {
                FileRealmStorageManager.validateUserName(programOpts.getUser());
            } catch (IllegalArgumentException ise) {
                throw new CommandValidationException(STRINGS.get("InvalidUserName", programOpts.getUser()));
            }
        }
    }

    public void verifyPortBase() throws CommandValidationException {
        if (usePortBase()) {
            setOptionsWithPortBase(convertPortStr(portBase));
        }
    }

    private void setOptionsWithPortBase(final int portbase) throws CommandValidationException {
        // set the option name and value in the options list
        verifyPortBasePortIsValid(ADMIN_PORT, portbase + PORTBASE_ADMINPORT_SUFFIX);
        adminPort = String.valueOf(portbase + PORTBASE_ADMINPORT_SUFFIX);

        verifyPortBasePortIsValid(INSTANCE_PORT, portbase + PORTBASE_INSTANCE_SUFFIX);
        instancePort = String.valueOf(portbase + PORTBASE_INSTANCE_SUFFIX);

        domainProperties = new Properties();
        verifyPortBasePortIsValid(DomainConfig.K_HTTP_SSL_PORT, portbase + PORTBASE_HTTPSSL_SUFFIX);
        domainProperties.put(DomainConfig.K_HTTP_SSL_PORT, String.valueOf(portbase + PORTBASE_HTTPSSL_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_IIOP_SSL_PORT, portbase + PORTBASE_IIOPSSL_SUFFIX);
        domainProperties.put(DomainConfig.K_IIOP_SSL_PORT, String.valueOf(portbase + PORTBASE_IIOPSSL_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_IIOP_MUTUALAUTH_PORT, portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX);
        domainProperties.put(DomainConfig.K_IIOP_MUTUALAUTH_PORT, String.valueOf(portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JMS_PORT, portbase + PORTBASE_JMS_SUFFIX);
        domainProperties.put(DomainConfig.K_JMS_PORT, String.valueOf(portbase + PORTBASE_JMS_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_ORB_LISTENER_PORT, portbase + PORTBASE_IIOP_SUFFIX);
        domainProperties.put(DomainConfig.K_ORB_LISTENER_PORT, String.valueOf(portbase + PORTBASE_IIOP_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JMX_PORT, portbase + PORTBASE_JMX_SUFFIX);
        domainProperties.put(DomainConfig.K_JMX_PORT, String.valueOf(portbase + PORTBASE_JMX_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_OSGI_SHELL_TELNET_PORT, portbase + PORTBASE_OSGI_SUFFIX);
        domainProperties.put(DomainConfig.K_OSGI_SHELL_TELNET_PORT, String.valueOf(portbase + PORTBASE_OSGI_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JAVA_DEBUGGER_PORT, portbase + PORTBASE_DEBUG_SUFFIX);
        domainProperties.put(DomainConfig.K_JAVA_DEBUGGER_PORT, String.valueOf(portbase + PORTBASE_DEBUG_SUFFIX));

        verifyPortBasePortIsValid(HAZELCAST_DAS_PORT, portbase + PORTBASE_HAZELCAST_DAS_PORT_SUFFIX);
        hazelcastDasPort = String.valueOf(portbase + PORTBASE_HAZELCAST_DAS_PORT_SUFFIX);

        verifyPortBasePortIsValid(HAZELCAST_START_PORT, portbase + PORTBASE_HAZELCAST_START_PORT_SUFFIX);
        hazelcastStartPort = String.valueOf(portbase + PORTBASE_HAZELCAST_START_PORT_SUFFIX);

    }

    @Override
    protected int executeCommand() throws CommandException, CommandValidationException {

        // Domain validation upfront (i.e. before we prompt)
        try {
            DomainsManager manager = new PEDomainsManager();
            DomainConfig config = new DomainConfig(domainName, domainDir);
            manager.validateDomain(config, false);
            verifyPortBase();
        } catch (DomainException e) {
            throw new CommandException(STRINGS.get("CouldNotCreateDomain", domainName), e);
        }

        /*
         * The admin user is specified with the --user program option. If not specified (because the user hit Enter at the
         * prompt), we use the default, which allows unauthenticated login.
         */
        adminUser = programOpts.getUser();

        if (!ok(adminUser)) {
            adminUser = DEFAULT_ADMIN_USER;
            adminPassword = DEFAULT_ADMIN_PASSWORD;
        } else if (noPassword) {
            adminPassword = DEFAULT_ADMIN_PASSWORD;
        } else {
            char[] pwdArr = getAdminPassword();
            adminPassword = pwdArr != null ? new String(pwdArr) : null;
        }

        if (saveMasterPassword) {
            useMasterPassword = true;
        }

        if (masterPassword == null) {
            if (useMasterPassword) {
                char[] mpArr = getMasterPassword();
                masterPassword = mpArr != null ? new String(mpArr) : null;
            } else {
                masterPassword = DEFAULT_MASTER_PASSWORD;
            }
        }

        try {
            // Verify admin port is valid if specified on command line
            if (adminPort != null) {
                verifyPortIsValid(adminPort);
            }

            if (hazelcastDasPort != null) {
                verifyPortIsValid(hazelcastDasPort);
            }

            if (hazelcastStartPort != null) {
                verifyPortIsValid(hazelcastStartPort);
            }

            // Instance option is entered then verify instance port is valid
            if (instancePort != null) {
                verifyPortIsValid(instancePort);
            }

            // Saving the login information happens inside this method
            createTheDomain(domainDir, domainProperties);
        } catch (Exception e) {
            throw new CommandException(STRINGS.get("CouldNotCreateDomain", domainName), e);
        }

        return 0;
    }

    /**
     * Get the admin password as a required option.
     */
    private char[] getAdminPassword() throws CommandValidationException {
        // Create a required ParamModel for the password
        ParamModelData paramModelData = new ParamModelData(ADMIN_PASSWORD, String.class, false, null);
        paramModelData.prompt = STRINGS.get("AdminPassword");
        paramModelData.promptAgain = STRINGS.get("AdminPasswordAgain");
        paramModelData.param._password = true;

        return getPassword(paramModelData, DEFAULT_ADMIN_PASSWORD, true);
    }

    /**
     * Get the master password as a required option (by default it is not required)
     */
    private char[] getMasterPassword() throws CommandValidationException {
        // Create a required ParamModel for the password
        ParamModelData paramModelData = new ParamModelData(MASTER_PASSWORD, String.class, false /* optional */, null);
        paramModelData.prompt = STRINGS.get("MasterPassword");
        paramModelData.promptAgain = STRINGS.get("MasterPasswordAgain");
        paramModelData.param._password = true;

        return getPassword(paramModelData, DEFAULT_MASTER_PASSWORD, true);
    }

    /**
     * Verify that the port is valid. Port must be greater than 0 and less than 65535. This method will also check if the
     * port is in use. If checkPorts is false it does not throw an Exception if it is in use.
     *
     * @param portNum - the port number to verify
     * @throws CommandException if Port is not valid
     * @throws CommandValidationException is port number is not a numeric value.
     */
    private void verifyPortIsValid(String portNum) throws CommandException, CommandValidationException {

        int portToVerify = convertPortStr(portNum);

        if (!isPortValid(portToVerify)) {
            throw new CommandException(STRINGS.get("InvalidPortRange", portNum));
        }

        if (!checkPorts) {
            // Do NOT make any network calls!
            logger.log(FINER, "Port ={0}", portToVerify);
            return;
        }

        switch (checkPort(portToVerify)) {
            case illegalNumber:
                throw new CommandException(STRINGS.get("InvalidPortRange", portNum));

            case inUse:
                throw new CommandException(STRINGS.get("PortInUseError", domainName, portNum));

            case noPermission:
                throw new CommandException(STRINGS.get("NoPermissionForPortError", portNum, domainName));

            case unknown:
                throw new CommandException(STRINGS.get("UnknownPortMsg", portNum));

            case OK:
                logger.log(FINER, "Port ={0}", portToVerify);
                break;
            default:
                break;
        }
    }

    /**
     * Converts the port string to port int
     *
     * @param port the port number
     * @return the port number as an int
     * @throws CommandValidationException if port string is not numeric
     */
    private int convertPortStr(String port) throws CommandValidationException {
        try {
            return Integer.parseInt(port);
        } catch (Exception e) {
            throw new CommandValidationException(STRINGS.get("InvalidPortNumber", port));
        }
    }

    /**
     * Verify that the portbase port is valid Port must be greater than 0 and less than 65535. This method will also check
     * if the port is in used.
     *
     * @param portNum the port number to verify
     * @throws CommandException if Port is not valid
     * @throws CommandValidationException is port number is not a numeric value.
     */
    private void verifyPortBasePortIsValid(String portName, int portNum) throws CommandValidationException {
        if (portNum <= 0 || portNum > PORT_MAX_VAL) {
            throw new CommandValidationException(STRINGS.get("InvalidPortBaseRange", portNum, portName));
        }

        if (checkPorts && !NetUtils.isPortFree(portNum)) {
            throw new CommandValidationException(STRINGS.get("PortBasePortInUse", portNum, portName));
        }

        logger.log(FINER, "Port ={0}", portNum);
    }

    /**
     * Create the domain.
     *
     * @param domainPath domain path to insert in domainConfig
     * @param domainProperties properties to insert in domainConfig
     * @throws CommandException if domain cannot be created
     */
    private void createTheDomain(final String domainPath, Properties domainProperties) throws DomainException, CommandValidationException {

        // fix for bug# 4930684
        // domain name is validated before the ports

        String domainFilePath = (domainPath + File.separator + domainName);
        if (FileUtils.safeGetCanonicalFile(new File(domainFilePath)).exists()) {
            throw new CommandValidationException(STRINGS.get("DomainExists", domainName));
        }

        DomainConfig domainConfig = null;
        if (template == null || template.endsWith(".jar")) {
            domainConfig = new DomainConfig(domainName, domainPath, adminUser, adminPassword, masterPassword, saveMasterPassword, adminPort,
                    instancePort, hazelcastDasPort, hazelcastStartPort, hazelcastAutoIncrement, domainProperties);
            domainConfig.put(K_VALIDATE_PORTS, checkPorts);
            domainConfig.put(KEYTOOLOPTIONS, keytoolOptions);
            domainConfig.put(K_TEMPLATE_NAME, template);
            domainConfig.put(K_PORTBASE, portBase);
            domainConfig.put(K_INITIAL_ADMIN_USER_GROUPS, Version.getInitialAdminGroups());
            if (masterPasswordLocation != null) {
                File potentialFolder = new File(masterPasswordLocation);
                if (potentialFolder.isDirectory()) {
                    masterPasswordLocation = new File(potentialFolder, MASTERPASSWORD_FILE).getAbsolutePath();
                }

                domainConfig.put(K_MASTER_PASSWORD_LOCATION, masterPasswordLocation);
            }
            initSecureAdminSettings(domainConfig);

            try {
                DomainBuilder domainBuilder = new DomainBuilder(domainConfig);
                domainBuilder.validateTemplate();
                domainBuilder.run();
            } catch (Exception e) {
                throw new DomainException(e.getMessage(), e);
            }
        } else {
            throw new DomainException(STRINGS.get("InvalidTemplateValue", template));
        }

        logger.info(STRINGS.get("DomainCreated", domainName));
        Integer aPort = (Integer) domainConfig.get(K_ADMIN_PORT);
        logger.info(STRINGS.get("DomainPort", domainName, Integer.toString(aPort)));

        if (adminPassword != null && adminPassword.equals(DEFAULT_ADMIN_PASSWORD)) {
            logger.info(STRINGS.get("DomainAllowsUnauth", domainName, adminUser));
        } else {
            logger.info(STRINGS.get("DomainAdminUser", domainName, adminUser));
        }

        if (saveLoginOpt) {
            saveLogin(aPort, adminUser, adminPassword != null ? adminPassword.toCharArray() : null, domainName);
        }
    }

    /**
     * Saves the login information to the login store. Usually this is the file ".asadminpass" in user's home directory.
     */
    private void saveLogin(final int port, final String user, final char[] password, final String dn) {
        try {
            // by definition, the host name will default to "localhost"
            // and entry is overwritten
            final LoginInfoStore store = LoginInfoStoreFactory.getStore(null);
            final LoginInfo login = new LoginInfo("localhost", port, user, password);
            if (store.exists(login.getHost(), login.getPort())) {
                // just let the user know that the user has chosen to overwrite
                // the login information. This is non-interactive, on purpose
                logger.info(STRINGS.get("OverwriteLoginMsgCreateDomain", login.getHost(), "" + login.getPort()));
            }
            store.store(login, true);
            logger.info(STRINGS.get("LoginInfoStoredCreateDomain", user, dn, store.getName()));
        } catch (final Throwable e) {
            logger.warning(STRINGS.get("LoginInfoNotStoredCreateDomain", user, dn));
            printExceptionStackTrace(e);
        }
    }

    /**
     * Check if portbase option is specified. Portbase is mutually exclusive to adminport and domainproperties options. If
     * portbase options is specfied and also adminport or domainproperties is specified as well, then throw an exception.
     */
    private boolean usePortBase() throws CommandValidationException {
        if (portBase == null) {
            return false;
        }

        if (adminPort != null) {
            throw new CommandValidationException(STRINGS.get("MutuallyExclusiveOption", ADMIN_PORT, PORTBASE_OPTION));
        }

        if (instancePort != null) {
            throw new CommandValidationException(STRINGS.get("MutuallyExclusiveOption", INSTANCE_PORT, PORTBASE_OPTION));
        }

        if (domainProperties != null) {
            throw new CommandValidationException(STRINGS.get("MutuallyExclusiveOption", DOMAIN_PROPERTIES, PORTBASE_OPTION));
        }

        return true;
    }

    private void initSecureAdminSettings(final DomainConfig config) {
        config.put(K_ADMIN_CERT_DN, KeystoreManager.getDASCertDN(config));
        config.put(K_INSTANCE_CERT_DN, KeystoreManager.getInstanceCertDN(config));
        config.put(K_SECURE_ADMIN_IDENTIFIER, secureAdminIdentifier());
    }

    private String secureAdminIdentifier() {
        return UUID.randomUUID().toString();
    }
}
