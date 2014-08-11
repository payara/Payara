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

package com.sun.enterprise.security.auth.realm.jdbc;

import java.nio.charset.CharacterCodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import javax.sql.DataSource;
//import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;

import com.sun.enterprise.universal.GFBase64Encoder;

import javax.security.auth.login.LoginException;
import com.sun.enterprise.security.auth.realm.IASRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.api.Password;
import com.sun.enterprise.security.ee.auth.realm.DigestRealmBase;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.util.Utility;

import java.io.Reader;
import java.util.Arrays;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;

/**
 * Realm for supporting JDBC authentication.
 *
 * <P>The JDBC realm needs the following properties in its configuration:
 * <ul>
 *   <li>jaas-context : JAAS context name used to access LoginModule for
 *       authentication (for example JDBCRealm).
 *   <li>datasource-jndi : jndi name of datasource
 *   <li>db-user : user name to access the datasource
 *   <li>db-password : password to access the datasource
 *   <li>digest: digest mechanism
 *   <li>charset: charset encoding
 *   <li>user-table: table containing user name and password
 *   <li>group-table: table containing user name and group name
 *   <li>user-name-column: column corresponding to user name in user-table and group-table
 *   <li>password-column : column corresponding to password in user-table
 *   <li>group-name-column : column corresponding to group in group-table
 * </ul>
 *
 * @see com.sun.enterprise.security.auth.login.SolarisLoginModule
 *
 */
@Service
public final class JDBCRealm extends DigestRealmBase {
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "jdbc";
    public static final String PRE_HASHED = "HASHED";
    public static final String PARAM_DATASOURCE_JNDI = "datasource-jndi";
    public static final String PARAM_DB_USER = "db-user";
    public static final String PARAM_DB_PASSWORD = "db-password";

    public static final String PARAM_DIGEST_ALGORITHM = "digest-algorithm";
    public static final String NONE = "none";

    public static final String PARAM_ENCODING = "encoding";
    public static final String HEX = "hex";
    public static final String BASE64 = "base64";
    public static final String DEFAULT_ENCODING = HEX; // for digest only

    public static final String PARAM_CHARSET = "charset";
    public static final String PARAM_USER_TABLE = "user-table";
    public static final String PARAM_USER_NAME_COLUMN = "user-name-column";
    public static final String PARAM_PASSWORD_COLUMN = "password-column";
    public static final String PARAM_GROUP_TABLE = "group-table";
    public static final String PARAM_GROUP_NAME_COLUMN = "group-name-column";
    public static final String PARAM_GROUP_TABLE_USER_NAME_COLUMN = "group-table-user-name-column";

    private static final char[] HEXADECIMAL = { '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private Map<String, Vector> groupCache;
    private Vector<String> emptyVector;
    private String passwordQuery = null;
    private String groupQuery = null;
    private MessageDigest md = null;


    
    private ActiveDescriptor<ConnectorRuntime> cr ;
    
    
    /**
     * Initialize a realm with some properties.  This can be used
     * when instantiating realms from their descriptions.  This
     * method may only be called a single time.  
     *
     * @param props Initialization parameters used by this realm.
     * @exception BadRealmException If the configuration parameters
     *     identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters
     *     specify a realm which doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public synchronized void init(Properties props)
            throws BadRealmException, NoSuchRealmException{
        super.init(props);
        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        String dbUser = props.getProperty(PARAM_DB_USER);
        String dbPassword = props.getProperty(PARAM_DB_PASSWORD);
        String dsJndi = props.getProperty(PARAM_DATASOURCE_JNDI);
        String digestAlgorithm = props.getProperty(PARAM_DIGEST_ALGORITHM,
            getDefaultDigestAlgorithm());
        String encoding = props.getProperty(PARAM_ENCODING);
        String charset = props.getProperty(PARAM_CHARSET);
        String userTable = props.getProperty(PARAM_USER_TABLE);
        String userNameColumn = props.getProperty(PARAM_USER_NAME_COLUMN);
        String passwordColumn = props.getProperty(PARAM_PASSWORD_COLUMN);
        String groupTable = props.getProperty(PARAM_GROUP_TABLE);
        String groupNameColumn = props.getProperty(PARAM_GROUP_NAME_COLUMN);
        String groupTableUserNameColumn = props.getProperty(PARAM_GROUP_TABLE_USER_NAME_COLUMN,userNameColumn);
        cr = (ActiveDescriptor<ConnectorRuntime>)
                Util.getDefaultHabitat().getBestDescriptor(BuilderHelper.createContractFilter(ConnectorRuntime.class.getName()));
        
        if (jaasCtx == null) {
            String msg = sm.getString(
                "realm.missingprop", IASRealm.JAAS_CONTEXT_PARAM, "JDBCRealm");
            throw new BadRealmException(msg);
        }

        if (dsJndi == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_DATASOURCE_JNDI, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        if (userTable == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_USER_TABLE, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        if (groupTable == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_GROUP_TABLE, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        if (userNameColumn == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_USER_NAME_COLUMN, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        if (passwordColumn == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_PASSWORD_COLUMN, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        if (groupNameColumn == null) {
            String msg = sm.getString(
                "realm.missingprop", PARAM_GROUP_NAME_COLUMN, "JDBCRealm");
            throw new BadRealmException(msg);
        }
        
        passwordQuery = "SELECT " + passwordColumn + " FROM " + userTable +
            " WHERE " + userNameColumn + " = ?";

        groupQuery = "SELECT " + groupNameColumn + " FROM " + groupTable +
            " WHERE " + groupTableUserNameColumn + " = ? ";

        if (!NONE.equalsIgnoreCase(digestAlgorithm)) {
            try {
                md = MessageDigest.getInstance(digestAlgorithm);
            } catch(NoSuchAlgorithmException e) {
                String msg = sm.getString("jdbcrealm.notsupportdigestalg",
                    digestAlgorithm);
                throw new BadRealmException(msg);
            }
        }
        if (md != null && encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        this.setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);
        if (dbUser != null && dbPassword != null) {
            this.setProperty(PARAM_DB_USER, dbUser);
            this.setProperty(PARAM_DB_PASSWORD, dbPassword);
        }
        this.setProperty(PARAM_DATASOURCE_JNDI, dsJndi);
        this.setProperty(PARAM_DIGEST_ALGORITHM, digestAlgorithm);
        if (encoding != null) {
            this.setProperty(PARAM_ENCODING, encoding);
        }
        if (charset != null) {
            this.setProperty(PARAM_CHARSET, charset);
        }

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("JDBCRealm : " + 
                IASRealm.JAAS_CONTEXT_PARAM + "= " + jaasCtx + ", " +
                PARAM_DATASOURCE_JNDI + " = " + dsJndi + ", " +
                PARAM_DB_USER + " = " + dbUser + ", " +
                PARAM_DIGEST_ALGORITHM + " = " + digestAlgorithm + ", " +
                PARAM_ENCODING + " = " + encoding + ", " +
                PARAM_CHARSET + " = " + charset);
        }

        groupCache = new HashMap<String, Vector>();
        emptyVector = new Vector<String>();
    }

    /**
     * Returns a short (preferably less than fifteen characters) description
     * of the kind of authentication which is supported by this realm.
     *
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
    public String getAuthType(){
        return AUTH_TYPE;
    }

    /**
     * Returns the name of all the groups that this user belongs to.
     * It loads the result from groupCache first.
     * This is called from web path group verification, though
     * it should not be.
     *
     * @param username Name of the user in this realm whose group listing
     *     is needed.
     * @return Enumeration of group names (strings).
     * @exception InvalidOperationException thrown if the realm does not
     *     support this operation - e.g. Certificate realm does not support
     *     this operation.
     */
    public Enumeration getGroupNames(String username)
            throws InvalidOperationException, NoSuchUserException {
        Vector vector = groupCache.get(username);
        if (vector == null) {
            String[] grps = findGroups(username);
            setGroupNames(username, grps);
            vector = groupCache.get(username);
        }
        return vector.elements();
    }

    private void setGroupNames(String username, String[] groups) {
        Vector<String> v = null;
        
        if (groups == null) {
            v = emptyVector;

        } else {
            v = new Vector<String>(groups.length + 1);
            for (int i=0; i<groups.length; i++) {
                v.add(groups[i]);
            }
        }
        
        synchronized (this) {
            groupCache.put(username, v);
        }
    }


    /**
     * Invoke the native authentication call.
     *
     * @param username User to authenticate.
     * @param password Given password.
     * @returns true of false, indicating authentication status.
     *
     */
    public String[] authenticate(String username, char[] password) {
        String[] groups = null;
        if (isUserValid(username, password)) {
            groups = findGroups(username);
            groups = addAssignGroups(groups);
            setGroupNames(username, groups);
        }
        return groups;
    }

    @Override
    public boolean validate(String username, DigestAlgorithmParameter[] params) {
        final Password pass = getPassword(username);
        if (pass == null) {
            return false;
        }
        return validate(pass, params);
    }
    
    private Password getPassword(String username) {

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        boolean valid = false;

        try {
            connection = getConnection();
            statement = connection.prepareStatement(passwordQuery);
            statement.setString(1, username);
            rs = statement.executeQuery();

            if (rs.next()) {
                final String pwd = rs.getString(1);
                if (!PRE_HASHED.equalsIgnoreCase(getProperty(PARAM_ENCODING))) {
                    return new Password() {

                        public byte[] getValue() {
                            return pwd.getBytes();
                        }

                        public int getType() {
                            return Password.PLAIN_TEXT;
                        }
                    };
                } else {
                    return new Password() {

                        public byte[] getValue() {
                            return pwd.getBytes();
                        }

                        public int getType() {
                            return Password.HASHED;
                        }
                    };
                }
            }
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "jdbcrealm.invaliduser", username);
            _logger.log(Level.SEVERE, null, ex);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Cannot validate user", ex);
            }
        } finally {
            close(connection, statement, rs);
        }
        return null;

    }


    /**
     * Test if a user is valid
     * @param user user's identifier
     * @param password user's password
     * @return true if valid
     */
    private boolean isUserValid(String user, char[] password) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        boolean valid = false;

        try {
            char[] hpwd = hashPassword(password);
            connection = getConnection();
            statement =  connection.prepareStatement(passwordQuery);
            statement.setString(1, user);
            rs = statement.executeQuery();
            if (rs.next()) {
                //Obtain the password as a char[] with a  max size of 50
                Reader reader =  rs.getCharacterStream(1);
                char[] pwd = new char[1024];
                int noOfChars = reader.read(pwd);

                /*Since pwd contains 1024 elements arbitrarily initialized,
                    construct a new char[] that has the right no of char elements
                    to be used for equal comparison*/
                if (noOfChars < 0) {
                    noOfChars = 0;
                }
                char[] passwd = new char[noOfChars];
                System.arraycopy(pwd, 0, passwd, 0, noOfChars);
                if (HEX.equalsIgnoreCase(getProperty(PARAM_ENCODING))) {
                    valid = true;
                    //Do a case-insensitive equals
                    for(int i = 0; i < noOfChars; i ++) {
                        if (!(Character.toLowerCase(passwd[i]) == Character.toLowerCase(hpwd[i]))) {
                            valid = false;
                            break;
                        }
                    }
                } else {
                    valid = Arrays.equals(passwd, hpwd);
                }
            }
        } catch(SQLException ex) {
                _logger.log(Level.SEVERE, "jdbcrealm.invaliduserreason", 
                        new String [] {user,ex.toString()});
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Cannot validate user", ex);
            } 
        } catch(Exception ex) {
            _logger.log(Level.SEVERE, "jdbcrealm.invaliduser", user);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Cannot validate user", ex);
            }
        } finally {
            close(connection, statement, rs);
        }
        return valid;
    }

    private char[] hashPassword( char[] password)
            throws CharacterCodingException {
        byte[] bytes = null;
        char[] result = null;
        String charSet = getProperty(PARAM_CHARSET);        
        bytes = Utility.convertCharArrayToByteArray(password, charSet);
        
        if (md != null) {
            synchronized(md) {
                md.reset();
                bytes = md.digest(bytes);
            }
        }

        String encoding = getProperty(PARAM_ENCODING);
        if (HEX.equalsIgnoreCase(encoding)) {
            result = hexEncode(bytes);
        } else if (BASE64.equalsIgnoreCase(encoding)) {
            result = base64Encode(bytes).toCharArray();
        } else { // no encoding specified
            result = Utility.convertByteArrayToCharArray(bytes, charSet);
        }
        return result;
    }

    private char[] hexEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int low = (int)(bytes[i] & 0x0f);
            int high = (int)((bytes[i] & 0xf0) >> 4);
            sb.append(HEXADECIMAL[high]);
            sb.append(HEXADECIMAL[low]);
        }
        char[] result = new char[sb.length()];
        sb.getChars(0, sb.length(), result, 0);
        return result;
    }

    private String base64Encode(byte[] bytes) {
        GFBase64Encoder encoder = new GFBase64Encoder();
        return encoder.encode(bytes);

        
    }

    /**
     * Delegate method for retreiving users groups
     * @param user user's identifier
     * @return array of group key
     */
    private String[] findGroups(String user) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try{
            connection = getConnection();
            statement =  connection.prepareStatement(groupQuery);
            statement.setString(1, user);
            rs = statement.executeQuery();
            final List<String> groups = new ArrayList<String>();
            while (rs.next()) {
                groups.add(rs.getString(1));
            }
            final String[] groupArray = new String[groups.size()];
            return groups.toArray(groupArray);
        } catch(Exception ex) {
            _logger.log(Level.SEVERE, "jdbcrealm.grouperror", user);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Cannot load group", ex);
            }
            return null;
        } finally {
            close(connection, statement, rs);
        }
    }

    private void close(Connection conn, PreparedStatement stmt,
            ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch(Exception ex) {
            }
        }
            
        if (stmt != null) {
            try {
                stmt.close();
            } catch(Exception ex) {
            }
        }
            
        if (conn != null) {
            try {
                conn.close();
            } catch(Exception ex) {
            }
        }
    }

    /**
     * Return a connection from the properties configured
     * @return a connection
     */
    private Connection getConnection() throws LoginException {

        final String dsJndi = this.getProperty(PARAM_DATASOURCE_JNDI);
        final String dbUser = this.getProperty(PARAM_DB_USER);
        final String dbPassword = this.getProperty(PARAM_DB_PASSWORD);
        try{
         /*String nonTxJndiName = dsJndi +"__nontx";
            InitialContext ic = new InitialContext();
             final DataSource dataSource = 
                //V3 Commented (DataSource)ConnectorRuntime.getRuntime().lookupNonTxResource(dsJndi,false);
                //replacement code suggested by jagadish
               (DataSource)ic.lookup(nonTxJndiName);*/
            ConnectorRuntime connectorRuntime = Util.getDefaultHabitat().getServiceHandle(cr).getService();
            final DataSource dataSource = 
                (DataSource) connectorRuntime.lookupNonTxResource(dsJndi,false);
         //(DataSource)ConnectorRuntime.getRuntime().lookupNonTxResource(dsJndi,false);
            Connection connection = null;
            if (dbUser != null && dbPassword != null) {
                connection = dataSource.getConnection(dbUser, dbPassword);
            } else {
                connection = dataSource.getConnection();
            }
            return connection;
        } catch(Exception ex) {
            String msg = sm.getString("jdbcrealm.cantconnect", dsJndi, dbUser);
            LoginException loginEx = new LoginException(msg);
            loginEx.initCause(ex);
            throw loginEx;
        }
    }
}
