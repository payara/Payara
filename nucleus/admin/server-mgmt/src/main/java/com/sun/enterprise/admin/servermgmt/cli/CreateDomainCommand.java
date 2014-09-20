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
package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStore;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;
import com.sun.appserv.server.util.Version;
import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.servermgmt.*;
import com.sun.enterprise.admin.servermgmt.domain.DomainBuilder;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import static com.sun.enterprise.config.util.PortConstants.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.net.NetUtils;
import java.io.Console;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.common.FileRealmHelper;
import org.jvnet.hk2.annotations.Service;

/**
 * This is a local command that creates a domain.
 */
@Service(name = "create-domain")
@PerLookup
public final class CreateDomainCommand extends CLICommand {
    // constants for create-domain options
    private static final String ADMIN_PORT = "adminport";
    private static final String ADMIN_PASSWORD = "password";
    private static final String MASTER_PASSWORD = "masterpassword";
    private static final String DEFAULT_MASTER_PASSWORD =
            RepositoryManager.DEFAULT_MASTER_PASSWORD;
    private static final String SAVE_MASTER_PASSWORD = "savemasterpassword";
    private static final String INSTANCE_PORT = "instanceport";
    private static final String DOMAIN_PROPERTIES = "domainproperties";
    private static final String PORTBASE_OPTION = "portbase";
    private String adminUser = null;
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
    @Param(name = "domain_name", primary = true)
    private String domainName;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(CreateDomainCommand.class);

    public CreateDomainCommand() {
    }

    /**
     * Add --adminport and --instanceport options with proper default values.
     * (Can't set default values above because it conflicts with --portbase
     * option processing.)
     */
    @Override
    protected Collection<ParamModel> usageOptions() {
        Collection<ParamModel> opts = commandModel.getParameters();
        Set<ParamModel> uopts = new LinkedHashSet<ParamModel>();
        ParamModel aPort = new ParamModelData(ADMIN_PORT, String.class, true,
                Integer.toString(CLIConstants.DEFAULT_ADMIN_PORT));
        ParamModel iPort = new ParamModelData(INSTANCE_PORT, String.class, true,
                Integer.toString(DEFAULT_INSTANCE_PORT));
        for (ParamModel pm : opts) {
            if (pm.getName().equals(ADMIN_PORT)) {
                uopts.add(aPort);
            }
            else if (pm.getName().equals(INSTANCE_PORT)) {
                uopts.add(iPort);
            }
            else {
                uopts.add(pm);
            }
        }
        return uopts;
    }

    /**
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        if (domainDir == null) {
            domainDir = getSystemProperty(
                    SystemPropertyConstants.DOMAINS_ROOT_PROPERTY);
        }
        if (domainDir == null) {
            throw new CommandValidationException(
                    strings.get("InvalidDomainPath", domainDir));
        }

        /*
         * The only required value is the domain_name operand, which might have
         * been prompted for before we get here.
         *
         * If --user wasn't specified as a program option, we treat it as a
         * required option and prompt for it if possible, unless --nopassword
         * was specified in which case we default the user name.
         *
         * The next prompted-for value will be the admin password, if required.
         */
        if (programOpts.getUser() == null && !noPassword) {
            // prompt for it (if interactive)
            Console cons = System.console();
            if (cons != null && programOpts.isInteractive()) {
                cons.printf("%s", strings.get("AdminUserRequiredPrompt",
                        SystemPropertyConstants.DEFAULT_ADMIN_USER));
                String val = cons.readLine();
                if (ok(val)) {
                    programOpts.setUser(val);
                    if (adminPassword == null) {
                        adminPassword = getAdminPassword();
                    }
                }
            }
            else {
                //logger.info(strings.get("AdminUserRequired"));
                throw new CommandValidationException(
                        strings.get("AdminUserRequired"));
            }
        }
        if (programOpts.getUser() != null) {
            try {
                FileRealmHelper.validateUserName(programOpts.getUser());
            }
            catch (IllegalArgumentException ise) {
                throw new CommandValidationException(
                        strings.get("InvalidUserName", programOpts.getUser()));
            }
        }
    }

    public void verifyPortBase() throws CommandValidationException {
        if (usePortBase()) {
            final int portbase = convertPortStr(portBase);
            setOptionsWithPortBase(portbase);
        }
    }

    private void setOptionsWithPortBase(final int portbase)
            throws CommandValidationException {
        // set the option name and value in the options list
        verifyPortBasePortIsValid(ADMIN_PORT,
                portbase + PORTBASE_ADMINPORT_SUFFIX);
        adminPort = String.valueOf(portbase + PORTBASE_ADMINPORT_SUFFIX);

        verifyPortBasePortIsValid(INSTANCE_PORT,
                portbase + PORTBASE_INSTANCE_SUFFIX);
        instancePort = String.valueOf(portbase + PORTBASE_INSTANCE_SUFFIX);

        domainProperties = new Properties();
        verifyPortBasePortIsValid(DomainConfig.K_HTTP_SSL_PORT,
                portbase + PORTBASE_HTTPSSL_SUFFIX);
        domainProperties.put(DomainConfig.K_HTTP_SSL_PORT,
                String.valueOf(portbase + PORTBASE_HTTPSSL_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_IIOP_SSL_PORT,
                portbase + PORTBASE_IIOPSSL_SUFFIX);
        domainProperties.put(DomainConfig.K_IIOP_SSL_PORT,
                String.valueOf(portbase + PORTBASE_IIOPSSL_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_IIOP_MUTUALAUTH_PORT,
                portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX);
        domainProperties.put(DomainConfig.K_IIOP_MUTUALAUTH_PORT,
                String.valueOf(portbase + PORTBASE_IIOPMUTUALAUTH_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JMS_PORT,
                portbase + PORTBASE_JMS_SUFFIX);
        domainProperties.put(DomainConfig.K_JMS_PORT,
                String.valueOf(portbase + PORTBASE_JMS_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_ORB_LISTENER_PORT,
                portbase + PORTBASE_IIOP_SUFFIX);
        domainProperties.put(DomainConfig.K_ORB_LISTENER_PORT,
                String.valueOf(portbase + PORTBASE_IIOP_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JMX_PORT,
                portbase + PORTBASE_JMX_SUFFIX);
        domainProperties.put(DomainConfig.K_JMX_PORT,
                String.valueOf(portbase + PORTBASE_JMX_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_OSGI_SHELL_TELNET_PORT,
                portbase + PORTBASE_OSGI_SUFFIX);
        domainProperties.put(DomainConfig.K_OSGI_SHELL_TELNET_PORT,
                String.valueOf(portbase + PORTBASE_OSGI_SUFFIX));

        verifyPortBasePortIsValid(DomainConfig.K_JAVA_DEBUGGER_PORT,
                portbase + PORTBASE_DEBUG_SUFFIX);
        domainProperties.put(DomainConfig.K_JAVA_DEBUGGER_PORT,
                String.valueOf(portbase + PORTBASE_DEBUG_SUFFIX));

    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {

        // domain validation upfront (i.e. before we prompt)
        try {
            DomainsManager manager = new PEDomainsManager();
            DomainConfig config =
                    new DomainConfig(domainName, domainDir);
            manager.validateDomain(config, false);
            verifyPortBase();
        }
        catch (DomainException e) {
            logger.fine(e.getLocalizedMessage());
            throw new CommandException(
                    strings.get("CouldNotCreateDomain", domainName), e);
        }

        /*
         * The admin user is specified with the --user program option. If not
         * specified (because the user hit Enter at the prompt), we use the
         * default, which allows unauthenticated login.
         */
        adminUser = programOpts.getUser();
        if (!ok(adminUser)) {
            adminUser = SystemPropertyConstants.DEFAULT_ADMIN_USER;
            adminPassword = SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD;
        }
        else if (noPassword) {
            adminPassword = SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD;
        }
        else {
            adminPassword = getAdminPassword();
            boolean haveAdminPwd = true;
        }

        if (saveMasterPassword) {
            useMasterPassword = true;
        }

        if (masterPassword == null) {
            if (useMasterPassword) {
                masterPassword = getMasterPassword();
            } else {
                masterPassword = DEFAULT_MASTER_PASSWORD;
            }
        }

        try {
            // verify admin port is valid if specified on command line
            if (adminPort != null) {
                verifyPortIsValid(adminPort);
            }
            // instance option is entered then verify instance port is valid
            if (instancePort != null) {
                verifyPortIsValid(instancePort);
            }

            // saving the login information happens inside this method
            createTheDomain(domainDir, domainProperties);
        }
        catch (CommandException ce) {
            logger.info(ce.getLocalizedMessage());
            throw new CommandException(
                    strings.get("CouldNotCreateDomain", domainName), ce);
        }
        catch (Exception e) {
            logger.fine(e.getLocalizedMessage());
            throw new CommandException(
                    strings.get("CouldNotCreateDomain", domainName), e);
        }
        return 0;
    }

    /**
     * Get the admin password as a required option.
     */
    private String getAdminPassword() throws CommandValidationException {
        // create a required ParamModel for the password
        ParamModelData po = new ParamModelData(ADMIN_PASSWORD, String.class, false, null);
        po.prompt = strings.get("AdminPassword");
        po.promptAgain = strings.get("AdminPasswordAgain");
        po.param._password = true;
        return getPassword(po, SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD, true);
    }
    
    /**
     * Get the master password as a required option (by default it is not required)
     */
    private String getMasterPassword() throws CommandValidationException {
        // create a required ParamModel for the password
        ParamModelData po = new ParamModelData(MASTER_PASSWORD, String.class, 
                false /* optional */, null);
        po.prompt = strings.get("MasterPassword");
        po.promptAgain = strings.get("MasterPasswordAgain");
        po.param._password = true;
        return getPassword(po, DEFAULT_MASTER_PASSWORD, true);
    }

    /**
     * Verify that the port is valid. Port must be greater than 0 and less than
     * 65535. This method will also check if the port is in use. If checkPorts
     * is false it does not throw an Exception if it is in use.
     *
     * @param portNum - the port number to verify
     * @throws CommandException if Port is not valid
     * @throws CommandValidationException is port number is not a numeric value.
     */
    private void verifyPortIsValid(String portNum)
            throws CommandException, CommandValidationException {

        final int portToVerify = convertPortStr(portNum);

        if (!NetUtils.isPortValid(portToVerify)) {
            throw new CommandException(strings.get("InvalidPortRange", portNum));
        }

        if (checkPorts == false) {
            // do NOT make any network calls!
            logger.log(Level.FINER, "Port ={0}", portToVerify);
            return;
        }

        NetUtils.PortAvailability avail = NetUtils.checkPort(portToVerify);

        switch (avail) {
            case illegalNumber:
                throw new CommandException(
                        strings.get("InvalidPortRange", portNum));

            case inUse:
                throw new CommandException(
                        strings.get("PortInUseError", domainName, portNum));

            case noPermission:
                throw new CommandException(
                        strings.get("NoPermissionForPortError",
                        portNum, domainName));

            case unknown:
                throw new CommandException(strings.get("UnknownPortMsg", portNum));

            case OK:
                logger.log(Level.FINER, "Port ={0}", portToVerify);
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
    private int convertPortStr(final String port)
            throws CommandValidationException {
        try {
            return Integer.parseInt(port);
        }
        catch (Exception e) {
            throw new CommandValidationException(
                    strings.get("InvalidPortNumber", port));
        }
    }

    /**
     * Verify that the portbase port is valid Port must be greater than 0 and
     * less than 65535. This method will also check if the port is in used.
     *
     * @param portNum the port number to verify
     * @throws CommandException if Port is not valid
     * @throws CommandValidationException is port number is not a numeric value.
     */
    private void verifyPortBasePortIsValid(String portName, int portNum)
            throws CommandValidationException {
        if (portNum <= 0 || portNum > PORT_MAX_VAL) {
            throw new CommandValidationException(
                    strings.get("InvalidPortBaseRange", portNum, portName));
        }
        if (checkPorts && !NetUtils.isPortFree(portNum)) {
            throw new CommandValidationException(
                    strings.get("PortBasePortInUse", portNum, portName));
        }
        logger.log(Level.FINER, "Port ={0}", portNum);
    }

    /**
     * Create the domain.
     *
     * @param domainPath domain path to insert in domainConfig
     * @param domainProperties properties to insert in domainConfig
     * @throws CommandException if domain cannot be created
     */
    private void createTheDomain(final String domainPath,
            Properties domainProperties)
            throws DomainException, CommandValidationException {

        //
        // fix for bug# 4930684
        // domain name is validated before the ports
        //
        String domainFilePath = (domainPath + File.separator + domainName);
        if (FileUtils.safeGetCanonicalFile(new File(domainFilePath)).exists()) {
            throw new CommandValidationException(strings.get("DomainExists", domainName));
        }
        DomainConfig domainConfig = null;
        if (template == null || template.endsWith(".jar")) {
            domainConfig = new DomainConfig(domainName,
                    domainPath, adminUser, adminPassword,
                    masterPassword, saveMasterPassword,
                    adminPort, instancePort,
                    domainProperties);
            domainConfig.put(DomainConfig.K_VALIDATE_PORTS,
                    Boolean.valueOf(checkPorts));
            domainConfig.put(DomainConfig.KEYTOOLOPTIONS, keytoolOptions);
            domainConfig.put(DomainConfig.K_TEMPLATE_NAME, template);
            domainConfig.put(DomainConfig.K_PORTBASE, portBase);
            domainConfig.put(DomainConfig.K_INITIAL_ADMIN_USER_GROUPS, Version.getInitialAdminGroups());
            initSecureAdminSettings(domainConfig);
            try {
                DomainBuilder domainBuilder = new DomainBuilder(domainConfig);
                domainBuilder.validateTemplate();
                domainBuilder.run();
            } catch (Exception e) {
                throw new DomainException(e.getMessage());
            }
        }
        else {
            throw new DomainException(strings.get("InvalidTemplateValue", template));
        }
        logger.info(strings.get("DomainCreated", domainName));
        Integer aPort = (Integer)domainConfig.get(DomainConfig.K_ADMIN_PORT);
        logger.info(strings.get("DomainPort", domainName, Integer.toString(aPort)));
        if (adminPassword.equals(
                SystemPropertyConstants.DEFAULT_ADMIN_PASSWORD)) {
            logger.info(strings.get("DomainAllowsUnauth", domainName,
                    adminUser));
        } else {
            logger.info(strings.get("DomainAdminUser", domainName, adminUser));
        }
        //checkAsadminPrefsFile();
        if (saveLoginOpt) {
            saveLogin(aPort, adminUser, adminPassword, domainName);
        }
    }

    /**
     * Saves the login information to the login store. Usually this is the file
     * ".asadminpass" in user's home directory.
     */
    private void saveLogin(final int port, final String user,
            final String password, final String dn) {
        try {
            // by definition, the host name will default to "localhost"
            // and entry is overwritten
            final LoginInfoStore store = LoginInfoStoreFactory.getStore(null);
            final LoginInfo login =
                    new LoginInfo("localhost", port, user, password);
            if (store.exists(login.getHost(), login.getPort())) {
                // just let the user know that the user has chosen to overwrite
                // the login information. This is non-interactive, on purpose
                logger.info(strings.get("OverwriteLoginMsgCreateDomain",
                        login.getHost(), "" + login.getPort()));
            }
            store.store(login, true);
            logger.info(strings.get("LoginInfoStoredCreateDomain",
                    user, dn, store.getName()));
        }
        catch (final Throwable e) {
            logger.warning(
                    strings.get("LoginInfoNotStoredCreateDomain", user, dn));
            printExceptionStackTrace(e);
        }
    }

    /**
     * Check if portbase option is specified. Portbase is mutually exclusive to
     * adminport and domainproperties options. If portbase options is specfied
     * and also adminport or domainproperties is specified as well, then throw
     * an exception.
     */
    private boolean usePortBase() throws CommandValidationException {
        if (portBase != null) {
            if (adminPort != null) {
                throw new CommandValidationException(
                        strings.get("MutuallyExclusiveOption",
                        ADMIN_PORT, PORTBASE_OPTION));
            }
            else if (instancePort != null) {
                throw new CommandValidationException(
                        strings.get("MutuallyExclusiveOption",
                        INSTANCE_PORT, PORTBASE_OPTION));
            }
            else if (domainProperties != null) {
                throw new CommandValidationException(
                        strings.get("MutuallyExclusiveOption",
                        DOMAIN_PROPERTIES, PORTBASE_OPTION));
            }
            else {
                return true;
            }
        }
        return false;
    }

    private void initSecureAdminSettings(final DomainConfig config) {
        config.put(DomainConfig.K_ADMIN_CERT_DN, KeystoreManager.getDASCertDN(config));
        config.put(DomainConfig.K_INSTANCE_CERT_DN, KeystoreManager.getInstanceCertDN(config));
        config.put(DomainConfig.K_SECURE_ADMIN_IDENTIFIER, secureAdminIdentifier());
    }

    private String secureAdminIdentifier() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
