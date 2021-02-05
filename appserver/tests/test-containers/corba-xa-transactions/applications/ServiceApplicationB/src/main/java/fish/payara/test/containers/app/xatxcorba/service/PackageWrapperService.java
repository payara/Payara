package fish.payara.test.containers.app.xatxcorba.service;

import fish.payara.test.containers.app.xatxcorba.entities.DataPackage;
import fish.payara.test.containers.app.xatxcorba.entities.PackageInstruction;
import fish.payara.test.containers.app.xatxcorba.entities.WrappedDataPackage;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageGenerator;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageWrapper;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * @author fabio
 */
@Stateless
@Remote(CrossPackageWrapper.class)
public class PackageWrapperService implements CrossPackageWrapper {

    private static final Logger LOG = Logger.getLogger(PackageWrapperService.class.getName());

    @EJB(name = "ejb/CrossPackager")
    private CrossPackageGenerator packageGenerator;

    @Override
    public List<WrappedDataPackage> generateAndWrap(List<PackageInstruction> instructions) {
        LOG.log(Level.INFO, "[CrossPackageWrapper] - Received {0} instructions", instructions.size());
        return instructions.stream()
                .map(this::process)
                .collect(Collectors.toList());
    }

    private WrappedDataPackage process(PackageInstruction instruction) {
        DataPackage dPackage = packageGenerator.generatePackage(instruction.getContentDetails());
        return new WrappedDataPackage(instruction.getSignature(), dPackage);
    }
}
