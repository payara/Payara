/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import org.glassfish.deployment.common.Descriptor;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A persistence.xml file can contain one or more <persistence-unit>s
 * This class represents information about a <persistence-unit>.
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitDescriptor extends Descriptor {

    private PersistenceUnitsDescriptor parent;

    private String name;

    private String transactionType = "JTA"; // in persistence.xsd default is JTA

    private String description;

    private String provider;

    private String jtaDataSource;

    private String nonJtaDataSource;

    private List<String> mappingFiles = new ArrayList<String>();

    private List<String> jarFiles = new ArrayList<String>();

    private List<String> classes = new ArrayList<String>();

    private Properties properties = new Properties();

    private boolean excludeUnlistedClasses = false;

    private SharedCacheMode sharedCacheMode;

    private ValidationMode validationMode;

    public PersistenceUnitDescriptor() {
    }

    public PersistenceUnitsDescriptor getParent() {
        return parent;
    }

    protected void setParent(PersistenceUnitsDescriptor parent) {
        assert(this.parent==null);
        this.parent = parent;
    }

    // NOW let's implement some methods specific to this descriptor
    // Most of these setter methods are invoked using reflection
    // by PersistenceNode. So any change here has to be reflcted there as
    // well. Compiler won't catch them for you.

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;

    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String value) {

        this.provider = value;
    }

    public String getJtaDataSource() {
        return jtaDataSource;
    }

    public void setJtaDataSource(String value) {
        this.jtaDataSource = value;

    }

    public String getNonJtaDataSource() {
        return nonJtaDataSource;
    }

    public void setNonJtaDataSource(String value) {
        this.nonJtaDataSource = value;

    }

    public List<String> getMappingFiles() {
        return Collections.unmodifiableList(mappingFiles);
    }

    public void addMappingFile(String mappingFile) {
        mappingFiles.add(mappingFile);
    }

    public List<String> getJarFiles() {
        return Collections.unmodifiableList(jarFiles);
    }

    public void addJarFile(String jarFile) {
        jarFiles.add(jarFile);

    }

    public List<String> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    public void addClass(String className) {
        classes.add(className);

    }

    public Properties getProperties() {
        return (Properties) properties.clone();
    }

    public void addProperty(String name, Object value) {
        properties.put(name, value);

    }

    public boolean isExcludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }

    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    public void setSharedCacheMode(String sharedCacheMode) {
        // Assume schema validation will only pass a valid value else user would correctly get an exception at deployment
        this.sharedCacheMode = SharedCacheMode.valueOf(sharedCacheMode);
    }

    public ValidationMode getValidationMode() {
        return validationMode;
    }

    public void setValidationMode(String validationMode) {
        // Assume schema validation will only pass a valid value else user would correctly get an exception at deployment
        this.validationMode = ValidationMode.valueOf(validationMode);
    }

    public String getPuRoot() {
        return parent.getPuRoot();
    }

    /**
     * @return the absolute path of the root of this persistence unit
     * @see #getPuRoot()
     * @see PersistenceUnitsDescriptor#getAbsolutePuRoot()
     */
    public String getAbsolutePuRoot() {
        return getParent().getAbsolutePuRoot();
     }

}
