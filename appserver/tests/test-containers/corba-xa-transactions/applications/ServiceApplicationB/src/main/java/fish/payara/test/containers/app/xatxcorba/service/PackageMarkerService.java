package fish.payara.test.containers.app.xatxcorba.service;

import fish.payara.test.containers.app.xatxcorba.entities.MarkedPackage;
import fish.payara.test.containers.app.xatxcorba.entities.PackageInstruction;
import fish.payara.test.containers.app.xatxcorba.interfaces.CrossPackageMarker;
import java.util.List;
import java.util.stream.Collectors;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(CrossPackageMarker.class)
public class PackageMarkerService implements CrossPackageMarker{

    @Override
    public List<MarkedPackage> markPackages(List<PackageInstruction> instructions) {
        return instructions.stream()
                    .map(PackageInstruction::getSignature)
                    .map(MarkedPackage::new)
                    .collect(Collectors.toList());
    }

}
