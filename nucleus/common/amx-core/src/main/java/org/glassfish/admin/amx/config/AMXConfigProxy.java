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

package org.glassfish.admin.amx.config;

import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.StdAttributesAccess;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

import javax.management.AttributeList;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import java.util.Map;

/**
@deprecated  Extending this proxy interface implies that the class is part of the MBean API for configuration,
that the interface is a dynamic proxy to a config MBean.
<p>
Note that considerable metadata is available for config MBeans, via MBeanInfo.getDescriptor().
@see AMXProxy
@see AMXConfigConstants
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@Deprecated
public interface AMXConfigProxy extends AMXProxy, AttributeResolver
{
    /**
    Return a Map of default values for the specified child type.
    The resulting Map is keyed by the  attribute name, either the AMX attribute name or the xml attribute name.
    @since Glassfish V3.
    @param type the J2EEType of the child
    @param useAMXAttributeName whether to key the values by the the AMX Attribute name or XML attribute name
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get the default values for child type")
    public Map<String, String> getDefaultValues(
            @Param(name = "type")
            final String type,
            @Param(name = "useAMXAttributeName")
            @Description("true to use Attribute names, false to use XML names")
            final boolean useAMXAttributeName);

    /**
    Return a Map of default values for this MBean.
    @param useAMXAttributeName whether to key the values by the XML attribute name vs the AMX Attribute name
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get the available default values")
    public Map<String, String> getDefaultValues(
            @Param(name = "useAMXAttributeName")
            @Description("true to use Attribute names, false to use XML names")
            final boolean useAMXAttributeName);

    /**
    Generic creation of an {@link AMXConfigProxy} based on the desired XML element type, which must
    be legitimate for the containing element.
    <p>
    Required attributes must be specified, and should all be 'String' (The Map value is declared
    with a type of of 'Object' anticipating future extensions).
    Use the ATTR_NAME key for the name.

    @param childType the XML element type
    @param params Map containing  attributes which are required by the @Configured and any
    optional attributes (as desired).
    @return proxy interface to the newly-created AMXConfigProxy
     */
    @ManagedOperation
    @Description("Create a child of the specified type")
    public AMXConfigProxy createChild(
            @Param(name = "childType") String childType,
            
            @Param(name = "params")
            @Description("name/value pairs for attributes")
            Map<String, Object> params);

    /** same as the Map variant, but the name/value are in the array; even entries are names, odd are values
    @ManagedOperation
    public AMXConfigProxy createChild(
            @Param(name = "childType") String childType,
            
            @Param(name = "params")
            @Description("name/value pairs, even entries are names, odd entries are values")
            Object[] params);
     */

    /**
     * Create one or more children of any type(s).  Outer map is keyed by type.
     * Inner maps are the attributes of each child.  At the same time, attributes can be set
     * on the parent element via 'attrs'.  The entire operation is transactional (all or none).
     */
    @ManagedOperation
    public AMXConfigProxy[] createChildren(
            @Param(name = "childrenMaps")
            @Description("Keyed by type, then one Map per child of that type, with each map containing name/value pairs for attributes")
            Map<String,Map<String,Object>[]> childrenMaps,
            
            @Param(name = "attrs")
            @Description("Attributes to be set on the parent element")
            Map<String,Object>  attrs );


    /**
    Remove a config by type and name.
    @param childType the AMX j2eeType as defined
    @param name the name of the child
    @return the ObjectName of the removed child, or null if not found
     */
    @ManagedOperation
    public ObjectName removeChild(
            @Param(name = "childType") String childType,
            @Param(name = "name") String name);

    /**
    Generically remove a config by type (child must be a singleton)
    @param childType the AMX j2eeType as defined
    @return the ObjectName of the removed child, or null if not found
     */
    @ManagedOperation
    public ObjectName removeChild(
            @Param(name = "childType") String childType);
            
    /**
        Direct access to the MBeanServer, calls conn.setAttributes(objectName, attrs).
        Unlike {@link StdAttributesAccess#setAttributes}, this method throws a generic Exception if there is a transaction failure.
    */
    @ManagedOperation
	public AttributeList	setAttributesTransactionally( @Param(name = "attrs") AttributeList attrs )
								throws Exception;

}







