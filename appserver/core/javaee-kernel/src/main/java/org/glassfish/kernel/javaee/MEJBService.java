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

package org.glassfish.kernel.javaee;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.api.naming.GlassfishNamingManager;

import com.sun.logging.LogDomains;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * MEJB service to register mejb with a temporary NamingObjectProxy at server 
 * start up time
 */
@Service
@RunLevel(InitRunLevel.VAL)
public class MEJBService implements PostConstruct {

    // we need to inject Globals as it used by the naming manager and
    // therefore needs to be allocated.
    @Inject
    Globals globals;

    @Inject
    ServiceLocator habitat;

    @Inject 
    Provider<GlassfishNamingManager> gfNamingManagerProvider;

    private static final Logger _logger = LogDomains.getLogger(
        MEJBService.class, LogDomains.EJB_LOGGER);
  
    public void postConstruct() {
        GlassfishNamingManager gfNamingManager =
            gfNamingManagerProvider.get();

        MEJBNamingObjectProxy mejbProxy = 
            new MEJBNamingObjectProxy(habitat);
        for(String next : MEJBNamingObjectProxy.getJndiNames()) {
            try {
                gfNamingManager.publishObject(next, mejbProxy, true);
            } catch (Exception e) {
                _logger.log(Level.WARNING, "Problem in publishing temp proxy for MEJB: " + 
                    e.getMessage(), e);
            }
        }
    }
}
