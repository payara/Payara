/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.web.WebModule;
import org.glassfish.api.invocation.InvocationManager;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;
import java.util.Set;
import java.util.Iterator;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.api.EJBInvocation;
import org.glassfish.internal.api.Globals;

/**
 * <p><b>NOT THREAD SAFE: mutable instance variables</b>
 */
public final class WebServiceContextImpl implements WSWebServiceContext {
    
    public static final ThreadLocal msgContext = new ThreadLocal();
    
    public static final ThreadLocal principal = new ThreadLocal();

    private WSWebServiceContext jaxwsContextDelegate;

    private static final String JAXWS_SERVLET = "org.glassfish.webservices.JAXWSServlet";

    private String servletName;

    private SecurityService  secServ;

    public WebServiceContextImpl() {
        if (Globals.getDefaultHabitat() != null) {
            secServ = Globals.get(org.glassfish.webservices.SecurityService.class);
        }
    }

    public void setContextDelegate(WSWebServiceContext wsc) {
        this.jaxwsContextDelegate = wsc;
    }
    
    public MessageContext getMessageContext() {
        return this.jaxwsContextDelegate.getMessageContext();
    }

    public void setMessageContext(MessageContext ctxt) {
        msgContext.set(ctxt);
    }

    public WSWebServiceContext getContextDelegate(){
        return jaxwsContextDelegate;
    }

    /*
     * this may still be required for EJB endpoints
     *
     */
    public void setUserPrincipal(Principal p) {
        principal.set(p);
    }
    
    public Principal getUserPrincipal() {
        // This could be an EJB endpoint; check the threadlocal variable
        Principal p = (Principal) principal.get();
        if (p != null) {
            return p;
        }
        // This is a servlet endpoint
        p = this.jaxwsContextDelegate.getUserPrincipal();
        //handling for WebService with WS-Security
        if (p == null && secServ != null) {
            WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
            InvocationManager mgr = wscImpl.getInvocationManager();
            boolean isWeb = ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION.
                    equals(mgr.getCurrentInvocation().getInvocationType()) ? true : false;
            p = secServ.getUserPrincipal(isWeb);
        }
        return p;
    }

    public boolean isUserInRole(String role) {
        WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
        ComponentInvocation.ComponentInvocationType EJBInvocationType = ComponentInvocation.ComponentInvocationType.EJB_INVOCATION;
        InvocationManager mgr = wscImpl.getInvocationManager();
        if ((mgr!=null) && (EJBInvocationType.equals(mgr.getCurrentInvocation().getInvocationType()))) {
           EJBInvocation inv = (EJBInvocation)mgr.getCurrentInvocation();
           boolean res = inv.isCallerInRole(role);
           return res;
        }
        // This is a servlet endpoint
        boolean ret = this.jaxwsContextDelegate.isUserInRole(role);
        //handling for webservice with WS-Security
        if (!ret && secServ != null) {

            if (mgr.getCurrentInvocation().getContainer() instanceof WebModule) {
                Principal p = getUserPrincipal();
                ret = secServ.isUserInRole((WebModule)mgr.getCurrentInvocation().getContainer(), p, servletName, role);
            }

        }
        return ret;
    }
    
    // TODO BM need to fix this after checking with JAXWS spec
    public EndpointReference getEndpointReference(Class clazz, org.w3c.dom.Element... params) {
        return this.jaxwsContextDelegate.getEndpointReference(clazz, params);
    }
    
    public EndpointReference getEndpointReference(org.w3c.dom.Element... params) {
        return this.jaxwsContextDelegate.getEndpointReference(params);
    }
    
    public Packet getRequestPacket() {
        return this.jaxwsContextDelegate.getRequestPacket();
    }

    void setServletName(Set webComponentDescriptors) {
        Iterator it = webComponentDescriptors.iterator();
        String endpointName = null;
        while (it.hasNext()) {
            WebComponentDescriptor desc = (WebComponentDescriptor)it.next();
            String name = desc.getCanonicalName();
            if (JAXWS_SERVLET.equals(desc.getWebComponentImplementation())) {
                endpointName = name;
            }
            if (desc.getSecurityRoleReferences().hasMoreElements()) {
                servletName = name;
                break;
            }
        }
        if (servletName == null) {
            servletName = endpointName;
        }
    }

}
