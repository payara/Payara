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

package com.sun.enterprise.admin.servermgmt.pe;

import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;

/**
 * This class defines the tokens required by the startserv & stopserv scripts.
 */
public final class PEScriptsTokens
{
    public static final String CONFIG_HOME = "CONFIG_HOME";
    public static final String INSTANCE_ROOT = "INSTANCE_ROOT";
    public static final String SERVER_NAME = "SERVER_NAME";
    public static final String DOMAIN_NAME = "DOMAIN_NAME";

    /**
     * @return Returns the TokenValueSet that has the (token, value) pairs for
     * startserv & stopserv scripts.     
     * @param domainConfig
     */
    public static TokenValueSet getTokenValueSet(DomainConfig domainConfig)
    {
        final PEFileLayout layout = new PEFileLayout(domainConfig);

        final TokenValueSet tokens = new TokenValueSet();

        final String configRootDir = domainConfig.getConfigRoot();            
        TokenValue tv = new TokenValue(CONFIG_HOME, configRootDir);
        tokens.add(tv);

        final String instanceRoot = 
            layout.getRepositoryDir().getAbsolutePath();
        tv = new TokenValue(INSTANCE_ROOT, instanceRoot);
        tokens.add(tv);

        final String instanceName = (String)domainConfig.get(DomainConfig.K_SERVERID);
        if((instanceName == null) || (instanceName.equals("")))
            tv = new TokenValue(SERVER_NAME, PEFileLayout.DEFAULT_INSTANCE_NAME);
        else
            tv = new TokenValue(SERVER_NAME, instanceName);
        tokens.add(tv);

        tv = new TokenValue(DOMAIN_NAME, domainConfig.getDomainName());
        tokens.add(tv);

        return ( tokens );
    }
}
