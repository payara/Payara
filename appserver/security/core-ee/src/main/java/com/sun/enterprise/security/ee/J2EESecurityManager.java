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

package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.ee.CachedPermissionImpl;
import com.sun.enterprise.security.ssl.SSLUtils;
import java.net.SocketPermission;

import java.util.*;
// IASRI 4660742 START
// IASRI 4660742 END

/**
 * Java 2 security manager that enforces code security.
 * @author Harish Prabandham
 */
public class J2EESecurityManager extends java.rmi.RMISecurityManager {

    private CachedPermissionImpl connectPerm;

    private PermissionCache cache;

    private boolean cacheEnabled = false;

    public J2EESecurityManager() {
    }

/*
   public void checkAccess(ThreadGroup t) {
   Class[] clss = getClassContext();
   for(int i=1; i < clss.length; ++i) {
// IASRI 4660742   System.out.println(clss[i] + " : " + clss[i].getProtectionDomain());
// START OF IASRI 4660742
            _logger.log(Level.FINE,clss[i] + " : " + clss[i].getProtectionDomain());
// END OF IASRI 4660742
   }
   
   System.out.flush();
   
   // JDK 1.1. implementation...
   Class[] clss = getClassContext();
   for(int i=1; i < clss.length; ++i) {
   checkIfInContainer(clss[i]);
   }
   }
   
   // JDK 1.1. implementation...
    private void checkIfInContainer(Class clazz) {
	Class[] parents = clazz.getDeclaredClasses();
	for(int i=0; i < parents.length; ++i) {
	    if(parents[i] == com.sun.ejb.Container.class) 
		throw new SecurityException("Got it....");
	}
    }
*/

    @Override
   public void checkAccess(ThreadGroup t) {
       super.checkAccess(t);
       checkPermission(new java.lang.RuntimePermission("modifyThreadGroup"));
   }
    
    @Override
    public void checkPackageAccess(final String pkgname) {
	// Remove this once 1.2.2 SecurityManager/ClassLoader bug is fixed.
	if(!pkgname.startsWith("sun."))
	    super.checkPackageAccess(pkgname);
    }

    @Override
    public void checkExit(int status) {
        // Verify exit permission
        super.checkExit(status);
    }

    @Override
    public void checkConnect(String host, int port) {
	if (checkConnectPermission()) {
	    return;
	}
	super.checkConnect(host, port);
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
	if (checkConnectPermission()) {
	    return;
	}
	super.checkConnect(host, port, context);
    }

    @Override
    public void checkPropertyAccess(String key) {
	if (checkProperty(key)) {
	    return;
	} 
	super.checkPropertyAccess(key);
    }

    private boolean checkConnectPermission() {
	if (cacheEnabled()) {
	    return connectPerm.checkPermission();
	} 
	return false;
    }

    private boolean checkProperty(String key) {
        if(key.equals("javax.net.ssl.keyStorePassword") || key.equals("javax.net.ssl.trustStorePassword")){
            SSLUtils.checkPermission(key);
        }
	if (cacheEnabled()) {
	    return cache.checkPermission(new PropertyPermission(key, "read"));
	} 
	return false;
    }

    public synchronized boolean cacheEnabled() {
	return cacheEnabled;
    }

    public synchronized void enablePermissionCache(PermissionCache c) {
	if (c != null) {
	    cache = c;
	    connectPerm = new CachedPermissionImpl
		(cache, new SocketPermission("*","connect"));
	    cacheEnabled = true;
	}
    }
   
}





