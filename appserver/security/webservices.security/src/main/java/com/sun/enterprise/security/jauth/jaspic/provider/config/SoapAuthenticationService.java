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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jauth.jaspic.provider.config;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.appclient.ConfigXMLParser;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.common.ClientSecurityContext;
import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.ee.authentication.jakarta.AuthMessagePolicy;
import com.sun.enterprise.security.ee.authentication.jakarta.ConfigDomainParser;
import com.sun.enterprise.security.ee.authentication.jakarta.WebServicesDelegate;
import com.sun.enterprise.security.ee.authorization.EJBPolicyContextDelegate;
import com.sun.enterprise.security.webservices.PipeConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ClientAuthContext;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.epicyro.config.module.configprovider.GFServerConfigProvider;
import org.glassfish.epicyro.services.BaseAuthenticationService;
import org.glassfish.epicyro.services.RegistrationWrapperRemover;
import org.glassfish.internal.api.Globals;

import static com.sun.enterprise.security.webservices.PipeConstants.*;
import static com.sun.xml.ws.api.SOAPVersion.SOAP_11;

public class SoapAuthenticationService extends BaseAuthenticationService {
    
    protected static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(PipeConstants.class);
    
    private AppServerAuditManager auditManager;
    private boolean isEjbEndpoint;
    private SEIModel seiModel;
    private SOAPVersion soapVersion;
    private InvocationManager invManager;
    private EJBPolicyContextDelegate ejbDelegate;

    public SoapAuthenticationService(String layer, Map<String, Object> properties, CallbackHandler callbackHandler) {
        init(layer, getAppCtxt(properties), properties, callbackHandler, null);

        isEjbEndpoint = processSunDeploymentDescriptor();
        seiModel = (SEIModel) properties.get(SEI_MODEL);
        
        WSBinding binding = (WSBinding) properties.get(BINDING);
        if (binding == null) {
            WSEndpoint endPoint = (WSEndpoint) properties.get(ENDPOINT);
            if (endPoint != null) {
                binding = endPoint.getBinding();
            }
        }
        
        soapVersion = binding != null ? binding.getSOAPVersion() : SOAP_11;
        AuditManager am = SecurityServicesUtil.getInstance() != null ? SecurityServicesUtil.getInstance().getAuditManager() : null;
        auditManager = am instanceof AppServerAuditManager ? (AppServerAuditManager) am : new AppServerAuditManager();// workaround
                                                                                                                                        // habitat
        invManager = SecurityServicesUtil.getInstance() != null
                ? SecurityServicesUtil.getInstance().getHabitat().<InvocationManager>getService(InvocationManager.class)
                : null;

        this.ejbDelegate = new EJBPolicyContextDelegate();
    }

    @Override
    public ClientAuthContext getClientAuthContext(MessageInfo info, Subject subject) throws AuthException {
        ClientAuthConfig clientConfig = (ClientAuthConfig) getAuthConfig(false);
        
        if (clientConfig == null) {
            return null;
        }
        
        addModel(info, map);
        
        return clientConfig.getAuthContext(clientConfig.getAuthContextID(info), subject, map);
    }

    @Override
    public ServerAuthContext getServerAuthContext(MessageInfo info, Subject subject) throws AuthException {
        ServerAuthConfig serverConfig = (ServerAuthConfig) getAuthConfig(true);
        
        if (serverConfig == null) {
            return null;
        }
        
        addModel(info, map);
        addPolicy(info, map);
        
        return serverConfig.getAuthContext(serverConfig.getAuthContextID(info), subject, map);
        
    }

    public static Subject getClientSubject() {

        Subject subject = null;

        if (SecurityServicesUtil.getInstance() == null || SecurityServicesUtil.getInstance().isACC()) {
            ClientSecurityContext clientSecurityContext = ClientSecurityContext.getCurrent();
            if (clientSecurityContext != null) {
                subject = clientSecurityContext.getSubject();
            }
        } else {
            SecurityContext securityContext = SecurityContext.getCurrent();
            if (securityContext != null && !securityContext.didServerGenerateCredentials()) {
                // make sure we don't use default unauthenticated subject,
                // so that module cannot change this important (constant)
                // subject.
                subject = securityContext.getSubject();
            }
        }

        if (subject == null) {
            subject = new Subject();
        }

        return subject;
    }

    public void getSessionToken(Map<String, Object> map, MessageInfo info, Subject subject) throws AuthException {
        ClientAuthConfig clientAuthConfig = (ClientAuthConfig) getAuthConfig(false);
        if (clientAuthConfig != null) {
            map.putAll(map);
            addModel(info, map);
            clientAuthConfig.getAuthContext(clientAuthConfig.getAuthContextID(info), subject, map);
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
            if (invManager == null) {
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
                            m = Class.forName(ejbImplClassName, true, Thread.currentThread()
                                    .getContextClassLoader())
                                    .getMethod("invoke", new Class[]{Object.class});

                        } catch (ReflectiveOperationException pae) {
                            throw new RuntimeException(pae);
                        }
                    }
                }

            }
            if (m != null) {
                if (ejbDelegate != null) {
                    try {
                        if (!ejbDelegate.authorize(inv, m)) {
                            throw new Exception(localStrings.getLocalString("enterprise.webservice.methodNotAuth",
                                    "Client not authorized for invocation of {0}", new Object[] { m }));
                        }
                    } catch (UnmarshalException e) {
                        String errorMsg = localStrings.getLocalString("enterprise.webservice.errorUnMarshalMethod",
                                "Error unmarshalling method for ejb {0}", new Object[] { ejbName() });
                        ie = new UnmarshalException(errorMsg);
                        ie.initCause(e);
                        throw ie;
                    } catch (Exception e) {
                        ie = new Exception(localStrings.getLocalString("enterprise.webservice.methodNotAuth",
                                "Client not authorized for invocation of {0}", new Object[] { m }));
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
            if (!isEjbEndpoint && request != null && request.supports(MessageContext.SERVLET_REQUEST)) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request.get(MessageContext.SERVLET_REQUEST);
                uri = httpServletRequest.getRequestURI();
            }
            String endpointName = null;
            if (map != null) {
                WebServiceEndpoint endpoint = (WebServiceEndpoint) map.get(PipeConstants.SERVICE_ENDPOINT);
                if (endpoint != null) {
                    endpointName = endpoint.getEndpointName();
                }
            }
            if (endpointName == null) {
                endpointName = "(no endpoint)";
            }

            if (isEjbEndpoint) {
                auditManager.ejbAsWebServiceInvocation(endpointName, AuthStatus.SUCCESS.equals(status));
            } else {
                auditManager.webServiceInvocation(((uri == null) ? "(no uri)" : uri), endpointName, AuthStatus.SUCCESS.equals(status));
            }
        }
    }

    public Object getModelName() {
        WSDLPort wsdlModel = (WSDLPort) getProperty(PipeConstants.WSDL_MODEL);
        return (wsdlModel == null ? "unknown" : wsdlModel.getName());
    }

    @Deprecated // should be unused, but left for compilation
    public void addModelAndPolicy(Packet request) {
    }

    // always returns response with embedded fault
    // public static Packet makeFaultResponse(Packet response, Throwable t) {
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
            WSDLPort wsdlModel = (WSDLPort) getProperty(PipeConstants.WSDL_MODEL);
            if (wsdlModel != null) {
                twoWay = (m.isOneWay(wsdlModel) ? false : true);
            }
        }
        return twoWay;
    }

    // returns empty response if request is determined to be one-way
    public Packet getFaultResponse(Packet request, Packet response, Throwable t) {
        boolean twoWay = true;
        try {
            twoWay = isTwoWay(true, request);
        } catch (Exception e) {
            // exception is consumed, and twoWay is assumed
        }
        if (twoWay) {
            return makeFaultResponse(response, t);
        } else {
            return new Packet();
        }
    }

    @Override
    public void disable() {
        getRegistrationWrapper().disableWithRefCount();
    }

    private boolean processSunDeploymentDescriptor() {
        if (authConfigFactory == null) {
            return false;
        }

        MessageSecurityBindingDescriptor binding = AuthMessagePolicy.getMessageSecurityBinding(com.sun.xml.wss.provider.wsit.PipeConstants.SOAP_LAYER, map);

        Function<MessageInfo, String> authContextIdGenerator =
                e -> Globals.get(WebServicesDelegate.class).getAuthContextID(e);

        BiFunction<String, Map<String, Object>, MessagePolicy[]> soapPolicyGenerator =
                (authContextId, properties) -> AuthMessagePolicy.getSOAPPolicies(
                        AuthMessagePolicy.getMessageSecurityBinding("SOAP", properties),
                        authContextId, true);

        String authModuleId = AuthMessagePolicy.getProviderID(binding);

        map.put("authContextIdGenerator", authContextIdGenerator);
        map.put("soapPolicyGenerator", soapPolicyGenerator);

        if (authModuleId != null) {
            map.put("authModuleId", authModuleId);
        }

        if (binding != null) {
            if (!hasExactMatchAuthProvider()) {
                String jmacProviderRegisID = authConfigFactory.registerConfigProvider(
                        new GFServerConfigProvider(
                                map,
                                isACC()? new ConfigXMLParser() : new ConfigDomainParser(),
                                authConfigFactory),
                        messageLayer, appContextId,
                        "GF AuthConfigProvider bound by Sun Specific Descriptor");

                setRegistrationId(jmacProviderRegisID);
            }
        }

        WebServiceEndpoint webServiceEndpoint = (WebServiceEndpoint) map.get(SERVICE_ENDPOINT);
        return webServiceEndpoint == null ? false : webServiceEndpoint.implementedByEjbComponent();
    }

    private static String getAppCtxt(Map map) {

        String rvalue;
        WebServiceEndpoint wse = (WebServiceEndpoint) map.get(SERVICE_ENDPOINT);
        
        // endpoint
        if (wse != null) {
            rvalue = getServerName(wse) + " " + getEndpointURI(wse);
            // client reference
        } else {
            ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) map.get(PipeConstants.SERVICE_REF);

            rvalue = getClientModuleID(srd) + " " + getRefName(srd, map);
        }
        return rvalue;
    }

    private static String getServerName(WebServiceEndpoint wse) {
        // XXX FIX ME: need to lookup real hostname
        String hostname = "localhost";
        return hostname;
    }

    private static String getRefName(ServiceReferenceDescriptor srd, Map map) {

        String name = null;
        if (srd != null) {
            name = srd.getName();
        }
        if (name == null) {
            EndpointAddress ea = (EndpointAddress) map.get(PipeConstants.ENDPOINT_ADDRESS);
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
                WebBundleDescriptor wbd = (WebBundleDescriptor) wse.getBundleDescriptor();
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
            info.getMap().put(PipeConstants.WSDL_MODEL, model);
        }
    }

    private static void addPolicy(MessageInfo info, Map map) {
        Object pol = map.get(PipeConstants.POLICY);
        if (pol != null) {
            info.getMap().put(PipeConstants.POLICY, pol);
        }
    }

    private String ejbName() {
        WebServiceEndpoint wSE = (WebServiceEndpoint) getProperty(PipeConstants.SERVICE_ENDPOINT);
        return (wSE == null ? "unknown" : wSE.getEjbComponentImpl().getName());
    }

    private static boolean isACC() {
        return SecurityServicesUtil.getInstance() == null || SecurityServicesUtil.getInstance().isACC();
    }
}
