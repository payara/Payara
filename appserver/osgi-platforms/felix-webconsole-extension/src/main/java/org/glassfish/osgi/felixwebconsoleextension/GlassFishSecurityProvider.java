/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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


package org.glassfish.osgi.felixwebconsoleextension;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.osgi.framework.BundleContext;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 *
 * @author tangyong@cn.fujitsu.com
 * @author sanjeeb.sahoo@oracle.com
 */
public class GlassFishSecurityProvider implements WebConsoleSecurityProvider {
	
	private BundleContext ctx;
	private GlassFish gf;
	
	public void setBundleContext(BundleContext context){
		ctx = context;
	}
	
	 private GlassFish getGlassFish() {
         GlassFish gf = (GlassFish) ctx.getService(ctx.getServiceReference(GlassFish.class.getName()));
         try {
             assert(gf.getStatus() == GlassFish.Status.STARTED);
         } catch (GlassFishException e) {
             throw new RuntimeException(e);
         }
         return gf;
     }

	@Override
	public Object authenticate(String username, String password) {
		gf = getGlassFish();
		AuthenticationService authService = null;
		try{
            authService = getAuthService();
		}catch(GlassFishException gfe){
			gfe.printStackTrace();
			return null;
		}

        Subject fs = null;

       try {
    	   fs = authService.login(username, password.toCharArray(), fs);
        } catch (LoginException e) {			
          e.printStackTrace();
          return null;
        }

          return fs;		
	}

    private AuthenticationService getAuthService() throws GlassFishException {
        // Authentication Service is protected, so we need to access within doPrivileged
        // It must be done irrespective of security manager, because the permission is enforced
        // when a security policy file is present.
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<AuthenticationService>() {
                @Override
                public AuthenticationService run() throws GlassFishException {
                    return gf.getService(AuthenticationService.class);
                }
            });
        } catch (PrivilegedActionException e) {
            throw GlassFishException.class.cast(e.getException());
        }

    }

    @Override
	public boolean authorize(Object user, String role) {
		// TODO Auto-generated method stub
		return false;
	}
}
