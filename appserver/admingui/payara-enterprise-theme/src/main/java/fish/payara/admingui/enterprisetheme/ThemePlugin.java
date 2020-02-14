package fish.payara.admingui.enterprisetheme;

import org.glassfish.api.admingui.ConsoleProvider;
import org.jvnet.hk2.annotations.Service;

import java.net.URL;

@Service
public class ThemePlugin implements ConsoleProvider {

    public URL getConfiguration() {
        return null;
    }

}
