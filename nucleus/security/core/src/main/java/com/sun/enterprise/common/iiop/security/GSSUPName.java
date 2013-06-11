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

package com.sun.enterprise.common.iiop.security;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.common.Util;
import java.util.*;
import java.util.logging.*;
import com.sun.logging.*;

/**
 * This class implements the GSSAPI exported name functionality 
 * as required by CSIV2.
 *
 * @author    Sekhar Vajjhala
 */

public class GSSUPName {
    private static java.util.logging.Logger _logger=null;
    static{
       _logger=SecurityLoggerInfo.getLogger();
        }
    public static final char   AT_CHAR       = '@';
    public static final String AT_STRING     = "@";
    public static final char   ESCAPE_CHAR   = '\\';
    public static final String ESCAPE_STRING = "\\";


    private String username;  // username
    private String realm;     // realmname
    private GSSUtilsContract gssUtils;

    public GSSUPName(String username, String realm)
    {

	this.username = username;
	this.realm    = realm;
        gssUtils = Util.getDefaultHabitat().getService(GSSUtilsContract.class);
    }

    /* Construct a GSSUPName from an exported name. This constructor
     * is for use on the server side.
     */
    public GSSUPName(byte[] GSSExportedName)
    {
        int             realm_index = 0 ; // start of realm 
        int             user_index  = -1 ; // start of user
        String expname = "";
        String name_value = "" ;
        String name_scope = "" ;
        byte[] exportedname = null ;

        gssUtils = Util.getDefaultHabitat().getService(GSSUtilsContract.class);
        assert(gssUtils != null);
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Attempting to create a mechanism specific name from the exported name.");
        }
	
        try {
            exportedname = gssUtils.importName(gssUtils.GSSUP_MECH_OID(), GSSExportedName);
            // extract from the "UTF8" encoding
            expname = new String(exportedname, "UTF8");
        } catch (Exception e) {
             _logger.log(Level.SEVERE, SecurityLoggerInfo.iiopImportNameError , e.getLocalizedMessage());
	}

	if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mechanism specific name: " + expname);
	}
        // Deterimine the starting indices of the username and realm name

        int at_index  = expname.indexOf(AT_CHAR);
        int esc_index = expname.indexOf(ESCAPE_CHAR);

        if (at_index == -1) {
            /* scoped-username is of the form:
             *     scoped-username ::== name_value
             */
            name_value = expname;

        }  else if (esc_index == -1 ) {
            if (at_index != 0) {	
	        /* name_value is not null i.e. scoped-username is of the form:
                 *     scoped-username ::= name_value@name_scope
		 */
                name_value = expname.substring(0, at_index);
	    }
            name_scope = expname.substring(at_index+1);
	}   else {
	    // exported name contains both AT_CHAR and ESCAPE_CHAR. Separate
            // the username and the realm name. The start of a realm name
	    // is indicated by an AT_CHAR that is not preceded by an
	    // ESCAPE_CHAR. 
	    // The username always starts at 0.

            user_index  = 0;
            realm_index = 0;
            int i = 0;
	    while ( (i = expname.indexOf(AT_CHAR, i)) != -1) {
	        if (expname.charAt(i-1) != ESCAPE_CHAR) {
                    realm_index = i;
                    break;
		}
                i += 1;
	    }
            name_value = expname.substring(user_index, realm_index);
            name_scope = expname.substring(realm_index+1);
	}

	if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"name_value: " + name_value + " ;  name_scope: " + name_scope);
	}

        if ((name_value.length() > 0) && (at_index != -1)) {
	    // remove the ESCAPE_CHAR from the username
            StringBuilder strbuf = new StringBuilder("");
            int starti = 0  ; // start index
            int endi   = 0  ; // end index

            while ((endi = name_value.indexOf(ESCAPE_CHAR, starti)) != -1) {
                strbuf.append(name_value.substring(starti, endi));
                starti = endi + 1;
	    }
            strbuf.append(name_value.substring(starti));
            name_value = strbuf.toString();
	}

        username = name_value;
        realm    = name_scope;
	if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"Constructed GSSUPName ( " + toString()+ " )");
        }
    }

    /**
     *  returns and exported name as an array of 1 or more UTF8 characters.
     */
    public byte[] getExportedName() {

        byte[] expname = {} ;
        byte[] expname_utf8 = null ;
        StringTokenizer strtok ;

	if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Going to create exported name for:" + toString());
	}

        StringBuffer strbuf = new StringBuffer("");
       
        /* Process the username for special characters AT_CHAR and ESCAPE_CHAR */

        int at_index  = username.indexOf(AT_CHAR);
        int esc_index = username.indexOf(ESCAPE_CHAR);
        
        if ( (at_index == -1) &&  (esc_index == -1))
	    strbuf = new StringBuffer(username); // just copy - no processing required.
	else {

	    // N.B. Order of processing is important

	    // Replace the ESCAPE_CHAR first
	    if (esc_index != -1) {
                strtok = new StringTokenizer(username, ESCAPE_STRING);
                while (strtok.hasMoreTokens()) { 
                    strbuf.append(strtok.nextToken());
                    strbuf.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
		}
	    }

            // Replace the AT_CHAR next
            if (at_index != -1) {
                strtok = new StringTokenizer(username, AT_STRING);
                while (strtok.hasMoreTokens()) { 
                    strbuf.append(strtok.nextToken());
                    strbuf.append(ESCAPE_CHAR).append(AT_CHAR);
		}
	    }
	}

	if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"username after processing for @ and \\: " + strbuf);
	}

        /** Do not append realm name: this ensures that we dont sent
            "default" or "certificate" to another appserver.

        // append an AT-CHAR only if realm is not null.
        // NOTe: In the current implementation, realm will never
        // be null. It is either "certificate" or "default".

        if (realm.length() > 0) {
            strbuf.append(AT_CHAR);
            strbuf.append(realm);
	}
        **/

	if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"username and realm name : " + strbuf);
	}
        try {
            expname_utf8 = strbuf.toString().getBytes("UTF8");
            assert(gssUtils != null);
            expname = gssUtils.createExportedName(
                       gssUtils.GSSUP_MECH_OID(), expname_utf8);
        } catch (Exception e) {
             _logger.log(Level.SEVERE, SecurityLoggerInfo.iiopCreateExportedNameError , e.getLocalizedMessage());
	}

	if(_logger.isLoggable(Level.FINE)) {
                assert(gssUtils != null);
		_logger.log(Level.FINE,"GSSUPName in exported format = " + gssUtils.dumpHex(expname));
	}
        return expname;
    }

    public String getRealm() {
	return realm;
    }

    public String getUser() {
        return username;
    }

    public boolean equals(Object o) {
	if(o instanceof GSSUPName) {
	    GSSUPName nm = (GSSUPName)o;
	    if (nm.getUser().equals(username) && nm.getRealm().equals(realm))
		return true;
	}
	return false;
    }

    /* Return the hashCode. */
    public int hashCode() {
	return username.hashCode() + realm.hashCode();
    }

    /* String representation of the GSSUPname */
    public String toString() {
	String s = "Username = " + username;
	s = s + " Realm = " + realm;
	return s;
    }


    // used locally by this file for test purposes
    private static void testGSSUP(String user, String realm)
    {
        GSSUPName gssname;
        GSSUPName gssname1;

        _logger.log(Level.FINE,"Running unit test for TestGSSUPName.");
        _logger.log(Level.FINE,"Creating a GSSUPName instance");
        gssname = new GSSUPName(user, realm);
        _logger.log(Level.FINE,"GSSUPName : " + gssname.toString());
        _logger.log(Level.FINE,"Obtaining an exported name form");
        byte[] expname = gssname.getExportedName();
        _logger.log(Level.FINE,"Creating a GSSUPName instance from exported name");
        gssname1 = new GSSUPName(expname);
        _logger.log(Level.FINE,"GSSUPName created from exported name: " + gssname1.toString());
    }

    public static void main(String[] args) {
        testGSSUP("sekhar@vajjha@la@", "sun.com");
        testGSSUP("sekhar", "sun.com");
        testGSSUP("sekhar", "");
        testGSSUP("", "sun.com");
    }
}
