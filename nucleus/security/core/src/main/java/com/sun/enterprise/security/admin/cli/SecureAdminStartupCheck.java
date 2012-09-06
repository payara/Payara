/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.security.admin.cli;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
/**
 *
 * Starting in GlassFish 3.1.2, the DAS uses SSL to send admin requests to
 * instances regardless of whether the user has enabled secure admin.  For this to
 * work correctly when upgrading from earlier 3.x releases, there are some changes 
 * to the configuration that must be in place.  This start-up service makes
 * sure that the config is correct as quickly as possible to avoid degrading
 * start-up performance. (Upgrades from 2.x are handled by the SecureAdminConfigUpgrade
 * upgrade service.)
 * <p>
 * For 3.1.2 and later the configuration needs to include:
 * <pre>
 * {@code
 * <secure-admin special-admin-indicator="xxx">
 *   at least one <secure-admin-principal> element; if none, supply these defaults:
 * 
 *   <secure-admin-principal dn="dn-for-DAS"/>
 *   <secure-admin-principal dn="dn-for-instances"/>
 * }
 * </pre>
 * 
 * Further, the sec-admin-listener set-up needs to be added (if not already there)
 * for the non-DAS configurations.  Note that the work to configure the
 * listeners and related protocols are already implemented by SecureAdminCommand,
 * so this class delegates much of its work to that logic.
 * 
 * @author Tim Quinn
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class SecureAdminStartupCheck extends SecureAdminUpgradeHelper implements PostConstruct {

    @Override
    public void postConstruct() {
        try {
            /*
             * If a formal upgrade is in progress then this Startup service
             * will be invoked first.  The upgrade should take care of things,
             * so this becomes a no-op.
             */
            if (isFormalUpgrade()) {
                return;
            }
            ensureSecureAdminReady();
            ensureNonDASConfigsReady();
            commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    
    private boolean isFormalUpgrade() {
        return Boolean.valueOf(startupArg("-upgrade"));
    }
}
