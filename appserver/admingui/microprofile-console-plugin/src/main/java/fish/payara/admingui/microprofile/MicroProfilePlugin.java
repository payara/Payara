
package fish.payara.admingui.microprofile;

import java.net.URL;
import org.glassfish.api.admingui.ConsoleProvider;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Susan Rai
 */
@Service
public class MicroProfilePlugin implements ConsoleProvider {
    
    @Override
    public URL getConfiguration() {
        return null;
    }
}