/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URISyntaxException;
import java.net.URI;

import javax.validation.constraints.NotNull;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

@Configured
@RestRedirects(
        {
          @RestRedirect(opType= RestRedirect.OpType.DELETE, commandName="undeploy"),
          @RestRedirect(opType= RestRedirect.OpType.POST, commandName = "redeploy")
        }
)
public interface Application extends Injectable, ApplicationName, PropertyBag {

    public static final String APP_LOCATION_PROP_NAME = "appLocation";
    public static final String DEPLOYMENT_PLAN_LOCATION_PROP_NAME = "deploymentPlanLocation";
    public static final String WEB_SNIFFER_TYPE = "web";
    public static final String EJB_SNIFFER_TYPE = "ejb";
    public static final String CONNECTOR_SNIFFER_TYPE = "connector";
    public static final String APPCLIENT_SNIFFER_TYPE = "appclient";
    public static final String JPA_SNIFFER_TYPE = "jpa";
    public static final String WEBSERVICE_SNIFFER_TYPE = "webservices";
    public static final String EAR_SNIFFER_TYPE = "ear";
    public static final String OSGI_SNIFFER_TYPE = "osgi";
    public static final String APPLICATION = "application";
    public static final String EXTERNAL_MODULE_TYPE = "external";

    /**
     * Gets the value of the contextRoot property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute    
    String getContextRoot();

    /**
     * Sets the value of the contextRoot property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setContextRoot(String value) throws PropertyVetoException;

    /**
     * Gets the value of the location property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getLocation();

    /**
     * Sets the value of the location property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLocation(String value) throws PropertyVetoException;

    /**
     * Gets the value of the objectType property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(required=true)
    @NotNull 
    String getObjectType();

    /**
     * Sets the value of the objectType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setObjectType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="true", dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the libraries property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getLibraries();

    /**
     * Sets the value of the libraries property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setLibraries(String value) throws PropertyVetoException;

    /**
     * Gets the value of the availabilityEnabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="false", dataType=Boolean.class)
    String getAvailabilityEnabled();

    /**
     * Sets the value of the availabilityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAvailabilityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the asyncReplication property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="true", dataType=Boolean.class)
    String getAsyncReplication();

    /**
     * Sets the value of the asyncReplication property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAsyncReplication(String value) throws PropertyVetoException;

    /**
     * Gets the value of the directoryDeployed property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="false", dataType=Boolean.class)
    String getDirectoryDeployed();

    /**
     * Sets the value of the directoryDeployed property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDirectoryDeployed(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDescription(String value) throws PropertyVetoException;


    @Element
    List<Module> getModule();
    
    /**
     * Gets the value of the engine property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the engine property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEngine().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Engine }
     */
    @Element
    List<Engine> getEngine();

    /**
     * Gets the value of the webServiceEndpoint property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the webServiceEndpoint property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWebServiceEndpoint().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link WebServiceEndpoint }
     */
    @Element
    List<WebServiceEndpoint> getWebServiceEndpoint();

    @Element
    Resources getResources();

    void setResources(Resources resources) throws PropertyVetoException;

    @DuckTyped
    Module getModule(String moduleName);

    @DuckTyped
    Properties getDeployProperties();

    @DuckTyped
    DeployCommandParameters getDeployParameters(ApplicationRef appRef);

    @DuckTyped
    Map<String, Properties> getModulePropertiesMap();

    @DuckTyped
    boolean isStandaloneModule();

    @DuckTyped
    boolean containsSnifferType(String snifferType);

    @DuckTyped
    boolean isLifecycleModule();

    @DuckTyped
    boolean isOSGiModule();

    @DuckTyped
    void recordFileLocations(File app, File plan);
    
    @DuckTyped
    File application();
    
    @DuckTyped
    File deploymentPlan();
    
    class Duck {
        public static Module getModule(Application instance, String name) {
            for (Module module : instance.getModule()) {
                if (module.getName().equals(name)) {
                    return module;
                }
            }
            return null;
        }

        public static Properties getDeployProperties(Application instance) {
            Properties deploymentProps = new Properties();
            for (Property prop : instance.getProperty()) {
                deploymentProps.put(prop.getName(), prop.getValue());
            }
            deploymentProps.setProperty(ServerTags.OBJECT_TYPE,
                instance.getObjectType());
            if (instance.getContextRoot() != null) {
                deploymentProps.setProperty(ServerTags.CONTEXT_ROOT,
                    instance.getContextRoot());
            }
            if (instance.getDirectoryDeployed() != null) {
                deploymentProps.setProperty(ServerTags.DIRECTORY_DEPLOYED,
                    instance.getDirectoryDeployed());
            }
            return deploymentProps;            
        }

        public static DeployCommandParameters getDeployParameters(Application app, ApplicationRef appRef) {

            URI uri = null;
            try {
                uri = new URI(app.getLocation());
            } catch (URISyntaxException e) {
                Logger.getAnonymousLogger().log(
                    Level.SEVERE, e.getMessage(), e);
            }

            if (uri == null) {
                return null;
            }

            DeployCommandParameters deploymentParams = new DeployCommandParameters(new File(uri));
            deploymentParams.name = app.getName();
            deploymentParams.description = app.getDescription();
            if (Boolean.valueOf(app.getEnabled()) && appRef != null && 
                Boolean.valueOf(appRef.getEnabled())) { 
                deploymentParams.enabled = Boolean.TRUE;
            } else {
                deploymentParams.enabled = Boolean.FALSE;
            }
            deploymentParams.contextroot = app.getContextRoot();
            deploymentParams.libraries = app.getLibraries();
            deploymentParams.availabilityenabled = Boolean.parseBoolean(app.getAvailabilityEnabled());
            deploymentParams.asyncreplication = Boolean.parseBoolean(app.getAsyncReplication());
            if (appRef != null) {
                deploymentParams.lbenabled = appRef.getLbEnabled();
            }
            if (appRef != null) {
                deploymentParams.virtualservers = appRef.getVirtualServers();
            }
            return deploymentParams;
        }

        public static Map<String, Properties> getModulePropertiesMap(
            Application me) {
            Map<String, Properties> modulePropertiesMap = 
                new HashMap<String, Properties>();
            for (Module module: me.getModule()) {
                if (module.getProperty() != null) {
                    Properties moduleProps = new Properties();
                    for (Property property : module.getProperty()) {
                        moduleProps.put(property.getName(), 
                            property.getValue());
                    }
                    modulePropertiesMap.put(module.getName(), moduleProps);
                }
            }
            return modulePropertiesMap;
        }

        public static boolean isStandaloneModule(Application me) {
            return !(Boolean.valueOf(me.getDeployProperties().getProperty
                (ServerTags.IS_COMPOSITE)));
        }

        public static boolean isLifecycleModule(Application me) {
            return Boolean.valueOf(me.getDeployProperties().getProperty
                (ServerTags.IS_LIFECYCLE));
        }

        public static boolean isOSGiModule(Application me) {
            return me.containsSnifferType(OSGI_SNIFFER_TYPE);
        }


        public static boolean containsSnifferType(Application app, 
            String snifferType) {
            List<Engine> engineList = new ArrayList<Engine>();

            // first add application level engines
            engineList.addAll(app.getEngine());

            // now add module level engines
            for (Module module: app.getModule()) {
                engineList.addAll(module.getEngines());
            }

            for (Engine engine : engineList) {
                if (engine.getSniffer().equals(snifferType)) {
                    return true;
                }
            }
            return false;
        }
        
        public static File application(final Application instance) {
            return fileForProp(instance, APP_LOCATION_PROP_NAME);
            
        }
        
        public static File deploymentPlan(final Application instance) {
            return fileForProp(instance, DEPLOYMENT_PLAN_LOCATION_PROP_NAME);
        }
        
        private static File fileForProp(final Application instance,
                final String propName) {
            for (Property p : instance.getProperty()) {
                if (p.getName().equals(propName)) {
                    return new File(URI.create(p.getValue()));
                }
            }
            return null;
        }
    }
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
