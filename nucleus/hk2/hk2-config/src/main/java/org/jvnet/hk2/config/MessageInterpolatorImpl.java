/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.MessageInterpolator;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;

/*
 * Custom MessageInterpolatorImpl for HK2
 *
 * This message interpolator is different from the default one in the following
 * ways:
 *
 * 1. It uses a class specified in the "payload" argument to the annotation to
 * find the class loader to find the resource bundle for messages. This allows
 * classes that are in OSGi modules to specify a resource bundle for bean
 * validation messages.
 *
 * 2. The "LocalStrings" resource bundle within the same package as the payload
 * class is used to search for messages.  If a message is not found, the message
 * is obtained from the default bean validation resource bundle.
 *
 * This class borrows heavily from the RI implementation of the ResourcBundleMessageInterpolator
 * authored by Emmanuel Bernard, Hardy Ferentschik, and Gunnar Morling.
 */
public class MessageInterpolatorImpl implements MessageInterpolator {

    /**
     * The name of the default message bundle.
     */
    public static final String DEFAULT_VALIDATION_MESSAGES = "org.hibernate.validator.ValidationMessages";
    /**
     * The name of the user-provided message bundle as defined in the specification.
     */
    public static final String USER_VALIDATION_MESSAGES = "ValidationMessages";
    /**
     * Regular expression used to do message interpolation.
     */
    private static final Pattern MESSAGE_PARAMETER_PATTERN = Pattern.compile("(\\{[^\\}]+?\\})");
    /**
     * The default locale for the current user.
     */
    private final Locale defaultLocale = Locale.getDefault();
    /**
     * Step 1-3 of message interpolation can be cached. We do this in this map.
     */
    private final Map<LocalisedMessage, String> resolvedMessages = new WeakHashMap<LocalisedMessage, String>();
    /**
     * Flag indicating whether this interpolator should chance some of the interpolation steps.
     */
    private static final boolean cacheMessages = true;

    public MessageInterpolatorImpl() { }

    @Override
    public String interpolate(String message, Context context) {
        // probably no need for caching, but it could be done by parameters since the map
        // is immutable and uniquely built per Validation definition, the comparison has to be based on == and not equals though
        return interpolate(message, context, defaultLocale);
    }

    /**
     * Runs the message interpolation according to algorithm specified in JSR 303.
     * <br/>
     * Note:
     * <br/>
     * Look-ups in user bundles is recursive whereas look-ups in default bundle are not!
     *
     * @param message the message to interpolate
     * @param annotationParameters the parameters of the annotation for which to interpolate this message
     * @param locale the {@code Locale} to use for the resource bundle.
     *
     * @return the interpolated message.
     */
    @Override
    public String interpolate(String message, Context context, Locale locale) {
        Map<String, Object> annotationParameters = context.getConstraintDescriptor().getAttributes();
        LocalisedMessage localisedMessage = new LocalisedMessage(message, locale);
        String resolvedMessage = null;

        if (cacheMessages) {
            resolvedMessage = resolvedMessages.get(localisedMessage);
        }

        // if the message is not already in the cache we have to run step 1-3 of the message resolution
        if (resolvedMessage == null) {
            ResourceBundle userResourceBundle = new ContextResourceBundle(context, locale);
            ClassLoader cl;
            if (System.getSecurityManager()!=null) {
                cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return MessageInterpolator.class.getClassLoader();
                    }
                });
            } else {
                cl = MessageInterpolator.class.getClassLoader();
            }
            ResourceBundle defaultResourceBundle = null;
            try {
                defaultResourceBundle = ResourceBundle.getBundle(DEFAULT_VALIDATION_MESSAGES,
                    locale,
                    cl);
            }
            catch (MissingResourceException mre) {
                // defaultResourceBundle is already null
            }

            String userBundleResolvedMessage;
            resolvedMessage = message;
            boolean evaluatedDefaultBundleOnce = false;
            do {
                // search the user bundle recursive (step1)
                userBundleResolvedMessage = replaceVariables(
                        resolvedMessage, userResourceBundle, locale, true);

                // exit condition - we have at least tried to validate against the default bundle and there was no
                // further replacements
                if (evaluatedDefaultBundleOnce
                        && !hasReplacementTakenPlace(userBundleResolvedMessage, resolvedMessage)) {
                    break;
                }

                // search the default bundle non recursive (step2)
                resolvedMessage = replaceVariables(userBundleResolvedMessage, defaultResourceBundle, locale, false);
                evaluatedDefaultBundleOnce = true;
                if (cacheMessages) {
                    resolvedMessages.put(localisedMessage, resolvedMessage);
                }
            } while (true);
        }

        // resolve annotation attributes (step 4)
        resolvedMessage = replaceAnnotationAttributes(resolvedMessage, annotationParameters);

        // last but not least we have to take care of escaped literals
        resolvedMessage = resolvedMessage.replace("\\{", "{");
        resolvedMessage = resolvedMessage.replace("\\}", "}");
        resolvedMessage = resolvedMessage.replace("\\\\", "\\");
        return resolvedMessage;
    }

    private boolean hasReplacementTakenPlace(String origMessage, String newMessage) {
        return !origMessage.equals(newMessage);
    }

    private String replaceVariables(String message, ResourceBundle bundle, Locale locale, boolean recurse) {
        Matcher matcher = MESSAGE_PARAMETER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        String resolvedParameterValue;
        while (matcher.find()) {
            String parameter = matcher.group(1);
            resolvedParameterValue = resolveParameter(
                    parameter, bundle, locale, recurse);

            matcher.appendReplacement(sb, escapeMetaCharacters(resolvedParameterValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceAnnotationAttributes(String message, Map<String, Object> annotationParameters) {
        Matcher matcher = MESSAGE_PARAMETER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String resolvedParameterValue;
            String parameter = matcher.group(1);
            Object variable = annotationParameters.get(removeCurlyBrace(parameter));
            if (variable != null) {
                resolvedParameterValue = escapeMetaCharacters(variable.toString());
            } else {
                resolvedParameterValue = parameter;
            }
            matcher.appendReplacement(sb, resolvedParameterValue);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveParameter(String parameterName, ResourceBundle bundle, Locale locale, boolean recurse) {
        String parameterValue;
        try {
            if (bundle != null) {
                parameterValue = bundle.getString(removeCurlyBrace(parameterName));
                if (recurse) {
                    parameterValue = replaceVariables(parameterValue, bundle, locale, recurse);
                }
            } else {
                parameterValue = parameterName;
            }
        } catch (MissingResourceException e) {
            // return parameter itself
            parameterValue = parameterName;
        }
        return parameterValue;
    }

    private String removeCurlyBrace(String parameter) {
        return parameter.substring(1, parameter.length() - 1);
    }

    /**
     * @param s The string in which to replace the meta characters '$' and '\'.
     *
     * @return A string where meta characters relevant for {@link Matcher#appendReplacement} are escaped.
     */
    private String escapeMetaCharacters(String s) {
        String escapedString = s.replace("\\", "\\\\");
        escapedString = escapedString.replace("$", "\\$");
        return escapedString;
    }

    /*
     * A resource bundle that takes strings from the annotation context. This class
     * looks for the "LocalStrings" resource bundle in the same package as the
     * class that is specified in the payload of the annotation. If a String
     * cannot be found there, then it looks in the user resource bundle as defined
     * in JSR-303.
     */

    private static class ContextResourceBundle extends ResourceBundle {
        ResourceBundle contextBundle;
        ResourceBundle userBundle;

        ContextResourceBundle(Context context, Locale locale) {
            ConstraintDescriptor descriptor = context.getConstraintDescriptor();
            Set<Class<? extends Payload>> payload = descriptor.getPayload();

            // Load the LogStrings.properties for the argument Locale, from the ClassLoader
            // of the payload.
            if (!payload.isEmpty()) {
                final Class payloadClass = payload.iterator().next();
                String baseName = payloadClass.getPackage().getName() + ".LocalStrings";
                ClassLoader cl;
                if (System.getSecurityManager()!=null) {
                    cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return payloadClass.getClassLoader();
                        }
                    });
                } else {
                    cl = payloadClass.getClassLoader();
                }

                try {
                    contextBundle = ResourceBundle.getBundle(baseName, locale, cl);
                } catch (MissingResourceException mre) {
                    contextBundle = null;
                }
            }
            try {
                ClassLoader cl = System.getSecurityManager()==null?Thread.currentThread().getContextClassLoader():
                        AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                            @Override
                            public ClassLoader run() {
                                return Thread.currentThread().getContextClassLoader();
                            }
                        });
                userBundle = ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, 
                        locale,
                        cl);
            } catch (MissingResourceException mre) {
                    userBundle = null;
            }

            if (userBundle != null) {
                setParent(userBundle);
            }
        }

        @Override
        protected Object handleGetObject(String key) {
            if (contextBundle != null) {
                return contextBundle.getObject(key);
            }
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            Set<String> keys = new TreeSet<String>();
            if (contextBundle != null) {
                keys.addAll(Collections.list(contextBundle.getKeys()));
            }
            if (userBundle != null) {
                keys.addAll(Collections.list(userBundle.getKeys()));
            }
            return Collections.enumeration(keys);
        }
    }

    private static class LocalisedMessage {

        private final String message;
        private final Locale locale;

        LocalisedMessage(String message, Locale locale) {
            this.message = message;
            this.locale = locale;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LocalisedMessage that = (LocalisedMessage) o;

            if (locale != null ? !locale.equals(that.locale) : that.locale != null) {
                return false;
            }
            if (message != null ? !message.equals(that.message) : that.message != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = message != null ? message.hashCode() : 0;
            result = 31 * result + (locale != null ? locale.hashCode() : 0);
            return result;
        }
    }
}
