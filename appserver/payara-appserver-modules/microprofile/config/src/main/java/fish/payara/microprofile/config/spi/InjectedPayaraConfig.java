/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.config.spi;

import java.io.Serializable;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 */
public class InjectedPayaraConfig implements Config, Serializable {
    
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        ensureDelegate();
        return delegate.getConfigSources();
    }
    
    private void ensureDelegate() {
        if (delegate == null) {
            delegate = (PayaraConfig) ((ConfigProviderResolverImpl)ConfigProviderResolverImpl.instance()).getNamedConfig(appName);
        }
    }
    
}
