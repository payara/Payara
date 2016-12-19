/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import com.sun.enterprise.util.uuid.UuidGenerator;
import com.sun.enterprise.web.session.PersistenceType;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.runtime.SessionManager;
import org.jvnet.hk2.annotations.Service;

import java.lang.String;
import java.text.MessageFormat;
import java.util.logging.Level;

@Service(name="memory")
public class MemoryStrategyBuilder extends BasePersistenceStrategyBuilder {

    public void initializePersistenceStrategy(
            Context ctx,
            SessionManager smBean,
            ServerConfigLookup serverConfigLookup) {

        super.initializePersistenceStrategy(ctx, smBean, serverConfigLookup);

        String persistenceType = PersistenceType.MEMORY.getType();        

        String ctxPath = ctx.getPath();
        if(ctxPath != null && !ctxPath.equals("")) {    
            if (_logger.isLoggable(Level.FINE)) {
                Object[] params = { ctx.getPath(), persistenceType };
                _logger.log(Level.FINE, LogFacade.NO_PERSISTENCE, params);
            }
        }

        StandardManager mgr = new StandardManager();
        if (sessionFilename == null) {
            mgr.setPathname(sessionFilename);
        } else {
            mgr.setPathname(prependContextPathTo(sessionFilename, ctx));
        }

        mgr.setMaxActiveSessions(maxSessions);

        // START OF 6364900
        mgr.setSessionLocker(new PESessionLocker(ctx));
        // END OF 6364900        

        ctx.setManager(mgr);

        // START CR 6275709
        if (sessionIdGeneratorClassname != null &&
                sessionIdGeneratorClassname.length() > 0) {
            try {
                UuidGenerator generator = (UuidGenerator)
                    serverConfigLookup.loadClass(
                        sessionIdGeneratorClassname).newInstance();
                mgr.setUuidGenerator(generator);
            } catch (Exception ex) {
                String msg = _rb.getString(LogFacade.UNABLE_TO_LOAD_SESSION_UUID_GENERATOR);
                msg = MessageFormat.format(msg, sessionIdGeneratorClassname);
                _logger.log(Level.SEVERE, msg, ex);
            }
        }
        // END CR 6275709
        
        if (!((StandardContext)ctx).isSessionTimeoutOveridden()) {
            mgr.setMaxInactiveInterval(sessionMaxInactiveInterval); 
        }        
    }    
}
