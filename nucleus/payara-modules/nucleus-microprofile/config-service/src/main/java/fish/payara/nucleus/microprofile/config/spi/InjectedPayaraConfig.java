/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 */
public class InjectedPayaraConfig implements Config, Serializable {

    private static final long serialVersionUID = 1L;

    private transient Config delegate;
    private String appName;

    public InjectedPayaraConfig(Config delegate, String appName) {
        this.delegate = delegate;
        this.appName = appName;
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        ensureDelegate();
        return delegate.getValue(propertyName, propertyType);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        ensureDelegate();
        return delegate.getOptionalValue(propertyName, propertyType);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        ensureDelegate();
        return delegate.getPropertyNames();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        ensureDelegate();
        return delegate.getConfigSources();
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        ensureDelegate();
        return delegate.getConfigValue(propertyName);
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        ensureDelegate();
        return delegate.getConverter(forType);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        ensureDelegate();
        return delegate.unwrap(type);
    }

    private void ensureDelegate() {
        if (delegate == null) {
            delegate = ((ConfigProviderResolverImpl) ConfigProviderResolver.instance()).getNamedConfig(appName);
        }
    }

}
