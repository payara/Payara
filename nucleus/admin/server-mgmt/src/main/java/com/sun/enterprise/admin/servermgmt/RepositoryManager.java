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

/*
 * RepositoryManager.java
 *
 * Created on August 19, 2003, 2:29 PM
 */
package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.admin.util.LineTokenReplacer;
import com.sun.enterprise.util.*;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.admin.util.AdminConstants;
import com.sun.enterprise.security.store.PasswordAdapter;

//import com.sun.enterprise.admin.common.Status;
//import com.sun.enterprise.util.system.GFSystem;
import java.io.*;

import java.util.*;
import com.sun.enterprise.util.zip.ZipFile;


//import javax.management.remote.JMXAuthenticator;
//import com.sun.enterprise.admin.server.core.jmx.auth.ASJMXAuthenticator;
//import com.sun.enterprise.admin.server.core.jmx.auth.ASLoginDriverImpl;

//iMQ imports
//import com.sun.enterprise.jms.IASJmsUtil;
//import com.sun.messaging.jmq.jmsspi.JMSAdmin;
//import javax.jms.JMSException;

import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.security.common.FileRealmHelper;
//import com.sun.enterprise.util.ExecException;

/**
 * The RepositoryManager serves as a common base class for the following
 * PEDomainsManager, PEInstancesManager, AgentManager (the SE Node Agent). Its
 * purpose is to abstract out any shared functionality related to lifecycle
 * management of domains, instances and node agents. This includes creation,
 * deletion, listing, and starting and stopping.
 *
 * @author kebbs
 */
public class RepositoryManager extends MasterPasswordFileManager {
    /**
     * The RepositoryManagerMessages class is used to abstract out
     * ResourceBundle messages that are specific to a domain, node-agent, or
     * server instance.
     */
    protected static class RepositoryManagerMessages {
        private StringManager _strMgr;
        private String _badNameMessage;
        private String _repositoryNameMessage;
        private String _repositoryRootMessage;
        private String _existsMessage;
        private String _noExistsMessage;
        private String _repositoryNotValidMessage;
        private String _cannotDeleteMessage;
        private String _invalidPathMessage;
        private String _listRepositoryElementMessage;
        private String _cannotDeleteInstance_invalidState;
        private String _instanceStartupExceptionMessage;
        private String _cannotStartInstance_invalidStateMessage;
        private String _startInstanceTimeOutMessage;
        private String _portConflictMessage;
        private String _startupFailedMessage;
        private String _cannotStopInstance_invalidStateMessage;
        private String _cannotStopInstanceMessage;
        private String _timeoutStartingMessage;
        private String _cannotDeleteJmsProviderInstance;

        public RepositoryManagerMessages(
                StringManager strMgr,
                String badNameMessage,
                String repositoryNameMessage,
                String repositoryRootMessage,
                String existsMessage,
                String noExistsMessage,
                String repositoryNotValidMessage,
                String cannotDeleteMessage,
                String invalidPathMessage,
                String listRepositoryElementMessage,
                String cannotDeleteInstance_invalidState,
                String instanceStartupExceptionMessage,
                String cannotStartInstance_invalidStateMessage,
                String startInstanceTimeOutMessage,
                String portConflictMessage,
                String startupFailedMessage,
                String cannotStopInstance_invalidStateMessage,
                String cannotStopInstanceMessage,
                String timeoutStartingMessage) {
            _strMgr = strMgr;
            _badNameMessage = badNameMessage;
            _repositoryNameMessage = repositoryNameMessage;
            _repositoryRootMessage = repositoryRootMessage;
            _existsMessage = existsMessage;
            _noExistsMessage = noExistsMessage;
            _repositoryNotValidMessage = repositoryNotValidMessage;
            _cannotDeleteMessage = cannotDeleteMessage;
            _invalidPathMessage = invalidPathMessage;
            _listRepositoryElementMessage = listRepositoryElementMessage;
            _cannotDeleteInstance_invalidState = cannotDeleteInstance_invalidState;
            _instanceStartupExceptionMessage = instanceStartupExceptionMessage;
            _cannotStartInstance_invalidStateMessage =
                    cannotStartInstance_invalidStateMessage;
            _startInstanceTimeOutMessage = startInstanceTimeOutMessage;
            _portConflictMessage = portConflictMessage;
            _startupFailedMessage = startupFailedMessage;
            _cannotStopInstance_invalidStateMessage =
                    cannotStopInstance_invalidStateMessage;
            _cannotStopInstanceMessage = cannotStopInstanceMessage;
            _timeoutStartingMessage = timeoutStartingMessage;
        }

        public String getRepositoryNameMessage() {
            return _strMgr.getString(_repositoryNameMessage);
        }

        public String getBadNameMessage(String repositoryName) {
            return _strMgr.getString(_badNameMessage, repositoryName);
        }

        public String getRepositoryRootMessage() {
            return _strMgr.getString(_repositoryRootMessage);
        }

        public String getNoExistsMessage(String repositoryName, String repositoryLocation) {
            return _strMgr.getString(_noExistsMessage, repositoryName, repositoryLocation);
        }

        public String getExistsMessage(String repositoryName, String repositoryLocation) {
            return _strMgr.getString(_existsMessage, repositoryName, repositoryLocation);
        }

        public String getRepositoryNotValidMessage(String path) {
            return _strMgr.getString(_repositoryNotValidMessage, path);
        }

        public String getCannotDeleteMessage(String repositoryName) {
            return _strMgr.getString(_cannotDeleteMessage, repositoryName);
        }

        public String getInvalidPathMessage(String path) {
            return _strMgr.getString(_invalidPathMessage, path);
        }

        public String getListRepositoryElementMessage(String repositoryName,
                String repositoryStatus) {
            return _strMgr.getString(_listRepositoryElementMessage, repositoryName,
                    repositoryStatus);
        }

        public String getCannotDeleteInstanceInvalidState(String name,
                String state) {
            return _strMgr.getString(_cannotDeleteInstance_invalidState,
                    name, state);
        }

        public String getInstanceStartupExceptionMessage(String name) {
            return _strMgr.getString(_instanceStartupExceptionMessage, name);
        }

        public String getCannotStartInstanceInvalidStateMessage(String name,
                String state) {
            return _strMgr.getString(_cannotStartInstance_invalidStateMessage,
                    name, state);
        }

        public String getStartInstanceTimeOutMessage(String name) {
            return _strMgr.getString(_startInstanceTimeOutMessage, name);
        }

        public String getStartupFailedMessage(String name) {
            return _strMgr.getString(_startupFailedMessage, name);
        }

        public String getStartupFailedMessage(String name, int port) {
            if (port != 0) {
                return _strMgr.getString(_portConflictMessage, new Object[]{name, String.valueOf(port)});
            }
            else {
                return _strMgr.getString(_startupFailedMessage, name);
            }
        }

        public String getCannotStopInstanceInvalidStateMessage(String name,
                String state) {
            return _strMgr.getString(_cannotStopInstance_invalidStateMessage,
                    name, state);
        }

        public String getCannotStopInstanceMessage(String name) {
            return _strMgr.getString(_cannotStopInstanceMessage, name);
        }

        public String getTimeoutStartingMessage(String name) {
            return _strMgr.getString(_timeoutStartingMessage, name);
        }
    }
    protected static final String CERTUTIL_CMD = System.getProperty(SystemPropertyConstants.NSS_BIN_PROPERTY) + "/certutil";
    protected static final String NEW_LINE =
            System.getProperty("line.separator");
    private static final StringManager _strMgr =
            StringManager.getManager(RepositoryManager.class);
    protected RepositoryManagerMessages _messages = null;
    public static final String DEBUG = "Debug";

    /**
     * Creates a new instance of RepositoryManager
     */
    public RepositoryManager() {
        super();
        setMessages(new RepositoryManagerMessages(
                StringManager.getManager(PEDomainsManager.class),
                "illegalDomainName",
                "domainName", "domainsRoot", "domainExists",
                "domainDoesntExist", "domainDirNotValid",
                "cannotDeleteDomainDir", "invalidDomainDir",
                "listDomainElement", "cannotDeleteInstance_invalidState",
                "instanceStartupException", "cannotStartInstance_invalidState",
                "startInstanceTimeOut", "portConflict", "startupFailed",
                "cannotStopInstance_invalidState",
                "cannotStopInstance", "timeoutStarting"));
    }

    protected void setMessages(RepositoryManagerMessages messages) {
        _messages = messages;
    }

    protected RepositoryManagerMessages getMessages() {
        return _messages;
    }

    protected void generateFromTemplate(TokenValueSet tokens,
            File template, File destinationFile) throws IOException {
        LineTokenReplacer replacer = new LineTokenReplacer(tokens, "UTF-8");
        replacer.replace(template, destinationFile);
    }

    protected boolean repositoryExists(RepositoryConfig config) {
        return FileUtils.safeGetCanonicalFile(getRepositoryDir(config)).exists();
    }

    protected boolean isValidRepository(File f) {
        return new File(new File(f, PEFileLayout.CONFIG_DIR),
                PEFileLayout.DOMAIN_XML_FILE).exists();
    }

    protected boolean isValidRepository(RepositoryConfig config) {
        return getFileLayout(config).getDomainConfigFile().exists();
    }

    protected File getRepositoryDir(RepositoryConfig config) {
        return getFileLayout(config).getRepositoryDir();
    }

    protected File getRepositoryRootDir(RepositoryConfig config) {
        return getFileLayout(config).getRepositoryRootDir();
    }

    protected void checkRepository(RepositoryConfig config)
            throws RepositoryException {
        checkRepository(config, true, true);
    }

    public void checkRepository(RepositoryConfig config,
            boolean existingRepository)
            throws RepositoryException {
        checkRepository(config, existingRepository, true);
    }

    /**
     * Sanity check on the repository. This is executed prior to
     * create/delete/start/stop.
     */
    public void checkRepository(RepositoryConfig config,
            boolean existingRepository,
            boolean checkRootDir)
            throws RepositoryException {
        String repositoryName = config.getDisplayName();

        //check domain name for validity
        new RepositoryNameValidator(getMessages().getRepositoryNameMessage()).validate(repositoryName);

        if (checkRootDir || existingRepository) {
            //check domain root directory is read/writable
            new FileValidator(getMessages().getRepositoryRootMessage(), "drw").validate(
                    config.getRepositoryRoot());
        }

        //check installation root directory is readable
        new FileValidator(_strMgr.getString("installRoot"), "dr").validate(
                config.getInstallRoot());

        //Ensure that the domain exists or does not exist
        if (existingRepository) {
            if (!repositoryExists(config)) {
                if (Boolean.getBoolean(DEBUG)) {
                    throw new RepositoryException(
                            getMessages().getNoExistsMessage(repositoryName,
                            getBigNoExistsMessage(config)));
                }
                else {
                    throw new RepositoryException(
                            getMessages().getNoExistsMessage(repositoryName,
                            getRepositoryDir(config).getAbsolutePath()));
                }
            }
            else if (!isValidRepository(config)) {
                throw new RepositoryException(
                        getMessages().getRepositoryNotValidMessage(
                        getRepositoryDir(config).getAbsolutePath()));
            }
        }
        else {
            if (repositoryExists(config)) {
                throw new RepositoryException(
                        getMessages().getExistsMessage(repositoryName,
                        getRepositoryRootDir(config).getAbsolutePath()));
            }
        }
    }

    private String getBigNoExistsMessage(RepositoryConfig config) {
        File repdir = getRepositoryDir(config);
        File canrepdir = FileUtils.safeGetCanonicalFile(repdir);
        File canrepdirparent = canrepdir.getParentFile();

        String s = "";
        s += "\nRep. Dir:" + repdir;
        s += "\nDump of RepositoryConfig: " + config.toString();
        s += "\nCanonical File: " + canrepdir;
        s += "\nParent File: " + canrepdirparent;


        boolean regex = repdir.exists();
        boolean canex = canrepdir.exists();
        boolean parentex = canrepdirparent.exists();
        boolean regdir = repdir.isDirectory();
        boolean candir = canrepdir.isDirectory();
        boolean parentdir = canrepdirparent.isDirectory();

        s += "\nrepdir exists: " + regex + ", canon exists: " + canex + ", parent exists: " + parentex
                + ", reg is dir: " + regdir + ", canon isdir: " + candir
                + ", parent is dir: " + parentdir;
        s += "\nInstance root sys property (";
        s += SystemPropertyConstants.INSTANCE_ROOT_PROPERTY;
        s += "): ";
        s += System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY);

        return s;
    }

    /**
     * Sets the permissions for the domain directory, its config directory,
     * startserv/stopserv scripts etc.
     */
    protected void setPermissions(RepositoryConfig repositoryConfig) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(repositoryConfig);
        final File domainDir = layout.getRepositoryDir();
        try {
            chmod("-R 755", domainDir);
        }
        catch (Exception e) {
            throw new RepositoryException(
                    _strMgr.getString("setPermissionError"), e);
        }
    }

    /**
     * Deletes the repository (domain, node agent, server instance).
     */
    protected void deleteRepository(
            RepositoryConfig config)
            throws RepositoryException {
        deleteRepository(config, true);
    }

    /**
     * Deletes the repository (domain, node agent, server instance). If the
     * deleteJMSProvider flag is set, we delete the jms instance. The jms
     * instance is present in the domain only and not when the repository
     * corresponds to a server instance or node agent.
     */
    protected void deleteRepository(
            RepositoryConfig config, boolean deleteJMSProvider)
            throws RepositoryException {
        checkRepository(config, true);

        //Ensure that the entity to be deleted is stopped
        // commenting out status check for now
        /*
         * final int status = getInstancesManager(config).getInstanceStatus();
         * if (status != Status.kInstanceNotRunningCode) { throw new
         * RepositoryException(
         * getMessages().getCannotDeleteInstanceInvalidState(
         * config.getDisplayName(), Status.getStatusString(status))); }
         */
        // FIXME: This is set temporarily so the instances that are deleted
        // don't require domain.xml (instance may never have been started) and it
        // also removes the dependencey on imqadmin.jar.
        // This should ne move in some way to PEDomainsManager since
        // JMS providers are really only present in the domain and not node agent
        // or server instance.
        //if (deleteJMSProvider) {
        //    deleteJMSProviderInstance(config);
        //}

        //Blast the directory
        File repository = getRepositoryDir(config);
        try {
            FileUtils.liquidate(repository);
        }
        catch (Exception e) {
            throw new RepositoryException(getMessages().getCannotDeleteMessage(
                    repository.getAbsolutePath()), e);
        }

        //Double check to ensure that it was really deleted
        if (repositoryExists(config)) {
            throw new RepositoryException(
                    getMessages().getCannotDeleteMessage(repository.getAbsolutePath()));
        }
    }

    /**
     * Return all repositories (domains, node agents, server instances)
     */
    protected String[] listRepository(RepositoryConfig config) throws RepositoryException {
        File repository = getRepositoryRootDir(config);
        String[] dirs;
        try {
            File f = repository.getCanonicalFile();
            if (!f.isDirectory()) {
                throw new RepositoryException(getMessages().getInvalidPathMessage(
                        f.getAbsolutePath()));
            }
            dirs = f.list(new FilenameFilter() {
                //Only accept directories that are valid (contain the property startserv script)
                @Override
                public boolean accept(File dir, String name) {
                    File f = new File(dir, name);
                    if (!f.isDirectory()) {
                        return false;
                    }
                    else {
                        return isValidRepository(f);
                    }
                }
            });
            if (dirs == null) {
                dirs = new String[0];
            }
        }
        catch (Exception e) {
            throw new RepositoryException(e);
        }
        return dirs;
    }
    /*
     * public InstancesManager getInstancesManager(RepositoryConfig config) {
     * return new PEInstancesManager(config); }
     */

    /**
     * Return all repositories (domains, node agents, server instances) and
     * their corresponding status (e.g. running or stopped) in string form.
     */
    /*
     * protected String[] listDomainsAndStatusAsString( RepositoryConfig config)
     * throws RepositoryException { try { RuntimeStatusList statusList =
     * getRuntimeStatus(config); RuntimeStatus status = null; String[] result =
     * new String[statusList.size()]; for (int i = 0; i < statusList.size();
     * i++) { status = statusList.getStatus(i); result[i] =
     * getMessages().getListRepositoryElementMessage( status.getName(),
     * status.toShortString()); } return result; } catch (Exception e) { throw
     * new RepositoryException(e); } }
     */
    protected RepositoryConfig getConfigForRepositoryStatus(RepositoryConfig config,
            String repository) {
        //The repository here corresponds to either the domain or node agent name
        return new RepositoryConfig(repository, config.getRepositoryRoot());
    }

    /**
     * We validate the master password by trying to open the password alias
     * keystore. This means that the keystore must already exist.
     *
     * @param config
     * @param password
     * @throws RepositoryException
     */
    public void validateMasterPassword(RepositoryConfig config,
            String password) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File passwordAliases = layout.getPasswordAliasKeystore();
        try {
            // WBN July 2007
            // we are constructing this object ONLY to see if it throws
            // an Exception.  We do not use the object.
            new PasswordAdapter(passwordAliases.getAbsolutePath(),
                    password.toCharArray());
        }
        catch (IOException ex) {
            throw new RepositoryException(_strMgr.getString("masterPasswordInvalid"));
        }
        catch (Exception ex) {
            throw new RepositoryException(
                    _strMgr.getString("couldNotValidateMasterPassword", passwordAliases), ex);
        }
    }

    /**
     * retrieve clear password from password alias keystore
     *
     * @param config
     * @param password
     * @param alias for which the clear text password would returns
     * @throws RepositoryException
     */
    public String getClearPasswordForAlias(RepositoryConfig config,
            String password, String alias) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File passwordAliases = layout.getPasswordAliasKeystore();
        try {
            PasswordAdapter p = new PasswordAdapter(passwordAliases.getAbsolutePath(),
                    password.toCharArray());
            String clearPwd = p.getPasswordForAlias(alias);
            return clearPwd;
        }
        catch (Exception ex) {
            return null;
        }
    }
    /*
     * public void validateAdminUserAndPassword(RepositoryConfig config, String
     * user, String password) throws RepositoryException { try { //Read in
     * domain.xml. This will fail with a ConfigException if there is no
     * domain.xml final PEFileLayout layout = getFileLayout(config);
     * ConfigContext configContext = getConfigContext(config); //Fetch the name
     * of the realm for the DAS system jmx connector String dasName =
     * ServerHelper.getDAS(configContext).getName(); JmxConnector conn =
     * ServerHelper.getServerSystemConnector(configContext, dasName); String
     * realmName = conn.getAuthRealmName(); SecurityService security =
     * ServerHelper.getConfigForServer(configContext,
     * dasName).getSecurityService(); //Load in the file realm //Before loading
     * the realm, we must ensure that com.sun.aas.instanceRoot //is set
     * correcty, since the keyfile is most likely referenced using this. //In
     * addition java.security.auth.login.config must be setup. String oldRoot =
     * System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY);
     * String oldConf = System.getProperty("java.security.auth.login.config");
     * GFSystem.setProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY,
     * layout.getRepositoryDir().getAbsolutePath());
     * GFSystem.setProperty("java.security.auth.login.config",
     * layout.getLoginConf().getAbsolutePath());
     * RealmConfig.createRealms(realmName, new AuthRealm[]
     * {security.getAuthRealmByName(realmName)}); //Restore previous values just
     * in case. if (oldRoot != null) {
     * GFSystem.setProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY,
     * oldRoot); } if (oldConf != null) {
     * GFSystem.setProperty("java.security.auth.login.config", oldConf); }
     * //Finally do the authentication of user and password final
     * ASJMXAuthenticator authenticator = new ASJMXAuthenticator();
     * authenticator.setRealmName(realmName); authenticator.setLoginDriver(new
     * ASLoginDriverImpl()); authenticator.authenticate(new String[] {user,
     * password}); } catch (Exception ex) { throw new RepositoryException(
     * _strMgr.getString("couldNotValidateMasterPassword", user), ex); } }
     */

    /**
     * Change the password protecting the password alias keystore
     *
     * @param config
     * @param oldPassword old password
     * @param newPassword new password
     * @throws RepositoryException
     */
    protected void changePasswordAliasKeystorePassword(RepositoryConfig config,
            String oldPassword, String newPassword) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File passwordAliases = layout.getPasswordAliasKeystore();

        //Change the password of the keystore alias file
        if (passwordAliases.exists()) {
            try {
                PasswordAdapter p = new PasswordAdapter(passwordAliases.getAbsolutePath(),
                        oldPassword.toCharArray());
                p.changePassword(newPassword.toCharArray());
            }
            catch (Exception ex) {
                throw new RepositoryException(
                        _strMgr.getString("passwordAliasPasswordNotChanged", passwordAliases), ex);
            }
        }
    }

    /**
     * Create JBI instance.
     */
    protected void createJBIInstance(String instanceName,
            RepositoryConfig config) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        layout.createJBIDirectories();
        final TokenValueSet tvSet = new TokenValueSet();
        final String tvDelimiter = "@";
        final String tJbiInstanceName = "JBI_INSTANCE_NAME";
        final String tJbiInstanceRoot = "JBI_INSTANCE_ROOT";
        try {

            final TokenValue tvJbiInstanceName = new TokenValue(tJbiInstanceName,
                    instanceName, tvDelimiter);
            final TokenValue tvJbiInstanceRoot = new TokenValue(tJbiInstanceRoot,
                    layout.getRepositoryDir().getCanonicalPath(), tvDelimiter);
            tvSet.add(tvJbiInstanceName);
            tvSet.add(tvJbiInstanceRoot);
            final File src = layout.getJbiTemplateFile();
            final File dest = layout.getJbiRegistryFile();

            generateFromTemplate(tvSet, src, dest);

            final File httpConfigSrc = layout.getHttpBcConfigTemplate();
            final File httpConfigDest = layout.getHttpBcConfigFile();
            //tokens will be added in a follow-up integration
            final TokenValueSet httpTvSet = new TokenValueSet();
            generateFromTemplate(httpTvSet, httpConfigSrc, httpConfigDest);

            createHttpBCInstallRoot(layout);
            createJavaEESEInstallRoot(layout);
            createWSDLSLInstallRoot(layout);

        }
        catch (Exception ioe) {
            throw new RepositoryException(
                    _strMgr.getString("jbiRegistryFileNotCreated"), ioe);
        }
    }

    /**
     * This method is used to create httpsoapbc install root
     *
     * @param layout PEFileLayout
     */
    public void createHttpBCInstallRoot(PEFileLayout layout)
            throws Exception {

        FileUtils.copy(
                layout.getHttpBcArchiveSource(),
                layout.getHttpBcArchiveDestination());

        ZipFile zf = new ZipFile(layout.getHttpBcArchiveSource(), layout.getHttpBcInstallRoot());
        zf.explode();
    }

    /**
     * This method is used to create Java EESE install root
     *
     * @param layout PEFileLayout
     */
    public void createJavaEESEInstallRoot(PEFileLayout layout)
            throws Exception {
        FileUtils.copy(
                layout.getJavaEESEArchiveSource(),
                layout.getJavaEESEArchiveDestination());

        ZipFile zf = new ZipFile(layout.getJavaEESEArchiveSource(), layout.getJavaEESEInstallRoot());
        zf.explode();
    }

    /**
     * This method is used to create WSDLSL install root
     *
     * @param layout PEFileLayout
     */
    public void createWSDLSLInstallRoot(PEFileLayout layout)
            throws Exception {
        FileUtils.copy(
                layout.getWSDLSLArchiveSource(),
                layout.getWSDLSLArchiveDestination());

        ZipFile zf = new ZipFile(layout.getWSDLSLArchiveSource(), layout.getWSDLSLInstallRoot());
        zf.explode();

    }

    /**
     * Create MQ instance.
     */
    protected void createMQInstance(
            RepositoryConfig config) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File broker = layout.getImqBrokerExecutable();
        final File mqVarHome = layout.getImqVarHome();
        try {
            FileUtils.mkdirsMaybe(mqVarHome);
            final List<String> cmdInput = new ArrayList<String>();
            cmdInput.add(broker.getAbsolutePath());
            cmdInput.add("-init");
            cmdInput.add("-varhome");
            cmdInput.add(mqVarHome.getAbsolutePath());
            ProcessExecutor pe = new ProcessExecutor(cmdInput.toArray(new String[cmdInput.size()]));
            pe.execute(false, false);
        }
        catch (Exception ioe) {
            /*
             * Dont do anything. * IMQ instance is created just to make sure
             * that Off line IMQ commands can be executed, even before starting
             * the broker. A typical scenario is while on-demand startup is off,
             * user might try to do imqusermgr. Here broker may not have
             * started.
             *
             * Failure in creating the instance doesnt need to abort domain
             * creation.
             */
        }
    }

    /**
     * Create the timer database wal file.
     */
    protected void createTimerWal(
            RepositoryConfig config) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File src = layout.getTimerWalTemplate();
        final File dest = layout.getTimerWal();
        try {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe) {
            throw new RepositoryException(
                    _strMgr.getString("timerWalNotCreated"), ioe);
        }
    }

    /**
     * Create the timer database dbn file.
     */
    protected void createTimerDbn(
            RepositoryConfig config) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        final File src = layout.getTimerDbnTemplate();
        final File dest = layout.getTimerDbn();
        try {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe) {
            throw new RepositoryException(
                    _strMgr.getString("timerDbnNotCreated"), ioe);
        }
    }

    /**
     * A ConfigContext is maintained. The resetConfigContext method can be
     * called to reset the ConfigContext, causing getConfigContext() to reread
     * the config contex from disk.
     */
    /*
     * protected synchronized void resetConfigContext() { _configContext = null;
     * }
     *
     * protected synchronized ConfigContext getConfigContext(RepositoryConfig
     * config) throws ConfigException { if (_configContext == null) { final
     * PEFileLayout layout = getFileLayout(config); _configContext =
     * ConfigFactory.createConfigContext(
     * layout.getDomainConfigFile().getAbsolutePath()); } return _configContext;
     * }
     *
     * protected boolean domainUsesNSS(final RepositoryConfig rc) { try { final
     * ConfigContext cc = getConfigContext(rc); final String sn =
     * SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME; final boolean
     * useNSS = ServerHelper.serverUsesNss(cc, sn); return ( useNSS ); } catch
     * (final Exception e) { throw new RuntimeException(e); //can't recover :) }
     * }
     */
    /**
     * Cleans the mq broker instances created for all the server instances that
     * are managed by this domain. This method is added to this class for the
     * following reasons 1) Depends on the preConditions of the
     * deleteRespository method - like instance not running. 2) Requires the
     * repository to exist.
     *
     * @param config
     * @throws RepositoryException
     */
    /*
     * void deleteJMSProviderInstance(RepositoryConfig config) throws
     * RepositoryException { final PEFileLayout layout = getFileLayout(config);
     * final String repositoryName = config.getRepositoryName(); try { final
     * JMSAdmin jmsAdmin = IASJmsUtil.getJMSAdminFactory(). getJMSAdmin(); final
     * ConfigContext ctx = getConfigContext(config); final Server[] servers =
     * getServers(ctx); for (int i = 0; i < servers.length; i++) { final String
     * mqInstanceName = IASJmsUtil.getBrokerInstanceName( repositoryName,
     * servers[i].getName(), getJmsService(servers[i], ctx)); final String
     * javaHome = getJavaHome(servers[i], ctx);
     *
     * try { String iMQBin = System.getProperty(
     * SystemPropertyConstants.IMQ_BIN_PROPERTY,
     * layout.getImqBinDir().getAbsolutePath()); String iMQInstances =
     * layout.getRepositoryDir() + File.separator + IASJmsUtil.MQ_DIR_NAME;
     * String[] optArgs = new String[4];
     *
     * optArgs[0] = "-javahome"; optArgs[1] = javaHome; optArgs[2] = "-varhome";
     * optArgs[3] = iMQInstances;
     *
     * //4966940 jmsAdmin.deleteProviderInstance( iMQBin, optArgs,
     * mqInstanceName); //4966940 } catch (JMSException jmse) { //Eating the
     * exception for now. This exception will //be thrown even in cases whre the
     * broker instance //was not yet created (broker instance is created //only
     * during server startup). } } } catch (Exception e) { throw new
     * RepositoryException(
     * _strMgr.getString("cannotDeleteJmsProviderInstance"), e); } }
     */
    protected String[] getInteractiveOptions(String user, String password,
            String masterPassword, HashMap<Object, Object> extraPasswords) {
        int numKeys = extraPasswords == null ? 0 : extraPasswords.size();
        String[] options = new String[3 + numKeys];
        // set interativeOptions for security to hand to starting process from ProcessExecutor
        options[0] = user;
        options[1] = password;
        options[2] = masterPassword;
        if (extraPasswords != null) {
            int i = 3;
            for (Map.Entry<Object, Object> me : extraPasswords.entrySet()) {
                options[i++] = (String) me.getKey() + "=" + (String) me.getValue();
            }
        }
        return options;
    }
    /*
     * private static Server[] getServers(ConfigContext ctx) throws
     * ConfigException { return ServerHelper.getServersInDomain(ctx); }
     *
     * private static String getJavaHome(Server server, ConfigContext ctx)
     * throws ConfigException { final JavaConfig javaConfig = getConfig(server,
     * ctx).getJavaConfig(); return javaConfig.getJavaHome(); }
     *
     * private static JmsService getJmsService(Server server, ConfigContext ctx)
     * throws ConfigException { return getConfig(server, ctx).getJmsService(); }
     *
     * private static Config getConfig(Server server, ConfigContext ctx) throws
     * ConfigException { return ServerHelper.getConfigForServer(ctx,
     * server.getName()); }
     */

    /**
     * Determines if the NSS support is available in this installation. The
     * check involves availability of the
     * <code> certutil </code> executable.
     *
     * @return true if certutil exists false otherwise
     */
    public static boolean isNSSSupportAvailable() {
        File certUtilFile = null;
        if (OS.isWindows()) {
            certUtilFile = new File(CERTUTIL_CMD + ".exe");
        }
        else {
            certUtilFile = new File(CERTUTIL_CMD);
        }
        if (certUtilFile.exists()) {
            return (true);
        }
        return (false);
    }
}
