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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.deployment.node;

import static com.sun.enterprise.deployment.xml.DeclaredPermissionsTagNames.PERMS_ROOT;
import static com.sun.enterprise.deployment.xml.DeclaredPermissionsTagNames.PERM_ITEM;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.enterprise.deployment.PermissionItemDescriptor;
import com.sun.enterprise.deployment.PermissionsDescriptor;
import com.sun.enterprise.deployment.xml.DeclaredPermissionsTagNames;

public class PermissionsNode extends AbstractBundleNode<PermissionsDescriptor> {

    public final static String SCHEMA_ID = "permissions_9.xsd";
    public final static String SPEC_VERSION = "9";
    
    private final static List<String> systemIDs = initSystemIDs();

    // The XML tag associated with this Node
    public final static XMLElement ROOT_ELEMENT = new XMLElement(DeclaredPermissionsTagNames.PERMS_ROOT);

    private static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<>();
        systemIDs.add(SCHEMA_ID);
        
        return unmodifiableList(systemIDs);
    }

    private PermissionsDescriptor permDescriptor;

    public PermissionsNode() {
        if (handlers != null) {
            handlers.clear();
        }

        permDescriptor = new PermissionsDescriptor();

        registerElementHandler(new XMLElement(PERM_ITEM), PermissionItemNode.class);

        SaxParserHandler.registerBundleNode(this, PERMS_ROOT);
    }

    public PermissionsNode(PermissionsDescriptor permDescriptor) {
        this();
        this.permDescriptor = permDescriptor;
    }

    @Override
    public PermissionsDescriptor getDescriptor() {
        return permDescriptor;
    }

    @Override
    public String registerBundle(Map<String, String> publicIDToSystemIDMapping) {
        return ROOT_ELEMENT.getQName();
    }

    @Override
    public Map<String, Class<?>> registerRuntimeBundle(Map<String, String> publicIDToSystemIDMapping, final Map<String, List<Class<?>>> versionUpgrades) {
        return Collections.emptyMap();
    }

    @Override
    public String getDocType() {
        return null;
    }

    @Override
    public String getSystemID() {
        return SCHEMA_ID;
    }

    @Override
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    @Override
    public String getSpecVersion() {
        return SPEC_VERSION;
    }

    @Override
    protected XMLElement getXMLRootTag() {
        return ROOT_ELEMENT;
    }

    @Override
    public void addDescriptor(Object descriptor) {
        if (descriptor instanceof PermissionItemDescriptor) {
            getDescriptor().addPermissionItemdescriptor(PermissionItemDescriptor.class.cast(descriptor));
        }
    }
}
