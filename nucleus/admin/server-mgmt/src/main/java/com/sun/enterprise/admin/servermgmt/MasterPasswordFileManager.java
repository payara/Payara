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
// Portions Copyright [2016-2025] [Payara Foundation and/or affiliates]

package com.sun.enterprise.admin.servermgmt;

import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.MASTERPASSWORD_FILE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;

import jakarta.annotation.Nullable;
import org.glassfish.security.common.FileProtectionUtility;


/**
 * The RepositoryManager serves as a common base class for the following
 * PEDomainsManager, PEInstancesManager, AgentManager (the SE Node Agent).
 * Its purpose is to abstract out any shared functionality related to 
 * lifecycle management of domains, instances and node agents. This includes
 * creation, deletion, listing, and starting and stopping.
 *
 * @author  kebbs
 * @since August 19, 2003, 2:29 PM
 */
public class MasterPasswordFileManager extends KeystoreManager {              

    private static final String ENCODED_CHARSET = "UTF-8";
    private static final int SALT_SIZE = 8;
    
    private static final StringManager STRING_MANAGER = StringManager.getManager(MasterPasswordFileManager.class);                  

    /** Creates a new instance of RepositoryManager */
    public MasterPasswordFileManager() {
        super();
    }    

    /**
     *
     * @return The password protecting the master password keystore
     */    
    private char[] getMasterPasswordPassword() throws RepositoryException 
    {
        //XXX fixed String
        return MASTERPASSWORD_FILE.toCharArray();
    }
    
    protected void deleteMasterPasswordFile(RepositoryConfig config)
    {
        final PEFileLayout layout = getFileLayout(config);
        final File pwdFile = layout.getMasterPasswordFile();    
        FileUtils.deleteFile(pwdFile);
    }

    /**
     * Create the master password keystore at the default path. This routine can also modify the master password
     * if the keystore already exists.
     * @param config
     * @param masterPassword
     * @throws RepositoryException
     */
    @Deprecated
    protected void createMasterPasswordFile(RepositoryConfig config, String masterPassword)
        throws RepositoryException
    {
        this.createMasterPasswordFile(config, masterPassword, null);
    }

    /**
     * Create the master password keystore. This routine can also modify the master password
     * if the keystore already exists
     * @param config The config information.
     * @param masterPassword The password to set.
     * @param path The path to save the master password to.
     * @throws RepositoryException
     */
    protected void createMasterPasswordFile(RepositoryConfig config, String masterPassword, @Nullable String path)
        throws RepositoryException
    {
        final PEFileLayout layout = getFileLayout(config);
        File mpLocation = layout.getMasterPasswordLocationFile();
        if (path != null) {
            File potentialFolder = new File(path);
            if (potentialFolder.isDirectory()) {
                path = new File(potentialFolder, MASTERPASSWORD_FILE).getAbsolutePath();
            }

            try (FileWriter writer = new FileWriter(mpLocation)) {
                writer.write(path);
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE,
                    "Failed to write master-password-location file: ", e);
            }
        } else {
            if (mpLocation.exists()) {
                if (!mpLocation.delete()) {
                    Logger.getAnonymousLogger().log(Level.WARNING,
                        "Failed to delete old master-password-location file.");
                }
            }
        }

        final File pwdFile = layout.getMasterPasswordFile();
        try {                    
            PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(), getMasterPasswordPassword());
            p.setPasswordForAlias(MASTERPASSWORD_FILE, masterPassword.getBytes());
            FileProtectionUtility.chmod0600(pwdFile);
        } catch (Exception ex) {                        
            throw new RepositoryException(STRING_MANAGER.getString("masterPasswordFileNotCreated", pwdFile),
                ex);
        } 
    }
    
    /**
     * Return the master password stored in the master password keystore.
     * @param config
     * @throws RepositoryException
     * @return
     */    
    public String readMasterPasswordFile(
        RepositoryConfig config) throws RepositoryException
    {               
        final PEFileLayout layout = getFileLayout(config);
        final File pwdFile = layout.getMasterPasswordFile();        
        if (pwdFile.exists()) {            
            try {                    
                PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(), 
                    getMasterPasswordPassword());
                return p.getPasswordForAlias(MASTERPASSWORD_FILE);
            } catch (Exception ex) {            
                throw new RepositoryException(STRING_MANAGER.getString("masterPasswordFileNotRead", pwdFile),
                    ex);
            } 
        } else {
            //Return null if the password file does not exist.
            return null;
        }
    }

    /**
     * Changes the master password in the master password file
     * @param saveMasterPassword
     * @param config
     * @param newPassword
     * @throws RepositoryException
     */
    @Deprecated
    protected void changeMasterPasswordInMasterPasswordFile(RepositoryConfig config, String newPassword,
        boolean saveMasterPassword) throws RepositoryException
    {
        this.changeMasterPasswordInMasterPasswordFile(config, newPassword, saveMasterPassword, null);
    }

    /**
     * Changes the master password in the master password file
     * @param saveMasterPassword
     * @param config
     * @param newPassword
     * @throws RepositoryException
     */
    protected void changeMasterPasswordInMasterPasswordFile(RepositoryConfig config, String newPassword,
        boolean saveMasterPassword, @Nullable String newMPLocation) throws RepositoryException
    {
        deleteMasterPasswordFile(config);
        if (saveMasterPassword) {
            createMasterPasswordFile(config, newPassword, newMPLocation);
        }         
    }

    /**
     * Changes the master password in the master password file
     * @param pwdFile the master password file
     * @param newPassword the new password
     * @param saveMasterPassword whether to save the new password. If this is false then
     * the old file will simply be deleted
     * @throws RepositoryException
     */
    public void changeMasterPasswordInMasterPasswordFile(File pwdFile, String newPassword,
        boolean saveMasterPassword) throws RepositoryException 
    {
    	    FileUtils.deleteFile(pwdFile);
        if (saveMasterPassword) {
        	 try {                    
                 PasswordAdapter p = new PasswordAdapter(pwdFile.getAbsolutePath(), 
                     getMasterPasswordPassword());
                 p.setPasswordForAlias(MASTERPASSWORD_FILE, newPassword.getBytes());
                 FileProtectionUtility.chmod0600(pwdFile);
             } catch (Exception ex) {                        
                 throw new RepositoryException(STRING_MANAGER.getString("masterPasswordFileNotCreated", pwdFile),
                     ex);
             } 
        }         
    }
}
