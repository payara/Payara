/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

//import com.sun.enterprise.admin.servermgmt.launch.LaunchConstants;
import com.sun.enterprise.admin.servermgmt.*;
import com.sun.enterprise.admin.servermgmt.util.DomainXmlSAXParser;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;

public class PEDomainsManager extends RepositoryManager 
    implements DomainsManager
{
    /**
     * i18n strings manager object
     */
    private static final StringManager strMgr = 
        StringManager.getManager(PEDomainsManager.class);
    private static final String NAME_DELIMITER = ",";
    
    /* These properties are public interfaces, handle with care */
    public static final String PROFILEPROPERTY_DOMAINXML_STYLESHEETS = "domain.xml.style-sheets";
    public static final String PROFILEPROPERTY_DOMAINXML_TOKENVALUES = "domain.xml.token-values";
    /* These properties are public interfaces, handle with care */
    
    public PEDomainsManager()
    {
        super();
    }
    
    //PE does not require that an admin user / password is available at start-domain time.
    //SE/SEE does require it.
    @Override
    public BitSet getDomainFlags()
    {
        BitSet bs = new BitSet();        
        bs.set(DomainConfig.K_FLAG_START_DOMAIN_NEEDS_ADMIN_USER, false);
        return bs;
    }
    
    @Override
    public void validateDomain(DomainConfig domainConfig, boolean domainExists)
        throws DomainException
    {
        try {
            checkRepository(domainConfig, domainExists, domainExists);
        } catch (RepositoryException ex) {
            throw new DomainException(ex);
        }
    }

    /*
    public void validateAdminUserAndPassword(DomainConfig domainConfig) 
        throws DomainException
    {
        try {
            validateAdminUserAndPassword(domainConfig, getDomainUser(domainConfig),
                getDomainPasswordClear(domainConfig));
        } catch (RepositoryException ex) {
            throw new DomainException(ex);
        }
    }
    */
    @Override
    public void validateMasterPassword(DomainConfig domainConfig) 
        throws DomainException
    {
        try {
            validateMasterPassword(domainConfig, getMasterPasswordClear(domainConfig));
        } catch (RepositoryException ex) {
            throw new DomainException(ex);
        }
    }
    
    @Override
    public void createDomain(DomainConfig domainConfig) 
        throws DomainException
    {
        PEFileLayout layout = getFileLayout(domainConfig);
        
        try {
            new RepositoryNameValidator(strMgr.getString("domainsRoot")).
                    checkValidXmlToken(
                        layout.getRepositoryRootDir().getAbsolutePath());
            layout.createRepositoryRoot();        
            new PEDomainConfigValidator().validate(domainConfig);
            checkRepository(domainConfig, false);
        } catch (Exception ex) {
            throw new DomainException(ex);
        }
        
        try {            
            String masterPassword = getMasterPasswordClear(domainConfig);
            layout.createRepositoryDirectories();
            createDomainXml(domainConfig);
            createDomainXmlEvents(domainConfig);
            //createScripts(domainConfig);
            createServerPolicyFile(domainConfig);
            createAdminKeyFile(domainConfig, getDomainUser(domainConfig), 
                getDomainPasswordClear(domainConfig));
            createKeyFile(domainConfig, getDomainUser(domainConfig),
                getDomainPasswordClear(domainConfig));
            createAppClientContainerXml(domainConfig);
            createIndexFile(domainConfig);
            createDefaultWebXml(domainConfig);
            createLoginConf(domainConfig);
            createWssServerConfig(domainConfig);
            createWssServerConfigOld(domainConfig);
            createSSLCertificateDatabase(domainConfig, masterPassword);                                     
            changeMasterPasswordInMasterPasswordFile(domainConfig, masterPassword, 
                saveMasterPassword(domainConfig));
            createPasswordAliasKeystore(domainConfig, masterPassword);
            createLoggingProperties(domainConfig);
            //createTimerWal(domainConfig);
            //createTimerDbn(domainConfig);
            //createMQInstance(domainConfig);
            //createJBIInstance(getDefaultInstance(), domainConfig);
            setPermissions(domainConfig);
        } catch (DomainException de) {
            //rollback
            FileUtils.liquidate(getDomainDir(domainConfig));
            throw de;
        } catch (Exception ex) {
            //rollback
            FileUtils.liquidate(getDomainDir(domainConfig));
            throw new DomainException(ex);
        }
    }
    /**
     */
    protected void createJBIInstance(String instanceName, 
                   DomainConfig domainConfig) throws DomainException
    {        
        try {
            getFileLayout(domainConfig).createJBIDomainDirectories();
            super.createJBIInstance(instanceName, domainConfig);

        } catch (Exception ex) {
            throw new DomainException(ex);
        }
    }
    /**
     * Sets the permissions for the domain directory, its config directory,
     * startserv/stopserv scripts etc.
     */
    protected void setPermissions(DomainConfig domainConfig) throws DomainException
    {        
        final PEFileLayout layout = getFileLayout(domainConfig);
        try {
            //4958533
            chmod("-R u+x ", layout.getBinDir());
            chmod("-R g-rwx,o-rwx ", layout.getConfigRoot());
            //4958533
        } catch (Exception e) {
            throw new DomainException(
                strMgr.getString("setPermissionError"), e);
        }   
    }

    @Override
    public void deleteDomain(DomainConfig domainConfig) 
        throws DomainException
    {               
        try {
            deleteRepository(domainConfig);
        } catch (Exception e) {
            throw new DomainException(e);
        }
    }
    
    /**
     * Lists all the domains.
     */
    @Override
    public String[] listDomains(DomainConfig domainConfig)
        throws DomainException
    {        
        try {
            return listRepository(domainConfig); 
        } catch (Exception e) {
            throw new DomainException(e);
        }        
    }


    protected void createDomainXmlEvents(DomainConfig domainConfig) 
        throws DomainException {
            try {
                final PEFileLayout layout = getFileLayout(domainConfig);
                final File domainXml = layout.getDomainConfigFile();
                DomainXmlSAXParser parser = new DomainXmlSAXParser();
                try {
                    parser.parse(domainXml);
                }
                catch(Exception e) {
                    throw new DomainException(
                        strMgr.getString("domainXmlNotParsed"), e);
                }
                String className = parser.getDomainXmlEventListenerClass();
                if(className!=null) {
                    DomainXmlEventListener listener = (DomainXmlEventListener) Class.forName(className).newInstance();
                    listener.handleCreateEvent(domainConfig);
                }
            }
            catch(Exception e) {
                throw new DomainException(
                    strMgr.getString("domainXmlEventsNotCreated"), e);
            }
    }

    /**
     * The EEDomains manager needs to have an augmented set of tokens
     */
    protected TokenValueSet getDomainXmlTokens(DomainConfig domainConfig) {
        return PEDomainXmlTokens.getTokenValueSet(domainConfig);
    }
    
    protected void createDomainXml(DomainConfig domainConfig) 
        throws DomainException
    {
        try
        {
            final PEFileLayout layout = getFileLayout(domainConfig);
            final File dx = layout.getDomainConfigFile();
            TokenValueSet tokens = getDomainXmlTokens(domainConfig);
            String tn = (String)domainConfig.get(DomainConfig.K_TEMPLATE_NAME);
            if((tn == null)||(tn.equals(""))) {
                File tr = new File(layout.getTemplatesDir(), PEFileLayout.DOMAIN_XML_FILE);
                generateFromTemplate(tokens, tr, dx);
            }
            else {
                File dxt = layout.getDomainXmlTemplate(tn);
                generateFromTemplate(tokens, dxt, dx);
            }
        }
        catch(Exception e)
        {
            throw new DomainException(
                strMgr.getString("domainXmlNotCreated"), e);
        }
    }

    protected void createScripts(DomainConfig domainConfig)
        throws DomainException
    {
        final TokenValueSet tokens = PEScriptsTokens.getTokenValueSet(domainConfig);
        createStartServ(domainConfig, tokens);
        createStopServ(domainConfig, tokens);
    }

    void createStartServ(DomainConfig domainConfig, 
        TokenValueSet  tokens) throws DomainException
    {
        try
        {
            final PEFileLayout  layout = getFileLayout(domainConfig);
            final File startServTemplate = layout.getStartServTemplate();
            final File startServ = layout.getStartServ();
            generateFromTemplate(tokens, startServTemplate, startServ);
        }
        catch (Exception e)
        {
            throw new DomainException(
                strMgr.getString("startServNotCreated"), e);
        }
    }

    void createStopServ(DomainConfig domainConfig, 
        TokenValueSet  tokens) throws DomainException
    {
        try
        {
            final PEFileLayout  layout = getFileLayout(domainConfig);
            final File stopServTemplate = layout.getStopServTemplate();
            final File stopServ = layout.getStopServ();
            generateFromTemplate(tokens, stopServTemplate, stopServ);
            //final File killServ = layout.getKillServTemplate();
            //generateFromTemplate(new TokenValueSet(), 
		//layout.getKillServTemplate(), layout.getKillServ());
        }
        catch (Exception e)
        {
            throw new DomainException(
                strMgr.getString("stopServNotCreated"), e);
        }
    } 

    protected void createAppClientContainerXml(
        DomainConfig domainConfig) throws DomainException
    {
        try
        {
            final PEFileLayout layout = getFileLayout(domainConfig);
            for (Map.Entry<File,File> accXmlTemplateFileEntry : layout.getAppClientContainerTemplateAndXml().entrySet()) {
                final File accXmlTemplate = accXmlTemplateFileEntry.getKey();
                final File accXml = accXmlTemplateFileEntry.getValue();
                TokenValueSet tokens = PEAccXmlTokens.getTokenValueSet(domainConfig);
                generateFromTemplate(tokens, accXmlTemplate, accXml);
            }
        }
        catch(Exception e)
        {
            throw new DomainException(strMgr.getString("accXmlNotCreated"), e);
        }
    }

    protected void createIndexFile(
        DomainConfig domainConfig) throws DomainException
    {
        final PEFileLayout layout = getFileLayout(domainConfig);
        final File src = layout.getIndexFileTemplate();
        final File dest = layout.getIndexFile();
        try
        {
            final TokenValueSet tokens = IndexHtmlTokens.getTokenValueSet(domainConfig);
            generateFromTemplate(tokens, src, dest);
            handleLocalizedIndexHtmls(layout, tokens);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("indexFileNotCreated"), ioe);
        }
    }

    protected void createLoggingProperties(DomainConfig domainConfig) 
            throws DomainException
    {
        final PEFileLayout layout = getFileLayout(domainConfig);
        final File src = layout.getLoggingPropertiesTemplate();
        final File dest = layout.getLoggingProperties();
        try
        {
            generateFromTemplate(new TokenValueSet(), src, dest);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("loggingPropertiesNotCreated"), ioe);
        }
    }
    
    
    private void handleLocalizedIndexHtmls(PEFileLayout layout, TokenValueSet tokens) {
        Locale locale = Locale.getDefault();
        if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage()))
            return; //don't do anything in the case of English "language", not just locale
        //rename the existing index.file first
        File src  = layout.getNonEnglishIndexFileTemplate(locale);
        File dest = layout.getIndexFile();
        if (src.exists()) {
            if (!dest.renameTo(layout.getEnglishIndexFile())) {
                String zero = strMgr.getString("problemRenaming", dest.getAbsolutePath(), 
                        layout.getEnglishIndexFile().getAbsolutePath());
                System.out.println(zero);
                return;
            }
            dest = layout.getIndexFile();
            try {
                generateFromTemplate(tokens, src, dest);
            } catch(IOException e) {
                String one = strMgr.getString("problemCopyingIndexHtml", src.getAbsolutePath(), dest.getAbsolutePath());
                System.out.println(one);
            }
        } else {
            String two = strMgr.getString("localeFileNotFound", locale, src.getAbsolutePath());
            System.out.println(two);
        }
    }
    
    protected void createDefaultWebXml(
        DomainConfig domainConfig) throws DomainException
    {
        final PEFileLayout layout = getFileLayout(domainConfig);
        final File src = layout.getDefaultWebXmlTemplate();
        final File dest = layout.getDefaultWebXml();
        try
        {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("defaultWebXmlNotCreated"), ioe);
        }
    }
    
    protected void createLoginConf(
        RepositoryConfig config) throws DomainException
    {
        final PEFileLayout layout = getFileLayout(config);
        final File src = layout.getLoginConfTemplate();
        final File dest = layout.getLoginConf();
        try
        {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("loginConfNotCreated"), ioe);
        }
    }    

    protected void createWssServerConfigOld(RepositoryConfig config)
        throws DomainException
    {
        final PEFileLayout layout = getFileLayout(config);
        final File src = layout.getWssServerConfigOldTemplate();
        final File dest = layout.getWssServerConfigOld();
        try
        {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("wssserverconfignotcreated"), ioe);
        }

    }

    protected void createWssServerConfig(RepositoryConfig config)
        throws DomainException
    {
        final PEFileLayout layout = getFileLayout(config);
        final File src = layout.getWssServerConfigTemplate();
        final File dest = layout.getWssServerConfig();
        try
        {
            FileUtils.copy(src, dest);
        }
        catch (IOException ioe)
        {
            throw new DomainException(
                strMgr.getString("wssserverconfignotcreated"), ioe);
        }

    }

    protected File getDomainDir(DomainConfig domainConfig)
    {
        return getRepositoryDir(domainConfig);
    }

    protected File getDomainRoot(DomainConfig domainConfig)
    {
        return getRepositoryRootDir(domainConfig);
    }

    String getDefaultInstance()
    {
        return PEFileLayout.DEFAULT_INSTANCE_NAME;
    }
     
    /** Returns the domain user from the domainConfig.
     *  @param domainConfig that represents the domain configuration
     *  @return String representing the domain user if the given map contains
     *  it, null otherwise
    */

    protected static String getDomainUser(DomainConfig domainConfig) 
    {
        return ( (String) domainConfig.get(DomainConfig.K_USER) );
    }
    
    /** Returns the domain user's password in cleartext from the domainConfig.
     *  @param domainConfig that represents the domain configuration
     *  @return String representing the domain user password if the 
     *  given map contains it, null otherwise
    */

    protected static String getDomainPasswordClear(DomainConfig domainConfig) 
    {
        return ( (String) domainConfig.get(DomainConfig.K_PASSWORD) );
    } 
    
    protected static String getMasterPasswordClear(DomainConfig domainConfig)
    {
        return ((String)domainConfig.get(DomainConfig.K_MASTER_PASSWORD));
    }
    
    protected static String getNewMasterPasswordClear(DomainConfig domainConfig)
    {
        return ((String)domainConfig.get(DomainConfig.K_NEW_MASTER_PASSWORD));
    }
    
    protected static boolean saveMasterPassword(DomainConfig domainConfig) {
        Boolean b = (Boolean)domainConfig.get(DomainConfig.K_SAVE_MASTER_PASSWORD);
        return b.booleanValue();
    }
    
    /**
     * Changes the master password for the domain
     */    
    public void changeMasterPassword(DomainConfig config) throws DomainException
    {                                      
        try {
            String oldPass = getMasterPasswordClear(config);
            String newPass = getNewMasterPasswordClear(config);                        
            
            //Change the password of the keystore alias file
            changePasswordAliasKeystorePassword(config, oldPass, newPass);
            
            //Change the password of the keystore and truststore
            changeSSLCertificateDatabasePassword(config, oldPass, newPass);

            //Change the password in the masterpassword file or delete the file if it is 
            //not to be saved.
            changeMasterPasswordInMasterPasswordFile(config, newPass, saveMasterPassword(config));
        } catch (Exception ex) {
            throw new DomainException(
                strMgr.getString("masterPasswordNotChanged"), ex);
        }
    }
    @Override
    public String[] getExtraPasswordOptions(DomainConfig config)
        throws DomainException
    {
        return null;
    }
}
