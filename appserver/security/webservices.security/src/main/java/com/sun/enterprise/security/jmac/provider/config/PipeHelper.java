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

package com.sun.enterprise.security.jmac.provider.config;

import com.sun.xml.ws.api.server.WSEndpoint;
import java.net.URL;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ClientAuthConfig;
import javax.security.auth.message.config.ClientAuthContext;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.xml.ws.WebServiceException;
//import com.sun.ejb.Container;
//import com.sun.ejb.Invocation;
//import com.sun.enterprise.InvocationManager;
//import com.sun.enterprise.Switch;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import org.glassfish.deployment.common.ModuleDescriptor;

import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;

import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
//import com.sun.enterprise.security.audit.AuditManagerFactory;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.ee.authorize.EJBPolicyContextDelegate;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.common.ClientSecurityContext;
import com.sun.enterprise.security.jmac.AuthMessagePolicy;
import com.sun.enterprise.security.webservices.PipeConstants;
//TODO: replace the one below with the one above later
import com.sun.enterprise.security.jmac.config.ConfigHelper;
import com.sun.enterprise.security.jmac.config.GFServerConfigProvider;
import com.sun.enterprise.security.jmac.config.HandlerContext;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;

import com.sun.xml.ws.api.model.JavaMethod;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.UnmarshalException;
import javax.xml.ws.handler.MessageContext;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;


public class PipeHelper extends ConfigHelper {
    private  AppServerAuditManager auditManager = null;
            //AuditManagerFactory.getAuditManagerInstance();

    protected static final LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(PipeConstants.class);

    private boolean isEjbEndpoint;
    private SEIModel seiModel;
    private SOAPVersion soapVersion;
    private InvocationManager invManager = null;
    private EJBPolicyContextDelegate ejbDelegate = null;

    public PipeHelper(String layer, Map map, CallbackHandler cbh) {
        init(layer, getAppCtxt(map), map, cbh);

	this.isEjbEndpoint = processSunDeploymentDescriptor();
	this.seiModel = (SEIModel) map.get(PipeConstants.SEI_MODEL);
        WSBinding binding = (WSBinding)map.get(PipeConstants.BINDING);
        if (binding == null) {
            WSEndpoint endPoint = (WSEndpoint)map.get(PipeConstants.ENDPOINT);
            if (endPoint != null) {
                binding = endPoint.getBinding();
            }
        }
        this.soapVersion = (binding != null) ? binding.getSOAPVersion() : SOAPVersion.SOAP_11;
        AuditManager am = (SecurityServicesUtil.getInstance() != null)
                ? SecurityServicesUtil.getInstance().getAuditManager()
                : null;
        auditManager = (am != null && (am instanceof AppServerAuditManager)) ? (AppServerAuditManager) am : new AppServerAuditManager();//workaround for standalone clients where no habitat
        invManager = (SecurityServicesUtil.getInstance() != null)
                ? SecurityServicesUtil.getInstance().getHabitat().<InvocationManager>getService(InvocationManager.class) : null;

        this.ejbDelegate = new EJBPolicyContextDelegate();
   }

    @Override
    public ClientAuthContext getClientAuthContext(MessageInfo info, Subject s)
            throws AuthException {
	ClientAuthConfig c = (ClientAuthConfig)getAuthConfig(false);
	if (c != null) {
            addModel(info, map);
	    return c.getAuthContext(c.getAuthContextID(info),s,map);
	}
	return null;
    }

    @Override
    public ServerAuthContext getServerAuthContext(MessageInfo info, Subject s) 
    throws AuthException {
	ServerAuthConfig c = (ServerAuthConfig)getAuthConfig(true);
	if (c != null) {
            addModel(info, map);
            addPolicy(info,map);
	    return c.getAuthContext(c.getAuthContextID(info),s,map);
	}
	return null;
    }

    public static Subject getClientSubject() {

	Subject s = null;

        if ((SecurityServicesUtil.getInstance() == null) || SecurityServicesUtil.getInstance().isACC()) {
            ClientSecurityContext sc = ClientSecurityContext.getCurrent();
            if (sc != null) {
                s = sc.getSubject();
            }
            if (s == null) {
                s = Subject.getSubject(AccessController.getContext());
            }
        } else {
            SecurityContext sc = SecurityContext.getCurrent();
            if (sc != null && !sc.didServerGenerateCredentials()) {
                // make sure we don't use default unauthenticated subject,
                // so that module cannot change this important (constant)
                // subject.
                s = sc.getSubject();
            }
        }

	if (s == null) {
	    s = new Subject();
	}

	return s;
    }

    public void getSessionToken(Map m, 
				MessageInfo info, 
				Subject s) throws AuthException {
	ClientAuthConfig c = (ClientAuthConfig) getAuthConfig(false);    
	if (c != null) {
	    m.putAll(map);
            addModel(info, map);
	    c.getAuthContext(c.getAuthContextID(info),s,m);
	}
    }

    public void authorize(Packet request) throws Exception {

	// SecurityContext constructor should set initiator to
	// unathenticated if Subject is null or empty
	Subject s = (Subject) request.invocationProperties.get(PipeConstants.CLIENT_SUBJECT);

        if (s == null || (s.getPrincipals().isEmpty() && s.getPublicCredentials().isEmpty())) {
            SecurityContext.setUnauthenticatedContext();
        } else {
	    SecurityContext sC = new SecurityContext(s);
            SecurityContext.setCurrent(sC);
        }

	// we should try to replace this endpoint specific
	// authorization check with a generic web service message check
	// and move the endpoint specific check down stream
        
	if (isEjbEndpoint) {
            if (invManager == null){
                throw new RuntimeException(localStrings.getLocalString("enterprise.webservice.noEjbInvocationManager",
                        "Cannot validate request : invocation manager null for EJB WebService"));
            }
            ComponentInvocation inv = (ComponentInvocation) invManager.getCurrentInvocation();
            // one need to copy message here, otherwise the message may be
            // consumed
            if (ejbDelegate != null) {
                ejbDelegate.setSOAPMessage(request.getMessage(), inv);
            }
            Exception ie;
            Method m = null;
            if (seiModel != null) {
                JavaMethod jm = request.getMessage().getMethod(seiModel);
                m = (jm != null) ? jm.getMethod() : null;
            } else { // WebServiceProvider

                WebServiceEndpoint endpoint = (WebServiceEndpoint) map.get(PipeConstants.SERVICE_ENDPOINT);
                EjbDescriptor ejbDescriptor = endpoint.getEjbComponentImpl();
                if (ejbDescriptor != null) {
                    final String ejbImplClassName = ejbDescriptor.getEjbImplClassName();
                    if (ejbImplClassName != null) {
                        try {
                            m = (Method) AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {

                                @Override
                                public Object run() throws Exception {
                                    ClassLoader loader =
                                            Thread.currentThread().getContextClassLoader();
                                    Class clazz =
                                            Class.forName(ejbImplClassName, true, loader);
                                    return clazz.getMethod("invoke",
                                            new Class[]{Object.class                                            });
                                }
                            });
                        } catch (PrivilegedActionException pae) {
                            throw new RuntimeException(pae.getException());
                        }
                    }
                }

            }
            if (m != null) {
                if (ejbDelegate != null) {
                    try {
                        if (!ejbDelegate.authorize(inv, m)) {
                            throw new Exception(localStrings.getLocalString("enterprise.webservice.methodNotAuth",
                                    "Client not authorized for invocation of {0}",
                                    new Object[]{m}));
                        }
                    } catch (UnmarshalException e) {
                        String errorMsg = localStrings.getLocalString("enterprise.webservice.errorUnMarshalMethod",
                                "Error unmarshalling method for ejb {0}",
                                new Object[]{ejbName()});
                        ie = new UnmarshalException(errorMsg);
                        ie.initCause(e);
                        throw ie;
                    } catch (Exception e) {
                        ie = new Exception(localStrings.getLocalString("enterprise.webservice.methodNotAuth",
                                "Client not authorized for invocation of {0}",
                                new Object[]{m}));
                        ie.initCause(e);
                        throw ie;
                    }

                }
            }
        }
    }
	
    public void auditInvocation(Packet request, AuthStatus status) {

	if (auditManager.isAuditOn()) {
	    String uri = null;
	    if (!isEjbEndpoint && request != null &&
                    request.supports(MessageContext.SERVLET_REQUEST)) {
                HttpServletRequest httpServletRequest =
                    (HttpServletRequest)request.get(
                    MessageContext.SERVLET_REQUEST);
                uri = httpServletRequest.getRequestURI();
	    } 
            String endpointName = null;
            if (map != null) {
                WebServiceEndpoint endpoint = (WebServiceEndpoint)
                       map.get(PipeConstants.SERVICE_ENDPOINT);
                if (endpoint != null) {
                    endpointName = endpoint.getEndpointName();
                }
            }
            if (endpointName == null) {
                endpointName = "(no endpoint)";
            }
            
            if (isEjbEndpoint) {
                auditManager.ejbAsWebServiceInvocation(
                    endpointName, AuthStatus.SUCCESS.equals(status));
            } else {
                auditManager.webServiceInvocation(
                    ((uri==null) ? "(no uri)" : uri), 
                    endpointName, AuthStatus.SUCCESS.equals(status));
            }
	}
    }

    public Object getModelName() { 
 	WSDLPort wsdlModel = (WSDLPort) getProperty(PipeConstants.WSDL_MODEL);
 	return (wsdlModel == null ? "unknown" : wsdlModel.getName());
    }

    @Deprecated // should be unused, but left for compilation
    public void  addModelAndPolicy(Packet request) {
    }
  
    // always returns response with embedded fault
    //public static Packet makeFaultResponse(Packet response, Throwable t) {
    public Packet makeFaultResponse(Packet response, Throwable t) {
	// wrap throwable in WebServiceException, if necessary
	if (!(t instanceof WebServiceException)) {
	    t = (Throwable) new WebServiceException(t);
	}
 	if (response == null) {
 	    response = new Packet();
  	} 
	// try to create fault in provided response packet, if an exception
	// is thrown, create new packet, and create fault in it.
	try {
	    return response.createResponse(Messages.create(t, this.soapVersion));
	} catch (Exception e) {
	    response = new Packet();
	}
 	return response.createResponse(Messages.create(t, this.soapVersion));
    }
    
    public boolean isTwoWay(boolean twoWayIsDefault, Packet request) { 
 	boolean twoWay = twoWayIsDefault;
 	Message m = request.getMessage();
 	if (m != null) {
	    WSDLPort wsdlModel =
		(WSDLPort) getProperty(PipeConstants.WSDL_MODEL);
	    if (wsdlModel != null) {
		twoWay = (m.isOneWay(wsdlModel) ? false : true);
	    }
	}
 	return twoWay;
    }
 
    // returns empty response if request is determined to be one-way
    public Packet getFaultResponse(Packet request, Packet response, 
	Throwable t) {
	boolean twoWay = true;
	try {
	    twoWay = isTwoWay(true,request);
	} catch (Exception e) {
	    // exception is consumed, and twoWay is assumed
 	} 
	if (twoWay) {
	    return makeFaultResponse(response,t);
 	} else {
	    return new Packet();
	}
    }
 
    @Override
    public void disable() {
	listenerWrapper.disableWithRefCount();
    }
    
    @Override
    protected HandlerContext getHandlerContext(Map map) {
        String realmName = null;
        WebServiceEndpoint wSE = (WebServiceEndpoint)
                map.get(PipeConstants.SERVICE_ENDPOINT);
        if (wSE != null) {
            Application app = wSE.getBundleDescriptor().getApplication();
            if (app != null) {
                realmName = app.getRealm();
            }
            if (realmName == null) {
                realmName = wSE.getRealm();
            }
        }

        final String fRealmName = realmName;
        return new HandlerContext() {
            @Override
            public String getRealmName() {
                return fRealmName;
            }
        };
    }

    private boolean processSunDeploymentDescriptor() {

	if (factory == null) {
	    return false;
	}

	MessageSecurityBindingDescriptor binding =
	    AuthMessagePolicy.getMessageSecurityBinding
	    (PipeConstants.SOAP_LAYER,map);

	if (binding != null) {
	    if (!hasExactMatchAuthProvider()) {
		String jmacProviderRegisID = factory.registerConfigProvider(
                    new GFServerConfigProvider(null, null),
                    layer, appCtxt,
                    "GF AuthConfigProvider bound by Sun Specific Descriptor");
                this.setJmacProviderRegisID(jmacProviderRegisID);
	    }
	}

	WebServiceEndpoint e = (WebServiceEndpoint)
	    map.get(PipeConstants.SERVICE_ENDPOINT);

	return (e == null ? false : e.implementedByEjbComponent());
    }

    private static String getAppCtxt(Map map) {

        String rvalue;
        WebServiceEndpoint wse = 
            (WebServiceEndpoint) map.get(PipeConstants.SERVICE_ENDPOINT);
        // endpoint
        if (wse != null) {
            rvalue = getServerName(wse) + " " + getEndpointURI(wse);
        // client reference
        } else {
            ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) 
                map.get(PipeConstants.SERVICE_REF);

            rvalue = getClientModuleID(srd) + " " + getRefName(srd,map);
        }
        return rvalue;
    }

    private static String getServerName(WebServiceEndpoint wse) {
        //XXX FIX ME: need to lookup real hostname
        String hostname = "localhost"; 
        return hostname;
    }

    private static String getRefName(ServiceReferenceDescriptor srd, Map map) {

        String name = null;
        if (srd != null) {
            name = srd.getName();
        }
        if (name == null) {
            EndpointAddress ea = 
                (EndpointAddress) map.get(PipeConstants.ENDPOINT_ADDRESS);
            if (ea != null) {
                URL url = ea.getURL();
                if (url != null) {
                    name = url.toString();
                }
            }
        }
        if (name == null) {
            name = "#default-ref-name#";
        }
        return name;
    }

    private static String getEndpointURI(WebServiceEndpoint wse) {

        String uri = "#default-endpoint-context#";

        if (wse != null) {
            uri = wse.getEndpointAddressUri();
            if (uri != null && (!uri.startsWith("/"))) {
                uri = "/" + uri;
            }

            if (wse.implementedByWebComponent()) {
                WebBundleDescriptor wbd = (WebBundleDescriptor)
                    wse.getBundleDescriptor();
                if (wbd != null) {
                    String contextRoot = wbd.getContextRoot();
                    if (contextRoot != null) {
                        if (!contextRoot.startsWith("/")) {
                            contextRoot = "/" + contextRoot;
                        }
                        uri = contextRoot + uri;
                    }
                }
            }
        }
        return uri;
    }

    private static String getClientModuleID(ServiceReferenceDescriptor srd) {

        String rvalue = "#default-client-context#";

        if (srd != null) {
            ModuleDescriptor md = null;
            BundleDescriptor bd = (BundleDescriptor) srd.getBundleDescriptor();

            if (bd != null) {
                md = bd.getModuleDescriptor();
            }

            Application a = (bd == null) ? null : bd.getApplication();
            if (a != null) {
                if (a.isVirtual()) {
                    rvalue = a.getRegistrationName();
                } else if (md != null) {
                    rvalue = FileUtils.makeFriendlyFilename(md.getArchiveUri());
                } 
            } else if (md != null) {
                rvalue = FileUtils.makeFriendlyFilename(md.getArchiveUri());
            }
        } 

        return rvalue;
    }

    private static void addModel(MessageInfo info, Map map) {
        Object model = map.get(PipeConstants.WSDL_MODEL);
        if (model != null) {
            info.getMap().put(PipeConstants.WSDL_MODEL,model);
        }
    }

    private static void addPolicy(MessageInfo info, Map map) {
        Object pol = map.get(PipeConstants.POLICY);
        if (pol != null) {
            info.getMap().put(PipeConstants.POLICY,pol);
        }
    }

    private String ejbName() { 
	WebServiceEndpoint wSE = (WebServiceEndpoint) 
	    getProperty(PipeConstants.SERVICE_ENDPOINT);
	return (wSE == null ? "unknown" : wSE.getEjbComponentImpl().getName());
    }
}
