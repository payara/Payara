package fish.payara.microprofile.config.cdi;

import javax.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.config.inject.ConfigProperty;

// FIXME: this class may not be needed, unless you go down the root of modifying
// injection targets to utilise the standard @ConfigProperty injection
// machanism for fields of @ConfigProperties beans
final class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {

    private static final long serialVersionUID = 1L;
    private final String name;

    protected ConfigPropertyLiteral(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String defaultValue() {
        return ConfigProperty.UNCONFIGURED_VALUE;
    }
}