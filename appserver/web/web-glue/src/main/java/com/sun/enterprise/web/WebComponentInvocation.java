/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.invocation.ComponentInvocation;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.SingleThreadModel;
import java.lang.reflect.Method;

public class WebComponentInvocation extends ComponentInvocation {


    /**
     * Used by container within JAXRPC handler processing code.
     */
    private Object webServiceTie;
    private Method webServiceMethod;

    public WebComponentInvocation(WebModule wm) {
        this(wm, null);
    }

    public WebComponentInvocation(WebModule wm, Object instance) {
        setComponentInvocationType(
                ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION);
        componentId = wm.getComponentId();
        jndiEnvironment = wm.getWebBundleDescriptor();
        container = wm;
        this.instance = instance;
        setResourceTableKey(_getResourceTableKey());
        
        moduleName = wm.getModuleName();
        appName = wm.getWebBundleDescriptor().getApplication().getAppName();
    }
    
    public WebComponentInvocation(WebModule wm, Object instance, String instanceName) {
      this(wm, instance);
      setInstanceName(instanceName);
    }

    private Object _getResourceTableKey() {
        Object resourceTableKey = null;
        if (instance instanceof Servlet || instance instanceof Filter) {
            // Servlet or Filter
            if (instance instanceof SingleThreadModel) {
                resourceTableKey = instance;
            } else {
                resourceTableKey =
                        new PairKey(instance, Thread.currentThread());
            }
        } else {
            resourceTableKey = instance;
        }

        return resourceTableKey;
    }

    public void setWebServiceTie(Object tie) {
        webServiceTie = tie;
    }

    public Object getWebServiceTie() {
        return webServiceTie;
    }

    public void setWebServiceMethod(Method method) {
        webServiceMethod = method;
    }

    public Method getWebServiceMethod() {
        return webServiceMethod;
    }

    private static class PairKey {
        private Object instance = null;
        private Thread thread = null;
        int hCode = 0;

        private PairKey(Object inst, Thread thr) {
            instance = inst;
            thread = thr;
            if (inst != null) {
                hCode = 7 * inst.hashCode();
            }
            if (thr != null) {
                hCode += thr.hashCode();
            }
        }

        @Override
        public int hashCode() {
            return hCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            boolean eq = false;
            if (obj != null && obj instanceof PairKey) {
                PairKey p = (PairKey)obj;
                if (instance != null) {
                    eq = (instance.equals(p.instance));
                } else {
                    eq = (p.instance == null);
                }

                if (eq) {
                    if (thread != null) {
                        eq = (thread.equals(p.thread));
                    } else {
                        eq = (p.thread == null);
                    }
                }
            }
            return eq;
        }
    }
}
