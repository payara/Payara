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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *  Representation of a Manifest file and its available extensions and
 *  required extensions
 *  
 * @author Greg Murray
 * @author Justyna Horwat
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:28:18 $
 * 
 */
public class ManifestResource {
    
    // ------------------------------------------------------------- Properties

    // These are the resource types for determining effect error messages
    public static final int SYSTEM = 1;
    public static final int WAR = 2;
    public static final int APPLICATION = 3;
    
    private HashMap<String, Extension> availableExtensions = null;
    private ArrayList<Extension> requiredExtensions = null;
    
    private String resourceName = null;
    private int resourceType = -1;
        
    public ManifestResource(String resourceName, Manifest manifest, 
                            int resourceType) {
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        processManifest(manifest);
    }
    
    /**
     * Gets the name of the resource
     *
     * @return The name of the resource
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Gets the map of available extensions
     *
     * @return Map of available extensions
     */
    public HashMap<String, Extension> getAvailableExtensions() {
        return availableExtensions;
    }
    
    /**
     * Gets the list of required extensions
     *
     * @return List of required extensions
     */
    public ArrayList<Extension> getRequiredExtensions() {
        return requiredExtensions;   
    }
    
    // --------------------------------------------------------- Public Methods

    /**
     * Gets the number of available extensions
     *
     * @return The number of available extensions
     */
    public int getAvailableExtensionCount() {
        return (availableExtensions != null) ? availableExtensions.size() : 0;
    }
    
    /**
     * Gets the number of required extensions
     *
     * @return The number of required extensions
     */
    public int getRequiredExtensionCount() {
        return (requiredExtensions != null) ? requiredExtensions.size() : 0;
    }
    
    /**
     * Convenience method to check if this <code>ManifestResource</code>
     * has an requires extensions.
     *
     * @return true if required extensions are present
     */
    public boolean requiresExtensions() {
        return (requiredExtensions != null) ? true : false;
    }
    
    /**
     * Convenience method to check if this <code>ManifestResource</code>
     * has an extension available.
     *
     * @param key extension identifier
     *
     * @return true if extension available
     */
    public boolean containsExtension(String key) {
        return (availableExtensions != null) ?
                availableExtensions.containsKey(key) : false;
    }
    
    /**
     * Returns <code>true</code> if all required extension dependencies
     * have been meet for this <code>ManifestResource</code> object.
     *
     * @return boolean true if all extension dependencies have been satisfied
     */
    public boolean isFulfilled() {
        if (requiredExtensions == null) {
            return true;
        }
        Iterator<Extension> it = requiredExtensions.iterator();
        while (it.hasNext()) {
            Extension ext = it.next();
            if (!ext.isFulfilled()) return false;            
        }
        return true;
    }
    
    public String toString() {

        StringBuilder sb = new StringBuilder("ManifestResource[");
        sb.append(resourceName);

        sb.append(", isFulfilled=");
        sb.append(isFulfilled() +"");
        sb.append(", requiredExtensionCount =");
        sb.append(getRequiredExtensionCount());
        sb.append(", availableExtensionCount=");
        sb.append(getAvailableExtensionCount());
        switch (resourceType) {
            case SYSTEM : sb.append(", resourceType=SYSTEM"); break;
            case WAR : sb.append(", resourceType=WAR"); break;
            case APPLICATION : sb.append(", resourceType=APPLICATION"); break;
            default: sb.append(", resourceType=" + resourceType); break;
        }
        sb.append("]");
        return (sb.toString());
    }


    // -------------------------------------------------------- Private Methods

    private void processManifest(Manifest manifest) {
        availableExtensions = getAvailableExtensions(manifest);
        requiredExtensions = getRequiredExtensions(manifest);
    }
    
    /**
     * Return the set of <code>Extension</code> objects representing optional
     * packages that are required by the application associated with the
     * specified <code>Manifest</code>.
     *
     * @param manifest Manifest to be parsed
     *
     * @return List of required extensions, or null if the application
     * does not require any extensions
     */
    private ArrayList<Extension> getRequiredExtensions(Manifest manifest) {

        Attributes attributes = manifest.getMainAttributes();
        String names = attributes.getValue("Extension-List");
        if (names == null)
            return null;

        ArrayList<Extension> extensionList = new ArrayList<Extension>();
        names += " ";

        while (true) {

            int space = names.indexOf(' ');
            if (space < 0)
                break;
            String name = names.substring(0, space).trim();
            names = names.substring(space + 1);

            String value =
                attributes.getValue(name + "-Extension-Name");
            if (value == null)
                continue;
            Extension extension = new Extension();
            extension.setExtensionName(value);
            extension.setImplementationURL
                (attributes.getValue(name + "-Implementation-URL"));
            extension.setImplementationVendorId
                (attributes.getValue(name + "-Implementation-Vendor-Id"));
            String version = attributes.getValue(name + "-Implementation-Version");
            extension.setImplementationVersion(version);
            extension.setSpecificationVersion
                (attributes.getValue(name + "-Specification-Version"));
            extensionList.add(extension);
        }
        return extensionList;
    }
    
    /**
     * Return the set of <code>Extension</code> objects representing optional
     * packages that are bundled with the application associated with the
     * specified <code>Manifest</code>.
     *
     * @param manifest Manifest to be parsed
     *
     * @return Map of available extensions, or null if the web application
     * does not bundle any extensions
     */
    private HashMap<String, Extension> getAvailableExtensions(Manifest manifest) {

        Attributes attributes = manifest.getMainAttributes();
        String name = attributes.getValue("Extension-Name");
        if (name == null)
            return null;

        HashMap<String, Extension> extensionMap = new HashMap<String, Extension>();

        Extension extension = new Extension();
        extension.setExtensionName(name);
        extension.setImplementationURL(
            attributes.getValue("Implementation-URL"));
        extension.setImplementationVendor(
            attributes.getValue("Implementation-Vendor"));
        extension.setImplementationVendorId(
            attributes.getValue("Implementation-Vendor-Id"));
        extension.setImplementationVersion(
            attributes.getValue("Implementation-Version"));
        extension.setSpecificationVersion(
            attributes.getValue("Specification-Version"));

        if (!extensionMap.containsKey(extension.getUniqueId())) {
            extensionMap.put(extension.getUniqueId(), extension);
        }

        return extensionMap;
    }
    
}
