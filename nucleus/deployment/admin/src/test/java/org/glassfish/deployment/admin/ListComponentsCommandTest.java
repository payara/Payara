/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]
package org.glassfish.deployment.admin;

import java.io.File;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.deployment.DeployCommandParameters;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;


/**
 * junit test to test ListComponentsCommand class
 */
public class ListComponentsCommandTest {
    private ListComponentsCommand lcc = null;

    @Test
    public void isApplicationOfThisTypeTest() {
        try {
            ApplicationTest app = new ApplicationTest();
            Engine eng = new EngineTest();
            eng.setSniffer("web");
            List<Engine> engines = new ArrayList<Engine>();
            engines.add(eng);
            List<Module> modules = new ArrayList<Module>();
            ModuleTest aModule = new ModuleTest();
            aModule.setEngines(engines);
            modules.add(aModule);
            app.setModules(modules);
        
            boolean ret = lcc.isApplicationOfThisType(app, "web");
            assertTrue("test app with sniffer engine=web", true==lcc.isApplicationOfThisType(app, "web"));
            //negative testcase
            assertFalse("test app with sniffer engine=web", true==lcc.isApplicationOfThisType(app, "ejb"));
        }
        catch (Exception ex) {
            //ignore exception
        } 
    }

        @Test
    public void getSnifferEnginesTest() {
        try {
            Engine eng1 = new EngineTest();
            eng1.setSniffer("web");
            Engine eng2 = new EngineTest();
            eng2.setSniffer("security");
            List<Engine> engines = new ArrayList<Engine>();
            engines.add(eng1);
            engines.add(eng2);
            
            ApplicationTest app = new ApplicationTest();
            List<Module> modules = new ArrayList<Module>();              
            ModuleTest aModule = new ModuleTest();
            aModule.setEngines(engines);
            modules.add(aModule);
            app.setModules(modules);
            String snifferEngines = lcc.getSnifferEngines(app.getModule().get(0), true);
            assertEquals("compare all sniffer engines", "<web, security>",
                        snifferEngines);
        }
        catch (Exception ex) {
            //ignore exception
        } 
    }


    @Before
    public void setup() {
        lcc = new ListComponentsCommand();
    }

    public class RandomConfig implements ConfigBeanProxy {

        @DuckTyped
        @Override
        public ConfigBeanProxy getParent() {
            // TODO
            throw new UnsupportedOperationException();
        }
        @DuckTyped
        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            // TODO
            throw new UnsupportedOperationException();
        }
        @DuckTyped
        public Property getProperty(String name) {
            // TODO
            throw new UnsupportedOperationException();
        }

        @DuckTyped
        public String getPropertyValue(String name) {
            // TODO
            throw new UnsupportedOperationException();
        }

        @DuckTyped
        public String getPropertyValue(String name, String defaultValue) {
            // TODO
            throw new UnsupportedOperationException();
        }

        @DuckTyped
        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) {
            throw new UnsupportedOperationException();
        }

        //hk2's Injectable class
        public void injectedInto(Object target){}
    }

    public class ModuleTest extends RandomConfig implements Module {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String value) throws PropertyVetoException {}

        private List<Engine> engineList = null;

        @Override
        public List<Engine> getEngines() {
            return engineList;
        }
        @Override
        public Engine getEngine(String snifferType) {return null;}

        public void setEngines(List<Engine> engines) {
            this.engineList = engines;
        }

        @Override
       public List<Property> getProperty() {return null;}

        @Override
        public Property addProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property lookupProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setResources(Resources res){}
        @Override
        public Resources getResources(){return null;}


    }
        //mock-up Application object
    public class ApplicationTest extends RandomConfig implements Application {
        private List<Module> modules = null;
        
        @Override
        public String getName() {
            return "hello";
        }
        @Override
        public void setResources(Resources res){}
        @Override
        public Resources getResources(){return null;}
        @Override
        public void setName(String value) throws PropertyVetoException {}
        @Override
        public String getContextRoot() { return "hello";}
        @Override
        public void setContextRoot(String value) throws PropertyVetoException {}
        @Override
        public String getLocation(){ return "";}
        @Override
        public void setLocation(String value) throws PropertyVetoException{}
        @Override
        public String getObjectType(){ return "";}
        @Override
        public void setObjectType(String value) throws PropertyVetoException{}
        @Override
        public String getEnabled(){ return "";}
        @Override
        public void setEnabled(String value) throws PropertyVetoException{}
        @Override
        public String getLibraries(){ return "";}
        @Override
        public void setLibraries(String value) throws PropertyVetoException{}
        @Override
        public String getAvailabilityEnabled(){ return "";}
        @Override
        public void setAvailabilityEnabled(String value) throws PropertyVetoException{}
        @Override
        public String getAsyncReplication() { return "";}
        @Override
        public void setAsyncReplication (String value) throws PropertyVetoException {}
        @Override
        public String getDirectoryDeployed(){ return "";}
        @Override
        public void setDirectoryDeployed(String value) throws PropertyVetoException{}
        @Override
        public String getDescription(){ return "";}
        @Override
        public void setDescription(String value) throws PropertyVetoException{}
        @Override
        public String getDeploymentOrder() { return "100"; }
        @Override
        public void setDeploymentOrder(String value) throws PropertyVetoException {}
        @Override
        public String getDeploymentTime() { return "0"; }
        @Override
        public void setDeploymentTime(String value) throws PropertyVetoException {}
        @Override
        public String getTimeDeployed() { return "0"; }
        @Override
        public void setTimeDeployed(String value) throws PropertyVetoException {}
        @Override
        public List<Engine> getEngine(){ return null;}
        @Override
        public List<Property> getProperty(){ return null;}


        @Override
        public Property addProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property lookupProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        public <T extends ApplicationConfig> T getApplicationConfig(Class<T> type) {return null;}

        public List<ApplicationConfig> getApplicationConfigs() {return null;}
        @Override
        public Map<String, Properties> getModulePropertiesMap() {return null;}

        @Override
        public Module getModule(String moduleName) {return null;}
        @Override
        public boolean isStandaloneModule() {return false;}
        @Override
        public boolean isLifecycleModule() {return false;}
        @Override
        public boolean containsSnifferType(String snifferType) {return false;}
        @Override
        public List<Module> getModule() {
            return modules;
        }

        public void setModules(List<Module> modules) {
        this.modules = modules;
        }

        @Override
            public Properties getDeployProperties() {
                return new Properties();
            }

        @Override
            public DeployCommandParameters getDeployParameters(ApplicationRef appRef) {
                return new DeployCommandParameters();
            }
        
        @Override
        public File application() {return null;}
        @Override
        public File deploymentPlan() {return null;}
        @Override
        public String archiveType() {return null;}
        @Override
        public void recordFileLocations(File appFile, File deploymentPlanFile) {}

        @Override
        public AppTenants getAppTenants() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAppTenants(AppTenants appTenants) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ApplicationExtension> getExtensions() {return null;}

        @Override
        public <T extends ApplicationExtension> T getExtensionByType(Class<T> type) {
            return null;
        }
        @Override
        public <T extends ApplicationExtension> List<T> getExtensionsByType(Class<T> type) {
            return null;
        }
    }

            //mock-up Engine object
    public class EngineTest extends RandomConfig implements Engine {
        private String sniffer = "";
        @Override
        public String getSniffer() {return sniffer;}
        @Override
        public void setSniffer(String value) throws PropertyVetoException {
            sniffer = value;
        }
        @Override
        public String getDescription() {return "";}
        @Override
        public void setDescription(String value) {}
        @Override
        public List<Property> getProperty() {return null;}


        @Override
        public Property addProperty(Property property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property lookupProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Property removeProperty(Property property) {
            throw new UnsupportedOperationException();
        }


        //config.serverbeans.Modules
        public String getName() { 
            return "hello";
        }
        public void setName(String value) throws PropertyVetoException {}

        public ApplicationConfig getConfig() {
            return null;
        }

        public void setConfig(ApplicationConfig config) throws PropertyVetoException {}

        @Override
        public List<ApplicationConfig> getApplicationConfigs() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public ApplicationConfig getApplicationConfig() {
            return null;
        }

        @Override
        public void setApplicationConfig(ApplicationConfig config) {
            // no-op for this test
        }

        @Override
        public <T extends ApplicationConfig> T newApplicationConfig(Class<T> configType) throws TransactionFailure {
            return null;
        }
    }
}
