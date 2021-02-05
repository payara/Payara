package fish.payara.test.containers.app.xatxcorba.service;

import fish.payara.test.containers.app.xatxcorba.entities.MarkedPackage;
import fish.payara.test.containers.app.xatxcorba.entities.PackageInstruction;
import fish.payara.test.containers.app.xatxcorba.entities.WrappedDataPackage;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageMarker;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageWrapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
//@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PackageProcesserService {

    private static final Logger LOG = Logger.getLogger(PackageProcesserService.class.getName());

    @EJB(name = "ejb/CrossPackageWrapper")
    private CrossPackageWrapper packageWrapper;

    @EJB(name = "ejb/CrossPackageMarker")
    private CrossPackageMarker packageMarker;

    public List<WrappedDataPackage> processPackages(int size) {
        LOG.log(Level.INFO, "[PackageProcesserService, Generating] - Will start process for {0} instructions", size);
        return packageWrapper.generateAndWrap(IntStream.range(0, size)
                .mapToObj(step -> generateInstruction())
                .collect(Collectors.toList()));
    }

    public List<MarkedPackage> markPackages(int size){
        LOG.log(Level.INFO, "[PackageProcesserService, Marking] - Will start process for {0} instructions", size);
        return packageMarker.markPackages(IntStream.range(0, size)
                .mapToObj(step -> generateInstruction())
                .collect(Collectors.toList()));
    }

    private PackageInstruction generateInstruction() {
        String signature = UUID.randomUUID().toString();
        int details = ThreadLocalRandom.current().nextInt(8) + 2;
        return new PackageInstruction(signature, details);
    }
}
