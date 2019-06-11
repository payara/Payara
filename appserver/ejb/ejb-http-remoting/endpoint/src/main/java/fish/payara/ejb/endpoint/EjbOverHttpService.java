package fish.payara.ejb.endpoint;

import javax.naming.NamingException;

public interface EjbOverHttpService {

    ClassLoader getAppClassLoader(String applicationName);

    Object getBean(String jndiName) throws NamingException;
}
