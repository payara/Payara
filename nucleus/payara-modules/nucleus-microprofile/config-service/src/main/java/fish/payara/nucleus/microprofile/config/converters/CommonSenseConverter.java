/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.microprofile.config.converters;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @author steve
 */
public class CommonSenseConverter implements Converter<Object> {

    /**
     * Return implicit converter for a class following section "Automatic Converters" of the spec, if class has
     * matching construct
     *
     * @param type target type of property
     * @param <T> target type of property
     * @return Optional of converter using a method found in the class, empty if none were found
     */
    public static <T> Optional<Converter<T>> forClass(Class<T> type) {
        return Stream.<Supplier<Converter<T>>>of(
                () -> forMethod(type, "of", String.class),
                () -> forMethod(type, "valueOf", String.class),
                () -> forConstructor(type, String.class),
                () -> forMethod(type, "parse", CharSequence.class))
                .map(Supplier::get)
                .filter(converter -> converter != null)
                .findFirst();
    }

    private Method conversionMethod;
    private Constructor constructor;
    
    private CommonSenseConverter(Method method) {
        conversionMethod = method;
    }
    
    private CommonSenseConverter(Constructor method) {
        constructor = method;
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.equals(ConfigProperty.UNCONFIGURED_VALUE)) return null;

        if (conversionMethod != null) {
            try {
                return conversionMethod.invoke(null, value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Unable to convert value to type  for value " + value, ex);
            }
        } else if (constructor != null) {
            try {
                return constructor.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Unable to convert value to type  for value " + value, ex);
            }
        }
        throw new IllegalStateException("CommonSenseConverter created without constructor or method to call");
    }

    private static <T> Converter<T> forMethod(Class<T> type, String method, Class<?>... argumentTypes) {
        try {
            Method factoryMethod = type.getMethod(method, argumentTypes);
            if (Modifier.isStatic(factoryMethod.getModifiers()) && Modifier.isPublic(factoryMethod.getModifiers())) {
                return (Converter<T>) new CommonSenseConverter(factoryMethod);
            } else {
                return null;
            }
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    private static <T> Converter<T> forConstructor(Class<T> type, Class<?>... argumentTypes) {
        try {
            Constructor<T> constructor = type.getConstructor(argumentTypes);
            if (Modifier.isPublic(constructor.getModifiers())) {
                return (Converter<T>) new CommonSenseConverter(constructor);
            } else {
                return null;
            }
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

}
