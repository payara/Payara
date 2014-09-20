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

package com.sun.enterprise.deployment.node;

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.xml.PersistenceTagNames;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;

/**
 * This node is responsible for reading details about one <persistence-unit/>
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitNode extends DeploymentDescriptorNode {

    /**
     * map of element names to method names in {@link PersistenceUnitDescriptor}
     */
    private Map<String, String> dispatchTable;

    /**
     * This is the default constructor which is also called from other
     * constructors of this class. Inside this constructor, we clear the
     * handlers set up by super classes' constructors because they are
     * not applicable in the context of PersistenceNode because
     * unlike standard Java EE schemas, persistence.xsd does not include
     * javaee_5.xsd for things like description, version etc.
     */
    public PersistenceUnitNode() {
        // clear all the handlers set up by super classes
        // because that sets up a handler for description which we are not
        // interested in.
        if (handlers != null) handlers.clear();
        initDispatchTable();
    }

    @Override public void startElement(
            XMLElement element, Attributes attributes) {
        if (PersistenceTagNames.PROPERTY.equals(element.getQName())) {
            assert(attributes.getLength() == 2);
            assert(attributes.getIndex(PersistenceTagNames.PROPERTY_NAME) !=
                    -1);
            assert(attributes.getIndex(PersistenceTagNames.PROPERTY_VALUE) !=
                    -1);
            PersistenceUnitDescriptor persistenceUnitDescriptor = (PersistenceUnitDescriptor) getDescriptor();
            String propName = attributes.getValue(
                    PersistenceTagNames.PROPERTY_NAME);
            String propValue = attributes.getValue(
                    PersistenceTagNames.PROPERTY_VALUE);
            persistenceUnitDescriptor.addProperty(propName, propValue);
            return;
        }
        super.startElement(element, attributes);
    }

    /**
     * This returns the dispatch table for this node.
     * Please note, unlike Java EE schemas persistence.xsd does not include
     * standard elements or attributes (e.g. version, descriptionGroupRef etc.)
     * from javaee_5.xsd, we don't use super classes' dispatch table.
     * @return map of element names to method names in PersistenceUnitDescriptor
     * @see super#getDispatchTable()
     * @see #initDispatchTable()
     */
    protected Map getDispatchTable() {
        return dispatchTable;
    }

    /**
     * Please note, unlike Java EE schemas persistence.xsd does not include
     * standard elements or attributes (e.g. version, descriptionGroupRef etc.)
     * from javaee_5.xsd, we don't use super classes' dispatch table.
     */
    private void initDispatchTable() {
        assert(dispatchTable == null);

        // we don't do super.getDispatchTable() because we are not
        // interested in any of super classes' disptcah table entries.
        Map<String, String> table = new HashMap<String, String>();

        // the values being put into the map represent method names
        // in PersistenceUnitDescriptor class.
        table.put(PersistenceTagNames.NAME, "setName");
        table.put(PersistenceTagNames.TRANSACTION_TYPE, "setTransactionType");
        table.put(PersistenceTagNames.DESCRIPTION, "setDescription");
        table.put(PersistenceTagNames.PROVIDER, "setProvider");
        table.put(PersistenceTagNames.JTA_DATA_SOURCE, "setJtaDataSource");
        table.put(PersistenceTagNames.NON_JTA_DATA_SOURCE, "setNonJtaDataSource");
        table.put(PersistenceTagNames.MAPPING_FILE, "addMappingFile");
        table.put(PersistenceTagNames.JAR_FILE, "addJarFile");
        table.put(PersistenceTagNames.EXCLUDE_UNLISTED_CLASSES, "setExcludeUnlistedClasses");
        table.put(PersistenceTagNames.CLASS, "addClass");
        table.put(PersistenceTagNames.SHARED_CACHE_MODE, "setSharedCacheMode");
        table.put(PersistenceTagNames.VALIDATION_MODE, "setValidationMode");
        this.dispatchTable = table;
    }

}
