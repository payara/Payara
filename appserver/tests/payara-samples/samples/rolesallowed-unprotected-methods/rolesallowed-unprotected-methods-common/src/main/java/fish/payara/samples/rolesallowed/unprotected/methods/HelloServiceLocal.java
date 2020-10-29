package fish.payara.samples.rolesallowed.unprotected.methods;

import javax.ejb.Local;

@Local
public interface HelloServiceLocal {

    String sayHello();

    String secureSayHello();
}