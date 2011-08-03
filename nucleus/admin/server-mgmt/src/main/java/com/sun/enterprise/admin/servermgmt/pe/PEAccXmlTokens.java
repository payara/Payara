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

package com.sun.enterprise.admin.servermgmt.pe;

import java.io.File;

import com.sun.enterprise.util.io.FileUtils;

import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.admin.servermgmt.DomainConfig;

public final class PEAccXmlTokens
{
    public static final String SERVER_ROOT  = "SERVER_ROOT";

    public static final String SERVER_NAME  = "SERVER_NAME";

    public static final String ORB_LISTENER1_PORT = "ORB_LISTENER1_PORT"; 

    public static TokenValueSet getTokenValueSet(DomainConfig domainConfig)
    {       
        final PEFileLayout layout = new PEFileLayout(domainConfig);

        final TokenValueSet tokens = new TokenValueSet();

        final String installDir = layout.getInstallRootDir().getAbsolutePath();
        TokenValue tv = new TokenValue(SERVER_ROOT, 
                            FileUtils.makeForwardSlashes(installDir));
        tokens.add(tv);

        tv = new TokenValue(SERVER_NAME, 
                (String)domainConfig.get(DomainConfig.K_HOST_NAME));
        tokens.add(tv);

        final Integer orbPort = 
            (Integer)domainConfig.get(DomainConfig.K_ORB_LISTENER_PORT);
        tv = new TokenValue(ORB_LISTENER1_PORT, orbPort.toString());
        tokens.add(tv);

        return ( tokens );
    }
}
