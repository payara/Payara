/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2022] Payara Fondation and/or affiliates

package org.glassfish.concurrent.runtime.deployer;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.glassfish.concurrent.config.ContextService;
import org.glassfish.concurrent.runtime.ContextSetupProviderImpl;

/**
 * Contains configuration information for a ContextService object
 */
public class ContextServiceConfig extends BaseConfig {

    private final Set<String> propagatedContexts;
    private final Set<String> clearedContexts;
    private final Set<String> uchangedContexts;

    public ContextServiceConfig(String jndiName) {
        super(jndiName, null, "true");
        this.propagatedContexts = parseContextInfo(this.contextInfo, this.isContextInfoEnabledBoolean());
        this.clearedContexts = new HashSet<>();
        this.uchangedContexts = new HashSet<>();
    }

    public ContextServiceConfig(ContextService config) {
        super(config.getJndiName(), config.getContextInfo(), config.getContextInfoEnabled());
        this.propagatedContexts = parseContextInfo(this.contextInfo, this.isContextInfoEnabledBoolean());
        this.clearedContexts = new HashSet<>();
        this.uchangedContexts = new HashSet<>();
    }

    public ContextServiceConfig(String jndiName, String contextInfo, String contextInfoEnabled, Set<String> propagatedContexts, Set<String> clearedContexts, Set<String> uchangedContexts) {
        super(jndiName, contextInfo, contextInfoEnabled);
        this.propagatedContexts = propagatedContexts;
        this.clearedContexts = clearedContexts;
        this.uchangedContexts = uchangedContexts;
    }

    @Override
    public TYPE getType() {
        return TYPE.CONTEXT_SERVICE;
    }

    public Set<String> getPropagatedContexts() {
        return propagatedContexts;
    }

    public Set<String> getClearedContexts() {
        return clearedContexts;
    }

    public Set<String> getUchangedContexts() {
        return uchangedContexts;
    }

    public static Set<String> parseContextInfo(String contextInfo, boolean isContextInfoEnabled) {
        Set<String> contextTypeArray = new HashSet<>();
        if (contextInfo == null) {
            // by default, if no context info is passed, we propagate all context types
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_CLASSLOADING);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_NAMING);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_SECURITY);
            contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA);
        } else if (isContextInfoEnabled) {
            StringTokenizer st = new StringTokenizer(contextInfo, ",", false);
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (org.glassfish.concurrent.runtime.ConcurrentRuntime.CONTEXT_INFO_CLASSLOADER.equalsIgnoreCase(token)) {
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_CLASSLOADING);
                } else if (org.glassfish.concurrent.runtime.ConcurrentRuntime.CONTEXT_INFO_JNDI.equalsIgnoreCase(token)) {
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_NAMING);
                } else if (org.glassfish.concurrent.runtime.ConcurrentRuntime.CONTEXT_INFO_SECURITY.equalsIgnoreCase(token)) {
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_SECURITY);
                } else if (org.glassfish.concurrent.runtime.ConcurrentRuntime.CONTEXT_INFO_WORKAREA.equalsIgnoreCase(token)) {
                    contextTypeArray.add(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA);
                } else {
                    contextTypeArray.add(token); // custom context
                }
            }
        }
        return contextTypeArray;
    }

}
