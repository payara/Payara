package fish.payara.appserver.fang.service.adapter;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ServerTags;
import fish.payara.appserver.fang.service.configuration.PayaraFangConfiguration;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Optional;

/**
 *
 * @author Andrew Pielage
 */
public class PayaraFangEndpointDecider {
    private String contextRoot;
    
    private int port;
    private InetAddress address;
    private int maxThreadPoolSize = 5;
    private Config config;
    private Logger logger = Logger.getLogger(PayaraFangEndpointDecider.class.getName());
    private List<String> hosts;
    private PayaraFangConfiguration fangServiceConfiguration;
    
    private final static String DEFAULT_CONTEXT_ROOT = "/fang";
    
    public static final int DEFAULT_ADMIN_PORT = 4848;
    
    @Inject
    ServiceLocator habitat;
    
    public PayaraFangEndpointDecider(Config config, PayaraFangConfiguration fangServiceConfiguration) {
        if (config == null || logger == null)
            throw new IllegalArgumentException("config or logger can't be null");
        this.config = config;
        this.fangServiceConfiguration = fangServiceConfiguration;
        setValues();
    }
    
    public int getListenPort() {
        return port;
    }

    public InetAddress getListenAddress() {
        return address;
    }

    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }
    
    public String getContextRoot() {
        return contextRoot;
    }
    
    private void setValues() {
        NetworkListener networkListener = config.getAdminListener();
        ThreadPool threadPool = networkListener.findThreadPool();
        
        // Set Thread pool size
        if (threadPool != null) {
            try {
                maxThreadPoolSize = Integer.parseInt(threadPool.getMaxThreadPoolSize());
            } catch (NumberFormatException ex) {
                
            }
        }
        
        String defaultVirtualServer = networkListener.findHttpProtocol().getHttp().getDefaultVirtualServer();
        hosts = Collections.unmodifiableList(Arrays.asList(defaultVirtualServer));
        
        // Set network address
        try {
            address = InetAddress.getByName(networkListener.getAddress());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        
        // Set the context root and port number
        if (ServerTags.ADMIN_LISTENER_ID.equals(networkListener.getName())) {
            // Get the context root from the Payara Fang service
            if (fangServiceConfiguration == null) {
                contextRoot = DEFAULT_CONTEXT_ROOT;
            } else {
                contextRoot = fangServiceConfiguration.getContextRoot();
            }
            
            try {
                port = Integer.parseInt(networkListener.getPort());
            } catch(NumberFormatException ne) {
                port = DEFAULT_ADMIN_PORT;
            }
        }
        else {
            try {
                port = Integer.parseInt(networkListener.getPort());
            } catch(NumberFormatException ne) {
                port = DEFAULT_ADMIN_PORT;
            }
            
            // Get the context root from the Payara Fang service
            if (fangServiceConfiguration == null) {
                contextRoot = DEFAULT_CONTEXT_ROOT;
            } else {
                contextRoot = fangServiceConfiguration.getContextRoot();
            }
        }
    }
    
    public List<String> getHosts() {
        return hosts;
    }
}
