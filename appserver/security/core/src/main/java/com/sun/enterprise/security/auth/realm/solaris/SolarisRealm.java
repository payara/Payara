/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.realm.solaris;

import java.util.*;

import java.util.logging.Level;

import com.sun.enterprise.security.auth.realm.IASRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import org.jvnet.hk2.annotations.Service;


/**
 * Realm wrapper for supporting Solaris authentication.
 *
 * <P>The Solaris realm needs the following properties in its configuration:
 * <ul>
 *   <li>jaas-ctx - JAAS context name used to access LoginModule for
 *       authentication.
 * </ul>
 *
 * @see com.sun.enterprise.security.auth.login.SolarisLoginModule
 *
 */
@Service
public final class SolarisRealm extends IASRealm
{
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "solaris";
    public static final String OS_ARCH = "os.arch";
    public static final String SOL_SPARC_OS_ARCH = "sparc";
    public static final String SOL_X86_OS_ARCH = "x86";

    private HashMap groupCache;
    private Vector emptyVector;
    private static String osArchType = null;


    // Library for native methods
    static {
        osArchType = System.getProperty(OS_ARCH);
        if(SOL_SPARC_OS_ARCH.equals(osArchType)) {
            System.loadLibrary("solsparcauth");
        }
        else if (SOL_X86_OS_ARCH.equals(osArchType)) {
            System.loadLibrary("solx86auth");
        }
    }

    
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
     *
     */
    public synchronized void init(Properties props)
        throws BadRealmException, NoSuchRealmException
    {
        super.init(props);
        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        if (jaasCtx == null) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.warning("realmconfig.noctx");
            }
            String msg = sm.getString("solarisrealm.nojaas");
            throw new BadRealmException(msg);
        }

        this.setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("SolarisRealm : "+IASRealm.JAAS_CONTEXT_PARAM+
                       "="+jaasCtx);
        }

        groupCache = new HashMap();
        emptyVector = new Vector();
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
     * Returns the name of all the groups that this user belongs to.
     * This is called from web path role verification, though
     * it should not be.
     *
     * @param username Name of the user in this realm whose group listing
     *     is needed.
     * @return Enumeration of group names (strings).
     * @exception InvalidOperationException thrown if the realm does not
     *     support this operation - e.g. Certificate realm does not support
     *     this operation.
     */
    public Enumeration getGroupNames (String username)
        throws InvalidOperationException, NoSuchUserException
    {
        Vector v = (Vector)groupCache.get(username);
        if (v == null) {
            v = loadGroupNames(username);
        }
        
        return v.elements();
    }


    /**
     * Set group membership info for a user.
     *
     * <P>See bugs 4646133,4646270 on why this is here.
     *
     */
    private void setGroupNames(String username, String[] groups)
    {
        Vector v = null;
        
        if (groups == null) {
            v = emptyVector;

        } else {
            v = new Vector(groups.length + 1);
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
    public String[] authenticate(String username, char[] password)
    {
        String[] grps = nativeAuthenticate(username, new String(password));
        if(grps != null){
            grps = addAssignGroups(grps);
        }
        setGroupNames(username, grps);
        return grps;
    }


    /**
     * Loads groups names for the given user by calling native method.
     *
     * <P>Group info is loaded when user authenticates, however in some
     * cases (such as run-as) the group membership info is needed
     * without an authentication event.
     *
     */
    private Vector loadGroupNames(String username)
    {
        String[] grps = nativeGetGroups(username);
        if (grps == null) {
            _logger.fine("No groups returned for user: "+username);
        }
        
        grps = addAssignGroups(grps);
        setGroupNames(username, grps);
        return (Vector)groupCache.get(username);
    }


    /**
     * Native method. Authenticate using PAM.
     *
     */
    private static native String[] nativeAuthenticate(String user,
                                                     String password);
    
    /**
     * Native method. Retrieve Solaris groups for user.
     *
     */
    private static native String[] nativeGetGroups(String user);

    
}
