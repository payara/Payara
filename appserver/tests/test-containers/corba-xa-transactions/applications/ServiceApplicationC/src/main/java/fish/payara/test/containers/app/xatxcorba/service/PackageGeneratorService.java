package fish.payara.test.containers.app.xatxcorba.service;

import fish.payara.test.containers.app.xatxcorba.entities.DataPackage;
import com.thedeanda.lorem.LoremIpsum;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageGenerator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(CrossPackageGenerator.class)
public class PackageGeneratorService implements CrossPackageGenerator{

    private static final Logger LOG = Logger.getLogger(PackageGeneratorService.class.getName());

    @EJB
    CounterService counterService;

    private final LoremIpsum loremIpsum = LoremIpsum.getInstance();

    @Override
    public List<DataPackage> generatePackages(int size){
        LOG.log(Level.INFO, "[CrossPackageGenerator] - Generating {0} packages", size);
        return IntStream.range(0, size)
                 .mapToObj(step -> new DataPackage(counterService.getNextId(), extractContents(step + 1)))
                 .collect(Collectors.toList());
    }

    @Override
    public DataPackage generatePackage(int details){
        LOG.log(Level.INFO, "[CrossPackageGenerator] - Generating package with {0} details", details);
        return new DataPackage(counterService.getNextId(), extractContents(details));
    }

    private String extractContents(int paragraphs){
        return loremIpsum.getParagraphs(1, paragraphs);
    }
}
