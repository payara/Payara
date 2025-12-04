/*
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package com.sun.enterprise.security.ee.authentication.jakarta;

import static com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor.AUTH_LAYER;
import static com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor.PROVIDER_ID;
import static org.glassfish.epicyro.config.helper.HttpServletConstants.SOAP;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import jakarta.security.auth.message.MessagePolicy;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import org.glassfish.internal.api.Globals;

/**
 * Utility class for Jakarta Authentication appserver implementation.
 */
public class AuthMessagePolicy {

    public static final String WEB_BUNDLE = "WEB_BUNDLE";

    private static final String HANDLER_CLASS_PROPERTY = "security.jmac.config.ConfigHelper.CallbackHandler";
    private static final String DEFAULT_HANDLER_CLASS = "com.sun.enterprise.security.ee.authentication.jakarta.callback.ContainerCallbackHandler";

    private static String handlerClassName;

    private AuthMessagePolicy() {
    }

    public static MessageSecurityBindingDescriptor getMessageSecurityBinding(String layer, Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }

        MessageSecurityBindingDescriptor binding = null;

        WebServiceEndpoint webServiceEndpoint = (WebServiceEndpoint) properties.get("SERVICE_ENDPOINT");

        if (webServiceEndpoint != null) {
            binding = webServiceEndpoint.getMessageSecurityBinding();
        } else {
            ServiceReferenceDescriptor serviceReferenceDescriptor = (ServiceReferenceDescriptor) properties.get("SERVICE_REF");
            if (serviceReferenceDescriptor != null) {
                WebServicesDelegate delegate = Globals.get(WebServicesDelegate.class);
                if (delegate != null) {
                    binding = delegate.getBinding(serviceReferenceDescriptor, properties);
                }
            }
        }

        if (binding != null) {
            String bindingLayer = (String) binding.getValue(AUTH_LAYER);
            if (bindingLayer == null || layer.equals(bindingLayer)) {
                return binding;
            }
        }

        return null;
    }

    public static MessagePolicy getMessagePolicy(ProtectionDescriptor protectionDescriptor) {
        MessagePolicy messagePolicy = null;
        if (protectionDescriptor != null) {
            String source = protectionDescriptor.getAttributeValue(ProtectionDescriptor.AUTH_SOURCE);
            String recipient = protectionDescriptor.getAttributeValue(ProtectionDescriptor.AUTH_RECIPIENT);
            messagePolicy = org.glassfish.epicyro.config.helper.AuthMessagePolicy.getMessagePolicy(source, recipient);
        }

        return messagePolicy;
    }

    public static String getProviderID(MessageSecurityBindingDescriptor binding) {
        if (binding == null) {
            return null;
        }

        if (!SOAP.equals(binding.getValue(AUTH_LAYER))) {
            return null;
        }

        return binding.getValue(PROVIDER_ID);
    }

    public static MessagePolicy[] getSOAPPolicies(MessageSecurityBindingDescriptor binding, String operation, boolean onePolicy) {
        MessagePolicy requestPolicy = null;
        MessagePolicy responsePolicy = null;

        if (binding != null) {
            List<MessageSecurityDescriptor> messageSecurityDescriptors = null;
            String layer = binding.getValue(AUTH_LAYER);
            if (SOAP.equals(layer)) {
                messageSecurityDescriptors = binding.getMessageSecurityDescriptors();
            }

            if (messageSecurityDescriptors != null) {
                if (onePolicy) {
                    if (messageSecurityDescriptors.size() > 0) {
                        MessageSecurityDescriptor messageSecurityDescriptor = messageSecurityDescriptors.get(0);
                        requestPolicy = getMessagePolicy(messageSecurityDescriptor.getRequestProtectionDescriptor());
                        responsePolicy = getMessagePolicy(messageSecurityDescriptor.getResponseProtectionDescriptor());
                    }
                } else { // try to match
                    MessageSecurityDescriptor matchMsd = null;
                    for (int i = 0; i < messageSecurityDescriptors.size(); i++) {
                        MessageSecurityDescriptor msd = messageSecurityDescriptors.get(i);
                        List<MessageDescriptor> msgDescs = msd.getMessageDescriptors();
                        for (int j = i + 1; j < msgDescs.size(); j++) {
                            // XXX don't know how to get JavaMethod from operation
                            MessageDescriptor msgDesc = msgDescs.get(j);
                            String opName = msgDesc.getOperationName();
                            if (opName == null && matchMsd == null) {
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
        List<MessageSecurityDescriptor> msgSecDescs = null;
        if (binding != null) {
            String layer = binding.getAttributeValue(AUTH_LAYER);
            if (SOAP.equals(layer)) {
                msgSecDescs = binding.getMessageSecurityDescriptors();
            }
        }

        if (msgSecDescs == null) {
            return true;
        }

        for (int i = 0; i < msgSecDescs.size(); i++) {

            MessageSecurityDescriptor msd = msgSecDescs.get(i);

            // Determine if all the different messageSecurityDesriptors have the
            // same policy which will help us interpret the effective policy if
            // we cannot determine the opcode of a request at runtime.
            for (int j = 0; j < msgSecDescs.size(); j++) {
                if (j != i && !policiesAreEqual(msd, msgSecDescs.get(j))) {
                    onePolicy = false;
                }
            }
        }

        return onePolicy;
    }

    public static SunWebApp getSunWebApp(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }

        WebBundleDescriptor webBundle = (WebBundleDescriptor) properties.get(WEB_BUNDLE);
        return webBundle.getSunDescriptor();
    }

    public static String getProviderID(SunWebApp sunWebApp) {
        if (sunWebApp == null) {
            return null;
        }

        return sunWebApp.getAttributeValue(SunWebApp.HTTPSERVLET_SECURITY_PROVIDER);
    }


    public static CallbackHandler getDefaultCallbackHandler() {
        try {
            if (handlerClassName == null) {
                handlerClassName = System.getProperty(HANDLER_CLASS_PROPERTY, DEFAULT_HANDLER_CLASS);
            }

            return (CallbackHandler)
                    Class.forName(handlerClassName, true, Thread.currentThread().getContextClassLoader())
                            .getDeclaredConstructor()
                            .newInstance();

        } catch (ReflectiveOperationException pae) {
            throw new RuntimeException(pae);
        }
    }

    private static boolean policiesAreEqual(MessageSecurityDescriptor reference, MessageSecurityDescriptor other) {
        return protectionDescriptorsAreEqual(reference.getRequestProtectionDescriptor(), other.getRequestProtectionDescriptor())
                && protectionDescriptorsAreEqual(reference.getResponseProtectionDescriptor(), other.getResponseProtectionDescriptor());
    }

    private static boolean protectionDescriptorsAreEqual(ProtectionDescriptor pd1, ProtectionDescriptor pd2) {
        String authSource1 = pd1.getAttributeValue(ProtectionDescriptor.AUTH_SOURCE);
        String authRecipient1 = pd1.getAttributeValue(ProtectionDescriptor.AUTH_RECIPIENT);

        String authSource2 = pd2.getAttributeValue(ProtectionDescriptor.AUTH_SOURCE);
        String authRecipient2 = pd2.getAttributeValue(ProtectionDescriptor.AUTH_RECIPIENT);

        boolean sameAuthSource = authSource1 == null && authSource2 == null || authSource1 != null && authSource1.equals(authSource2);
        boolean sameAuthRecipient = authRecipient1 == null && authRecipient2 == null
                || authRecipient1 != null && authRecipient1.equals(authRecipient2);

        return sameAuthSource && sameAuthRecipient;
    }
}

