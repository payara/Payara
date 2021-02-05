package fish.payara.test.containers.app.xatxcorba.interfaces;

import fish.payara.test.containers.app.xatxcorba.entities.PackageInstruction;
import fish.payara.test.containers.app.xatxcorba.entities.WrappedDataPackage;
import java.util.List;

public interface CrossPackageWrapper {

    List<WrappedDataPackage> generateAndWrap(List<PackageInstruction> instructions);
}
