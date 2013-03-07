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

package org.glassfish.loadbalancer.admin.cli.reader.impl;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.IdempotentUrlPatternReader;

import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.WebModuleVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

/**
 * Provides web module information relavant to Load balancer tier.
 *
 * @author Kshitiz Saxena
 */
public class WebModuleReaderImpl implements WebModuleReader {

    public WebModuleReaderImpl(String name, ApplicationRef ref, Application application, WebBundleDescriptor webBundleDescriptor) {
        _applicationRef = ref;
        _application = application;
        _name = name;
        _webBundleDescriptor = webBundleDescriptor;
        _sunWebApp = webBundleDescriptor.getSunDescriptor();
    }

    @Override
    public String getContextRoot() throws LbReaderException {
        if (_application.getContextRoot() != null) {
            return _application.getContextRoot();
        }
        return _webBundleDescriptor.getContextRoot();
    }

    @Override
    public String getErrorUrl() throws LbReaderException {
        return _sunWebApp.getAttributeValue(SunWebApp.ERROR_URL);
    }

    @Override
    public boolean getLbEnabled() throws LbReaderException {
        return Boolean.valueOf(_applicationRef.getLbEnabled()).booleanValue();
    }

    @Override
    public String getDisableTimeoutInMinutes() throws LbReaderException {
        return _applicationRef.getDisableTimeoutInMinutes();
    }

    @Override
    public IdempotentUrlPatternReader[] getIdempotentUrlPattern()
            throws LbReaderException {
        if (_sunWebApp == null) {
            return null;
        } else {
            int len = _sunWebApp.sizeIdempotentUrlPattern();
            if (len == 0) {
                return null;
            }
            IdempotentUrlPatternReader[] iRdrs =
                    new IdempotentUrlPatternReader[len];
            for (int i = 0; i < len; i++) {
                iRdrs[i] = new IdempotentUrlPatternReaderImpl(_sunWebApp.getIdempotentUrlPattern(i));
            }
            return iRdrs;
        }
    }

    @Override
    public void accept(Visitor v) throws Exception {
		if (v instanceof WebModuleVisitor) {
			WebModuleVisitor wv = (WebModuleVisitor) v;
			wv.visit(this);
		}
    }
    // ---- VARIABLE(S) - PRIVATE -----------------------------
    private String _name;
    private ApplicationRef _applicationRef = null;
    private Application _application = null;
    private WebBundleDescriptor _webBundleDescriptor;
    private SunWebApp _sunWebApp;
}
