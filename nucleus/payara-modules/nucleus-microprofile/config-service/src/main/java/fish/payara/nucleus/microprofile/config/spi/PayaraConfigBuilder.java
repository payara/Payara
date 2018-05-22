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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Priority;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class PayaraConfigBuilder implements ConfigBuilder {

    LinkedList<ConfigSource> sources;
    Map<Type, Converter> converters;
    ConfigProviderResolverImpl resolver;
    ClassLoader loader;

    public PayaraConfigBuilder(ConfigProviderResolverImpl resolver, ClassLoader loader) {
        this.resolver = resolver;
        sources = new LinkedList<>();
        converters = new HashMap<>();
        this.loader = loader;
    }
    
    public PayaraConfigBuilder(ConfigProviderResolverImpl resolver) {
        this(resolver,Thread.currentThread().getContextClassLoader());
    }

    @Override
    public ConfigBuilder addDefaultSources() {
        sources.addAll(resolver.getDefaultSources());
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredSources() {
        sources.addAll(resolver.getDiscoveredSources(resolver.getAppInfo(loader)));
        return this;
     }

    @Override
    public ConfigBuilder addDiscoveredConverters() {
        Map<Type, Converter> discoveredConverters = resolver.getDiscoveredConverters(resolver.getAppInfo(loader));
        converters.putAll(discoveredConverters);
        return this;
    }

    @Override
    public ConfigBuilder forClassLoader(ClassLoader loader) {
        this.loader = loader;
        return this;
    }

    @Override
    public ConfigBuilder withSources(ConfigSource... sources) {
        this.sources.addAll(Arrays.asList(sources));
        return this;
    }

    @Override
    public ConfigBuilder withConverters(Converter... converters) {
        addConvertersToMap(Arrays.asList(converters));
        return this;
    }

    @Override
    public Config build() {
        this.converters.putAll(resolver.getDefaultConverters());
        return new PayaraConfig(sources, converters);
    }

    @Override
    public <T> ConfigBuilder withConverter(Class<T> type, int i, Converter<T> cnvrtr) {
        Converter old = converters.get(type);
        if (old != null) {
            if (i > getPriority(old)) {
                this.converters.put(type, cnvrtr);
            }
        } else {
            this.converters.put(type, cnvrtr);
        }
        return this;
    }

    public static Type getTypeForConverter(Converter converter) {
        // add each converter to the map for later lookup
        Type types[] = converter.getClass().getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type args[] = ((ParameterizedType) type).getActualTypeArguments();
                if (args.length >= 1) {
                    // check if there is one there already
                    return args[0];
                }
            }
        }
        return null;
    }
    
        
    private int getPriority(Converter converter) {
        int result = 100;
        Priority annotation = converter.getClass().getAnnotation(Priority.class);
        if (annotation != null) {
            result = annotation.value();
        }
        return result;
    }
    
        
    private void addConvertersToMap(List<Converter> convertersList) {
        for (Converter converter : convertersList) {
            Type cType = getTypeForConverter(converter);
            if (cType != null) { 
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
