/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Utility class to find a {@link Method} of a certain name with the same argument types as a given sample method as
 * required to lookup the {@link Fallback}'s fallback method.
 *
 * @author Jan Bernitt
 */
public final class MethodLookupUtils {

    private MethodLookupUtils() {
        // util
    }

    public static Method findMethodWithMatchingNameAndArguments(String name, Method sample) {
        Class<?> currentType = sample.getDeclaringClass();
        while (currentType != Object.class) {
            try {
                Method candidate = currentType.getDeclaredMethod(name, sample.getParameterTypes());
                if (isMatchingParameterList(sample, candidate)) {
                    return candidate;
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // continue search
            }
            for (Method candidate : currentType.getDeclaredMethods()) {
                if (name.equals(candidate.getName()) && isMatchingParameterList(sample, candidate)) {
                    return candidate;
                }
            }
            for (Class<?> implemented : currentType.getInterfaces()) {
                for (Method candidate : implemented.getDeclaredMethods()) {
                    if (name.equals(candidate.getName()) && isMatchingParameterList(sample, candidate)) {
                        return candidate;
                    }
                }
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

    private static boolean isMatchingParameterList(Method sample, Method candidate) {
        return isMatchingParameterList(sample.getGenericParameterTypes(), candidate.getGenericParameterTypes());
    }

    private static boolean isMatchingParameterList(Type[] samples, Type[] candidates) {
        if (samples.length != candidates.length) {
            return false;
        }
        for (int i = 0; i < samples.length; i++) {
            if (!isMatchingType(samples[i], candidates[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMatchingType(Type sample, Type candidate) {
        return sample.equals(candidate)
                || (candidate instanceof TypeVariable)
                || (candidate instanceof GenericArrayType)
                || isMatchingGenericType(sample, candidate)
                || isMatchingWildcardType(sample, candidate);
    }

    private static boolean isMatchingWildcardType(Type sample, Type candidate) {
        if (sample instanceof WildcardType && candidate instanceof WildcardType) {
            WildcardType sampleType = (WildcardType) sample;
            WildcardType candidateType = (WildcardType) candidate;
            return isMatchingParameterList(sampleType.getLowerBounds(), candidateType.getLowerBounds())
                    && isMatchingParameterList(sampleType.getUpperBounds(), candidateType.getUpperBounds());
        }
        return false;
    }

    private static boolean isMatchingGenericType(Type sample, Type candidate) {
        if (sample instanceof ParameterizedType && candidate instanceof ParameterizedType) {
            ParameterizedType sampleType = (ParameterizedType) sample;
            ParameterizedType candidateType = (ParameterizedType) candidate;
            if (sampleType.getRawType() != candidateType.getRawType()) {
                return false;
            }
            return isMatchingParameterList(sampleType.getActualTypeArguments(),
                    candidateType.getActualTypeArguments());
        }
        return false;
    }
}
