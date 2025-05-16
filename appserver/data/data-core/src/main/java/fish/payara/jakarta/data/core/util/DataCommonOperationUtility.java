/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.jakarta.data.core.util;

import fish.payara.jakarta.data.core.cdi.extension.QueryData;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class for common utility methods
 */
public class DataCommonOperationUtility {

    public static Predicate<Class<?>> evaluateReturnTypeVoidPredicate = returnType -> void.class.equals(returnType)
            || Void.class.equals(returnType);

    public static Object processReturnType(QueryData dataForQuery, List<Object> results) {
        Class<?> returnType = dataForQuery.getMethod().getReturnType();
        if (returnType.isArray()) {
            if (dataForQuery.getDeclaredEntityClass() != null && returnType.getComponentType().isAssignableFrom(dataForQuery.getDeclaredEntityClass())) {
                Object[] returnValue = (Object[]) Array.newInstance(dataForQuery.getDeclaredEntityClass(), results.size());
                return results.toArray(returnValue);
            } else {
                Object[] returnValue = (Object[]) Array.newInstance(returnType.getComponentType(), results.size());
                return results.toArray(returnValue);
            }
        } else if (Stream.class.equals(returnType)) {
            return results.stream();
        } else if (evaluateReturnTypeVoidPredicate.test(returnType)) {
            return null;
        } else if (!results.isEmpty()) {
            return results;
        }
        return null;
    }

    public static Class<?> findEntityTypeInMethod(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!void.class.equals(returnType) && !Void.class.equals(returnType)) {
            if (Collection.class.isAssignableFrom(returnType)
                    || Stream.class.isAssignableFrom(returnType)
                    || Optional.class.isAssignableFrom(returnType)) {
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericReturnType;
                    Type typeArgument = paramType.getActualTypeArguments()[0];
                    return getGenericClass(typeArgument);
                }
            } else if (returnType.isArray()) {
                return returnType.getComponentType();
            } else if (!returnType.isPrimitive() && !returnType.equals(String.class)) {
                return returnType;
            }
        }
        for (Parameter param : method.getParameters()) {
            Class<?> paramType = param.getType();
            if (!paramType.isPrimitive() && !paramType.equals(String.class)) {
                if (Collection.class.isAssignableFrom(paramType)
                        || Stream.class.isAssignableFrom(paramType)) {
                    Type paramGenericType = param.getParameterizedType();
                    if (paramGenericType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType) paramGenericType;
                        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                        return getGenericClass(typeArgument);
                    }
                } else if (paramType.isArray()) {
                    return paramType.getComponentType();
                } else {
                    return paramType;
                }
            }
        }
        return null;
    }

    public static Class<?> getGenericClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getRawType();
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0) {
                return getGenericClass(upperBounds[0]);
            }
        }
        return Object.class;
    }
}
