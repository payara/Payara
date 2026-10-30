/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.internal.notification;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * A collection of static methods used by notification sources.
 */
public final class NotifierUtils {
    
    private NotifierUtils() {}

    /**
     * @param descriptor the HK2 service descriptor for the notifier
     * @return a string name representing the notifier
     */
    public static final String getNotifierName(ActiveDescriptor<?> descriptor) {
        String name = descriptor.getName();
        if (name == null) {
            name = descriptor.getClassAnalysisName();
        }
        if (name == null) {
            name = descriptor.getImplementationClass().getSimpleName();
        }
        return name;
    }

    /**
     * List the names of all registered notifiers
     * 
     * @param serviceLocator the service locator to use to find the notifiers
     * @return a set of all notifier names
     * @see #getNotifierName(ActiveDescriptor)
     */
    public static final Set<String> getNotifierNames(ServiceLocator serviceLocator) {
        final List<ServiceHandle<PayaraNotifier>> notifierHandles = serviceLocator.getAllServiceHandles(PayaraNotifier.class);
        final Set<String> names = new HashSet<>();
        for (ServiceHandle<PayaraNotifier> handle : notifierHandles) {
            names.add(getNotifierName(handle.getActiveDescriptor()));
        }
        return names;
    }

    /**
     * @param <C>           a generic class of the notifier configuration class
     * @param notifierClass the notifier of the class
     * @return the class used to configure the configured notifier
     */
    @SuppressWarnings("unchecked")
    public static <C extends PayaraNotifierConfiguration> Class<C> getConfigurationClass(Class<?> notifierClass) {
        final ParameterizedType genericSuperclass = (ParameterizedType) notifierClass.getGenericSuperclass();
        return (Class<C>) genericSuperclass.getActualTypeArguments()[0];
    }

    /**
     * @return a camel cased string representing the result
     */
    public static String convertToCamelCase(String string) {
        if (string == null) {
            return null;
        }

        // Make sure the string has no leading or trailing whitespace or symbols
        string = string.trim()
            .replaceAll("^[^a-zA-Z0-9]+", "")
            .replaceAll("[^a-zA-Z0-9]+$", "");

        if (string.isEmpty()) {
            return string;
        }

        String result = "";

        // Track if a space or other character that requires an upper case character is encountered
        boolean upperCaseNextCharacter = false;

        // Count through each other character
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);

            // If a space is found, ignore and convert the next letter to upper case
            if (Character.isWhitespace(ch) || (!Character.isAlphabetic(ch) && !Character.isDigit(ch))) {
                upperCaseNextCharacter = true;
                continue;
            } else if (upperCaseNextCharacter) {
                upperCaseNextCharacter = false;
                result += Character.toUpperCase(ch);
            } else {
                result += Character.toLowerCase(ch);
            }
        }
        return result;
    }
}
