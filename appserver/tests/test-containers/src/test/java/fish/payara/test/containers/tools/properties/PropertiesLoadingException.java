package fish.payara.test.containers.tools.properties;

import java.io.File;

/**
 * Thrown if the properties cannot be loaded or are unusable for some reason.
 *
 * @author David Matějček
 */
public class PropertiesLoadingException extends RuntimeException {

    private static final long serialVersionUID = -2068187564953651151L;


    /**
     * @param path
     * @param refreshPeriod
     * @param cause
     */
    public PropertiesLoadingException(final String path, final long refreshPeriod, final Exception cause) {
        super(String.format("Cannot load a properties from file '%s' with refresh period '%d'", path, refreshPeriod),
            cause);
    }


    /**
     * @param path
     * @param refreshPeriod
     * @param cause
     */
    public PropertiesLoadingException(final File path, final long refreshPeriod, final Exception cause) {
        super(String.format("Cannot load properties from file '%s' with refresh period '%d'", path, refreshPeriod),
            cause);
    }


    /**
     * @param path
     * @param cause
     */
    public PropertiesLoadingException(final File path, final Exception cause) {
        super(String.format("Cannot load properties from file '%s'", path), cause);
    }


    /**
     * @param message an error message.
     */
    public PropertiesLoadingException(final String message) {
        super(message);
    }


    /**
     * @param message an error message.
     * @param cause
     */
    public PropertiesLoadingException(final String message, final Exception cause) {
        super(message, cause);
    }
}
