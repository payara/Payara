package fish.payara.test.containers.app.xatxcorba.interfaces;

import fish.payara.test.containers.app.xatxcorba.entities.DataPackage;
import java.util.List;

public interface CrossPackageGenerator {

    List<DataPackage> generatePackages(int size);

    DataPackage generatePackage(int details);
}
