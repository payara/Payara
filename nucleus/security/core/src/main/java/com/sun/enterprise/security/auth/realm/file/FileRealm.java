/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.realm.file;

import java.util.*;
import java.util.logging.Level;
import java.io.*;
import com.sun.enterprise.security.auth.realm.User;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.IASRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.util.IASSecurityException;
import org.glassfish.internal.api.RelativePathResolver;
import org.glassfish.security.common.FileRealmHelper;
import org.jvnet.hk2.annotations.Service;


/**
 * Realm wrapper for supporting file password authentication.
 *
 * <P>In addition to the basic realm functionality, this class provides
 * administration methods for the file realm.
 *
 * <P>Format of the keyfile used by this class is one line per user
 * containing <code>username;password;groups</code> where:
 * <ul>
 *   <li>username - Name string.
 *   <li>password - A salted SHA hash (SSHA) of the user password.
 *   <li>groups - A comma separated list of group memberships.
 * </ul>
 *
 * <P>The file realm needs the following properties in its configuration:
 * <ul>
 *   <li>file - Full path to the keyfile to load
 *   <li>jaas-ctx - JAAS context name used to access LoginModule for
 *       authentication.
 * </ul>
 *
 * @author Harry Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 */
@Service
public final class FileRealm extends IASRealm
{
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "filepassword";

    // These are property names which should be in auth-realm in server.xml
    public static final String PARAM_KEYFILE="file";

    FileRealmHelper helper;
    
    /**
     * Constructor.
     *
     * <P>The created FileRealm instance is not registered in the
     * Realm registry. This constructor can be used by admin tools
     * to create a FileRealm instance which can be edited by adding or
     * removing users and then saved to disk, without affecting the
     * installed realm instance.
     *
     * <P>The file provided should always exist. A default (empty) keyfile
     * is installed with the server so this should always be the case
     * unless the user has manually deleted this file.  If this file
     * path provided does not point to an existing file this constructor
     * will first attempt to create it. If this succeeds the constructor
     * returns normally and an empty keyfile will have been created; otherwise
     * an exception is thrown.
     *
     * @param keyfile Full path to the keyfile to read for user data.
     * @exception BadRealmException If the configuration parameters
     *     identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters
     *     specify a realm which doesn't exist.
     *
     */
    public FileRealm(String keyfile)
         throws BadRealmException, NoSuchRealmException
    {
        Properties p = new Properties();
        // The original keyfile is stored here for use in the init and refresh methods
        p.setProperty(PARAM_KEYFILE, keyfile);
        p.setProperty(IASRealm.JAAS_CONTEXT_PARAM, "ignore");
        this.init(p);
    }

    /* 
     * No arg constructor used by the Realm class when creating realms. 
     * This is followed by a call to the init() method. 
     */
    public FileRealm()
    {
    }
    
    /**
     * Return a list of the file names used by all file realms
     * defined for the specified config.
     *
     * @param   config  the config object
     * @return          a list of the file names for all files realms in the
     *                  config
     */
    public static List<String> getRealmFileNames(Config config) {
        List<String> files = new ArrayList<String>();
        SecurityService securityService = config.getSecurityService();
        for (AuthRealm authRealm : securityService.getAuthRealm()) {
            String fileRealmClassName = authRealm.getClassname();
            // skip it if it's not a file realm
            if (fileRealmClassName == null ||
                    !fileRealmClassName.equals(FileRealm.class.getName()))
                continue;
            String file = authRealm.getPropertyValue("file");
            if (file == null)           // skip if no "file" property
                continue;
            if (file.contains("$")) {
                file = RelativePathResolver.resolvePath(file);
            }
            files.add(file);
        }
        return files;
    }

    
    /**
     * Initialize a realm with some properties.  This can be used
     * when instantiating realms from their descriptions.  This
     * method is invoked from Realm during initialization.
     *
     * @param props Initialization parameters used by this realm.
     * @exception BadRealmException If the configuration parameters
     *     identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters
     *     specify a realm which doesn't exist.
     *
     */
    @Override
    protected void init(Properties props)
        throws BadRealmException, NoSuchRealmException
    {
        super.init(props);
        String file = props.getProperty(PARAM_KEYFILE);
        if (file == null) {
            String msg = sm.getString("filerealm.nofile");
            throw new BadRealmException(msg);
        }
        if (file.contains("$")) {
                file = RelativePathResolver.resolvePath(file);
        }
        
        this.setProperty(PARAM_KEYFILE, file);
        
        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        if (jaasCtx == null) {
            String msg = sm.getString("filerealm.nomodule");
            throw new BadRealmException(msg);
        }
        this.setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);

        _logger.log(Level.FINE, "FileRealm : "+ PARAM_KEYFILE + "={0}", file);
        _logger.log(Level.FINE, "FileRealm : "+ IASRealm.JAAS_CONTEXT_PARAM + "={0}", jaasCtx);
       try {
            if (Util.isEmbeddedServer()) {
                String embeddedFilePath = Util.writeConfigFileToTempDir(file).getAbsolutePath();
                file = embeddedFilePath;
            }
            helper = new FileRealmHelper(file);
        } catch (IOException ioe) {
            String msg = sm.getString("filerealm.noaccess", ioe.toString());
            throw new BadRealmException(msg);
        }
    }


    /**
     * Returns a short (preferably less than fifteen characters) description
     * of the kind of authentication which is supported by this realm.
     *
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
    @Override
    public String getAuthType()
    {
        return AUTH_TYPE;
    }
    

    /**
     * Returns names of all the users in this particular realm.
     *
     * @return enumeration of user names (strings)
     * @exception BadRealmException if realm data structures are bad
     */
    @Override
    public Enumeration<String> getUserNames()
         throws BadRealmException
    {
        return Collections.enumeration(helper.getUserNames());
    }


    /**
     * Returns the information recorded about a particular named user.
     *
     * @param name Name of the user whose information is desired.
     * @return The user object.
     * @exception NoSuchUserException if the user doesn't exist.
     * @exception BadRealmException if realm data structures are bad.
     */
    @Override
    public User getUser(String name)
        throws NoSuchUserException
    {
        
        FileRealmHelper.User u = helper.getUser(name);
        if (u == null) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new NoSuchUserException(msg);
        }
        return new FileRealmUser(u, null);
    }
    

    /**
     * Returns names of all the groups in this particular realm.
     * Note that this will not return assign-groups.
     *
     * @return enumeration of group names (strings)
     * @exception BadRealmException if realm data structures are bad
     */
    @Override
    public Enumeration getGroupNames()
        throws BadRealmException
    {
        return Collections.enumeration(helper.getGroupNames());
    }

    
    /**
     * Returns the name of all the groups that this user belongs to.
     * @param username Name of the user in this realm whose group listing
     *     is needed.
     * @return Enumeration of group names (strings).
     * @exception InvalidOperationException thrown if the realm does not
     *     support this operation - e.g. Certificate realm does not support
     *     this operation.
     */
    @Override
    public Enumeration getGroupNames(String username)
        throws NoSuchUserException
    {
        String groups[] = helper.getGroupNames(username);
        if ( groups == null ) {
          groups = new String[]{};
        }
        groups = addAssignGroups(groups);
        return Collections.enumeration(Arrays.asList(groups));
    }

    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * <P>A new FileRealm instance is created and initialized from the
     * keyfile on disk. The new instance is installed in the Realm registry
     * so future Realm.getInstance() calls will obtain the new data. Any
     * existing references to this instance (e.g. in active LoginModule
     * sessions) are unaffected.
     *
     * @exception BadRealmException if realm data structures are bad
     *
     */
    @Override
    public void refresh()
         throws BadRealmException
    {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Reloading file realm data.");
        }

        try {
            FileRealm newRealm = new FileRealm(getProperty(PARAM_KEYFILE));
            newRealm.init(getProperties());
            Realm.updateInstance(newRealm, this.getName());
        } catch (Exception e) {
            throw new BadRealmException(e.toString());
        }
    }

    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * <P>A new FileRealm instance is created and initialized from the
     * keyfile on disk. The new instance is installed in the Realm registry
     * so future Realm.getInstance() calls will obtain the new data. Any
     * existing references to this instance (e.g. in active LoginModule
     * sessions) are unaffected.
     * @param config
     * @exception BadRealmException if realm data structures are bad
     *
     */
    @Override
    public void refresh(String configName)
            throws BadRealmException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Reloading file realm data.");
        }
        
        try {
            FileRealm newRealm = new FileRealm(getProperty(PARAM_KEYFILE));
            newRealm.init(getProperties());
            Realm.updateInstance(configName, newRealm, this.getName());
        } catch (Exception e) {
            throw new BadRealmException(e.toString());
        }
    }

     /**
     * Adds new user to file realm. User cannot exist already.
     *
     * @param name User name.
     * @param password Cleartext password for the user.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     *
     */
    @Override
    public  void addUser(String name, char[] password, String[] groupList)
        throws BadRealmException, IASSecurityException  {
        helper.addUser(name, password, groupList);
    }

    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    @Override
     public void removeUser(String name)
        throws NoSuchUserException, BadRealmException {
        helper.removeUser(name);
     }

   /**
     * Update data for an existing user. User must exist.
     *
     * @param name Current name of the user to update.
     * @param newName New name to give this user. It can be the same as
     *     the original name. Otherwise it must be a new user name which
     *     does not already exist as a user.
     * @param password Cleartext password for the user. If non-null the user
     *     password is changed to this value. If null, the original password
     *     is retained.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    @Override
    public void updateUser(String name, String newName, char[] password,
                           String[] groups)
        throws NoSuchUserException, BadRealmException,
                               IASSecurityException {
        helper.updateUser(name, newName, password, groups);
    }


    /**
     * Authenticates a user.
     *
     * <P>This method is invoked by the FileLoginModule in order to
     * authenticate a user in the file realm. The authentication decision
     * is kept within the realm class implementation in order to keep
     * the password cache in a single location with no public accessors,
     * to simplify future improvements.
     *
     * @param user Name of user to authenticate.
     * @param password Password provided by client.
     * @returns Array of group names the user belongs to, or null if
     *      authentication fails.
     * @throws LoginException If there are errors during authentication.
     *
     */
    public String[] authenticate(String user, char[] password)
    {
        String groups[] = helper.authenticate(user, password);
        if (groups != null) {
            groups = addAssignGroups(groups);
        }
        return groups;
    }

    /*
     * Test whether their is a user in the FileRealm that has a password that 
     * has been set, i.e., something other than the resetKey.
     */
    public boolean hasAuthenticatableUser() 
    {
        return helper.hasAuthenticatableUser();
    }
      
    /**
     * @return true if the realm implementation support User Management (add,remove,update user)
     */
    @Override
    public boolean supportsUserManagement() {
        //File Realm supports UserManagement
        return true;
    }
    
    /**
     * Persist the realm data to permanent storage
     * @throws com.sun.enterprise.security.auth.realm.BadRealmException
     */
    @Override
    public void persist() throws BadRealmException {      
        try {
           helper.persist();
        } catch (IOException ex) {
            throw new BadRealmException(ex);
        }
    }    
}
