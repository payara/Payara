/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 * Automatic {@link Converter}s are created "on the fly" in case no {@link Converter} as found but the target type has a
 * suitable factory {@link Method} or {@link Constructor}.
 *
 * @author steve
 */
public class AutomaticConverter {

    /**
     * Return implicit converter for a class following section "Automatic Converters" of the spec, if class has
     * matching construct
     *
     * @param generalType target type of property
     * @param <T> target type of property
     * @return Optional of converter using a method found in the class, empty if none were found
     */
    public static <T> Optional<Converter<T>> forType(Class<T> type) {
        return Stream.<Supplier<Converter<T>>>of(
                () -> forMethod(type, "of", String.class),
                () -> forMethod(type, "valueOf", String.class),
                () -> forMethod(type, "parse", CharSequence.class),
                () -> forConstructor(type, String.class))
                .map(Supplier::get)
                .filter(converter -> converter != null)
                .findFirst();
    }

    private static final class MethodConverter<T> implements Converter<T> {

        private final Method conversionMethod;

        MethodConverter(Method conversionMethod) {
            this.conversionMethod = conversionMethod;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T convert(String value) {
            if (value == null || value.equals(ConfigProperty.UNCONFIGURED_VALUE)) return null;
            try {
                return (T) conversionMethod.invoke(null, value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Unable to convert value to type "
                        + conversionMethod.getReturnType().getName() + " for value `" + value + "`", ex);
            }
        }
    }

    private static final class ConstructorConverter<T> implements Converter<T> {

        private final Constructor<T> constructor;

        ConstructorConverter(Constructor<T> constructor) {
            this.constructor = constructor;
        }

        @Override
        public T convert(String value) {
            if (value == null || value.equals(ConfigProperty.UNCONFIGURED_VALUE)) return null;
            try {
                return constructor.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalArgumentException("Unable to convert value to type "
                        + constructor.getDeclaringClass().getName() + " for value `" + value + "`", ex);
            }
        }
    }

    private static <T> Converter<T> forMethod(Class<T> type, String method, Class<?>... argumentTypes) {
        try {
            Method factoryMethod = type.getMethod(method, argumentTypes);
            if (Modifier.isStatic(factoryMethod.getModifiers())
                    && Modifier.isPublic(factoryMethod.getModifiers())
                    && factoryMethod.getReturnType() == type) {
                return new MethodConverter<>(factoryMethod);
            }
            return null;
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    private static <T> Converter<T> forConstructor(Class<T> type, Class<?>... argumentTypes) {
        try {
            Constructor<T> constructor = type.getConstructor(argumentTypes);
            if (Modifier.isPublic(constructor.getModifiers())) {
                return new ConstructorConverter<>(constructor);
            }
            return null;
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

}
