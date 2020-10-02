package fish.payara.nucleus.microprofile.config.source.extension;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface ExtensionConfigSource extends ConfigSource {

    boolean setValue(String name, String value);

    boolean deleteValue(String name);

    default void bootstrap() {}

    default void destroy() {}
}
