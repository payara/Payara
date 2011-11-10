/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.Charset;
import java.security.*;
import com.sun.enterprise.security.auth.realm.User;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.IASRealm;
import com.sun.enterprise.security.util.*;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.util.Utility;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.SharedSecureRandom;
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

    // Separators in keyfile (user;pwd-info;group[,group]*)
    private static final String FIELD_SEP=";";
    private static final String GROUP_SEP=",";
    private static final String COMMENT="#";

    // Valid non-alphanumeric/whitespace chars in user/group name
    public static final String MISC_VALID_CHARS="_-.";
    
    // Number of bytes of salt for SSHA
    private static final int SALT_SIZE=8;
    
    // Contains cache of keyfile data
    private final Hashtable<String,FileRealmUser> userTable = new Hashtable<String, FileRealmUser>();  // user=>FileRealmUser
    private final Hashtable<String,Integer> groupSizeMap = new Hashtable<String, Integer>(); // maps of groups with value cardinality of group
    private static final String instanceRoot = getInstanceRoot();

    private static final String SSHA_TAG = "{SSHA}";
    private static final String SSHA_256_TAG = "{SSHA256}";
    private static final String algoSHA = "SHA";
    private static final String algoSHA256 = "SHA-256";
    private static final String resetKey = "RESET";

    //private boolean constructed = false;
    
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
        File fp = new File(keyfile);
                                // if not existent, try to create
        if (!fp.exists()) {
            FileOutputStream fout = null;
            try {
                fout =  new FileOutputStream(fp);
                fout.write("\n".getBytes());
            } catch (Exception e) {
                String msg = sm.getString("filerealm.noaccess", e.toString());
                throw new BadRealmException(msg);
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch(Exception ex) {
                        // ignore close exception
                    }
                }
            }
        }
        
        //constructed = true;
        Properties p = new Properties();
        p.setProperty(PARAM_KEYFILE, keyfile);
        p.setProperty(IASRealm.JAAS_CONTEXT_PARAM, "ignore");
        this.init(p);
    }

    
    /**
     * Constructor.
     *
     * <P>Do not use directly.
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
    protected void init(Properties props)
        throws BadRealmException, NoSuchRealmException
    {
        super.init(props);
        String file = props.getProperty(PARAM_KEYFILE);
        if (file == null) {
            String msg = sm.getString("filerealm.nofile");
            throw new BadRealmException(msg);
        }
        this.setProperty(PARAM_KEYFILE, file);
        
        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        if (jaasCtx == null) {
            String msg = sm.getString("filerealm.nomodule");
            throw new BadRealmException(msg);
        }
        this.setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("FileRealm : "+PARAM_KEYFILE+"="+file);
            _logger.fine("FileRealm : "+IASRealm.JAAS_CONTEXT_PARAM+"="+
                     jaasCtx);
        }
        
        loadKeyFile();
    }


    /**
     * Returns a short (preferably less than fifteen characters) description
     * of the kind of authentication which is supported by this realm.
     *
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
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
    public Enumeration<String> getUserNames()
         throws BadRealmException
    {
        return (new Vector(userTable.keySet())).elements(); // ugh
    }


    /**
     * Returns the information recorded about a particular named user.
     *
     * @param name Name of the user whose information is desired.
     * @return The user object.
     * @exception NoSuchUserException if the user doesn't exist.
     * @exception BadRealmException if realm data structures are bad.
     */
    public User getUser(String name)
        throws NoSuchUserException
    {
        FileRealmUser u = (FileRealmUser)userTable.get(name);
        if (u == null) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new NoSuchUserException(msg);
        }
        return u;
    }
    

    /**
     * Returns names of all the groups in this particular realm.
     * Note that this will not return assign-groups.
     *
     * @return enumeration of group names (strings)
     * @exception BadRealmException if realm data structures are bad
     */
    public Enumeration getGroupNames()
        throws BadRealmException
    {
        return groupSizeMap.keys();
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
    public Enumeration getGroupNames(String username)
        throws NoSuchUserException
    {
        FileRealmUser ud = (FileRealmUser)userTable.get(username);
        if (ud == null) {
            String msg = sm.getString("filerealm.nouser", username);
            throw new NoSuchUserException(msg);
        }

        String[] groups = ud.getGroups();
        groups = addAssignGroups(groups);
        Vector v = new Vector();
        if (groups != null) {
            for (int i = 0; i < groups.length; i++) {
               v.add(groups[i]);
            }
        }
        return v.elements();
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
    public void refresh()
         throws BadRealmException
    {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Reloading file realm data.");
        }

        FileRealm newRealm = new FileRealm();

        try {
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

        FileRealm newRealm = new FileRealm();

        try {
            newRealm.init(getProperties());
            Realm.updateInstance(configName, newRealm, this.getName());
        } catch (Exception e) {
            throw new BadRealmException(e.toString());
        }
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
        FileRealmUser ud = userTable.get(user);
        if (ud == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "No such user: [{0}]", user);
            }
            return null;
        }
        if (resetKey.equals(ud.getAlgo())) {
            _logger.log(Level.FINE, "File authentication failed for user [{0}]: password must be reset", user);
            return null;
        }

        boolean ok = false;

        try {
            
            ok = SSHA.verify(ud.getSalt(), ud.getHash(), Utility.convertCharArrayToByteArray(password, Charset.defaultCharset().displayName()), ud.getAlgo());

        } catch (Exception e) {
            _logger.log(Level.FINE, "File authentication failed for user [{0}]: {1}", 
                    new Object[] {user, e.toString()});
            return null;
        }

        if (!ok) {
            _logger.log(Level.FINE, "File authentication failed for: [{0}]", user);
            return null;
        }
        
        String[] groups = ud.getGroups();
        groups = addAssignGroups(groups);
        return groups;
    }

    /*
     * Test whether their is a user in the FileRealm that has a password that 
     * has been set, i.e., something other than the resetKey.
     */
    public boolean hasAuthenticatableUser() 
    {
        for (FileRealmUser ud : userTable.values()) {
            if (!resetKey.equals(ud.getAlgo())) {
                return true;
            }
        }
        return false;
    }

    //---------------------------------------------------------------------
    // File realm maintenance methods for admin.


    /**
     * Return false if any char of the string is not alphanumeric or space
     * or other permitted character.
     * For a username it will allow an @ symbol. To allow for the case of type
     * <i>username@foo.com</i>. It will not allow the same symbol for a group name 
     * @param String the name to be validated
     * @param boolean true if the string is a username, false if it is 
     * a group name
     * 
     */
    private static boolean isValid(String s, boolean userName)
    {
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c) &&
                !Character.isWhitespace(c) &&
                MISC_VALID_CHARS.indexOf(c) == -1) {
                if (userName && (c == '@')){
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    
    /**
     * Validates syntax of a user name.
     *
     * <P>This method throws an exception if the provided value is not
     * valid. The message of the exception provides a reason why it is
     * not valid. This is used internally by add/modify User to
     * validate the client-provided values. It is not necessary for
     * the client to call these methods first. However, these are
     * provided as public methods for convenience in case some client
     * (e.g. GUI client) wants to provide independent field validation
     * prior to calling add/modify user.
     *
     * @param name User name to validate.
     * @throws IASSecurityException Thrown if the value is not valid.
     *
     */
    public static void validateUserName(String name)
        throws IASSecurityException
    {
        if (name == null || name.length() == 0) {
            String msg = sm.getString("filerealm.noname");
            throw new IASSecurityException(msg);
        }

        if (!isValid(name, true)) {
            String msg = sm.getString("filerealm.badname", name);
            throw new IASSecurityException(msg);
        }

        if (!name.equals(name.trim())) {
            String msg = sm.getString("filerealm.badspaces", name);
            throw new IASSecurityException(msg);
        }
    }


    /**
     * Validates syntax of a password.
     *
     * <P>This method throws an exception if the provided value is not
     * valid. The message of the exception provides a reason why it is
     * not valid. This is used internally by add/modify User to
     * validate the client-provided values. It is not necessary for
     * the client to call these methods first. However, these are
     * provided as public methods for convenience in case some client
     * (e.g. GUI client) wants to provide independent field validation
     * prior to calling add/modify user.
     *
     * @param pwd Password to validate.
     * @throws IASSecurityException Thrown if the value is not valid.
     *
     */
    public static void validatePassword(char[] pwd)
        throws IASSecurityException
    {
        if (Arrays.equals(null, pwd)) {
            String msg = sm.getString("filerealm.emptypwd");
            throw new IASSecurityException(msg);
        }
        for(char c:pwd) {
            if (Character.isSpaceChar(c)) {
                String msg = sm.getString("filerealm.badspacespwd");
                throw new IASSecurityException(msg);
            }
        }
    }


    /**
     * Validates syntax of a group name.
     *
     * <P>This method throws an exception if the provided value is not
     * valid. The message of the exception provides a reason why it is
     * not valid. This is used internally by add/modify User to
     * validate the client-provided values. It is not necessary for
     * the client to call these methods first. However, these are
     * provided as public methods for convenience in case some client
     * (e.g. GUI client) wants to provide independent field validation
     * prior to calling add/modify user.
     *
     * @param group Group name to validate.
     * @throws IASSecurityException Thrown if the value is not valid.
     *
     */
    public static void validateGroupName(String group)
        throws IASSecurityException
    {
        if (group == null || group.length() == 0) {
            String msg = sm.getString("filerealm.nogroup");
            throw new IASSecurityException(msg);
        }

        if (!isValid(group, false)) {
            String msg = sm.getString("filerealm.badchars", group);
            throw new IASSecurityException(msg);
        }
        
        if (!group.equals(group.trim())) {
            String msg = sm.getString("filerealm.badspaces", group);
            throw new IASSecurityException(msg);
        }
    }

    
    /**
     * Validates syntax of a list of group names.
     *
     * <P>This is equivalent to calling validateGroupName on every element
     * of the groupList.
     *
     * @param groupList Array of group names to validate.
     * @throws IASSecurityException Thrown if the value is not valid.
     *     
     *
     */
    public static void validateGroupList(String[] groupList)
        throws IASSecurityException
    {
        if (groupList == null || groupList.length == 0) {
            return;             // empty list is ok
        }

        for (int i=0; i<groupList.length; i++) {
            validateGroupName(groupList[i]);
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
    public synchronized void addUser(String name, char[] password,
                        String[] groupList)
        throws BadRealmException, IASSecurityException
    {
        validateUserName(name);
        validatePassword(password);
        validateGroupList(groupList);
        
        if (userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.dupuser", name);
            throw new BadRealmException(msg);
        }

        addGroupNames(groupList);        
        FileRealmUser ud = createNewUser(name, password, groupList);
        userTable.put(name, ud);
    }
      

    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    public synchronized void removeUser(String name)
        throws NoSuchUserException, BadRealmException
    {
        if (!userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new NoSuchUserException(msg);
        }

        FileRealmUser oldUser = (FileRealmUser)userTable.get(name);
        userTable.remove(name);
        reduceGroups(oldUser.getGroups());
    }


    /**
     * Update data for an existing user. User must exist. This is equivalent
     * to calling removeUser() followed by addUser().
     *
     * @param name User name.
     * @param password Cleartext password for the user.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     * @throws NoSuchUserException If user does not exist.
     * @deprecated
     *
     */
    public synchronized void updateUser(String name, char[] password,
                           String[] groups)
        throws NoSuchUserException, BadRealmException,
                               IASSecurityException
    {
        updateUser(name, name, password, groups);
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
    public synchronized void updateUser(String name, String newName, char[] password,
                           String[] groups)
        throws NoSuchUserException, BadRealmException,
                               IASSecurityException
    {
                                // user to modify must exist first
        validateUserName(name);
        if (!userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new NoSuchUserException(msg);
        }

                                // do general validation
        validateUserName(newName);
        validateGroupList(groups);
        if (password != null) { // null here means re-use previous so is ok
            validatePassword(password);
        }
        
                                // can't duplicate unless modifying itself
        if (!name.equals(newName) && userTable.containsKey(newName)) {
            String msg = sm.getString("filerealm.dupuser", name);
            throw new BadRealmException(msg);
        }

        
        FileRealmUser oldUser = userTable.get(name);
        assert (oldUser != null);
        
                                // create user using new name
        FileRealmUser newUser = new FileRealmUser(newName);
        
        // set groups as provided by parameter
        // use old groups if no new groups given
        if (groups != null) {
            changeGroups(oldUser.getGroups(), groups);
            newUser.setGroups(groups);
        } else {
            newUser.setGroups(oldUser.getGroups());
        }
        
        // use old password if no new pwd given
        if (password==null) {
            newUser.setSalt(oldUser.getSalt());
            newUser.setHash(oldUser.getHash());
            newUser.setAlgo(oldUser.getAlgo());
            
        } else {
            setPassword(newUser, password);
            //ALways update passwords with SHA-256 algo
            newUser.setAlgo(algoSHA256);
        }        
        userTable.remove(name);
        userTable.put(newName, newUser);
    }
    
        
    /**
     * @return true if the realm implementation support User Management (add,remove,update user)
     */
    public boolean supportsUserManagement() {
        //File Realm supports UserManagement
        return true;
    }
    
    /**
     * Persist the realm data to permanent storage
     * @throws com.sun.enterprise.security.auth.realm.BadRealmException
     */
    public void persist() throws BadRealmException {
        String file = this.getProperty(PARAM_KEYFILE);
        try {
           writeKeyFile(file);
        } catch (IOException ex) {
            throw new BadRealmException(ex);
        }
    }
    
    /**
     * Write keyfile data out to disk. The file generation is sychronized
     * within this class only, caller is responsible for any other
     * file locking or revision management as deemed necessary.
     *
     * @param filename The name of the output file to create. 
     * @throws IOException If write fails.
     *
     */
    public void writeKeyFile(String filename)
         throws IOException
    {
        synchronized(FileRealm.class) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);

                for (Map.Entry<String, FileRealmUser> uval : userTable.entrySet()) {
                    String algo = uval.getValue().getAlgo();
                  //  if(algo == null) {
                    //    algo = algoSHA256;
                    //}
                    String entry = encodeUser(uval.getKey(), uval.getValue(),algo);
                    out.write(entry.getBytes());
                }
            } catch (IOException e) {
                throw e;

            } catch (Exception e) {
                String msg = sm.getString("filerealm.badwrite", e.toString());
                throw new IOException(msg);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Done writing " + filename);
        }
    }

    
    //---------------------------------------------------------------------
    // Private methods.

    
    /**
     * Add group names to the groups table. It is assumed all entries are
     * valid group names.
     *
     */
    private void addGroupNames(String[] groupList) {
        if (groupList != null) {
            for (int i=0; i < groupList.length; i++) {
                Integer groupSize = groupSizeMap.get(groupList[i]);
                groupSizeMap.put(groupList[i],Integer.valueOf((groupSize != null) ?
                    (groupSize.intValue() + 1): 1));
            }
        }
    }

    /**
     * This method reduces the group size by 1 and remove group name from
     * internal group list if resulting group size is 0.
     */
    private void reduceGroups(String[] groupList) {
        if (groupList != null) {
            for (int i=0; i < groupList.length; i++) {
                Integer groupSize = groupSizeMap.get(groupList[i]);
                if (groupSize != null) {
                    int gpSize = groupSize.intValue() - 1;
                    if (gpSize > 0) {
                        groupSizeMap.put(groupList[i], Integer.valueOf(gpSize));
                    } else {
                        groupSizeMap.remove(groupList[i]);
                    }
                }
            }
        }
    }

    /**
     * This method update the internal group list.
     */
    private void changeGroups(String[] oldGroupList, String[] newGroupList) {
        addGroupNames(newGroupList);
        reduceGroups(oldGroupList);
    }
    
    
    /**
     * Load keyfile from config and populate internal cache.
     *
     */
    private void loadKeyFile() throws BadRealmException
    {
        String file = this.getProperty(PARAM_KEYFILE);

        _logger.fine("Reading file realm: "+file);

        //Adding this feature of creating the empty keyfile
        // to satisfy some admin requirements.
        //allow the file creation only if it is inside glassfish instanceRoot
        File filePath = new File(file);
        if ((file != null) && !filePath.exists()) {
            try {
                if ((instanceRoot != null) && (filePath.getCanonicalPath().startsWith(instanceRoot))) {
                    if(!filePath.createNewFile()) {
                        throw new IOException();
                    }
                }
            } catch (IOException ex) {
                //ignore any exception, so the code below
                //will then throw No such file or directory
            }
        }

        BufferedReader input = null;
        
        try {
            if (Util.isEmbeddedServer()) {
                String embeddedFilePath = Util.writeConfigFileToTempDir(file).getAbsolutePath();
                this.setProperty(PARAM_KEYFILE, embeddedFilePath);
                input = new BufferedReader(new FileReader(embeddedFilePath));
            } else {
                input = new BufferedReader(new FileReader(file));
            }
            while (input.ready()) {
                
                String line = input.readLine();
                if (!line.startsWith(COMMENT) &&
                    line.indexOf(FIELD_SEP) > 0) {
                    FileRealmUser ud = decodeUser(line, groupSizeMap);
                    userTable.put(ud.getName(), ud);
                }
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING, "filerealm.readerror", e);
            throw new BadRealmException(e.toString());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch(Exception ex) {
                }
            }
        }
    }
    
    
    /**
     * Encodes one user entry containing info stored in FileRealmUser object.
     *
     * @param name User name.
     * @param ud User object containing info.
     * @returns String containing a line with encoded user data.
     * @throws IASSecurityException Thrown on failure.
     *
     */
    private static String encodeUser(String name, FileRealmUser ud, String algo)
    {
        StringBuffer sb = new StringBuffer();
        String cryptPwd = null;

        sb.append(name);
        sb.append(FIELD_SEP);
        if (resetKey.equals(algo)) {
            sb.append(resetKey);
        } else {
            String ssha = SSHA.encode(ud.getSalt(), ud.getHash(), algo);
            sb.append(ssha);
        }
        sb.append(FIELD_SEP);

        String[] groups = ud.getGroups();
        if (groups != null) {
            for (int grp = 0; grp < groups.length; grp++) {
                if (grp > 0) {
                    sb.append(GROUP_SEP);
                }
                sb.append((String)groups[grp]);
            }
        }
        sb.append("\n");
        return sb.toString();
    }


    /**
     * Decodes a line from the keyfile.
     *
     * @param encodedLine A line from the keyfile containing user data.
     * @param newGroupSizeMap Groups found in the encodedLine are added to
     *      this map.
     * @returns FileRealmUser Representing the loaded user.
     * @throws IASSecurityException Thrown on failure.
     *
     */
    private static FileRealmUser decodeUser(String encodedLine,
                                            Map newGroupSizeMap)
        throws IASSecurityException
    {
        StringTokenizer st = new StringTokenizer(encodedLine, FIELD_SEP);
        String algo  = algoSHA256;

        String user = null;
        String pwdInfo = null;
        String groupList = null;

        try {                   // these must be present
            user = st.nextToken();
            pwdInfo = st.nextToken();
        } catch (Exception e) {
            String msg = sm.getString("filerealm.syntaxerror", encodedLine);
            throw new IASSecurityException(msg);
        }
        
        if (st.hasMoreTokens()) { // groups are optional
            groupList = st.nextToken();
        }

        FileRealmUser ud = new FileRealmUser(user);
        if (resetKey.equals(pwdInfo)) {
            ud.setAlgo(resetKey);
        } else {

            if(encodedLine.contains(SSHA_TAG)) {
                algo = algoSHA;
            }

            int resultLength = 32;
            if (algoSHA.equals(algo)) {
                resultLength = 20;
            }

            byte[] hash = new byte[resultLength];
            byte[] salt = SSHA.decode(pwdInfo, hash, algo);

            ud.setHash(hash);
            ud.setSalt(salt);
            ud.setAlgo(algo);
        }
        
        Vector membership = new Vector();

        if (groupList != null) {
            StringTokenizer gst = new StringTokenizer(groupList,
                                                      GROUP_SEP);
            while (gst.hasMoreTokens()) {
                String g = gst.nextToken();
                membership.add(g);
                Integer groupSize = (Integer)newGroupSizeMap.get(g);
                newGroupSizeMap.put(g, Integer.valueOf((groupSize != null) ?
                    (groupSize.intValue() + 1) : 1));
            }
        }
        ud.setGroups(membership);
        
        return ud;
    }

    
    /**
     * Produce a user with given data.
     *
     * @param name User name.
     * @param pwd Cleartext password.
     * @param groups Group membership.
     * @returns FileRealmUser Representing the created user.
     * @throws IASSecurityException Thrown on failure.
     *
     */
    private static FileRealmUser createNewUser(String name, char[] pwd,
                                               String[] groups)
        throws IASSecurityException
    {
        FileRealmUser ud = new FileRealmUser(name);

        if (groups == null) {
            groups = new String[0];
        }
        ud.setGroups(groups);

        //Always create new users with SHA-256
        ud.setAlgo(algoSHA256);

        setPassword(ud, pwd);
     
        return ud;
    }


    /**
     * Sets the password in a user object. Of course the password is not
     * really stored so a salt is generated, hash computed, and these two
     * values are stored in the user object provided.
     *
     */
    private static void setPassword(FileRealmUser user, char[] pwd)
        throws IASSecurityException
    {
        assert (user != null);
        //Copy the password to another reference before storing it to the
        //instance field.
        byte[] pwdBytes = null;
        
        try {
            pwdBytes = Utility.convertCharArrayToByteArray(pwd, Charset.defaultCharset().displayName());
        } catch(Exception ex) {
            throw new IASSecurityException(ex);
        }
        
        SecureRandom rng=SharedSecureRandom.get();
        byte[] salt=new byte[SALT_SIZE];
        rng.nextBytes(salt);
        user.setSalt(salt);
        String algo = user.getAlgo();
        if(algo == null) {
            algo = algoSHA256;
        }

        byte[] hash = SSHA.compute(salt, pwdBytes, algo);
        user.setHash(hash);
    }



    /**
     * Test method. Not for production use.
     *
     */
/*    public static void main(String[] args)
    {
        if (args.length==0) {
            help();
        }

        try {
            if ("-c".equals(args[0])) {
                String[] groups=new String[0];
                if (args.length>3) {
                    groups=new String[args.length-3];
                    for (int i=3; i<args.length; i++) {
                        groups[i-3]=args[i];
                    }
                }
                FileRealmUser ud = createNewUser(args[1], args[2].toCharArray(), groups);
                String out=encodeUser(args[1], ud, algoSHA256);
                System.out.println(out);
                
                FileRealmUser u=decodeUser(out, new Hashtable());
                System.out.println("verifies: "+
                                   SSHA.verify(u.getSalt(), u.getHash(),
                                               args[2].getBytes(),algoSHA256));

            } else if ("-v".equals(args[0])) {
                FileRealmUser u=decodeUser(args[2], new Hashtable());
                System.out.println("user: "+u.getName());
                System.out.println("verifies: "+
                                   SSHA.verify(u.getSalt(), u.getHash(),
                                               args[1].getBytes(),algoSHA256));
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    } */
    /**
     * Show help for the test command line tool.
     *
     */
    private static void help()
    {
        System.out.println("FileRealm -c <name  <pwd  [group]*");
        System.out.println("FileRealm -v <pwd  `output of -c`");
        System.exit(1);
    }

    private static String getInstanceRoot() {
        try {
         ServerEnvironment se = (Globals.getDefaultHabitat() != null)?Globals.getDefaultHabitat().getComponent(ServerEnvironment.class):null;
         File fileInstanceRoot = (se == null)?null:se.getInstanceRoot();
         return (fileInstanceRoot != null)?fileInstanceRoot.getCanonicalPath():null;
        }
        catch(IOException e) {
            _logger.log(Level.FINE, "io_exception while getting the instanceRoot");
            return null;
        }
    }
}
