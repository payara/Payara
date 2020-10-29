package fish.payara.samples.rolesallowed.unprotected.methods;

import javax.ejb.Remote;

@Remote
public interface HelloServiceRemote {

    String sayHello();

    String secureSayHello();
}
