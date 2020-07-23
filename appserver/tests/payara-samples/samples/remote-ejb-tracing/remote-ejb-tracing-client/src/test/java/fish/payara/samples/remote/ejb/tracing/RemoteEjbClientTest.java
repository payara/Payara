package fish.payara.samples.remote.ejb.tracing;

import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class RemoteEjbClientTest {

    @Test
    public void executeRemoteEjbMethodTest() {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

        try {
            Context context = new InitialContext(contextProperties);
            EjbRemote ejb = (EjbRemote) context.lookup("java:global/remote-ejb-tracing-server/Ejb");
            System.out.println(ejb.annotatedMethod());
            System.out.println(ejb.nonAnnotatedMethod());
            System.out.println(ejb.shouldNotBeTraced());
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }


}
