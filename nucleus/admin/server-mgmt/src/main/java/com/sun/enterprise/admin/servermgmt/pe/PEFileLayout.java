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

package com.sun.enterprise.admin.servermgmt.pe;

import java.util.HashMap;
import java.util.Locale;
import java.io.File;

import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.i18n.StringManager;

import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.admin.servermgmt.RepositoryConfig;
import com.sun.enterprise.util.SystemPropertyConstants;

import com.sun.enterprise.security.store.PasswordAdapter;
import java.util.Map;

public class PEFileLayout
{
    private static final StringManager _strMgr =
        StringManager.getManager(PEFileLayout.class);

    public static final String DEFAULT_INSTANCE_NAME =
        SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;
   /* above field is taken from a central place */
    protected final RepositoryConfig _config;


    public PEFileLayout(RepositoryConfig config) {
        _config = config;
    }

    protected RepositoryConfig getConfig() {
        return _config;
    }

    public void createRepositoryRoot() throws RepositoryException
    {
        createDirectory(getRepositoryRootDir());
    }

    public void createJBIDirectories() throws RepositoryException
    {
        createDirectory(getJbiInstanceDir());
    }

    public void createJBIDomainDirectories() throws RepositoryException
    {
        createJBIDirectories();
        createDirectory(getJbiAuotoInstallDir());
        createDirectory(getJbiConfigDir());
        createDirectory(getJbiConfigPrivateDir());
        createJbiSystemComponentsLayout();
    }

    protected void createDirectory(File dir) throws RepositoryException
    {
        if (!dir.exists()) {
            try {
                if (!dir.mkdirs()) {
                    throw new RepositoryException(_strMgr.getString("directoryCreationError",
                        dir));
                }
	    } catch (Exception e) {
                throw new RepositoryException(_strMgr.getString("directoryCreationError",
                        dir), e);
            }
        }
    }

    public static final String ADDON_DIR = "addons";
    public File getAddonRoot()
    {
        return new File(getRepositoryDir(), ADDON_DIR);
    }

    public static final String CONFIG_DIR = "config";
    public File getConfigRoot()
    {
        return new File(getRepositoryDir(), CONFIG_DIR);
    }

    public static final String CONFIG_BACKUP_DIR = "backup";
    public File getRepositoryBackupRoot()
    {
        return new File(getConfigRoot(), CONFIG_BACKUP_DIR);
    }

    public static final String DOC_ROOT_DIR = "docroot";
        public File getDocRoot()
    {

        return new File(getRepositoryDir(), DOC_ROOT_DIR);
    }

    public static final String JAVA_WEB_START_DIR = "java-web-start";
        public File getJavaWebStartRoot()
    {

        return new File(getRepositoryDir(), JAVA_WEB_START_DIR);
    }

    public static final String LIB_DIR = "lib";
    public File getLibDir()
    {
        return new File(getRepositoryDir(), LIB_DIR);
    }

    public File getBinDir()
    {
        return new File(getRepositoryDir(), BIN_DIR);
    }

    public static final String CLASSES_DIR = "classes";
    public File getClassesDir()
    {
        return new File(getLibDir(), CLASSES_DIR);
    }

    public static final String APPLIBS_DIR = "applibs";
    public File getAppLibsDir()
    {
        return new File(getLibDir(), APPLIBS_DIR);
    }

    public static final String EXTLIB_DIR = "ext";
    public File getExtLibDir()
    {
        return new File(getLibDir(), EXTLIB_DIR);
    }

    public static final String TIMERDB_DIR = "databases";
    public File getTimerDatabaseDir()
    {
        return new File(getLibDir(), TIMERDB_DIR);
    }

    public static final String LOGS_DIR = "logs";
    public File getLogsDir()
    {
        return new File(getRepositoryDir(), LOGS_DIR);
    }

    public static final String APPS_ROOT_DIR = "applications";
    public File getApplicationsRootDir()
    {
        return new File(getRepositoryDir(), APPS_ROOT_DIR);
    }

    public static final String J2EE_APPS_DIR = "j2ee-apps";
    public File getJ2EEAppsDir()
    {
        return new File(getApplicationsRootDir(), J2EE_APPS_DIR);
    }

    public static final String J2EE_MODULES_DIR = "j2ee-modules";
    public File getJ2EEModulesDir()
    {
        return new File(getApplicationsRootDir(), J2EE_MODULES_DIR);
    }

    public static final String LIFECYCLE_MODULES_DIR = "lifecycle-modules";
    public File getLifecycleModulesDir()
    {
        return new File(getApplicationsRootDir(), LIFECYCLE_MODULES_DIR);
    }

    public static final String MBEAN_FOLDER_NAME = "mbeans";
    public File getMbeansDir()
    {
        return new File(getApplicationsRootDir(), MBEAN_FOLDER_NAME);
    }

    public static final String GENERATED_DIR = "generated";
    public File getGeneratedDir()
    {
        return new File(getRepositoryDir(), GENERATED_DIR);
    }

    // Begin EE: 4946914 - cluster deployment support

    public static final String POLICY_DIR = "policy";
    public static final String POLICY_FILE_EXT = "granted.policy";
    public File getPolicyDir()
    {
        return new File(getGeneratedDir(), POLICY_DIR);
    }

    // End EE: 4946914 - cluster deployment support

    public static final String JSP_DIR = "jsp";
    public File getJspRootDir()
    {
        return new File(getGeneratedDir(), JSP_DIR);
    }

    public static final String EJB_DIR = "ejb";
    public File getEjbRootDir()
    {
        return new File(getGeneratedDir(), EJB_DIR);
    }

    public static final String XML_DIR = "xml";
    public File getXmlRootDir()
    {
        return new File(getGeneratedDir(), XML_DIR);
    }

    public File getRepositoryDir()
    {
        return new File(getRepositoryRootDir(), getConfig().getRepositoryName());
    }

    public static final String DOMAIN_XML_FILE = "domain.xml";
    public File getDomainConfigFile()
    {
        return new File(getConfigRoot(), DOMAIN_XML_FILE);
    }

    public File getDomainConfigBackupFile()
    {
        return new File(getRepositoryBackupRoot(), DOMAIN_XML_FILE);
    }

    public static final String IMQ = "imq";
    public File getImqDir()
    {
        return new File(getInstallRootDir(), IMQ);
    }

    public static final String JBI_DIR = "jbi";
    public File getJbiDir()
    {
        return new File(getInstallRootDir(), JBI_DIR);
    }

    public static final String JBI_LIB_DIR = "lib";
    public File getJbiLibDir()
    {
        return new File(getJbiDir(), JBI_LIB_DIR);
    }

    public static final String JBI_LIB_INSTALL_DIR = "install";
    public File getJbiLibInstallDir()
    {
        return new File(getJbiLibDir(), JBI_LIB_INSTALL_DIR);
    }

    public static final String JBI_TEMPLATE_DIR = "templates";
    public File getJbiTemplateDir()
    {
        return new File(getJbiLibInstallDir(), JBI_TEMPLATE_DIR);
    }

    public static final String JBI_TEMPLATE_FILE = "jbi-registry.xml.template";
    public File getJbiTemplateFile()
    {
        return new File(getJbiTemplateDir(), JBI_TEMPLATE_FILE);
    }

    public static final String JBI_INSTANCE_DIR = "jbi";
    public File getJbiInstanceDir()
    {
        return new File(getRepositoryDir(), JBI_INSTANCE_DIR);
    }

    public static final String JBI_COMPONENTS_DIR = "components";
    public File getJbiComponentsDir()
    {
        return new File(getJbiInstanceDir(), JBI_COMPONENTS_DIR);
    }

    public static final String JAVAEE_SE_DIR = "sun-javaee-engine";
    public File getJavaEESEDir()
    {
        return new File(getJbiComponentsDir(), JAVAEE_SE_DIR);
    }

    public static final String HTTP_BC_DIR = "sun-http-binding";
    public File getHttpBcDir()
    {
        return new File(getJbiComponentsDir(), HTTP_BC_DIR);
    }

    public static final String JBI_COMPONENT_WS = "install_root" + File.separator + "workspace";
    public File getJavaEESEWorkSpace()
    {
        return new File(getJavaEESEDir(), JBI_COMPONENT_WS);
    }

    public File getHttpBcWorkSpace()
    {
        return new File(getHttpBcDir(), JBI_COMPONENT_WS);
    }

    public static final String JBI_SHAREDLIB_DIR = "shared-libraries";
    public File getJbiSharedLibDir()
    {
        return new File(getJbiInstanceDir(), JBI_SHAREDLIB_DIR);
    }

    public static final String JBI_CONFIG_DIR = "config";
    public File getJbiConfigDir()
    {
        return new File(getJbiInstanceDir(), JBI_CONFIG_DIR);
    }

    public static final String JBI_AUTOINSTALL_DIR = "autoinstall";
    public File getJbiAuotoInstallDir()
    {
        return new File(getJbiInstanceDir(), JBI_AUTOINSTALL_DIR);
    }

    public static final String JBI_CONFIG_PRIVATE_DIR = "private";
    public File getJbiConfigPrivateDir()
    {
        return new File(getJbiConfigDir(), JBI_CONFIG_PRIVATE_DIR);
    }

    public static final String JBI_REGISTRY_FILE = "jbi-registry.xml";
    public File getJbiRegistryFile()
    {
        return new File(getJbiConfigDir(), JBI_REGISTRY_FILE);
    }

    public static final String HTTP_BC_CONFIG = "config.properties";
    public File getHttpBcConfigTemplate()
    {
        return new File(getJbiTemplateDir(), HTTP_BC_CONFIG);
    }

    public File getHttpBcConfigFile()
    {
        return new File(getHttpBcWorkSpace(), HTTP_BC_CONFIG);
    }

    public static final String IMQ_VAR_DIR = "imq";
    public File getImqVarHome()
    {
        return new File(getRepositoryDir(), IMQ_VAR_DIR);
    }

    public static final String BIN_DIR = "bin";
    public File getImqBinDir()
    {
        return new File(getImqDir(), BIN_DIR);
    }

    public File getImqLibDir()
    {
        return new File(getImqDir(), LIB_DIR);
    }

    public File getInstallRootDir()
    {
        return getCanonicalFile(new File(getConfig().getInstallRoot()));
    }

    public File getRepositoryRootDir()
    {
        return getCanonicalFile(new File(getConfig().getRepositoryRoot()));
    }

    public static final String SHARE = "share";
    public File getShareDir()
    {
        return new File(getInstallRootDir(), SHARE);
    }

    public File getWebServicesLibDir()
    {
        return new File(getShareDir(), LIB_DIR);
    }

//$INSTALL_ROOT/lib/install/templates
    public static final String INSTALL_DIR         = "install";
    public static final String TEMPLATES_DIR       = "templates";
    public static final String COMMON_DIR       = "common";
    public static final String PROFILE_PROPERTIES  = "profile.properties";
    private static final String TEMPLATE_CONFIG_XML = "default-config.xml";
    
    public File getTemplatesDir()
    {
        final File lib = new File(getInstallRootDir(), LIB_DIR);
        //final File install = new File(lib, INSTALL_DIR);
        final File templates = new File(lib, TEMPLATES_DIR);
        return templates;
    }

    public File getProfileFolder(final String profileName)
    {
        /* Commented out for V3, till things can be more finalized. For
         * now there is only one profile and the template is in the
         * common template directory */
        
        assert profileName != null : "Name of the profile can't be null";
        final File pf = new File(getTemplatesDir(), profileName);        
        return pf;
    }

    public File getProfilePropertiesFile(final String profileName) {
        return ( new File (getProfileFolder(profileName), PROFILE_PROPERTIES) );
    }
    
    public File getPreExistingDomainXmlTemplateForProfile(final String profileName) {
        return ( new File (getProfileFolder(profileName), DOMAIN_XML_FILE) );
    }
    
    public File getTemplateConfigXml() {
        return ( new File(getTemplatesDir(), TEMPLATE_CONFIG_XML) );
    }

//$INSTALL_ROOT/lib/install/applications

    public static final String APPLICATIONS_DIR    = "applications";
    public File getInstallApplicationsDir()
    {
        final File lib = new File(getInstallRootDir(), LIB_DIR);
        final File install = new File(lib, INSTALL_DIR);
        return new File(install, APPLICATIONS_DIR);
    }

//$INSTALL_ROOT/lib/install/databases

    public static final String DATABASES_DIR    = "databases";
    public File getInstallDatabasesDir()
    {
        final File lib = new File(getInstallRootDir(), LIB_DIR);
        final File install = new File(lib, INSTALL_DIR);
        return new File(install, DATABASES_DIR);
    }

//$INSTALL_ROOT/lib/dtds

    public static final String DTDS_DIR    = "dtds";
    public File getDtdsDir()
    {
        final File lib = new File(getInstallRootDir(), LIB_DIR);
        return new File(lib, DTDS_DIR);
    }

    public static final String DOMAIN_XML_TEMPLATE = "default-domain.xml.template";
    public File getDomainXmlTemplate()
    {
        return new File(getTemplatesDir(), DOMAIN_XML_TEMPLATE);
    }

    public File getDomainXmlTemplate(String templateName)
    {
        // check to see if the user has specified a template file to be used for
        // domain creation. Assumed that the user specified template file
        // exists in the INSTALL_ROOT/lib/install/templates if path is not absolute.
        if(new File(templateName).isAbsolute())
            return new File(templateName);
        else
            return new File(getTemplatesDir(), templateName);
    }

    public static final String IMQBROKERD_UNIX = "imqbrokerd";
    public static final String IMQBROKERD_WIN = "imqbrokerd.exe";
    public static final String IMQBROKERD = isWindows() ? IMQBROKERD_WIN : IMQBROKERD_UNIX;
    public File getImqBrokerExecutable()
    {
        return new File(getImqBinDir(), IMQBROKERD);
    }

    public static final String START_SERV_UNIX = "startserv";
    public static final String START_SERV_WIN = "startserv.bat";
    public static final String START_SERV_OS = isWindows() ? START_SERV_WIN : START_SERV_UNIX;
    public File getStartServ()
    {
        return new File(getBinDir(), START_SERV_OS);
    }

    public static final String START_SERV_TEMPLATE_UNIX = "startserv.tomcat.template";
    public static final String START_SERV_TEMPLATE_WIN  = "startserv.tomcat.bat.template";
    public static final String START_SERV_TEMPLATE_OS = isWindows() ? START_SERV_TEMPLATE_WIN :
        START_SERV_TEMPLATE_UNIX;

    public File getStartServTemplate()
    {
        return new File(getTemplatesDir(), START_SERV_TEMPLATE_OS);
    }

    public static final String STOP_SERV_UNIX = "stopserv";
    public static final String STOP_SERV_WIN = "stopserv.bat";
    public static final String STOP_SERV_OS = isWindows() ? STOP_SERV_WIN : STOP_SERV_UNIX;

    public File getStopServ()
    {
        return new File(getBinDir(), STOP_SERV_OS);
    }

    public static final String KILL_SERV_UNIX = "killserv";
    public static final String KILL_SERV_WIN = "killserv.bat";
    public static final String KILL_SERV_OS = 
        isWindows() ? KILL_SERV_WIN : KILL_SERV_UNIX;

    public File getKillServ()
    {
        return new File(getBinDir(), KILL_SERV_OS);
    }

    public File getKillServTemplate()
    {
        return new File(getTemplatesDir(), KILL_SERV_OS);
    }

    public static final String STOP_SERV_TEMPLATE_UNIX = "stopserv.tomcat.template";
    public static final String STOP_SERV_TEMPLATE_WIN  = "stopserv.tomcat.bat.template";
    public static final String STOP_SERV_TEMPLATE_OS = isWindows() ? STOP_SERV_TEMPLATE_WIN :
        STOP_SERV_TEMPLATE_UNIX;
    public File getStopServTemplate()
    {
        return new File(getTemplatesDir(), STOP_SERV_TEMPLATE_OS);
    }

    public static final String POLICY_FILE = "server.policy";
    public File getPolicyFileTemplate()
    {
        return new File(getTemplatesDir(), POLICY_FILE);
    }

    public File getPolicyFile()
    {
        return new File(getConfigRoot(), POLICY_FILE);
    }


    public static final String STUB_FILE = "admch";
    public File getStubFile()
    {
        return new File(getConfigRoot(), STUB_FILE);
    }

    public static final String SEED_FILE = "admsn";
    public File getSeedFile()
    {
        return new File(getConfigRoot(), SEED_FILE);
    }

    public File getInstallConfigRoot()
    {
        return new File(getInstallRootDir(), CONFIG_DIR);
    }

    
    public static final String ACC_XML_TEMPLATE = "glassfish-acc.xml";
    
    public Map<File,File> getAppClientContainerTemplateAndXml() {
        final Map<File,File> result = new HashMap<File,File>();
        result.put(new File(getTemplatesDir(), ACC_XML_TEMPLATE), new File(getConfigRoot(), ACC_XML));
        return result;
    }
    
    public static final String ACC_XML = "glassfish-acc.xml";
    
    public static final String SESSION_STORE = "session-store";
    public File getSessionStore()
    {
        return new File(getRepositoryDir(), SESSION_STORE);
    }

    public static final String AUTO_DEPLOY = "autodeploy";
    public File getAutoDeployDir()
    {
        return new File(getRepositoryDir(), AUTO_DEPLOY);
    }

    public static final String AUTO_DEPLOY_STATUS = ".autodeploystatus";
    public File getAutoDeployStatusDir()
    {
        return new File(getAutoDeployDir(), AUTO_DEPLOY_STATUS);
    }

    private static final String AUTO_DEPLOY_OSGI_BUNDLES_DIR = "bundles";

    public static final String KEY_FILE_TEMPLATE = "keyfile";
    public File getKeyFileTemplate()
    {
        return new File(getTemplatesDir(), KEY_FILE_TEMPLATE);
    }

    public static final String KEY_FILE = "keyfile";
    public File getKeyFile()
    {
        return new File(getConfigRoot(), KEY_FILE);
    }

    public static final String ADMIN_KEY_FILE = "admin-keyfile";
    public File getAdminKeyFile()
    {
        return new File(getConfigRoot(), ADMIN_KEY_FILE);
    }

    public File getBackupKeyFile()
    {
        return new File(getRepositoryBackupRoot(), KEY_FILE);
    }

    public static final String INDEX_FILE = "index.html";
    public static final String DOC_ROOT = "docroot";
    public File getIndexFileTemplate()
    {
        final File docRoot = new File(getTemplatesDir(), DOC_ROOT);
        return new File(docRoot, INDEX_FILE);
    }
    
    private static final String LOCALES = "locales";
    public File getNonEnglishIndexFileTemplate(Locale locale) {
        File locales = new File (getTemplatesDir(), LOCALES);
        File givenLocale = new File (locales, locale.toString());
        return new File (givenLocale, INDEX_FILE);
    }
    
    public File getIndexFile()
    {
        return new File(getDocRoot(), INDEX_FILE);
    }
    
    private static final String ENGLISH_INDEX_FILE = "index_en.html";
    public File getEnglishIndexFile() {
        return new File(getDocRoot(), ENGLISH_INDEX_FILE);
    }

    public static final String DEFAULT_WEB_XML = "default-web.xml";
    public File getDefaultWebXmlTemplate()
    {
        return new File(getTemplatesDir(), DEFAULT_WEB_XML);
    }

    public File getDefaultWebXml()
    {
        return new File(getConfigRoot(), DEFAULT_WEB_XML);
    }

    public static final String LOGGING_PROPERTIES_FILE = "logging.properties";
    public File getLoggingPropertiesTemplate()
    {
        return new File(getTemplatesDir(), LOGGING_PROPERTIES_FILE);
    }

    public File getLoggingProperties()
    {
        return new File(getConfigRoot(), LOGGING_PROPERTIES_FILE);
    }

    public static final String LOGIN_CONF = "login.conf";
    public File getLoginConfTemplate()
    {
        return new File(getTemplatesDir(), LOGIN_CONF);
    }

    public File getLoginConf()
    {
        return new File(getConfigRoot(), LOGIN_CONF);
    }

    public static final String WSSSERVERCONFIGOLD = "wss-server-config-1.0.xml";
    public File getWssServerConfigOldTemplate()
    {
        return new File(getTemplatesDir(), WSSSERVERCONFIGOLD);
    }

    public File getWssServerConfigOld()
    {
        return new File(getConfigRoot(), WSSSERVERCONFIGOLD);
    }

    public static final String WSSSERVERCONFIG = "wss-server-config-2.0.xml";
    public File getWssServerConfigTemplate()
    {
        return new File(getTemplatesDir(), WSSSERVERCONFIG);
    }

    public File getWssServerConfig()
    {
        return new File(getConfigRoot(), WSSSERVERCONFIG);
    }

    public static final String KEYSTORE = "keystore.jks";
    public File getKeyStore()
    {
        return new File(getConfigRoot(), KEYSTORE);
    }

    public static final String TRUSTSTORE_TEMPLATE = "cacerts.jks";
    public File getTrustStoreTemplate()
    {
        return new File(getTemplatesDir(), TRUSTSTORE_TEMPLATE);
    }
    
    /** Should be used only in the cases where the actual keystore creation fails.
     *  The idea is when we are unable to create the JKS-keystore, we should
     *  copy the keystore in templates folder into the domain's config folder.
     *  This keystore is by no means a keystore for public use. It's just a
     *  keystore with a primary key "changeit" and associated certificate with
     *  signature:
     * --------------------------------------------------------------
        Keystore type: jks
        Keystore provider: SUN

        Your keystore contains 1 entry

        Alias name: s1as
        Creation date: Sep 11, 2008
        Entry type: keyEntry
        Certificate chain length: 1
        Certificate[1]:
        Owner: CN=localhost, OU=GlassFish, O=Sun Microsystems, L=Santa Clara, ST=California, C=US
        Issuer: CN=localhost, OU=GlassFish, O=Sun Microsystems, L=Santa Clara, ST=California, C=US
        Serial number: 48c9e075
        Valid from: Thu Sep 11 20:22:29 PDT 2008 until: Sun Sep 09 20:22:29 PDT 2018
        Certificate fingerprints:
	 MD5:  00:E5:5D:1F:07:CC:99:9F:CF:68:0E:AD:29:43:E0:48
	 SHA1: 1B:62:3E:B2:3D:D7:0B:63:80:92:EE:9A:59:F7:D5:9F:97:A3:FD:98
     * --------------------------------------------------------------
     * @return File representing the keystore path. Does not check if the
     *  path actually exists.
     */
    public File getKeyStoreTemplate() {
        return new File (getTemplatesDir(), KEYSTORE);
    }

    public static final String TRUSTSTORE = "cacerts.jks";
    public File getTrustStore()
    {
        return new File(getConfigRoot(), TRUSTSTORE);
    }

    public static final String MASTERPASSWORD_FILE = "master-password";
    public File getMasterPasswordFile()
    {
        return new File(getRepositoryDir(), MASTERPASSWORD_FILE);
    }

    public static final String PASSWORD_ALIAS_KEYSTORE = PasswordAdapter.PASSWORD_ALIAS_KEYSTORE;
    public File getPasswordAliasKeystore()
    {
        return new File(getConfigRoot(), PASSWORD_ALIAS_KEYSTORE);
    }

    public static final String TIMERDB_WAL_TEMPLATE = "ejbtimer$1.wal";
    public File getTimerWalTemplate() {
	return new File(getInstallDatabasesDir(), TIMERDB_WAL_TEMPLATE);
    }

    public static final String TIMERDB_WAL = "ejbtimer$1.wal";
    public File getTimerWal() {
	return new File(getTimerDatabaseDir(), TIMERDB_WAL);
    }

    public static final String TIMERDB_DBN_TEMPLATE = "ejbtimer.dbn";
    public File getTimerDbnTemplate() {
	return new File(getInstallDatabasesDir(), TIMERDB_DBN_TEMPLATE);
    }

    public static final String TIMERDB_DBN = "ejbtimer.dbn";
    public File getTimerDbn() {
	return new File(getTimerDatabaseDir(), TIMERDB_DBN);
    }
    public static final String DERBY_SQL_FILE = "ejbtimer_derby.sql";
    public static final String EJB_TIMER_TABLE_NAME = "EJB__TIMER__TBL"; //comes from sql file
    public File getDerbyEjbTimerSqlFile() {
	return new File(getInstallDatabasesDir(), DERBY_SQL_FILE);
    }
    public static final String DERBY_DATABASE_DIRECTORY = "ejbtimer";
    public File getDerbyEjbTimerDatabaseDirectory() {
        return new File(getTimerDatabaseDir(), DERBY_DATABASE_DIRECTORY);
        //this directory must not exist before creating the derby database
    }

    protected static boolean isWindows()
    {
	return OS.isWindows();
    }

    File getCanonicalFile(File f)
    {
        return FileUtils.safeGetCanonicalFile(f);
    }

    /**
     * This method is used to create the file layout for
     * JBI system components, HttpSoapBC, JavaEESE, WSDLSL
     */
    public void createJbiSystemComponentsLayout() throws RepositoryException
    {
        try{
            createDirectory(getHttpBcDir());
            createDirectory(getHttpBcInstallRoot());
            createDirectory(getJavaEESEDir());
            createDirectory(getJavaEESEInstallRoot());       
            createDirectory(getWSDLSLDir());
            createDirectory(getWSDLSLInstallRoot());
            createDirectory(getHttpBcWorkSpace());
            createDirectory(getJavaEESEWorkSpace());
        } catch (Exception e)
        {
            throw new RepositoryException(e);
        }
   }

   /**
     * This method is used to create WSDLSL install root
     */
    public void createWSDLSLInstallRoot() throws Exception
    {
       createDirectory(getWSDLSLDir());
       createDirectory(getWSDLSLInstallRoot());
    }

    public static final String JBI_COMPONENTS = "components";
    public File getJbiComponents()
    {
        return new File(getJbiDir(), JBI_COMPONENTS);
    }

    public static final String HTTP_BC_ARCHIVE =  "httpbc.jar";
    public File getHttpBcArchiveSource()
    {
        File bcDir = new File(getJbiComponents(), HTTP_BC_DIR);
        return new File (bcDir, HTTP_BC_ARCHIVE);
    }

    public File getHttpBcArchiveDestination()
    {
        return new File(getHttpBcDir(), HTTP_BC_ARCHIVE);
    }

    public static final String JBI_COMPONENTS_INSTALL_ROOT = "install_root";
    public File getHttpBcInstallRoot()
    {
        return new File(getHttpBcDir(), JBI_COMPONENTS_INSTALL_ROOT);
    }

    public static final String JAVAEE_SE_ARCHIVE =  "appserv-jbise.jar";
    public File getJavaEESEArchiveSource()
    {
        File seDir = new File(getJbiComponents(), JAVAEE_SE_DIR);
        return new File (seDir, JAVAEE_SE_ARCHIVE);
    }

    public File getJavaEESEArchiveDestination()
    {
        return new File(getJavaEESEDir(), JAVAEE_SE_ARCHIVE);
    }

    public File getJavaEESEInstallRoot()
    {
        return new File(getJavaEESEDir(), JBI_COMPONENTS_INSTALL_ROOT);
    }

    public static final String JBI_SHARED_LIBRARIES = "shared-libraries";
    public File getJbiSharedLibraries()
    {
        return new File(getJbiDir(), JBI_SHARED_LIBRARIES);
    }

    public static final String WSDLSL_DIR = "sun-wsdl-library";
    public File getWSDLSLDir()
    {
        return new File(getJbiSharedLibDir(), WSDLSL_DIR);
    }

    public static final String WSDLSL_ARCHIVE =  "wsdlsl.jar";
    public File getWSDLSLArchiveSource()
    {
        File slDir = new File(getJbiSharedLibraries(), WSDLSL_DIR);
        return new File (slDir, WSDLSL_ARCHIVE);
    }

    public File getWSDLSLArchiveDestination()
    {
        return new File(getWSDLSLDir(), WSDLSL_ARCHIVE);
    }

    public File getWSDLSLInstallRoot()
    {
        return new File(getWSDLSLDir(), JBI_COMPONENTS_INSTALL_ROOT);
    }



}
