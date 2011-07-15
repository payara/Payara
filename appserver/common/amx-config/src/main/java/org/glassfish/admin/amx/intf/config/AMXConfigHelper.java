/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.config.AMXConfigConstants;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.glassfish.admin.amx.config.AMXConfigConstants.*;

/**
 * Various shortcut methods for working with AMX config MBeans, particularly attributes and metadata.
 *
 * @author llc
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public class AMXConfigHelper {
    private final AMXConfigProxy mAMX;

    public AMXConfigHelper(final AMXConfigProxy amx) {
        mAMX = amx;
    }

    /**
     * match all attributes that have a Descriptor field with the specified value.
     * (could be extended to use regexp for value and/or field name).
     *
     * @param amx       the proxy
     * @param fieldName name of the field in the Descriptor for each Attribute
     * @param value     value of the field in the Descriptor for each Attribute
     */
    public Set<String> attributeNamesByDescriptorField(final String fieldName, final String value) {
        final Set<String> attrNames = new HashSet<String>();
        for (final MBeanAttributeInfo attrInfo : mAMX.extra().mbeanInfo().getAttributes()) {
            final Descriptor desc = attrInfo.getDescriptor();
            if (value.equals(desc.getFieldValue(fieldName))) {
                attrNames.add(attrInfo.getName());
            }
        }
        return attrNames;
    }

    /**
     * Get simple attribute names; those that are not Element kind.
     */
    public Set<String> simpleAttributes() {
        //final String elementKind = Element.class.getName();
        final String elementKind = "element";
        final Set<String> elementNames = attributeNamesByDescriptorField(DESC_KIND, elementKind);
        final Set<String> remaining = mAMX.attributeNames();
        remaining.removeAll(elementNames);
        return remaining;
    }

    /**
     * Get simple attributes; those that are not Element kind.
     */
    public Map<String, Object> simpleAttributesMap() {
        return mAMX.attributesMap(simpleAttributes());
    }

    /**
     * Get the JMX Descriptor for an Attribute.
     *
     * @see AMXConfigConstants
     */
    public final Descriptor attributeDescriptor(final String attrName) {
        final MBeanAttributeInfo info = mAMX.extra().attributeInfo(attrName);
        return info == null ? null : info.getDescriptor();
    }

    /**
     * Get the field value for an Attribute and specified field name, if any.
     *
     * @param attrName
     * @param fieldName
     * @see AMXConfigConstants
     */
    public final Object attributeDescriptorField(final String attrName, final String fieldName) {
        final Descriptor desc = attributeDescriptor(attrName);
        return desc == null ? null : desc.getFieldValue(fieldName);
    }

    /**
     * Return the units (if any) for the specified attribute.
     *
     * @param attrName
     */
    public String units(final String attrName) {
        return (String) attributeDescriptorField(attrName, DESC_UNITS);
    }

    /**
     * Return the minimum value (if any) for the specified numeric attribute.
     *
     * @param attrName
     */
    public Long min(final String attrName) {
        return (Long) attributeDescriptorField(attrName, DESC_MIN);
    }

    /**
     * Return the maximum value (if any) for the specified numeric attribute.
     *
     * @param attrName
     */
    public Long max(final String attrName) {
        return (Long) attributeDescriptorField(attrName, DESC_MAX);
    }

    /**
     * Return the dataType (if any) for the specified attribute eg java.lang.Boolean,
     *
     * @param attrName java.lang.Integer, java.lang.Long, java.lang.String
     */
    public String dataType(final String attrName) {
        return (String) attributeDescriptorField(attrName, DESC_DATA_TYPE);
    }

    /**
     * Return the regex pattern (if any) for the specified attribute.
     *
     * @param attrName
     */
    public String regexPattern(final String attrName) {
        return (String) attributeDescriptorField(attrName, DESC_PATTERN_REGEX);
    }

    /**
     * return true if the Attribute is a key value
     *
     * @param attrName
     */
    public boolean key(final String attrName) {
        return Boolean.parseBoolean("" + attributeDescriptorField(attrName, DESC_KEY));
    }

    /**
     * return true if the Attribute may not be null (is required)
     *
     * @param attrName
     */
    public boolean notNull(final String attrName) {
        return Boolean.parseBoolean("" + attributeDescriptorField(attrName, DESC_NOT_NULL));
    }

    /**
     * Return whether the attribute is required.  This can be implied by several
     * different annotations or field; this method checks them all.
     *
     * @param attrName
     */
    public Boolean required(final String attrName) {
        return notNull(attrName) || key(attrName) ||
                Boolean.parseBoolean("" + attributeDescriptorField(attrName, DESC_REQUIRED));
    }

    /**
     * Return the xml name of the attribute.
     *
     * @param attrName
     */
    public String xmlName(final String attrName) {
        return (String) attributeDescriptorField(attrName, DESC_XML_NAME);
    }

    /**
     * Return the default value of the attribute, or null if no default value.
     *
     * @param attrName
     */
    public String defaultValue(final String attrName) {
        return (String) attributeDescriptorField(attrName, DESC_DEFAULT_VALUE);
    }

}










