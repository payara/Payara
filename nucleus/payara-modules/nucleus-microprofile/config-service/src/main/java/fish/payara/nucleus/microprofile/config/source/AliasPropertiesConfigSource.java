package fish.payara.nucleus.microprofile.config.source;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.config.support.TranslatedConfigView;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AliasPropertiesConfigSource extends PayaraConfigSource implements ConfigSource {

    private final Properties properties;

    public AliasPropertiesConfigSource(Properties properties, String applicationName) {
        this.properties = properties;
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(configService.getMPConfig().getAliasPropertiesOrdinality());
    }

    @Override
    public Map<String, String> getProperties() {
        HashMap<String,String> result = new HashMap<>(properties.size());

        for (Object key : properties.keySet()) {
            String alias = properties.getProperty((String) key);
            String value = getValue((String) key);
            result.put(alias, value);
        }

        return result;
    }

    @Override
    public String getValue(String property) {
        String alias = properties.getProperty(property);

        // Null check for alias done in TranslatedConfigView.expandValue(alias)
        String value = TranslatedConfigView.expandValue(alias);

        // If returned value is null, or is the same as the pre-expanded alias, this means no match was found
        if (value == null && value.equals(alias)) {
            return null;
        }

        return value;
    }

    @Override
    public String getName() {
        return "Alias Properties";
    }
}
