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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jaspic;

import static com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor.AUTH_LAYER;
import static com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor.PROVIDER_ID;
import static com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor.AUTH_RECIPIENT;
import static com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor.AUTH_SOURCE;
import static com.sun.enterprise.deployment.runtime.web.SunWebApp.HTTPSERVLET_SECURITY_PROVIDER;
import static com.sun.enterprise.security.common.AppservAccessController.doPrivileged;
import static com.sun.enterprise.security.jaspic.config.GFServerConfigProvider.SOAP;
import static com.sun.enterprise.security.jaspic.config.HttpServletConstants.WEB_BUNDLE;
import static jakarta.security.auth.message.MessagePolicy.ProtectionPolicy.AUTHENTICATE_CONTENT;
import static jakarta.security.auth.message.MessagePolicy.ProtectionPolicy.AUTHENTICATE_RECIPIENT;
import static jakarta.security.auth.message.MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.MessagePolicy.TargetPolicy;

import org.glassfish.internal.api.Globals;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;

/**
 * Utility class for JASPIC appserver implementation.
 */
public class AuthMessagePolicy {

    private static final String SENDER = "sender";
    private static final String CONTENT = "content";
    private static final String BEFORE_CONTENT = "before-content";
    private static final String HANDLER_CLASS_PROPERTY = "security.jaspic.config.ConfigHelper.CallbackHandler";
    private static final String DEFAULT_HANDLER_CLASS = "com.sun.enterprise.security.jaspic.callback.ContainerCallbackHandler";

    // for HttpServlet profile
    private static final MessagePolicy MANDATORY_POLICY = getMessagePolicy(SENDER, null, true);
    private static final MessagePolicy OPTIONAL_POLICY = getMessagePolicy(SENDER, null, false);

    private static String handlerClassName;

    private AuthMessagePolicy() {
    }

    public static MessageSecurityBindingDescriptor getMessageSecurityBinding(String layer, Map<String, ?> properties) {
        if (properties == null) {
            return null;
        }

        MessageSecurityBindingDescriptor messageSecurityBinding = null;

        WebServiceEndpoint webServiceEndpoint = (WebServiceEndpoint) properties.get("SERVICE_ENDPOINT");

        if (webServiceEndpoint != null) {
            messageSecurityBinding = webServiceEndpoint.getMessageSecurityBinding();
        } else {
            ServiceReferenceDescriptor serviceReference = (ServiceReferenceDescriptor) properties.get("SERVICE_REF");
            if (serviceReference != null) {
                WebServicesDelegate webServicesDelegate = Globals.get(WebServicesDelegate.class);
                if (webServicesDelegate != null) {
                    messageSecurityBinding = webServicesDelegate.getBinding(serviceReference, properties);
                }
            }
        }

        if (messageSecurityBinding != null) {
            String bindingLayer = messageSecurityBinding.getAttributeValue(AUTH_LAYER);
            if (bindingLayer == null || layer.equals(bindingLayer)) {
                return messageSecurityBinding;
            }
        }

        return null;
    }

    public static MessagePolicy getMessagePolicy(String authSource, String authRecipient) {
        boolean sourceSender = SENDER.equals(authSource);
        boolean sourceContent = CONTENT.equals(authSource);
        boolean recipientAuth = authRecipient != null;
        boolean mandatory = (sourceSender || sourceContent) || recipientAuth;

        return getMessagePolicy(authSource, authRecipient, mandatory);
    }

    public static MessagePolicy getMessagePolicy(String authSource, String authRecipient, boolean mandatory) {
        boolean sourceSender = SENDER.equals(authSource);
        boolean sourceContent = CONTENT.equals(authSource);
        boolean recipientAuth = authRecipient != null;
        boolean beforeContent = BEFORE_CONTENT.equals(authRecipient);

        List<TargetPolicy> targetPolicies = new ArrayList<TargetPolicy>();

        if (recipientAuth && beforeContent) {
            targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_RECIPIENT));

            if (sourceSender) {
                targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_SENDER));
            } else if (sourceContent) {
                targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_CONTENT));
            }
        } else {
            if (sourceSender) {
                targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_SENDER));
            } else if (sourceContent) {
                targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_CONTENT));
            }

            if (recipientAuth) {
                targetPolicies.add(new TargetPolicy(null, () -> AUTHENTICATE_RECIPIENT));
            }
        }

        return new MessagePolicy(targetPolicies.toArray(new TargetPolicy[targetPolicies.size()]), mandatory);
    }

    public static MessagePolicy getMessagePolicy(ProtectionDescriptor protectionDescriptor) {
        if (protectionDescriptor == null) {
            return null;
        }

        String source = protectionDescriptor.getAttributeValue(AUTH_SOURCE);
        String recipient = protectionDescriptor.getAttributeValue(AUTH_RECIPIENT);

        return getMessagePolicy(source, recipient);
    }

    public static String getProviderID(MessageSecurityBindingDescriptor binding) {
        String providerID = null;
        if (binding != null) {
            String layer = binding.getAttributeValue(AUTH_LAYER);
            if (SOAP.equals(layer)) {
                providerID = binding.getAttributeValue(PROVIDER_ID);
            }
        }
        
        return providerID;
    }

    public static MessagePolicy[] getSOAPPolicies(MessageSecurityBindingDescriptor binding, String operation, boolean onePolicy) {

        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;

        if (binding != null) {
            List<MessageSecurityDescriptor> messageSecurityDescriptors = null;
            String layer = binding.getAttributeValue(AUTH_LAYER);
            if (SOAP.equals(layer)) {
                messageSecurityDescriptors = binding.getMessageSecurityDescriptors();
            }

            if (messageSecurityDescriptors != null) {
                if (onePolicy) {
                    if (messageSecurityDescriptors.size() > 0) {
                        MessageSecurityDescriptor msd = messageSecurityDescriptors.get(0);
                        requestPolicy = getMessagePolicy(msd.getRequestProtectionDescriptor());
                        responsePolicy = getMessagePolicy(msd.getResponseProtectionDescriptor());
                    }
                } else { // try to match
                    MessageSecurityDescriptor matchMsd = null;
                    for (int i = 0; i < messageSecurityDescriptors.size(); i++) {
                        MessageSecurityDescriptor msd = messageSecurityDescriptors.get(i);
                        List<MessageDescriptor> msgDescs = msd.getMessageDescriptors();
                        for (int j = i + 1; j < msgDescs.size(); j++) {
                            // XXX don't know how to get JavaMethod from operation
                            MessageDescriptor msgDesc = (MessageDescriptor) msgDescs.get(j);
                            String opName = msgDesc.getOperationName();
                            if ((opName == null && matchMsd == null)) {
                                matchMsd = msd;
                            } else if (opName != null && opName.equals(operation)) {
                                matchMsd = msd;
                                break;
                            }
                        }

                        if (matchMsd != null) {
                            requestPolicy = getMessagePolicy(matchMsd.getRequestProtectionDescriptor());
                            responsePolicy = getMessagePolicy(matchMsd.getResponseProtectionDescriptor());
                        }
                    }
                }
            }
        }

        return new MessagePolicy[] { requestPolicy, responsePolicy };
    }

    public static boolean oneSOAPPolicy(MessageSecurityBindingDescriptor binding) {
        boolean onePolicy = true;
        List<MessageSecurityDescriptor> messageSecurityDescriptor = null;
        
        if (binding != null) {
            if (SOAP.equals(binding.getAttributeValue(AUTH_LAYER))) {
                messageSecurityDescriptor = binding.getMessageSecurityDescriptors();
            }
        }

        if (messageSecurityDescriptor == null) {
            return true;
        }

        for (int i = 0; i < messageSecurityDescriptor.size(); i++) {

            MessageSecurityDescriptor msd = messageSecurityDescriptor.get(i);

            // Determine if all the different messageSecurityDesriptors have the
            // same policy which will help us interpret the effective policy if
            // we cannot determine the opcode of a request at runtime.

            for (int j = 0; j < messageSecurityDescriptor.size(); j++) {
                if (j != i && !policiesAreEqual(msd, messageSecurityDescriptor.get(j))) {
                    onePolicy = false;
                }
            }
        }

        return onePolicy;
    }

    public static SunWebApp getSunWebApp(Map<String, ?> properties) {
        if (properties == null) {
            return null;
        }

        return ((WebBundleDescriptor) properties.get(WEB_BUNDLE)).getSunDescriptor();
    }

    public static String getProviderID(SunWebApp sunWebApp) {
        String providerID = null;
        if (sunWebApp != null) {
            providerID = sunWebApp.getAttributeValue(HTTPSERVLET_SECURITY_PROVIDER);
        }
        
        return providerID;
    }

    public static MessagePolicy[] getHttpServletPolicies(String authContextID) {
        if (Boolean.valueOf(authContextID)) {
            return new MessagePolicy[] { MANDATORY_POLICY, null };
        }
            
        return new MessagePolicy[] { OPTIONAL_POLICY, null };
    }

    public static CallbackHandler getDefaultCallbackHandler() {
        // Get the default handler class
        try {
            return (CallbackHandler) doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (handlerClassName == null) {
                        handlerClassName = System.getProperty(HANDLER_CLASS_PROPERTY, DEFAULT_HANDLER_CLASS);
                    }
                    
                    return Class.forName(handlerClassName, true, loader)
                                .newInstance();
                }
            });

        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae.getException());
        }
    }

    private static boolean policiesAreEqual(MessageSecurityDescriptor reference, MessageSecurityDescriptor other) {
        return (protectionDescriptorsAreEqual(reference.getRequestProtectionDescriptor(), other.getRequestProtectionDescriptor())
                && protectionDescriptorsAreEqual(reference.getResponseProtectionDescriptor(), other.getResponseProtectionDescriptor()));
    }

    private static boolean protectionDescriptorsAreEqual(ProtectionDescriptor pd1, ProtectionDescriptor pd2) {
        String authSource1 = pd1.getAttributeValue(AUTH_SOURCE);
        String authRecipient1 = pd1.getAttributeValue(AUTH_RECIPIENT);

        String authSource2 = pd2.getAttributeValue(AUTH_SOURCE);
        String authRecipient2 = pd2.getAttributeValue(AUTH_RECIPIENT);

        boolean sameAuthSource = (authSource1 == null && authSource2 == null) || (authSource1 != null && authSource1.equals(authSource2));
        boolean sameAuthRecipient = (authRecipient1 == null && authRecipient2 == null)
                || (authRecipient1 != null && authRecipient1.equals(authRecipient2));

        return sameAuthSource && sameAuthRecipient;
    }
}
