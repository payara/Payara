/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.glassfish.api.deployment.archive.ArchiveType;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This descriptor contains all common information amongst root element 
 * of the J2EE Deployment Descriptors (application, ejb-jar, web-app, 
 * connector...). 
 *
 * @author Jerome Dochez
 */
public abstract class RootDeploymentDescriptor extends Descriptor {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "invalidSpecVersion:  {0}", level="WARNING")
    private static final String INVALID_SPEC_VERSION = "NCLS-DEPLOYMENT-00046";

    /**
     * each module is uniquely identified with a moduleID
     */
    protected String moduleID;
    
    /**
     * version of the specification loaded by this descriptor
     */
    private String specVersion;

    /**
     * optional index string to disambiguate when serveral extensions are
     * part of the same module
     */
    private String index=null;
    
    /**
     * class loader associated to this module to load classes 
     * contained in the archive file
     */
    protected transient ClassLoader classLoader = null;

    /**
     * Extensions for this module descriptor, keyed by type, indexed using the instance's index
     */
    protected Map<Class<? extends RootDeploymentDescriptor>, List<RootDeploymentDescriptor>> extensions =
            new HashMap<Class<? extends RootDeploymentDescriptor>, List<RootDeploymentDescriptor>>();

        /**
     * contains the information for this module (like it's module name)
     */
    protected ModuleDescriptor moduleDescriptor;

    private final static List<?> emptyList = Collections.emptyList();

    /**
     * Construct a new RootDeploymentDescriptor 
     */
    public RootDeploymentDescriptor() {
        super();
    }
    
    /**
     * Construct a new RootDeploymentDescriptor with a name and description
     */
    public RootDeploymentDescriptor(String name, String description) {
        super(name, description);
    }
    
    /**
     * each module is uniquely identified with a moduleID
     * @param moduleID for this module
     */
    public void setModuleID(String moduleID) {
        this.moduleID = moduleID;
    }
    
    /**
     * @return the module ID for this module descriptor
     */
    public abstract String getModuleID();

    /**
     * @return the default version of the deployment descriptor
     * loaded by this descriptor
     */
    public abstract String getDefaultSpecVersion();

    /**
     * Return true if this root deployment descriptor does not describe anything
     * @return true if this root descriptor is empty
     */
    public abstract boolean isEmpty();

        
    /**
     * @return the specification version of the deployment descriptor
     * loaded by this descriptor
     */
    public String getSpecVersion() {
        if (specVersion == null) {
            specVersion = getDefaultSpecVersion();
        } 
        try {
            Double.parseDouble(specVersion); 
        } catch (NumberFormatException nfe) {
            deplLogger.log(Level.WARNING,
                           INVALID_SPEC_VERSION,
                           new Object[] {specVersion, getDefaultSpecVersion()});
            specVersion = getDefaultSpecVersion();
        }

        return specVersion;
    }
    
    /**
     * Sets the specification version of the deployment descriptor
     * @param specVersion version number
     */
    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }      
    
    /**
     * @return the module type for this bundle descriptor
     */
    public abstract ArchiveType getModuleType();

    /**
     * @return the tracer visitor for this descriptor
     */
    public DescriptorVisitor getTracerVisitor() {
        return null;
    }

    /**
     * Sets the class loader for this application
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /**               
     * @return the class loader associated with this module
     */
    public abstract ClassLoader getClassLoader();

    /**
     * sets the display name for this bundle
     */
    public void setDisplayName(String name) {
        super.setName(name);
    }
    
    /** 
     * @return the display name
     */
    public String getDisplayName() {
        return super.getName();
    }
    
    /**
     * as of J2EE1.4, get/setName are deprecated, 
     * people should use the set/getDisplayName or 
     * the set/getModuleID.
     */
    public void setName(String name) {
        setModuleID(name);
    }
    
    /**
     * as of J2EE1.4, get/setName are deprecated, 
     * people should use the set/getDisplayName or 
     * the set/getModuleID.
     * note : backward compatibility
     */     
    public String getName() {
        if (getModuleID()!=null) {
            return getModuleID();
        } else {
            return getDisplayName();
        }
    }
        
    public void setSchemaLocation(String schemaLocation) {
        addExtraAttribute("schema-location", schemaLocation);
    }
    
    public String getSchemaLocation() {
        return (String) getExtraAttribute("schema-location");
    }

   /**
     * @return the module descriptor for this bundle
     */
    public ModuleDescriptor getModuleDescriptor() {
        if (moduleDescriptor==null) {
            moduleDescriptor = new ModuleDescriptor();
            moduleDescriptor.setModuleType(getModuleType());
            moduleDescriptor.setDescriptor(this);
        }
        return moduleDescriptor;
    }

    /**
     * Sets the module descriptor for this bundle
     * @param descriptor for the module
     */
    public void setModuleDescriptor(ModuleDescriptor descriptor) {
        moduleDescriptor = descriptor;
        for (List<RootDeploymentDescriptor> extByType : this.extensions.values()) {
            if (extByType!=null) {
                for (RootDeploymentDescriptor ext : extByType) {
                    ext.setModuleDescriptor(descriptor);
                }
            }

        }
    }    
    
    /**
     * @return true if this module is an application object 
     */
    public abstract boolean isApplication();
    
    /**
     * print a meaningful string for this object
     */
    public void print(StringBuffer toStringBuffer) {
        super.print(toStringBuffer);
        toStringBuffer.append("\n Module Type = ").append(getModuleType());
        toStringBuffer.append("\n Module spec version = ").append(getSpecVersion());
        if (moduleID!=null) 
            toStringBuffer.append("\n Module ID = ").append(moduleID);
        if (getSchemaLocation()!=null)
            toStringBuffer.append("\n Client SchemaLocation = ").append(getSchemaLocation());
    }

    /**
     * This method returns all the extensions deployment descriptors in the scope
     * @return an unmodifiable collection of extensions or empty collection if none.
     */
    public Collection<RootDeploymentDescriptor> getExtensionsDescriptors() {
        ArrayList<RootDeploymentDescriptor> flattened = new ArrayList<RootDeploymentDescriptor>();
        for (List<? extends RootDeploymentDescriptor> extensionsByType : extensions.values()) {
            flattened.addAll(extensionsByType);
        }
        return Collections.unmodifiableCollection(flattened);
    }

    /**
     * This method returns all extensions of the passed type in the scope
     * @param type requested extension type
     * @return an unmodifiable collection of extensions or empty collection if none.
     */
    public <T extends RootDeploymentDescriptor> Collection<T> getExtensionsDescriptors(Class<T> type) {
        for (Map.Entry<Class<? extends RootDeploymentDescriptor>, List<RootDeploymentDescriptor>> entry : extensions.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return Collections.unmodifiableCollection((Collection<T>) entry.getValue());
            }
        }
        return (Collection<T>) emptyList;
    }

    /**
     * This method returns one extension of the passed type in the scope with the right index
     * @param type requested extension type
     * @param index is the instance index
     * @return an unmodifiable collection of extensions or empty collection if none.
     */
    public <T extends RootDeploymentDescriptor>  T getExtensionsDescriptors(Class<? extends RootDeploymentDescriptor> type, String index) {
        for (T extension : (Collection<T>) getExtensionsDescriptors(type)) {
            String extensionIndex = ((RootDeploymentDescriptor)extension).index;
            if (index==null) {
                if (extensionIndex==null) {
                    return extension;
                }
            } else {
                if (index.equals(extensionIndex)) {
                    return extension;
                }
            }
        }
        return null;
    }

    public synchronized <T extends RootDeploymentDescriptor> void addExtensionDescriptor(Class<? extends RootDeploymentDescriptor> type, T instance, String index) {
        List<RootDeploymentDescriptor> values;
        if (extensions.containsKey(type)) {
            values = extensions.get(type);
        } else {
            values = new ArrayList<RootDeploymentDescriptor>();
            extensions.put(type, values);
        }
        ((RootDeploymentDescriptor)instance).index = index;
        values.add(instance);

    }

    /**
     * @return whether this descriptor is an extension descriptor
     *         of a main descriptor, e.g. the EjbBundleDescriptor for
     *         ejb in war case should return true.
     */
    public boolean isExtensionDescriptor() {
        if (getModuleDescriptor().getDescriptor() != this) {
            return true;
        }
        return false;
    }

    /**
     * @return the main descriptor associated with it if it's
     *         an extension descriptor, otherwise return itself
     */
    public RootDeploymentDescriptor getMainDescriptor() {
        if (isExtensionDescriptor()) {
            return getModuleDescriptor().getDescriptor();
        } else {
            return this;
        }
    }
}

