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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Descriptor is the root class for all objects
 * representing deployment information in J2EE. Descriptors
 * notify listeners of state changes, and have a name, description,
 * and icons.
 *
 * @author Danny Coward
 */

public class Descriptor extends DynamicAttributesDescriptor {

    /**
     * Notification String for a general descriptor change.
     */
    public static final String DESCRIPTOR_CHANGED = "Descriptor change";
    public static final String NAME_CHANGED = "NameChanged";
    public static final String DESCRIPTION_CHANGED = "DescriptionChanged";
    public static final String LARGE_ICON_CHANGED = "LargeIconChanged";
    public static final String SMALL_ICON_CHANGED = "SmallIconChanged";

    /**
     * static flag to indicate descriptors should bounds check.
     */
    private static boolean boundsChecking = true;

    /**
     * My display name indexed by language
     */
    private Map<String, String> displayNames = null;

    /**
     * My descriptions indexed by language
     */
    private Map<String, String> descriptions = null;

    /**
     * icons map indexed by language
     */
    private Map<String, String> largeIcons = null;
    private Map<String, String> smallIcons = null;

    private Map<Class<? extends Descriptor>, List<? extends Descriptor>> descriptorExtensions = new HashMap<Class<? extends Descriptor>, List<? extends Descriptor>>();

    /**
     * The default constructor. Constructs a descriptor with
     * name, description and icons as empty Strings.
     */
    public Descriptor() {
    }

    /**
     * Add a child descriptor to the parent descriptor as an extension.
     *
     * @param dde the child descriptor  
     *
     */
    public <T extends Descriptor> void addDescriptorExtension(final T dde) {
        List<T> descriptorList = (List<T>)
            descriptorExtensions.get(dde.getClass());
        if (descriptorList == null) {
            descriptorList = new ArrayList<T>();
            descriptorExtensions.put(dde.getClass(), descriptorList);
        }
        descriptorList.add(dde);
    }

    /**
     * Get all child descriptor extensions for a given type.
     *
     * @param c the child descriptor type  
     * @return the list of descriptor extension for a given type
     *
     */
    public <T extends Descriptor> List<T> getDescriptorExtensions (
        final Class<T> c) {
        return (List<T>) descriptorExtensions.get(c);
    }

    /**
     * Get child descriptor extension for a given type.
     *
     * It is a convenience API to get the single child extension descriptor 
     * if the XML element it represents can only occur once.
     * 
     * Returns that single descriptor if the XML element that the given type 
     * represents can only occur once.
     * Returns the first element of the list of descriptors if the XML element 
     * that the given type represents can occur multiple times.
     *
     * @param c the child descriptor type  
     * @return the single or the first descriptor extension for a given type
     *
     */
    public <T extends Descriptor> T getDescriptorExtension(final Class<T> c) {
        List<T> descriptorList = (List<T>) descriptorExtensions.get(c);
        if (descriptorList == null || descriptorList.isEmpty()) {
            return null;
        } else {
            return descriptorList.get(0);
        }
    }

    /**
     * The copy constructor.
     * @param other the source descriptor
     */
    protected Descriptor(Descriptor other) {
        if (other.displayNames != null)
            this.displayNames = new HashMap<String, String>(other.displayNames);
        if (other.descriptions != null)
            this.descriptions = new HashMap<String, String>(other.descriptions);
        if (other.largeIcons != null)
            this.largeIcons = new HashMap<String, String>(other.largeIcons);
        if (other.smallIcons != null)
            this.smallIcons = new HashMap<String, String>(other.smallIcons);
    }

    /**
     * Constructs a descriptor with given
     * name, description.
     *
     * @param name        the name of the descriptor.
     * @param description the name of the descriptor.
     */
    public Descriptor(String name, String description) {
        this();
        setLocalizedDisplayName(null, name);
        setLocalizedDescription(null, description);
    }

    /**
     * Sets a global flag to enable or disable boudsn checking
     * of deployment information
     *
     * @param b true for bounds checking on, false else.
     */
    public static synchronized void setBoundsChecking(boolean b) {
        boundsChecking = b;
    }

    /**
     * Answers whether the object model is bounds checking.
     *
     * @return true for boudsn checking, false else.
     */
    public static synchronized boolean isBoundsChecking() {
        return boundsChecking;
    }

    /**
     * Sets the name of this descriptor.
     *
     * @param name the new name of the descriptor.
     */
    public void setName(String name) {
        setLocalizedDisplayName(null, name);
    }

    /**
     * The name of this descriptor as a String.
     *
     * @return the name of this descriptor
     */
    public String getName() {
        return getLocalizedDisplayName(null);
    }

    /**
     * Add a localized display name for this descriptor
     *
     * @param lang        the local identifier (null if using default locale)
     * @param displayName the localized string
     */
    public void setLocalizedDisplayName(String lang, String displayName) {

        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        if (displayNames == null) {
            displayNames = new HashMap<String, String>();
        }
        displayNames.put(lang, displayName);
    }

    /**
     * @param language the language
     * @return the localized display name for the passed language
     */
    public String getLocalizedDisplayName(String language) {

        if (displayNames == null) {
            return "";
        }

        String originalLanguage = language;
        if (language == null) {
            language = Locale.getDefault().getLanguage();
        }

        String localizedName = displayNames.get(language);
        if (localizedName != null) {
            return localizedName;
        }

        // so far, no luck, it is possible that this
        // environment property was transfered through jndi 
        // between machines with different locales, if I have
        // at least one value, and no language was specified,
        // let's return it.
        if (originalLanguage == null && displayNames.size() > 0) {
            return displayNames.values().iterator().next();
        }
        // all other cases return empty strings
        return "";
    }

    /**
     * @return the localized display name indexed by language
     */
    public Map<? extends String, ? extends String> getLocalizedDisplayNames() {
        return displayNames;
    }

    /**
     * sets the display name for this bundle
     * @param name the display name
     */
    public void setDisplayName(String name) {
        setName(name);
    }

    /**
     * @return the display name
     */
    public String getDisplayName() {
        return getName();
    }

    /**
     * Sets the description text of this descriptor.
     *
     * @param description the new description text of the descriptor.
     */
    public void setDescription(String description) {
        setLocalizedDescription(null, description);
    }

    /**
     * The description text of this descriptor as a String.
     *
     * @return the description text of this descriptor
     */
    public String getDescription() {
        return getLocalizedDescription(null);
    }

    /**
     * Add a localized description for this descriptor
     *
     * @param lang        the local identifier (null if using default locale)
     * @param description the localized string
     */
    public void setLocalizedDescription(String lang, String description) {
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        if (descriptions == null) {
            descriptions = new HashMap<String, String>();
        }
        descriptions.put(lang, description);
    }

    /**
     * @param lang the local language
     * @return the localized description
     */
    public String getLocalizedDescription(String lang) {

        if (descriptions == null)
            return "";
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        String description = descriptions.get(lang);
        if (description == null) {
            return "";
        }
        return description;
    }

    /**
     * @return a Map of localized description, where lang is the key
     */
    public Map<? extends String, ? extends String> getLocalizedDescriptions() {
        return descriptions;
    }

    /**
     * Sets the large icon uri for a particular language
     *
     * @param lang the language identifier
     * @param uri  the large icon uri
     */
    public void setLocalizedLargeIconUri(String lang, String uri) {
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        if (largeIcons == null) {
            largeIcons = new HashMap<String, String>();
        }
        largeIcons.put(lang, uri);
    }

    /**
     * @param lang the language or null for the current locale
     * @return the large icon uri for a language
     */
    public String getLocalizedLargeIconUri(String lang) {
        if (largeIcons == null) {
            return null;
        }
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        return largeIcons.get(lang);
    }

    /**
     * @return a map of localized large icons uris indexed
     *         by language
     */
    public Map getLocalizedLargeIconUris() {
        return largeIcons;
    }

    /**
     * set the localized small icon uri for the passed language
     *
     * @param lang the language
     * @param uri  the uri for the small icon
     */
    public void setLocalizedSmallIconUri(String lang, String uri) {
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        if (smallIcons == null) {
            smallIcons = new HashMap<String, String>();
        }
        smallIcons.put(lang, uri);
    }

    /**
     * @param lang the language
     * @return the small icon uri for the passed language
     */
    public String getLocalizedSmallIconUri(String lang) {
        if (smallIcons == null) {
            return null;
        }
        if (lang == null) {
            lang = Locale.getDefault().getLanguage();
        }
        return smallIcons.get(lang);
    }

    /**
     *
     * @return the map of small icons indexed by language
     */
    public Map<? extends String, ? extends String> getLocalizedSmallIconUris() {
        return smallIcons;
    }

    /**
     * The large icon name of this descriptor as a String.
     *
     * @return the large icon name of this descriptor
     */
    public String getLargeIconUri() {
        return getLocalizedLargeIconUri(null);
    }

    /**
     * Sets the large icon name of this descriptor as a String.
     *
     * @param largeIconUri the large icon name of this descriptor
     */
    public void setLargeIconUri(String largeIconUri) {
        setLocalizedLargeIconUri(null, largeIconUri);
    }

    /**
     * The small icon name of this descriptor as a String.
     *
     * @return the small icon name of this descriptor
     */
    public String getSmallIconUri() {
        return getLocalizedSmallIconUri(null);
    }

    /**
     * Sets the small icon name of this descriptor as a String.
     *
     * @param smallIconUri the small icon name of this descriptor
     */
    public void setSmallIconUri(String smallIconUri) {
        setLocalizedSmallIconUri(null, smallIconUri);
    }

    /**
     * Returns the largest substring of the given string that
     * does not have an integer at the end.
     * @param s the source string
     * @return the stripped string
     */
    private static String stripIntegerEndingFrom(String s) {
        return recursiveStripIntegerEndingFrom(s);
    }

    /**
     * Returns the largest substring of the given string that
     * does not have an integer at the end.
     * @param s the source string
     * @return the stripped string
     */
    private static String recursiveStripIntegerEndingFrom(String s) {
        if (s.length() > 1) {
            String shorterByOne = s.substring(0, s.length() - 1);

            String lastBit = s.substring(s.length() - 1, s.length());
            try {
                Integer.parseInt(lastBit);
                return recursiveStripIntegerEndingFrom(shorterByOne);
            } catch (NumberFormatException nfe) {
                return s;
            }

        }
        return s;
    }

    /**
     * Returns String based on the trial name that is guaramteed to be different
     * from any of the strings in the vector of String names.
     *
     * @param trialName  the suggested name
     * @param v The Vector of String objects none of which will be the same as the return
     * @param index the index in the vector
     * @return the unique String
     */
    private static String uniquifyString(String trialName, Vector<String> v, int index) {
        for (String next : v) {
            if (next.equals(trialName)) {
                index++;
                return uniquifyString(stripIntegerEndingFrom(trialName) + index, v, index);
            }
        }
        return trialName;
    }

    /**
     * Returns String based on the trial name that is guaramteed to be different
     * from any of the strings in the vector of String names.
     *
     * @param trialName  the suggested name
     * @param otherNames The Vector of String objects none of which will be the same as the return
     * @return the unique String
     */
    public static String createUniqueFilenameAmongst(String trialName, Vector<String> otherNames) {

        /* extract file.ext */
        int p = trialName.lastIndexOf(".");
        if (p < 0) {
            return uniquifyString(trialName, otherNames, 0);
        }
        String ext = trialName.substring(p);
        String file = trialName.substring(0, p);

        /* get list of filenames less extension */
        Vector<String> nameList = new Vector<String>();
        for (Enumeration e = otherNames.elements(); e.hasMoreElements();) {
            String name = e.nextElement().toString();
            if (name.endsWith(ext)) {
                nameList.add(name.substring(0, name.length() - ext.length()));
            }
        }
        String unique = uniquifyString(file, nameList, 0);
        return unique + ext;

    }

    /**
     * Returns String based on the trial name that is guaramteed to be different
     * from any of the strings in the vector of String names.
     *
     * @param trialName  the suggested name
     * @param otherNames The Vector of String objects none of which will be the same as the return
     * @return the unique String
     */
    public static String createUniqueNameAmongst(String trialName, Vector<String> otherNames) {
        return uniquifyString(trialName, otherNames, 0);
    }

    /**
     * Returns String based on the trial name that is guaramteed to be different
     * from any of the strings returnsed by the getName() call in any of the Descriptor objects in the Set supplied.
     *
     * @param trialName   the suggested name
     * @param descriptors The Set of Descriptor objects to whose name attribute will not be the same as the return
     * @return the unique String
     */
    public static String createUniqueNameAmongstNamedDescriptors(String trialName, Set<? extends Descriptor> descriptors) {
        Vector<String> v = new Vector<String>();
        for (Descriptor next : descriptors) {
            v.addElement(next.getName());
        }
        return createUniqueNameAmongst(trialName, v);
    }

    /**
     * @return an iterator on the deployment-extension
     */
    public Iterator getDeploymentExtensions() {
        Vector extensions = (Vector) getExtraAttribute("deployment-extension");
        if (extensions != null) {
            return extensions.iterator();
        }
        return null;
    }

    /**
     * add a prefix mapping
     * @param mapping the mapping
     * @param uri the uri
     */
    public void addPrefixMapping(String mapping, String uri) {
        Map<String, String> prefixMapping = getPrefixMapping();
        if (prefixMapping == null) {
            prefixMapping = new java.util.HashMap<String, String>();
            addExtraAttribute("prefix-mapping", prefixMapping);
        }
        prefixMapping.put(mapping, uri);
    }

    /**
     * @return the map of prefix to namepace uri
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getPrefixMapping() {
        return (Map<String, String>) getExtraAttribute("prefix-mapping");
    }

    /**
     * A String representation of this object.
     */
    public void print(StringBuffer sb) {

        if (displayNames != null) {
            sb.append("Display Names:");
            displayLocalizedMap(sb, displayNames);
        }
        if (descriptions != null) {
            sb.append("\n Descriptions");
            displayLocalizedMap(sb, descriptions);
        }
        if (smallIcons != null) {
            sb.append("\n SmallIcons");
            displayLocalizedMap(sb, smallIcons);
        }
        if (largeIcons != null) {
            sb.append("\n LargeIcons");
            displayLocalizedMap(sb, largeIcons);
        }
        Map<String, String> prefix = getPrefixMapping();
        if (prefix != null)
            sb.append("\n Prefix Mapping = ").append(prefix);
        Iterator itr = getDeploymentExtensions();
        if (itr != null && itr.hasNext()) {
            do {
                sb.append("\n Deployment Extension : ");
                ((Descriptor) (itr.next())).print(sb);
            } while (itr.hasNext());
        }
        sb.append("\n");
        super.print(sb);
    }

    /**
     * helper method to display a localized map
     * @param sb the buffer
     * @param localizedMap the localized Map
     */
    private void displayLocalizedMap(StringBuffer sb, Map<String, String> localizedMap) {
        for (Map.Entry<String, String> entry : localizedMap.entrySet()) {
            sb.append("\n   lang[");
            sb.append(entry.getKey());
            sb.append("]  = ");
            sb.append(localizedMap.get(entry.getValue()));
        }
    }


    /**
     * Visitor API implementation, all descriptors must be visitable
     *
     * @param aVisitor the visitor implementation
     */
    public void visit(DescriptorVisitor aVisitor) {
        aVisitor.accept(this);
    }

    //start IASRI 4713550
    public String docType = null;

    public String getDocType() {
        return docType;
    }

    public void fillDocType(InputStream in) {
        try {
            BufferedReader inr = new BufferedReader(new InputStreamReader(in));
            String s = inr.readLine();
            while (s != null) {
                if (s.indexOf("DOCTYPE") > -1) {
                    docType = s;
                    in.close();
                    return;
                }
                s = inr.readLine();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
