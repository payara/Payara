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
package fish.payara.microprofile.faulttolerance.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.microprofile.config.Config;
import static org.eclipse.microprofile.config.Config.PROFILE;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * A {@link Config} implementations for testing where properties can be set using the
 * {@link #override(Method, Class, String, Object)} methods.
 * 
 * The implementation will use the to and from {@link String} conversion to make sure the usage is proper.
 * 
 * @author Jan Bernitt
 */
public final class ConfigOverrides implements Config {

    private final Properties overrides = new Properties();

    public void override(Method annotatedMethod, Class<? extends Annotation> annotationType, String propertyName,
            Object value) {
        override(String.format("%s/%s/%s/%s", annotatedMethod.getDeclaringClass().getName(),
                annotatedMethod.getName(), annotationType.getSimpleName(), propertyName), value);
    }

    public void override(Class<?> target, Class<? extends Annotation> annotationType, String propertyName,
            Object value) {
        override(String.format("%s/%s/%s", target.getName(), annotationType.getSimpleName(), propertyName), value);
    }

    public void override(Class<? extends Annotation> annotationType, String propertyName, Object value) {
        override(String.format("%s/%s", annotationType.getSimpleName(), propertyName), value);
    }

    public void override(String key, Object value) {
        overrides.put(key, toString(value));
    }

    private static String toString(Object value) {
        if (value instanceof Class) {
            return ((Class<?>) value).getName();
        }
        if (value instanceof Class[]) {
            Class<?>[] classes = (Class<?>[]) value;
            String classNameList = "";
            for (int i = 0; i < classes.length; i++) {
                if (i > 0) {
                    classNameList += ",";
                }
                classNameList += classes[i].getName();
            }
            return classNameList;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        String value = overrides.getProperty(propertyName);
        if (value == null) {
            return Optional.empty();
        }
        if (propertyType == String.class) {
            return (Optional<T>) Optional.of(value);
        }
        if (propertyType == Integer.class) {
            return (Optional<T>) Optional.of(Integer.valueOf(value));
        }
        if (propertyType == Long.class) {
            return (Optional<T>) Optional.of(Long.valueOf(value));
        }
        if (propertyType == Boolean.class) {
            return (Optional<T>) Optional.of(Boolean.valueOf(value));
        }
        if (propertyType.isEnum()) {
            return (Optional<T>) Optional.of(enumValue(propertyType, value));
        }
        throw new UnsupportedOperationException("value type not supported: " + propertyType.getSimpleName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <E extends Enum<E>> Enum<?> enumValue(Class type, String value) {
        return Enum.valueOf(type, value.toUpperCase());
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public <T> T unwrap(Class<T> type) {
        throw new UnsupportedOperationException("Unwrap not supported by test config.");
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ConfigValue getConfigValue(String string) {
        return new TestConfigValue(string, getValue(PROFILE, String.class));
    }
    
    class TestConfigValue implements ConfigValue {
        
        String name;
        String value;

        public TestConfigValue(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getRawValue() {
            return value;
        }

        @Override
        public String getSourceName() {
            return "test";
        }

        @Override
        public int getSourceOrdinal() {
            return 100;
        }
        
    }
    
}
