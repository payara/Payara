/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs;

import java.io.File;
import java.util.List;

import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Archive;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Component;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.FileEntry;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Group;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Property;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.PropertyType;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.StringsubsDefinition;

/**
 * An object which allows to set the custom behavior for string substitution
 * operation and facilitate String substitution process.
 * <p>String substitution is a process of substituting a string in a
 * file with another string.</p>
 */
public interface StringSubstitutor
{
    /**
     * Set's the {@link AttributePreprocessor} to customize the substitution
     * process. Attribute preprocessor takes care to retrieve the value of
     * substitutable key.
     * 
     * @param attributePreprocessor Custom implementation of {@link AttributePreprocessor}
     */
    void setAttributePreprocessor(AttributePreprocessor attributePreprocessor);

    /**
     * Set's a factory which can process a {@link FileEntry} or an {@link Archive} entry 
     * to retrieve all the {@link Substitutable} entries.
     * @param factory
     */
    void setEntryFactory(SubstitutableFactory factory);

    /**
     * TODO: Missing Implementation 
     * @param backupLocation
     */
    void setFileBackupLocation(File backupLocation);

    /**
     * Get's the default {@link Property} for the given {@link PropertyType}, If
     * the property type is null then all the default properties will be returned. 
     * 
     * @param type The type for which default properties has to be retrieved. 
     * @return List of default properties or empty list if no property found.
     */
    List<Property> getDefaultProperties(PropertyType type);

    /**
     * Get's the string-subs definition object. A {@link StringSubsDefiniton} object
     * contains the details of component, groups and files used in substitution.
     *
     * <p><b>NOTE</b>: This object is updatable. </p>
     * @return Parsed string-subs configuration object.
     */
    StringsubsDefinition getStringSubsDefinition();

    /**
     * Perform's string substitution.
     *
     * @throws StringSubstitutionException If any error occurs in string substitution.
     */
    void substituteAll() throws StringSubstitutionException;

    /**
     * Perform's string substitution for give components.
     *
     * @param component List of {@link Component} identifiers for which the string
     *  substitution has to be performed.
     * @throws StringSubstitutionException If any error occurs during
     *  substitution.
     */
    void substituteComponents(List<String> components) throws StringSubstitutionException;

    /**
     * Perform's string substitution for give groups.
     *
     * @param groups List of {@link Group} identifiers for which the string
     *  substitution has to be performed.
     * @throws StringSubstitutionException If any error occurs during
     *  substitution.
     */
    void substituteGroups(List<String> groups) throws StringSubstitutionException;
}
