/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import fish.payara.jacc.JaccConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;

/**
 * The core of this sample; installing our own JACC Policy for the current web application.
 *
 * @author Arjan Tijms
 *
 */
@WebListener
public class PolicyRegistrationInstaller implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        PolicyFactory policyFactory = PolicyFactory.getPolicyFactory();
        policyFactory.setPolicy(new LoggingTestPolicy(policyFactory.getPolicy()));
    }

    private String getAppContextId(ServletContext servletContext) {
        return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
    }

}