/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.spi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Priority;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class PayaraConfig implements Config {
    
    private final List<ConfigSource> configSources;
    private final HashMap<Type, Converter> converters;

    public PayaraConfig(List<ConfigSource> configSources, List<Converter> convertersList) {
        this.configSources = configSources;
        this.converters = new HashMap<>();
        for (Converter converter : convertersList) {
            // add each converter to the map for later lookup
            Type types[] = converter.getClass().getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    Type args[] = ((ParameterizedType) type).getActualTypeArguments();
                    if (args.length == 1) {
                        // check if there is one there already
                        Type cType = args[0];
                        Converter old = converters.get(cType);
                        if (old != null) {
                            if (getPriority(converter) > getPriority(old)) {
                                this.converters.put(cType, converter);                                
                            }
                        }else {
                            this.converters.put(cType, converter);
                        }
                    }
                }
            }
        }
        Collections.sort(configSources, new ConfigSourceComparator());
    }
    
    public <T> T getValue(String propertyName, String defaultValue, Class<T>  propertyType) {
        String result = getValue(propertyName);
        if (result == null) {
            result = defaultValue;
        }
        if (result == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
        return convertString(result, propertyType);
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        String result = getValue(propertyName);
        
        if (result == null) {
            throw new NoSuchElementException("Unable to find property with name " + propertyName);
        }
        return convertString(result, propertyType);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        String strValue = getValue(propertyName);
        
        if(String.class.equals(propertyType)) {
            return (Optional<T>) Optional.ofNullable(strValue);
        }
        
        Converter<T> converter = getConverter(propertyType);
        if (converter == null) {
            throw new IllegalArgumentException("No converter for class " + propertyType);
        }
        return Optional.ofNullable(converter.convert(strValue));
    }

    @Override
    public Iterable<String> getPropertyNames() {
        LinkedList<String> result = new LinkedList<>();
        for (ConfigSource configSource : configSources) {
            result.addAll(configSource.getProperties().keySet());
        }
        return result;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }
    
    public Set<Type> getConverterTypes() {
        return converters.keySet();
    }
    
    
    private String getValue(String propertyName) {
        String result = null;
        for (ConfigSource configSource : configSources) {
            result = configSource.getValue(propertyName);
            if (result != null) {
                break;
            }
        }
        return result;
    }
    
    private <T> Converter<T> getConverter(Class<T> propertyType) {
        Class type = propertyType;
        if (type.equals(int.class)) {
            type = Integer.class;
        } else if (type.equals(long.class)) {
            type = Long.class;
        } else if (type.equals(double.class)) {
            type = Double.class;
        } else if (type.equals(float.class)) {
            type = Float.class;
        } else if (type.equals(boolean.class)) {
            type = Boolean.class;
        }
        Converter<T> converter = converters.get(type);

            
        if (converter == null) {
            // search for a matching raw type
            for (Type type1 : converters.keySet()) {
                if (type1 instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType)type1;
                    if (ptype.getRawType().equals(propertyType)) {
                        converter = converters.get(type1);
                        break;
                    }
                }
            }
        }
        
        return converter;
    }
    
    private int getPriority(Converter converter) {
        int result = 100;
        Priority annotation = converter.getClass().getAnnotation(Priority.class);
        if (annotation != null) {
            result = annotation.value();
        }
        return result;
    }
    
    private <T> T convertString(String value, Class<T>  propertyType) {
        if (String.class.equals(propertyType)) {
            return (T) value;
        }
        
        // find a converter
        Converter<T> converter = getConverter(propertyType);
        if (converter == null) {
            throw new IllegalArgumentException("No converter for class " + propertyType);
        }
        
        return converter.convert(value);       
    }
    
}
