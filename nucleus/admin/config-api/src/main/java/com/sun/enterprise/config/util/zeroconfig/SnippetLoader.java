/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.config.util.zeroconfig;

import com.sun.enterprise.config.serverbeans.ConfigLoader;
import com.sun.enterprise.config.serverbeans.ConfigSnippetLoader;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.logging.Logger;

/**
 * Containing shared functionalists between different derived classes like ConfigSnippetLoader and so on.
 * Shared functionalists includes finding, loading the configuration and creating a ConFigBean from it.
 *
 * @author Masoud Kalali
 */
public abstract class SnippetLoader<C extends ConfigLoader, T extends ConfigBeanProxy> {
    private final static Logger LOG = Logger.getLogger(ConfigSnippetLoader.class.getName());

    protected C configLoader;
    protected Class<? extends T> configBeanType;

    public <U extends T> SnippetLoader(C configLoader, Class<U> configBeanProxyType) {
        this.configLoader = configLoader;
        this.configBeanType = configBeanProxyType;
    }

    /**
     * Assumes that the requested  configBeanType configuration does not exist in the domain.xml so we will
     * create it according to the priorities explained at
     * http://aseng-wiki.us.oracle.com/asengwiki/display/GlassFish/Zero+Config+One+Pager#ZeroConfigOnePager-ConfigSources
     *
     * @param configBeanType the @ConfigBean type we want it's configuration.
     * @return an instance of the requested configBeanType
     * @throws org.jvnet.hk2.config.TransactionFailure
     *
     */
    public abstract <U extends T> U createConfigBeanForType(Class<U> configBeanType)
            throws TransactionFailure;
}
