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

package com.sun.enterprise.security.appclient;

import com.sun.enterprise.iiop.security.AlternateSecurityInterceptorFactory;
import com.sun.enterprise.iiop.security.SecClientRequestInterceptor;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.admin.ProcessEnvironment;

import org.glassfish.enterprise.iiop.api.IIOPInterceptorFactory;


import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import javax.inject.Inject;

/**
 *
 * @author Kumar
 */
@Service(name="ClientSecurityInterceptorFactory")
@Singleton
public class AppclientIIOPInterceptorFactory implements IIOPInterceptorFactory {

    private static Logger _logger = null;
    final String interceptorFactory =
            System.getProperty(AlternateSecurityInterceptorFactory.SEC_INTEROP_INTFACTORY_PROP);

    static {
        _logger = LogDomains.getLogger(AppclientIIOPInterceptorFactory.class, LogDomains.SECURITY_LOGGER);
    }
    private ClientRequestInterceptor creq;
    @Inject
    private ProcessEnvironment penv;

    private AlternateSecurityInterceptorFactory altSecFactory;
    
    // are we supposed to add the interceptor and then return or just return an instance ?.
    public ClientRequestInterceptor createClientRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (penv.getProcessType().isServer()) {
            return null;
        }
        if (altSecFactory != null ||
                (interceptorFactory != null && createAlternateSecurityInterceptorFactory())) {
            return altSecFactory.getClientRequestInterceptor(codec);
        }
        ClientRequestInterceptor ret = getClientInterceptorInstance(codec);
        return ret;
    }

    public ServerRequestInterceptor createServerRequestInterceptor(ORBInitInfo info, Codec codec) {
        return null;
    }

    private synchronized boolean createAlternateSecurityInterceptorFactory() {
        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(interceptorFactory);
            if (AlternateSecurityInterceptorFactory.class.isAssignableFrom(clazz) &&
                    !clazz.isInterface()) {
                altSecFactory = (AlternateSecurityInterceptorFactory) clazz.newInstance();
                return true;
            } else {
                _logger.log(Level.INFO, "Not a valid factory class: " + interceptorFactory +
                        ". Must implement " + AlternateSecurityInterceptorFactory.class.getName());
            }
        } catch (ClassNotFoundException ex) {
            _logger.log(Level.INFO, "Interceptor Factory class " + interceptorFactory + " not loaded: ", ex);
        } catch (InstantiationException ex) {
            _logger.log(Level.INFO, "Interceptor Factory class " + interceptorFactory + " not loaded: ", ex);
        } catch (IllegalAccessException ex) {
            _logger.log(Level.INFO, "Interceptor Factory class " + interceptorFactory + " not loaded: ", ex);
        }
        return false;
    }

    private synchronized ClientRequestInterceptor getClientInterceptorInstance(Codec codec) {
        if (creq == null) {
            creq = new SecClientRequestInterceptor(
                "SecClientRequestInterceptor", codec);
        }
        return creq;
    }
    
}
