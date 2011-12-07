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

package com.sun.enterprise.iiop.security;

/**
 * GSSUPToken class creates a mechanism specific gssapi token for
 * the username, password mechanism
 * @author Nithya Subramanian
 */

import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMech;
import com.sun.corba.ee.org.omg.GSSUP.InitialContextToken;
import com.sun.corba.ee.org.omg.GSSUP.InitialContextTokenHelper;
import java.io.IOException;
import org.omg.CORBA.*;
import org.omg.IOP.*;


import java.util.*;



import com.sun.enterprise.security.auth.login.common.PasswordCredential;

import com.sun.enterprise.util.Utility;
import java.util.logging.*;
import com.sun.logging.*;
/**
 * GSSUPToken Represents the on the wire username/password credential on the 
 * client side and the server side. 
 * @author Sekhar Vajjhala
 * @author Harpreet Singh
 */

public class GSSUPToken {
    private static java.util.logging.Logger _logger=null;
    
    static{
       _logger=LogDomains.getLogger(GSSUPToken.class,LogDomains.SECURITY_LOGGER);
    }
    //START OF IASRI 4825735.
    // to allow for usernames of the type user@foobar.com
    // Will be represented as user\\@foobar.com@domain
    public  static final String DELIMITER_REGEXP = "\\@";
    // backslash for regexp is denoted as \\
    public static final String ESCAPE_CHAR_REGEXP = "\\\\\\@";
    public static final String ESCAPE_CHAR = "\\";
    //START OF IASRI 4825735
    
    public static final String DELIMITER = "@" ;
    public static final String DEFAULT_REALM_NAME = "default";
    
    /**
     * cdr_encoded_token is the GSSAPI mechanism specific token 
     * for the user, password mechanism. The mechanism specific
     * token is stored in the CDR encoded form.
     */
    private byte[] cdr_encoded_token = {} ;

    /* PasswordCredential that contains the username, password and realm */
    PasswordCredential pwdcred = null; 
    /**
     * Constructs mechanism token from a password credential, called from 
     * the client side interceptors
     * @param orb the ORB
     * @param codec the codec for translation
     * @param pwdcred the Password credential, populated with username/password
     * and the realm name
     * @return GSSUPToken instance of the GSSUPToken class.
     * @since 1.4
     */
    public static GSSUPToken getClientSideInstance(ORB orb, Codec codec, 
                    PasswordCredential pwdcred, CompoundSecMech mech){
        return new GSSUPToken(orb, codec, pwdcred, mech);
    }
    /**
     * Creates a GSSUPToken instance on the server side
     * @param orb the orb
     * @param codec the codec 
     * @param authok the authtoken received on the wire.
     * @throws SecurityMechanismException if a name/value pair is not found in 
     * the authtok
     * @since 1.4
     */
    public static GSSUPToken getServerSideInstance(ORB orb, Codec codec, 
                    byte[] authtok) throws SecurityMechanismException{
        return new GSSUPToken(orb, codec, authtok);
    }
    
    /**
     * Constructor used to construct a mechansim token from a 
     * PasswordCredential. This is used by a context initiator.
     * This is called on the Client Side
     */
    private GSSUPToken(ORB orb, Codec codec, PasswordCredential pwdcred, CompoundSecMech mech)
    {
        byte[] name_utf8      = {};  // username in UTF8 format
        byte[] password_utf8  = {};  // password in UTF8 format

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"IIOP: Going to construct a GSSUPToken:");
            _logger.log(Level.FINE, pwdcred.toString());
        }

        try {
            String _name_ = pwdcred.getUser(); 
            // if username is of type user@sun.com, realm - foo
            // do the following
            // create user\\@sun.com
            // if username is already of the type user\\@foo, dont do anything
            int index = _name_.indexOf(DELIMITER);
            if(index == -1){
                // no @ - ignore
            }else{ // check if it is already escaped
                int escaped_index = _name_.indexOf(ESCAPE_CHAR);
                if(escaped_index == -1){ // delimiter not escaped, escape it
                    _name_ = _name_.replaceAll(DELIMITER_REGEXP, ESCAPE_CHAR_REGEXP);
                }else{ // some are escaped, some may be not
                    // rather than traversing the string and escaping 
                    // some that are not escaped
                    // just remove the escape on all escaped delimiters
                    // and then re-escape them
                    _name_ = _name_.replaceAll(ESCAPE_CHAR_REGEXP, DELIMITER_REGEXP);
                    _name_ = _name_.replaceAll(DELIMITER_REGEXP, ESCAPE_CHAR_REGEXP);
                }
            }
            String realm = pwdcred.getRealm();
            // concatenation of name+realm
            if(realm != null){
                // cannot use StringBuffer, as StringBuffer eats away a \
                _name_ = _name_ + DELIMITER + realm; 
            }
            name_utf8 = _name_.getBytes("UTF8");
            //password_utf8 = pwdcred.getPassword().getBytes("UTF8");
            password_utf8 = Utility.convertCharArrayToByteArray(pwdcred.getPassword(), "UTF-8");
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.password_exception",e);
        }

        /* Get the target name from the IOR. 
         * 
         */
        byte[] target_name     = mech.as_context_mech.target_name;

        if(_logger.isLoggable(Level.FINE)){
            _logger.fine("Username (UTF8) " + GSSUtils.dumpHex(name_utf8));
            //_logger.fine("Password (UTF8) " + GSSUtils.dumpHex(password_utf8));
            _logger.fine("Password (UTF8) " + "########");
            _logger.fine("Targetname      " + GSSUtils.dumpHex(target_name));
        }
 
        /* Create an InitialContextToken */
        InitialContextToken inctxToken =  
            new InitialContextToken(name_utf8, password_utf8, target_name);

        /* Generate a CDR encoding */
        Any a = orb.create_any();
        InitialContextTokenHelper.insert(a, inctxToken);

        try {
	    cdr_encoded_token = codec.encode_value(a);
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.encode_exception",e);
        }
	if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"IIOP:Mech specific token length (CDR encoded) = " + cdr_encoded_token.length);
	}
    }

    /* Constructor used to construct a mechansim token from a CDR encoded
     * mechanism specific token. This is used by a context acceptor.
     * This is called on the server side.
     */

    private GSSUPToken(ORB orb, Codec codec, byte[] authtok)
	throws SecurityMechanismException
    {
        byte[] name_utf8      = null;  // username  in UTF8 format
        byte[] password_utf8  = null;  // password  in UTF8 format
        byte[] target_name    = null; // target name
        String username = "";
        //String userpwd  = "";
        char[] userpwd  = null;
        String realm    = "";
        byte[] encoded_token = null ;

       if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "IIOP:Going to construct a GSSUPToken:");
            _logger.log(Level.FINE, "IIOP:Getting CDR encoded GSSUP mechanism token from client authentication token");
       }
        /* get CDR encoded mechanism specific token */
        encoded_token = GSSUtils.getMechToken(GSSUtils.GSSUP_MECH_OID, authtok);
        
        /* create a GSSUPToken from the authentication token */
        if(_logger.isLoggable(Level.FINE)) {
             _logger.log(Level.FINE,"CDR encoded mech specific token length = "+ encoded_token.length);
        }
        /* Decode the cdr encoded token */
        Any a = orb.create_any();

        try {        
	    a = codec.decode_value(encoded_token, InitialContextTokenHelper.type());
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.decode_exception",e);
        }

        InitialContextToken inctxToken = InitialContextTokenHelper.extract(a);

        /* get UTF8 encodings from initial context token */
        password_utf8     = inctxToken.password;
        name_utf8         = inctxToken.username;
        target_name       = inctxToken.target_name;

        if(_logger.isLoggable(Level.FINE)){
            _logger.fine("IIOP:Username (UTF8) " + GSSUtils.dumpHex(name_utf8));
            //_logger.fine("IIOP:Password (UTF8) " + GSSUtils.dumpHex(password_utf8));
            _logger.fine("IIOP:Password (UTF8) " + "########");
            _logger.fine("IIOP:Targetname      " + GSSUtils.dumpHex(target_name));
        }
        /* Construct a PasswordCredential */
        try {
            username  = new String(name_utf8, "UTF8");
            //userpwd   = new String(password_utf8, "UTF8");
            userpwd   = Utility.convertByteArrayToCharArray(password_utf8, "UTF-8");
	} catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.user_password_exception",e);
	}

        /**
         * decode the username and realm as specified by CSIV2
         */
	String name;
	int index = username.indexOf(DELIMITER);
        int esc_index = username.indexOf(ESCAPE_CHAR);
        if ( index == -1 ) {
            name = username;
        }
        else if ( index == 0 || esc_index == 0) {
            // username is of the form "@realm" or
            // \\@realm or starts with a escape character
            throw new SecurityMechanismException("No name_value in username");
        } else if (esc_index != -1){
            // START IASRI 4825735 - Changed from 7.0 UR1 to take
            // care of realm-per-app features
            // username\\@sun.com@realm type
            if (esc_index+2 >= username.length()){
                // string ends at username\\@ - nothing follows
                name = username.replaceAll(ESCAPE_CHAR_REGEXP, DELIMITER);
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE, "IIOP:No Realm specified, "+
                    " creating a default realm for login");
                }
                realm = DEFAULT_REALM_NAME;
            }else {
                // locate the second @ token
                // index+2 starts from the first @ sign and thus
                // returns the index of the first @ sign
                // index +3 skips this @ and gives the index of second @
                int second_at_index = username.indexOf(DELIMITER, esc_index+3);
                if (second_at_index == -1){
                    // user\\@foobar.com - no realm specified
                    name = username.replaceAll(ESCAPE_CHAR_REGEXP, DELIMITER);

                    if(_logger.isLoggable(Level.FINE)){
                        _logger.log(Level.FINE, "IIOP:No Realm specified, "+
                        " creating a default realm for login");
                    }
                    realm = DEFAULT_REALM_NAME;
                }else {
                    name = username.substring(0, second_at_index);
                    name = name.replaceAll(ESCAPE_CHAR_REGEXP, DELIMITER);
                    realm = username.substring(second_at_index+1);
                    if(realm == null || realm.isEmpty()){
                        // user\\@foo.com@ type
                        if(_logger.isLoggable(Level.FINE)){
                            _logger.log(Level.FINE, "IIOP:No Realm specified, "+
                            " creating a default realm for login");
                        }
                        realm = DEFAULT_REALM_NAME;
                    }
                }
            }
            // End IASRI 4825735 - Changed from 7.0 UR1
        } else {
	    // parse the name and realm tokens
	    StringTokenizer strtok   = new StringTokenizer(username, DELIMITER);
	    name = strtok.nextToken();
	    // this checking is neccessary if the username="name@"
            if ( strtok.hasMoreTokens() ) {
        	realm = strtok.nextToken();
                // for realm-per-app
                // if ( !realm.equals("default") )
                // throw new SecurityMechanismException("Unknown realm");
                if(realm.isEmpty()){
                    if(_logger.isLoggable(Level.FINE)){
                        _logger.log(Level.FINE, "IIOP:No Realm specified, "+
                            " creating a default realm for login");
                    }
                    realm = DEFAULT_REALM_NAME;
                }
            }
	}
        String targetNameRealm = null;
        try {
            if (target_name != null && target_name.length != 0) {
                targetNameRealm = new String(GSSUtils.importName(GSSUtils.GSSUP_MECH_OID, target_name));
            }
        } catch (IOException ex) {
            _logger.log(Level.FINE, null, ex);
        }
        if (targetNameRealm != null && !DEFAULT_REALM_NAME.equals(targetNameRealm)) {
            realm = targetNameRealm;
        }
        pwdcred = new PasswordCredential(name, userpwd, realm, target_name);
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, pwdcred.toString());
        }
    }
    /** 
     * Returns the GSSToken for the GSSUPToken name that conforms to 
     * the GSSUP Mechanism id
     * @return byte[] the byte array representation of the GSSToken
     */    
    byte[] getGSSToken() throws IOException
    {
        if(_logger.isLoggable(Level.FINER)){
            _logger.log(Level.FINER, "IIOP:GSSUP mech token : " + GSSUtils.dumpHex(cdr_encoded_token));
        }
         /* construct a GSSAPI token ( hdr + mechanism token ) */
         byte[] gsstoken = GSSUtils.createMechIndToken(GSSUtils.GSSUP_MECH_OID, cdr_encoded_token);
         if(_logger.isLoggable(Level.FINER)){
             _logger.log(Level.FINER, "IIOP:GSSUP token length : " + gsstoken.length);
             _logger.log(Level.FINER, "IIOP:GSSUP token: " + GSSUtils.dumpHex(gsstoken));
         }
         return gsstoken;
    }
    
    /**
     * @return PasswordCredential the PasswordCredential object that has the 
     * username and password. This is called from the server side interceptor 
     */
    PasswordCredential getPwdcred() 
    {
       return pwdcred;
    }
}

