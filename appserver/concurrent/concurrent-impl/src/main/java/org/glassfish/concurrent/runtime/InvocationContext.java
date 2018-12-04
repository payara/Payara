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

// Portions Copyright [2018] [Payara Foundation and/or its affiliates.]

package org.glassfish.concurrent.runtime;

import com.hazelcast.logging.Logger;
import com.sun.enterprise.security.SecurityContext;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.notification.requesttracing.RequestTraceSpanContext;
import fish.payara.opentracing.propagation.MapToTextMap;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

public class InvocationContext implements ContextHandle {

    private transient ComponentInvocation invocation;
    private transient ClassLoader contextClassLoader;
    private transient SecurityContext securityContext;
    private transient Map spanContextMap;
    private boolean useTransactionOfExecutionThread;

    static final long serialVersionUID = 5642415011655486579L;

    public InvocationContext(ComponentInvocation invocation, ClassLoader contextClassLoader, SecurityContext securityContext,
                             boolean useTransactionOfExecutionThread) {
        this.invocation = invocation;
        this.contextClassLoader = contextClassLoader;
        this.securityContext = securityContext;
        this.useTransactionOfExecutionThread = useTransactionOfExecutionThread;
        saveTracingContext();
    }

    private void saveTracingContext() {
        ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        
        if (serviceLocator != null) {
            RequestTracingService requestTracing = serviceLocator.getService(RequestTracingService.class);
            OpenTracingService openTracing = serviceLocator.getService(OpenTracingService.class);
            
            // Check that there's actually a trace running
            if (requestTracing != null && requestTracing.isRequestTracingEnabled()
                    && requestTracing.isTraceInProgress() && openTracing != null) {
                
                Tracer tracer = openTracing.getTracer(openTracing.getApplicationName(
                        serviceLocator.getService(InvocationManager.class)));
                
                SpanContext spanContext = null;
                
                // Check if there's an active Span running
                Span activeSpan = tracer.activeSpan();
                if (activeSpan != null) {
                    // The traceId is likely incorrect at this point as it initialises as a random UUID
                    try {
                        ((RequestTraceSpan) activeSpan).setTraceId(requestTracing.getConversationID());
                    } catch (ClassCastException cce) {
                        Logger.getLogger(InvocationContext.class).log(
                                Level.FINE, 
                                "ClassCastException caught converting Span", 
                                cce);
                    }
                    
                    spanContext = activeSpan.context();
                } else {
                    // Create a new span context using the starting span as a parent - the request tracing service doesn't
                    // know about unfinished spans so we can't get the actual parent with the current impl
                    spanContext = new RequestTraceSpanContext(
                            requestTracing.getConversationID(), 
                            requestTracing.getStartingTraceID());
                }
                
                // Check to see if we're using the mock tracer to prevent ClassCastExceptions
                try {
                    tracer.inject(spanContext, Format.Builtin.TEXT_MAP, new MapToTextMap(spanContextMap = new HashMap()));
                } catch (ClassCastException cce) {
                    Logger.getLogger(InvocationContext.class).log(
                            Level.FINE, 
                            "ClassCastException caught injecting SpanContext", 
                            cce);
                }
            }   
        }    
    }
    
    public ComponentInvocation getInvocation() {
        return invocation;
    }

    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    public boolean isUseTransactionOfExecutionThread() {
        return useTransactionOfExecutionThread;
    }
    
    public Map getSpanContextMap() {
        return spanContextMap;
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeBoolean(useTransactionOfExecutionThread);
        // write values for invocation
        String componentId = null;
        String appName = null;
        String moduleName = null;
        if (invocation != null) {
            componentId = invocation.getComponentId();
            appName = invocation.getAppName();
            moduleName = invocation.getModuleName();
        }
        out.writeObject(componentId);
        out.writeObject(appName);
        out.writeObject(moduleName);
        // write values for securityContext
        String principalName = null;
        boolean defaultSecurityContext = false;
        Subject subject = null;
        if (securityContext != null) {
            if (securityContext.getCallerPrincipal() != null) {
                principalName = securityContext.getCallerPrincipal().getName();
                subject = securityContext.getSubject();
                // Clear principal set to avoid ClassNotFoundException during deserialization.
                // It will be set by new SecurityContext in readObject().
                subject.getPrincipals().clear();
            }
            if (securityContext == SecurityContext.getDefaultSecurityContext()) {
                defaultSecurityContext = true;
            }
        }
        out.writeObject(principalName);
        out.writeBoolean(defaultSecurityContext);
        out.writeObject(subject);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        useTransactionOfExecutionThread = in.readBoolean();
        // reconstruct invocation
        String componentId = (String) in.readObject();
        String appName = (String) in.readObject();
        String moduleName = (String) in.readObject();
        invocation = createComponentInvocation(componentId, appName, moduleName);
        // reconstruct securityContext
        String principalName = (String) in.readObject();
        boolean defaultSecurityContext = in.readBoolean();
        Subject subject = (Subject) in.readObject();
        if (principalName != null) {
            if (defaultSecurityContext) {
                securityContext = SecurityContext.getDefaultSecurityContext();
            }
            else {
                securityContext = new SecurityContext(principalName, subject, null);
            }
        }
        // reconstruct contextClassLoader
        ApplicationRegistry applicationRegistry = ConcurrentRuntime.getRuntime().getApplicationRegistry();
        if (appName != null) {
            ApplicationInfo applicationInfo = applicationRegistry.get(appName);
            if (applicationInfo != null) {
                contextClassLoader = applicationInfo.getAppClassLoader();
            }
        }
    }

    private ComponentInvocation createComponentInvocation(String componentId, String appName, String moduleName) {
        if (componentId == null && appName == null && moduleName == null) {
            return null;
        }
        ComponentInvocation newInv = new ComponentInvocation(
                componentId,
                ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION,
                null,
                appName,
                moduleName
        );
        return newInv;
    }

}
