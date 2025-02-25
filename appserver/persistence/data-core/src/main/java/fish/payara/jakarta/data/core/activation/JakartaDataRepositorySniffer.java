package fish.payara.jakarta.data.core.activation;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.deploy.shared.ModuleType;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.deployment.GenericSniffer;
import org.jvnet.hk2.annotations.Service;

/**
 * This is a sniffer for Jakarta Data
 * @author Alfonso Valdez
 */
@Service
@PerLookup
public class JakartaDataRepositorySniffer extends GenericSniffer {
    
    private static final Logger logger = Logger.getLogger(JakartaDataRepositorySniffer.class.getName());
    
    public JakartaDataRepositorySniffer() {
        super(null, null , null );
    }

    public JakartaDataRepositorySniffer(String containerName, String appStigma, String urlPattern) {
        super(containerName, appStigma, urlPattern);
    }

    @Override
    public Class<? extends Annotation>[] getAnnotationTypes() {
        return new Class[] {
                jakarta.data.repository.Repository.class,
                jakarta.data.repository.Query.class,
                jakarta.data.repository.Find.class,
                jakarta.data.repository.OrderBy.class,
                jakarta.data.repository.Insert.class,
                jakarta.data.repository.Update.class,
                jakarta.data.repository.Delete.class,
                jakarta.data.repository.Save.class
        };
    }

    @Override
    public String[] getAnnotationNames(DeploymentContext context) {
        final Class<?>[] types = getAnnotationTypes();
        if (types == null) {
            return null;
        }
        return Arrays.stream(types).map(Class::getName).collect(Collectors.toList()).toArray(String[]::new);
    }

    @Override
    public String getModuleType() {
        return "jakarta-data";
    }

    @Override
    public String[] getContainersNames() {
        return new String[0];
    }

    @Override
    public boolean supportsArchiveType(ArchiveType archiveType) {
        return archiveType.toString().equals(ModuleType.WAR.toString()) ||
                archiveType.toString().equals(ModuleType.EJB.toString()) ||
                archiveType.toString().equals(ModuleType.CAR.toString());
    }
}
