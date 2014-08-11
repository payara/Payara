/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.common;

import com.sun.enterprise.util.Utility;
import com.sun.enterprise.util.i18n.StringManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * Helper class for implementing file password authentication.
 *
 * <P>This class provides administration methods for the file realm. It is
 * used by the FileRealm class to provide the FileRealm functionality.  But
 * some administration classes that need direct access to the File without 
 * using the security module use this class directly. 
 *
 * <P>Format of the keyfile used by this class is one line per user
 * containing <code>username;password;groups</code> where:
 * <ul>
 *   <li>username - Name string.
 *   <li>password - A salted SHA hash (SSHA) of the user password or "RESET"
 *   <li>groups - A comma separated list of group memberships.
 * </ul>
 * <P>If the password is "RESET", then the password must be reset before
 * that user can authenticate.
 *
 * <P>The file realm needs the following properties in its configuration:
 * <ul>
 *   <li>file - Full path to the keyfile to load
 *   <li>jaas-ctx - JAAS context name used to access LoginModule for
 *       authentication.
 * </ul>
 *
 * @author Tom Mueller
 */

public final class FileRealmHelper
{
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
    private final HashMap<String,User> userTable = new HashMap<String, User>();  // user=>FileRealmUser
    private final HashMap<String,Integer> groupSizeMap = new HashMap<String, Integer>(); // maps of groups with value cardinality of group
    private File keyfile;
    private String keyfileName;

    private static final String SSHA_TAG = "{SSHA}";
    private static final String SSHA_256_TAG = "{SSHA256}";
    private static final String algoSHA = "SHA";
    private static final String algoSHA256 = "SHA-256";
    private static final String resetKey = "RESET";
    private static final StringManager sm =
        StringManager.getManager(FileRealmHelper.class);
    /**
     * Constructor.
     *
     * <P>The created FileRealmHelper instance is not registered in the
     * Realm registry. This constructor can be used by admin tools
     * to create a FileRealmHelper instance which can be edited by adding or
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
     * @exception IOException If the configuration parameters
     *     identify a corrupt keyfile
     */
    public FileRealmHelper(String keyfileName) throws IOException
    {
        this.keyfileName = keyfileName;
        keyfile = new File(keyfileName);
        // if not existent, try to create
        if (!keyfile.exists()) {
            if (keyfile.createNewFile() == false) {
                throw new IOException(sm.getString("filerealm.badwrite", keyfileName));
            }
        }
        loadKeyFile();
    }
    
    /**
     * Returns names of all the users in this particular realm.
     *
     * @return enumeration of user names (strings)
     */
    public Set<String> getUserNames()
    {  
        return userTable.keySet();
    }

    /**
     * Returns the information recorded about a particular named user.
     *
     * @param name Name of the user whose information is desired.
     * @return The user object, or null if the user doesn't exist.
     */
    public FileRealmHelper.User getUser(String name)
    {
        return userTable.get(name);
    }
    

    /**
     * Returns names of all the groups in this particular realm.
     * Note that this will not return assign-groups.
     *
     * @return enumeration of group names (strings)
     */
    public Set<String> getGroupNames()
    {
        return groupSizeMap.keySet();
    }

    
    /**
     * Returns the name of all the groups that this user belongs to.
     * @param username Name of the user in this realm whose group listing
     *     is needed.
     * @return Array of group names (strings) or null if the user does
     *     not exist.
     */
    public String[] getGroupNames(String username)
    {
        User ud = (User)userTable.get(username);
        if (ud == null) {
            return null;
        }
        return ud.getGroups();
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
     */
    public String[] authenticate(String user, char[] password)
    {
        User ud = userTable.get(user);
        if (ud == null) {
            return null;
        }
        if (resetKey.equals(ud.getAlgo())) {
             return null;
        }

        boolean ok = false;

        try {           
            ok = SSHA.verify(ud.getSalt(), ud.getHash(), 
                    Utility.convertCharArrayToByteArray(password, Charset.defaultCharset().displayName()), 
                    ud.getAlgo());

        } catch (Exception e) {
            return null;
        }

        if (!ok) {
            return null;
        }
        
        return ud.getGroups();
    }

    /*
     * Test whether their is a user in the FileRealm that has a password that 
     * has been set, i.e., something other than the resetKey.
     */
    public boolean hasAuthenticatableUser() 
    {
        for (User ud : userTable.values()) {
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
     * @throws IllegalArgumentException Thrown if the value is not valid.
     *
     */
    public static void validateUserName(String name)
        throws IllegalArgumentException
    {
        if (name == null || name.length() == 0) {
            String msg = sm.getString("filerealm.noname");
            throw new IllegalArgumentException(msg);
        }

        if (!isValid(name, true)) {
            String msg = sm.getString("filerealm.badname", name);
            throw new IllegalArgumentException(msg);
        }

        if (!name.equals(name.trim())) {
            String msg = sm.getString("filerealm.badspaces", name);
            throw new IllegalArgumentException(msg);
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
     * @throws IllegalArgumentException Thrown if the value is not valid.
     *
     */
    public static void validatePassword(char[] pwd)
        throws IllegalArgumentException
    {
        if (Arrays.equals(null, pwd)) {
            String msg = sm.getString("filerealm.emptypwd");
            throw new IllegalArgumentException(msg);
        }
        for(char c:pwd) {
            if (Character.isSpaceChar(c)) {
                String msg = sm.getString("filerealm.badspacespwd");
                throw new IllegalArgumentException(msg);
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
     * @throws IllegalArgumentException Thrown if the value is not valid.
     *
     */
    public static void validateGroupName(String group)
        throws IllegalArgumentException
    {
        if (group == null || group.length() == 0) {
            String msg = sm.getString("filerealm.nogroup");
            throw new IllegalArgumentException(msg);
        }

        if (!isValid(group, false)) {
            String msg = sm.getString("filerealm.badchars", group);
            throw new IllegalArgumentException(msg);
        }
        
        if (!group.equals(group.trim())) {
            String msg = sm.getString("filerealm.badspaces", group);
            throw new IllegalArgumentException(msg);
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
        throws IllegalArgumentException
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
     * @throws IllegalArgumentException If there are problems adding user.
     *
     */
    public synchronized void addUser(String name, char[] password,
                        String[] groupList)
        throws IllegalArgumentException
    {
        validateUserName(name);
        validatePassword(password);
        validateGroupList(groupList);
        
        if (userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.dupuser", name);
            throw new IllegalArgumentException(msg);
        }

        addGroupNames(groupList);        
        User ud = createNewUser(name, password, groupList);
        userTable.put(name, ud);
    }
      

    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     * @throws IllegalArgumentException If user does not exist.
     *
     */
    public synchronized void removeUser(String name)
        throws IllegalArgumentException
    {
        if (!userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new IllegalArgumentException(msg);
        }

        User oldUser = (User)userTable.get(name);
        userTable.remove(name);
        reduceGroups(oldUser.getGroups());
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
     * @throws IllegalArgumentException If there are problems adding user.
     */
    public synchronized void updateUser(String name, String newName, char[] password,
                           String[] groups)
        throws IllegalArgumentException
    {
                                // user to modify must exist first
        validateUserName(name);
        if (!userTable.containsKey(name)) {
            String msg = sm.getString("filerealm.nouser", name);
            throw new IllegalArgumentException(msg);
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
            throw new IllegalArgumentException(msg);
        }

        
        User oldUser = userTable.get(name);
        assert (oldUser != null);
        
                                // create user using new name
        User newUser = new User(newName);
        
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
            //If the original algo was RESET, change it to
            //the default - SHA-256
            if(oldUser.getAlgo().equals(resetKey)) {
                newUser.setAlgo(algoSHA256);
            }
            else {
                newUser.setAlgo(oldUser.getAlgo());
            }
            
        } else {
            setPassword(newUser, password);
            //ALways update passwords with SHA-256 algo
            newUser.setAlgo(algoSHA256);
        }        
        userTable.remove(name);
        userTable.put(newName, newUser);
    }
        
    /**
     * Write keyfile data out to disk. The file generation is sychronized
     * within this class only, caller is responsible for any other
     * file locking or revision management as deemed necessary. 
     * @throws IOException if there is a failure
     */
    public void persist() throws IOException {
        synchronized (FileRealmHelper.class) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(keyfile);

                for (Map.Entry<String, User> uval : userTable.entrySet()) {
                    String algo = uval.getValue().getAlgo();
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
    private void loadKeyFile() throws IOException
    {
        BufferedReader input = null;
        
        try {
            input = new BufferedReader(new FileReader(keyfile));
            while (input.ready()) {
                
                String line = input.readLine();
                if (line != null &&
                        !line.startsWith(COMMENT) &&
                        line.indexOf(FIELD_SEP) >= 0) {
                    User ud = decodeUser(line, groupSizeMap);
                    userTable.put(ud.getName(), ud);
                }
            }
        } catch (Exception e) {  
            throw new IOException(e.toString());
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
    private static String encodeUser(String name, User ud, String algo)
    {
        StringBuilder sb = new StringBuilder();
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
     * @throws IllegalArgumentException Thrown on failure.
     *
     */
    private static User decodeUser(String encodedLine,
                                            Map newGroupSizeMap)
        throws IllegalArgumentException
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
            throw new IllegalArgumentException(msg);
        }
        
        if (st.hasMoreTokens()) { // groups are optional
            groupList = st.nextToken();
        }

        User ud = new User(user);
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
        
        List<String> membership = new ArrayList<String>();

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
        ud.setGroups(membership.toArray(new String[membership.size()]));
        
        return ud;
    }

    
    /**
     * Produce a user with given data.
     *
     * @param name User name.
     * @param pwd Cleartext password.
     * @param groups Group membership.
     * @returns FileRealmUser Representing the created user.
     *
     */
    private static User createNewUser(String name, char[] pwd,
                                               String[] groups)
    {
        User ud = new User(name);

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
    private static void setPassword(User user, char[] pwd)
        throws IllegalArgumentException
    {
        assert (user != null);
        //Copy the password to another reference before storing it to the
        //instance field.
        byte[] pwdBytes = null;
        
        try {
            pwdBytes = Utility.convertCharArrayToByteArray(pwd, Charset.defaultCharset().displayName());
        } catch(Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        
        SecureRandom rng = SharedSecureRandomImpl.get();
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
     * Represents a FileRealm user.
     */
    public static class User extends PrincipalImpl
    {
        private String[] groups;
        private String realm;
        private byte[] salt;
        private byte[] hash;
        private String algo;

        /**
         * Constructor.
         *
         */
        public User(String name)
        {
            super(name);
        }

        /**
         * Constructor.
         *
         * @param name User name.
         * @param groups Group memerships.
         * @param realm Realm.
         * @param salt SSHA salt.
         * @param hash SSHA password hash.
         *
         */
        public User(String name, String[] groups, String realm,
                             byte[] salt, byte[] hash, String algo)
        {
            super(name);
            this.groups = groups;
            this.realm = realm;
            this.hash = hash;
            this.salt = salt;
            this.algo = algo;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final User other = (User) obj;
            if (!Arrays.deepEquals(this.groups, other.groups)) {
                return false;
            }
            if ((this.realm == null) ? (other.realm != null) : !this.realm.equals(other.realm)) {
                return false;
            }
            if (!Arrays.equals(this.salt, other.salt)) {
                return false;
            }
            if (!Arrays.equals(this.hash, other.hash)) {
                return false;
            }
            if ((this.algo == null) ? (other.algo != null) : !this.algo.equals(other.algo)) {
                return false;
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            int hc = 5;
            hc = 17 * hc + Arrays.deepHashCode(this.groups);
            hc = 17 * hc + (this.realm != null ? this.realm.hashCode() : 0);
            hc = 17 * hc + Arrays.hashCode(this.salt);
            hc = 17 * hc + Arrays.hashCode(this.hash);
            hc = 17 * hc + (this.algo != null ? this.algo.hashCode() : 0);
            hc = 17 * super.hashCode();
            return hc;
        }

        /**
         * Returns salt value.
         *
         */
        public byte[] getSalt()
        {
            return salt;
        }

        /**
         * Set salt value.
         *
         */
        public void setSalt(byte[] salt)
        {
            this.salt = salt;
        }

        /**
         * Get hash value.
         *
         */
        public byte[] getHash()
        {
            return hash;
        }

        /**
         * Set hash value.
         *
         */
        public void setHash(byte[] hash)
        {
            this.hash = hash;
        }

        /**
         * Return the names of the groups this user belongs to.
         *
         * @return String[] List of group memberships.
         *
         */
        public String[] getGroups()
        {
            return groups;
        }

        /**
         * Set group membership.
         */
        public void setGroups(String[] grp)
        {
            this.groups = grp;
        }

        /**
         * @return the algo
         */
        public String getAlgo() {
            return algo;
        }

        /**
         * @param algo the algo to set
         */
        public void setAlgo(String algo) {
            this.algo = algo;
        }
    }
}
