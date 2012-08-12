/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node;

import static com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY;

import java.io.File;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 *Provides the appropriate implementation depending on the current
 *runtime environment.
 *
 * @author tjquinn
 */
public class SaxParserHandlerFactory {
    
    /** Creates a new instance of SaxParserHandlerFactory */
    public SaxParserHandlerFactory() {
    }
    
    public static SaxParserHandler newInstance() {
        SaxParserHandler result = null;
        
        /*
         *If the property com.sun.aas.installRoot is defined, use the 
         *original implementation (SaxParserHandler) which fetches DTDs and
         *schemas from the installation directory tree.  Otherwise, assume that 
         *the app client container is running under Java Web Start. In that
         *case, there is no product installation directory (at least none can
         *be assumed).  The DTDs and schemas will be retrieved from the
         *JWS-specific jar file instead (SaxParserHandlerBundled).
         *
         *bnevins, Oct 16, 2008.  On Oct. 8, 2008 installRoot was changed to be setup
         *earlier in the startup.  As a result, Embedded GF broke.  It sets up a fake installRoot, 
         *because there is *no* install-root.
         *Improvement: don't just see if installRoot is set -- make sure installRoot
         *is bonafide.
          */ 
        
        final ServiceLocator habitat = Globals.getDefaultHabitat();

        if(installRootIsValid())
            result = habitat.getService(SaxParserHandler.class);
        else
            result = habitat.getService(SaxParserHandlerBundled.class);

        return result;
    }
    
    private static boolean installRootIsValid() {
        // In the context of this class, we need to make sure that we know if we 
        //have a route to local DTDs.  Period.
        
        String ir = System.getProperty(INSTALL_ROOT_PROPERTY);
        
        if(!ok(ir))
            return false;
        
        File dtds = new File(new File(ir), "lib/dtds");
        
        if(!dtds.isDirectory())
            return false;
        
        return true;
    }

    private static boolean ok(String ir) {
        return ir != null && ir.length() > 0;
    }
}
