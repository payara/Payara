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

package com.sun.enterprise.security.jmac;

import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler; 
import javax.security.auth.message.MessagePolicy;
import static javax.security.auth.message.MessagePolicy.*;


import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.jmac.config.HttpServletConstants;
import org.glassfish.internal.api.Globals;
import static com.sun.enterprise.security.jmac.config.GFServerConfigProvider.SOAP;
//V3:Commented webservices support
//import com.sun.xml.ws.api.model.wsdl.WSDLPort;


/**
 * Utility class for JMAC appserver implementation.
 */
public class AuthMessagePolicy {

    private static final String SENDER = "sender";
    private static final String CONTENT = "content";
    private static final String BEFORE_CONTENT = "before-content";
    private static final String HANDLER_CLASS_PROPERTY =
        "security.jmac.config.ConfigHelper.CallbackHandler";
    private static final String DEFAULT_HANDLER_CLASS =
        "com.sun.enterprise.security.jmac.callback.ContainerCallbackHandler"; 

    // for HttpServlet profile
    private static final MessagePolicy MANDATORY_POLICY =
            getMessagePolicy(SENDER, null, true);
    private static final MessagePolicy OPTIONAL_POLICY =
            getMessagePolicy(SENDER, null, false);

    private static String handlerClassName = null;

    private AuthMessagePolicy() {};

    public static MessageSecurityBindingDescriptor
    getMessageSecurityBinding(String layer, Map properties) {
	     
	if (properties == null) {
	    return null;
	}

	MessageSecurityBindingDescriptor binding = null;

	WebServiceEndpoint e = (WebServiceEndpoint) 
	    properties.get("SERVICE_ENDPOINT");

	if (e != null) {
	    binding = e.getMessageSecurityBinding();
	} else {
	    ServiceReferenceDescriptor s = (ServiceReferenceDescriptor) 
		properties.get("SERVICE_REF");
	    if (s != null) {
                WebServicesDelegate delegate = Globals.get(WebServicesDelegate.class);
                if (delegate != null) {
                    binding = delegate.getBinding(s, properties);
                }
	    }
	}

	if (binding != null) {
	    String bindingLayer = binding.getAttributeValue
		(MessageSecurityBindingDescriptor.AUTH_LAYER);
	    if (bindingLayer == null || layer.equals(bindingLayer)) {
		return binding;
	    }
	}

	return null;
    }

    public static MessagePolicy getMessagePolicy (String authSource, 
            String authRecipient) {
        boolean sourceSender = SENDER.equals(authSource);
        boolean sourceContent = CONTENT.equals(authSource);
        boolean recipientAuth = (authRecipient != null);
        boolean mandatory =
                (sourceSender || sourceContent) || recipientAuth;
        return getMessagePolicy(authSource, authRecipient, mandatory);
    }

    public static MessagePolicy getMessagePolicy (String authSource, 
            String authRecipient, boolean mandatory) {

        boolean sourceSender = SENDER.equals(authSource);
        boolean sourceContent = CONTENT.equals(authSource);
        boolean recipientAuth = (authRecipient != null);
        boolean beforeContent = BEFORE_CONTENT.equals(authRecipient);

        List<TargetPolicy> targetPolicies = new ArrayList<TargetPolicy>();
        if (recipientAuth && beforeContent) {
            targetPolicies.add(new TargetPolicy(null,
                new ProtectionPolicy() {
                    public String getID() {
                        return ProtectionPolicy.AUTHENTICATE_RECIPIENT;
                    } 
                })
            );
            if (sourceSender) {
                targetPolicies.add(new TargetPolicy(null,
                    new ProtectionPolicy() {
                        public String getID() {
                            return ProtectionPolicy.AUTHENTICATE_SENDER;
                        }
                    })
                );
            } else if (sourceContent) {
                targetPolicies.add(new TargetPolicy(null,
                    new ProtectionPolicy() {
                        public String getID() {
                            return ProtectionPolicy.AUTHENTICATE_CONTENT;
                        }
                    })
                );
            }
        } else {
            if (sourceSender) {
                targetPolicies.add(new TargetPolicy(null,
                    new ProtectionPolicy() {
                        public String getID() {
                            return ProtectionPolicy.AUTHENTICATE_SENDER;
                        } 
                    })
                );
            } else if (sourceContent) {
                targetPolicies.add(new TargetPolicy(null,
                    new ProtectionPolicy() {
                        public String getID() {
                            return ProtectionPolicy.AUTHENTICATE_CONTENT;
                        } 
                    })
                );
            }

            if (recipientAuth) {
                targetPolicies.add(new TargetPolicy(null,
                    new ProtectionPolicy() {
                        public String getID() {
                            return ProtectionPolicy.AUTHENTICATE_RECIPIENT;
                        } 
                    })
                );
            }
        }

        return new MessagePolicy(
                 targetPolicies.toArray(
                        new TargetPolicy[targetPolicies.size()]),
                 mandatory);
    }

    public static MessagePolicy getMessagePolicy(ProtectionDescriptor pd) {
	MessagePolicy messagePolicy = null;
        if (pd != null) {
            String source = pd.getAttributeValue
                (ProtectionDescriptor.AUTH_SOURCE);
            String recipient = pd.getAttributeValue
                (ProtectionDescriptor.AUTH_RECIPIENT);
            messagePolicy = getMessagePolicy(source, recipient);
        }
        return messagePolicy;
    }

    public static String getProviderID
    (MessageSecurityBindingDescriptor binding) {
        String providerID = null;
        if (binding != null) {
            String layer = binding.getAttributeValue
                (MessageSecurityBindingDescriptor.AUTH_LAYER);
            if (SOAP.equals(layer)) {
                providerID = binding.getAttributeValue
                    (MessageSecurityBindingDescriptor.PROVIDER_ID);
            }
        }
        return providerID;
    }

    public static MessagePolicy[] 
    getSOAPPolicies( MessageSecurityBindingDescriptor binding, 
		     String operation, boolean onePolicy) {

	MessagePolicy requestPolicy = null;
	MessagePolicy responsePolicy = null;

        if (binding != null) {
            ArrayList<MessageSecurityDescriptor> msgSecDescs = null;
            String layer = binding.getAttributeValue
                (MessageSecurityBindingDescriptor.AUTH_LAYER);
            if (SOAP.equals(layer)) {
                msgSecDescs = binding.getMessageSecurityDescriptors();
            }

            if(msgSecDescs != null) {
                if (onePolicy) {
                    if (msgSecDescs.size() > 0) {
                        MessageSecurityDescriptor msd = msgSecDescs.get(0);
                        requestPolicy = getMessagePolicy(
                                msd.getRequestProtectionDescriptor());
                        responsePolicy = getMessagePolicy(
                                msd.getResponseProtectionDescriptor());
                    }
                } else { // try to match
                    MessageSecurityDescriptor matchMsd = null;
                    for (int i = 0; i < msgSecDescs.size(); i++) {
                        MessageSecurityDescriptor msd = msgSecDescs.get(i);
                        ArrayList msgDescs = msd.getMessageDescriptors();
                        for (int j = i + 1; j < msgDescs.size(); j++) {
                            //XXX don't know how to get JavaMethod from operation
                            MessageDescriptor msgDesc =
                                    (MessageDescriptor) msgDescs.get(j);
                            String opName = msgDesc.getOperationName();
                            if ((opName == null && matchMsd == null)) {
                                matchMsd = msd;
                            } else if (opName != null && opName.equals(operation)) {
                                matchMsd = msd;
                                break;
                            }
                        }

                        if (matchMsd != null) {
                            requestPolicy = getMessagePolicy(
                                    matchMsd.getRequestProtectionDescriptor());
                            responsePolicy = getMessagePolicy(
                                    matchMsd.getResponseProtectionDescriptor());
                        }
                    }
                }
            }
        }

        return new MessagePolicy[] { requestPolicy, responsePolicy };
    }

    public static boolean oneSOAPPolicy
	(MessageSecurityBindingDescriptor binding) {

        boolean onePolicy = true;
        ArrayList msgSecDescs = null;
        if (binding != null) {
            String layer = binding.getAttributeValue
                (MessageSecurityBindingDescriptor.AUTH_LAYER);
            if (SOAP.equals(layer)) {
                msgSecDescs = binding.getMessageSecurityDescriptors();
            }
        }

	if (msgSecDescs == null) {
	    return true;
	}

        for (int i = 0; i < msgSecDescs.size(); i++) {

            MessageSecurityDescriptor msd = 
                (MessageSecurityDescriptor) msgSecDescs.get(i);

            // determine if all the different messageSecurityDesriptors have the
            // same policy which will help us interpret the effective policy if
            // we cannot determine the opcode of a request at runtime.

            for (int j = 0; j < msgSecDescs.size(); j++) {
                if (j != i && !policiesAreEqual
                    (msd,((MessageSecurityDescriptor)msgSecDescs.get(j)))) {
                    onePolicy = false;
                }
            }
        }

        return onePolicy;
    }

    public static SunWebApp getSunWebApp(Map properties) {
        if (properties == null) {
            return null;
        }

        WebBundleDescriptor webBundle =
            (WebBundleDescriptor)properties.get(HttpServletConstants.WEB_BUNDLE);
        return webBundle.getSunDescriptor();
    }

    public static String getProviderID(SunWebApp sunWebApp) {
        String providerID = null;
        if (sunWebApp != null) {
            providerID = sunWebApp.getAttributeValue(
                    SunWebApp.HTTPSERVLET_SECURITY_PROVIDER);
        }
        return providerID;
    }

    public static MessagePolicy[] getHttpServletPolicies(
            String authContextID) {
        if (Boolean.valueOf(authContextID)) {
            return new MessagePolicy[] { MANDATORY_POLICY, null };
        } else {
            return new MessagePolicy[] { OPTIONAL_POLICY, null };
        }
    }

    public static CallbackHandler getDefaultCallbackHandler() {
        // get the default handler class
        try {
            CallbackHandler rvalue = 
                (CallbackHandler)AppservAccessController.doPrivileged
                (new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        ClassLoader loader =
                            Thread.currentThread().getContextClassLoader();
                        if (handlerClassName == null) {
                            handlerClassName = System.getProperty(
                                HANDLER_CLASS_PROPERTY, DEFAULT_HANDLER_CLASS);
                        }
                        final String className = handlerClassName;
                        Class c = Class.forName(className, true, loader);
                        return c.newInstance();
                    }
                });
            return rvalue;

        } catch(PrivilegedActionException pae) {
            throw new RuntimeException(pae.getException());
        }
    }


    private static boolean policiesAreEqual(
            MessageSecurityDescriptor reference,
            MessageSecurityDescriptor other) {
        return (protectionDescriptorsAreEqual(
                reference.getRequestProtectionDescriptor(),
                other.getRequestProtectionDescriptor()) &&
            protectionDescriptorsAreEqual(
                reference.getResponseProtectionDescriptor(),
                other.getResponseProtectionDescriptor()));
    }

    private static boolean protectionDescriptorsAreEqual(
            ProtectionDescriptor pd1, ProtectionDescriptor pd2) {
        String authSource1 =
            pd1.getAttributeValue(ProtectionDescriptor.AUTH_SOURCE);
        String authRecipient1 = 
            pd1.getAttributeValue(ProtectionDescriptor.AUTH_RECIPIENT);

        String authSource2 =
            pd2.getAttributeValue(ProtectionDescriptor.AUTH_SOURCE);
        String authRecipient2 = 
            pd2.getAttributeValue(ProtectionDescriptor.AUTH_RECIPIENT);
        
        boolean sameAuthSource =
            (authSource1 == null && authSource2 == null) ||
            (authSource1 != null && authSource1.equals(authSource2));
        boolean sameAuthRecipient =
            (authRecipient1 == null && authRecipient2 == null) ||
            (authRecipient1 != null && authRecipient1.equals(authRecipient2));

        return sameAuthSource && sameAuthRecipient;
    }
}
