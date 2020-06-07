/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.omnifaces.jaccprovider.TestPolicyConfigurationFactory;

import fish.payara.jacc.JaccConfigurationFactory;

/**
 * The core of this sample; installing our own JACC Policy for the current web application.
 *
 * @author Arjan Tijms
 *
 */
@WebListener
public class JaccInstaller implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        JaccConfigurationFactory.getJaccConfigurationFactory().registerContextProvider(
                getAppContextId(sce.getServletContext()),
                new TestPolicyConfigurationFactory(),
                new LoggingTestPolicy());

    }

    private String getAppContextId(ServletContext servletContext) {
        return servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
    }

}