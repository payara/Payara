package fish.payara.test.containers.app.xatxcorba.interfaces;

import fish.payara.test.containers.app.xatxcorba.entities.MarkedPackage;
import fish.payara.test.containers.app.xatxcorba.entities.PackageInstruction;
import java.util.List;

public interface CrossPackageMarker {

    List<MarkedPackage> markPackages(List<PackageInstruction> instructions);
}
