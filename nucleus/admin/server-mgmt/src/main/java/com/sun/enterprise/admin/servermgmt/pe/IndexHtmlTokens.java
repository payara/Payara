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


import com.sun.enterprise.admin.util.TokenValue;
import com.sun.enterprise.admin.util.TokenValueSet;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.appserv.server.util.Version;

/**
 * This class defines the tokens required by the startserv & stopserv scripts.
 */
final class IndexHtmlTokens
{
    public static final String VERSION_TOKEN_NAME      = "VERSION";
    public static final String DOMAIN_NAME_TOKEN_NAME  = "DOMAIN_NAME";
    public static final String INSTALL_ROOT_TOKEN_NAME = "INSTALL_ROOT";

    public static final String INSTALL_ROOT_DEFAULT_VALUE = "INSTALL_ROOT";
    
    

    private IndexHtmlTokens() {
        //disallow;
    }
    /**
     * @return Returns the TokenValueSet that has the (token, value) pairs for
     * index.html file.     
     * @param domainConfig
     */
    final static TokenValueSet getTokenValueSet(final DomainConfig dc)
    {
        final PEFileLayout lo = new PEFileLayout(dc);

        final TokenValueSet tokens = new TokenValueSet();

        tokens.add(getInstallRoot(lo));
        tokens.add(getVersion());
        tokens.add(getDomainName(dc));
        return ( tokens );
    }
    
    private static TokenValue getInstallRoot(final PEFileLayout lo) {
        String ir;
        try {
            ir = lo.getInstallRootDir().getAbsolutePath();
        } catch(final Exception e) {
            ir = INSTALL_ROOT_DEFAULT_VALUE;
        }
        final TokenValue tv = new TokenValue(INSTALL_ROOT_TOKEN_NAME, ir);
        return ( tv );
    }
    private static TokenValue getVersion() {
        final String version = Version.getFullVersion();
        final TokenValue tv = new TokenValue(VERSION_TOKEN_NAME, version);
        return ( tv );
    }
    private static TokenValue getDomainName(final DomainConfig dc) {
        final String dn     = dc.getDomainName();
        final TokenValue tv = new TokenValue(DOMAIN_NAME_TOKEN_NAME, dn);
        return ( tv );
    }
}
