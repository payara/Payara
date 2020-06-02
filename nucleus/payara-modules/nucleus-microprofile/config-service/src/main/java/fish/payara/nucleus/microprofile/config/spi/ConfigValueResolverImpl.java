package fish.payara.nucleus.microprofile.config.spi;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

final class ConfigValueResolverImpl implements ConfigValueResolver {

    private final PayaraConfig config;
    private final String propertyName;
    private boolean acceptEmpty;
    private boolean throwsOnMissingProperty;
    private boolean throwOnFailedConversion;
    private String rawDefault;

    ConfigValueResolverImpl(PayaraConfig config, String propertyName) {
        this.config = config;
        this.propertyName = propertyName;
    }

    @Override
    public ConfigValueResolver withDefault(String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConfigValueResolver acceptEmpty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConfigValueResolver throwOnMissingProperty() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConfigValueResolver throwOnFailedConversion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T as(Class<T> type, T defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T asConvertedBy(Function<String, T> converter, T defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E> List<E> asList(Class<E> elementType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E> List<E> asList(Class<E> elementType, List<E> defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E> Set<E> asSet(Class<E> elementType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E> Set<E> asSet(Class<E> elementType, Set<E> defaultValue) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E> void forEach(Class<E> elementType, Consumer<? super E> action) {
        // TODO Auto-generated method stub

    }

}
