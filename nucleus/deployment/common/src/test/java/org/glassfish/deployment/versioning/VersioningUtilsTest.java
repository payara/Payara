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

package org.glassfish.deployment.versioning;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.deployment.DeployCommandParameters;
import com.sun.enterprise.config.serverbeans.*;

/**
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */

public class VersioningUtilsTest {

    private static final String APPLICATION_NAME = "foo";

    /**
     * Test of {@link org.glassfish.deployment.versioning.VersioningUtils.getUntaggedName}
     *
     * Check the extraction of untagged names from different application names
     * as version identifier, version expression or untagged application name.
     *
     * @throws VersioningSyntaxException if the given application name had some
     *  critical patterns.
     */
    @Test
    public void testGetUntaggedName() throws VersioningSyntaxException {

        // test an application name that contains a version expression
        // application name : foo:RC-*
        String expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-"
                + VersioningUtils.EXPRESSION_WILDCARD;

        String result = VersioningUtils.getUntaggedName(expression);
        assertEquals(APPLICATION_NAME, result);

        // test an application name that contains a version identifier
        // application name : foo:RC-1.0.0
        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.0";

        result = VersioningUtils.getUntaggedName(expression);
        assertEquals(APPLICATION_NAME, result);

        // test an application name that is an untagged version name
        // application name : foo
        expression = APPLICATION_NAME;

        result = VersioningUtils.getUntaggedName(expression);
        assertEquals(APPLICATION_NAME, result);

        // test an application name containing a critical pattern
        // application name : foo:
        expression = APPLICATION_NAME + VersioningUtils.EXPRESSION_SEPARATOR;

        try {
            result = VersioningUtils.getUntaggedName(expression);
            fail("the getUntagged method did not throw a VersioningDeploymentSyntaxException");
        }
        catch(VersioningSyntaxException e){}
    }

    /**
     * Test of {@link org.glassfish.deployment.versioning.VersioningUtils.getExpression}
     *
     * Check the extraction of version expression / identifier from different
     * application names.
     *
     * @throws VersioningSyntaxException if the given application name had some
     *  critical patterns.
     */
    @Test
    public void testGetExpression() throws VersioningSyntaxException {

        // test an application name containing a critical pattern
        // application name : foo:
        String expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR;

        try {
            String result = VersioningUtils.getExpression(expression);
            fail("the getExpression method did not throw a VersioningSyntaxException");
        } catch (VersioningSyntaxException e) {}

        // test an application name containing a critical pattern
        // application name : foo:RC-1;0.0
        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + "RC-1"
                + VersioningUtils.EXPRESSION_SEPARATOR
                + "0.0";

        try {
            String result = VersioningUtils.getExpression(expression);
            //fail("the getExpression method did not throw a VersioningSyntaxException");
        } catch (VersioningSyntaxException e) {}
    }

    /**
     * Test of {@link org.glassfish.deployment.versioning.VersioningUtils.getVersions}
     *
     * Check the extraction of a set of version(s) from a set of applications.
     */
    @Test
    public void testGetVersions() throws VersioningException {
        // the set of applications
        List<Application> listApplications = new ArrayList<Application>();
        listApplications.add(new ApplicationTest(APPLICATION_NAME));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                VersioningUtils.EXPRESSION_SEPARATOR+"BETA-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                VersioningUtils.EXPRESSION_SEPARATOR+"RC-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                "_RC-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                ";RC-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                ".RC-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                "-RC-1.0.0"));
        listApplications.add(new ApplicationTest(APPLICATION_NAME+
                APPLICATION_NAME));

        // the expected set of versions
        List<String> expResult = new ArrayList<String>();
        expResult.add(APPLICATION_NAME);
        expResult.add(APPLICATION_NAME+
                VersioningUtils.EXPRESSION_SEPARATOR+"BETA-1.0.0");
        expResult.add(APPLICATION_NAME+
                VersioningUtils.EXPRESSION_SEPARATOR+"RC-1.0.0");

        List result = VersioningUtils.getVersions(APPLICATION_NAME, listApplications);
        assertEquals(expResult, result);
    }
    
    /**
     * Test of {@link org.glassfish.deployment.versioning.VersioningUtils.matchExpression}
     *
     * Check the matching of version expression over a set of version
     * 
     * @throws VersioningException for registration issues, or if the given
     *  application name had some
     *  critical patterns.
     */
    @Test
    public void testMatchExpression() throws VersioningException {
        // the set of all foo versions
        List<String> listVersion = new ArrayList<String>();
        // ALPHA versions
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.2");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.1.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.1.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.1.2");
        // BETA versions
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.0.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.0.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.0.2");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.1.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.1.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.1.2");
        // RC versions
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.2");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.0");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.2");

        // **************************************************
        // TEST TYPE 1 : expression matching all the versions
        // **************************************************

        // the expected set of matched version is all the versions
        List expResult = new ArrayList<String>(listVersion);

        // ------------------------
        // application name foo:*
        // ------------------------

        String expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD;

        List result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // -----------------------------
        // application name foo:******
        // -----------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // *****************************************************
        // TEST TYPE 2 : expression matching all the RC versions
        // *****************************************************
        expResult.clear();
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.0");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.1");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.2");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.0");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.1");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.1.2");

        // --------------------------
        // application name foo:RC*
        // --------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC"
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // --------------------------
        // application name foo:*RC*
        // --------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD + "RC"
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // -------------------------------
        // application name foo:***RC***
        // -------------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD + "RC"
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // ********************************************************
        // TEST TYPE 3 : expression matching all the 1.0.2 versions
        // ********************************************************
        expResult.clear();
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.2");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "BETA-1.0.2");
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "RC-1.0.2");

        // ------------------------------
        // application name foo:*-1.0.2
        // ------------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD + "-1.0.2";

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // ----------------------------------
        // application name foo:***1.0.2***
        // ----------------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD + "-1.0.2"
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // ----------------------------------
        // application name foo:***1*0*2***
        // ----------------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD + "1"
                + VersioningUtils.EXPRESSION_WILDCARD + "0"
                + VersioningUtils.EXPRESSION_WILDCARD + "2"
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD
                + VersioningUtils.EXPRESSION_WILDCARD;

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // **************************************
        // TEST TYPE 4 : identifier as expression
        // **************************************
        expResult.clear();
        expResult.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.2");

        // ----------------------------------
        // application name foo:ALPHA-1.0.2
        // ----------------------------------

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "ALPHA-1.0.2";

        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(expResult, result);

        // *****************************************
        // check for pattern matching like issue 12132
        // *****************************************

        listVersion.clear();
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "abc-1");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "abc-2");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "abc-3");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "bac-4");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "cab-5");
        listVersion.add(APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "cba-6");
        
        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "a*";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 3);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "*a";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 0);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "a****1";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 1);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "*-*";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 6);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "*-4";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 1);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "b*";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 1);

        expression = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR + "b*";
        result = VersioningUtils.matchExpression(listVersion, expression);
        assertEquals(result.size(), 1);
    }

    /**
     * Test of getIdentifier method, of class VersioningUtils.
     * @throws VersioningException
     */
    @Test
    public void testGetIdentifier() throws VersioningException {
        // *****************************************
        // check for getIdentifier with and without '*'
        // *****************************************
        String versionIdentifier = "BETA-1";
        String appName = "foo" + VersioningUtils.EXPRESSION_SEPARATOR + versionIdentifier;
        try{
            VersioningUtils.checkIdentifier(appName);
        } catch (VersioningSyntaxException e){
            fail(e.getMessage());
        }

        String versionExpression = "BETA-*";
        appName = "foo" + VersioningUtils.EXPRESSION_SEPARATOR + versionExpression;
        try {
            VersioningUtils.checkIdentifier(appName);
            fail("the getIdentifier method should not accept version with '*' in it.");
        } catch (VersioningException e) {}
     }
    /**
     * Test of getRepositoryName method, of class VersioningUtils.
     * @throws VersioningSyntaxException
     */
    @Test
    public void testGetRepositoryName() throws VersioningSyntaxException {
        String versionIdentifier = "RC-1.0.0";

        String appName = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + versionIdentifier;

        String expectedResult = APPLICATION_NAME
                + VersioningUtils.REPOSITORY_DASH
                + versionIdentifier;

        String result = "";
        result = VersioningUtils.getRepositoryName(appName);
        assertEquals(expectedResult, result);

        //==========================================

        versionIdentifier = "RC:1.0.0";
        appName = APPLICATION_NAME
                + VersioningUtils.EXPRESSION_SEPARATOR
                + versionIdentifier;
        
        expectedResult = APPLICATION_NAME
                + VersioningUtils.REPOSITORY_DASH
                + versionIdentifier.replace(":", VersioningUtils.REPOSITORY_DASH);

        result = VersioningUtils.getRepositoryName(appName);
        assertEquals(expectedResult, result);
    }

     /**
     * Test of isUntagged method, of class VersioningUtils.
     */
    @Test
    public void testIsUntagged() {
        try{
            VersioningUtils.isUntagged(APPLICATION_NAME+":");
            fail("an exception has to be thrown when '"+APPLICATION_NAME+":' is supplied to isUntagged method");
        }
        catch(VersioningException e){}
        try{
            VersioningUtils.isUntagged(":BETA");
            fail("an exception has to be thrown when ':BETA' is supplied to isUntagged method");
        }
        catch(VersioningException e){}
        try{
            VersioningUtils.isUntagged("::");
            fail("an exception has to be thrown when '::' is supplied to isUntagged method");
        }
        catch(VersioningException e){}
        assertEquals(false, VersioningUtils.isUntagged(null));
        assertEquals(false, VersioningUtils.isUntagged(APPLICATION_NAME+":*"));
        assertEquals(false, VersioningUtils.isUntagged(APPLICATION_NAME+":BETA*"));
        assertEquals(false, VersioningUtils.isUntagged(APPLICATION_NAME+":BETA"));
        assertEquals(false, VersioningUtils.isUntagged(APPLICATION_NAME+"::"));
        assertEquals(false, VersioningUtils.isUntagged(APPLICATION_NAME+":BETA:2"));
    }

     /**
     * Test of isVersionExpression method, of class VersioningUtils.
     */
    @Test
    public void testIsVersionExpression() {
        assertEquals(false, VersioningUtils.isVersionExpression(null));
        assertEquals(false, VersioningUtils.isVersionExpression(APPLICATION_NAME));
        assertEquals(true, VersioningUtils.isVersionExpression(APPLICATION_NAME+":BETA"));
        assertEquals(true, VersioningUtils.isVersionExpression(APPLICATION_NAME+"::"));
    }

    /**
     * Test of isVersionIdentifier method, of class VersioningUtils.
     */
    @Test
    public void testIsVersionIdentifier() {
        assertEquals(false, VersioningUtils.isVersionIdentifier(APPLICATION_NAME+":*"));
        assertEquals(false, VersioningUtils.isVersionIdentifier(APPLICATION_NAME+":BETA*"));
    }

    // this class is used to fake the List<Application> 
    // so we can call the VersioningUtils.matchExpression
    // with an home made set of applications.
    private class ApplicationTest implements Application {
        private String name;

        public ApplicationTest(String value){
            this.name = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public AppTenants getAppTenants() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAppTenants(AppTenants appTenants) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setName(String value) throws PropertyVetoException{
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setResources(Resources resources){
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resources getResources(){
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getContextRoot() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setContextRoot(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getLocation() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setLocation(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getObjectType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setObjectType(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getEnabled() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setEnabled(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getLibraries() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setLibraries(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getAvailabilityEnabled() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAvailabilityEnabled(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getAsyncReplication() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAsyncReplication (String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getDirectoryDeployed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDirectoryDeployed(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getDescription() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDescription(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
 
        public String getDeploymentOrder() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setDeploymentOrder(String value) throws PropertyVetoException{
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Module> getModule() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Engine> getEngine() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Module getModule(String moduleName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Properties getDeployProperties() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DeployCommandParameters getDeployParameters(ApplicationRef appRef) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, Properties> getModulePropertiesMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }


        @Override
        public boolean isStandaloneModule() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isLifecycleModule() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean containsSnifferType(String snifferType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void recordFileLocations(File app, File plan) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public File application() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public File deploymentPlan() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String archiveType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<Property> getProperty() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Property getProperty(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPropertyValue(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPropertyValue(String name, String defaultValue) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ConfigBeanProxy getParent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type)
               throws TransactionFailure {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<ApplicationExtension> getExtensions() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ApplicationExtension> T getExtensionByType(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T extends ApplicationExtension> List<T> getExtensionsByType(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
