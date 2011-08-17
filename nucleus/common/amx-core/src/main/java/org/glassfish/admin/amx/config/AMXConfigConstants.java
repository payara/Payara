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

package org.glassfish.admin.amx.config;

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class AMXConfigConstants
{
    private AMXConfigConstants()
    {
    }

    /** prefix for all Descriptor fields for config */
    public static final String DESC_CONFIG_PREFIX = "amx.configbean.";

    /** prefix for all Descriptor fields for config */
    public static final String DESC_ANNOTATION_PREFIX = DESC_CONFIG_PREFIX + "annotation.";

    /** Descriptor: annotation type (full classname): HK2 @Attribute, @Element, @DuckTyped */
    public static final String DESC_KIND = DESC_CONFIG_PREFIX + "kind";

    /** Descriptor: class of items in an @Element collection (fully-qualified class name) */
    public static final String DESC_ELEMENT_CLASS = DESC_CONFIG_PREFIX + "elementClass";

    /** Descriptor: the xml name as found in domain.xml */
    public static final String DESC_XML_NAME = DESC_CONFIG_PREFIX + "xmlName";

    /** Descriptor: classname of data type (@Attribute only) */
    public static final String DESC_DATA_TYPE = DESC_CONFIG_PREFIX + "dataType";

    /** Descriptor: default value, omitted if none */
    public static final String DESC_DEFAULT_VALUE = DESC_CONFIG_PREFIX + "defaultValue";

    /** Descriptor: true | false: whether this is the primary key (name) */
    public static final String DESC_KEY = DESC_CONFIG_PREFIX + "key";

    /** Descriptor: true | false if this is required (implied if 'key') */
    public static final String DESC_REQUIRED = DESC_CONFIG_PREFIX + "required";

    /** Descriptor:  true | false whether this is a reference to another element */
    public static final String DESC_REFERENCE = DESC_CONFIG_PREFIX + "reference";

    /** Descriptor:  true | false whether variable expansion should be supplied */
    public static final String DESC_VARIABLE_EXPANSION = DESC_CONFIG_PREFIX + "variableExpansion";

    /** Descriptor:  true | false whether this field is required to be non-null */
    public static final String DESC_NOT_NULL = DESC_CONFIG_PREFIX + "notNull";

    /** Descriptor:  units of attribute quantities */
    public static final String DESC_UNITS = DESC_CONFIG_PREFIX + "units";

    /** Descriptor:  true | false whether variable expansion should be supplied */
    public static final String DESC_PATTERN_REGEX = DESC_CONFIG_PREFIX + "pattern";

    /** Descriptor:  minimum value, as a String */
    public static final String DESC_MIN = DESC_CONFIG_PREFIX + "min";
    
    /** Descriptor:  maximum value, as a String */
    public static final String DESC_MAX = DESC_CONFIG_PREFIX + "max";

    /**
    The type of the Notification emitted when a config element
    is created.
     */
    public static final String CONFIG_CREATED_NOTIFICATION_TYPE =
            "org.glassfish.admin.amx.intf.ConfigCreated";

    /**
    The type of the Notification emitted when a config element
    is removed.
     */
    public static final String CONFIG_REMOVED_NOTIFICATION_TYPE =
            "org.glassfish.admin.amx.config.ConfigRemoved";

    /**
    The key within the Notification's Map of type
    CONFIG_REMOVED_NOTIFICATION_TYPE which yields the ObjectName
    of the  created or removed config.
     */
    public static final String CONFIG_OBJECT_NAME_KEY = "ConfigObjectName";
    
    
    /** feature stating that the AMXConfig is ready for use after having been started.  Data is the ObjectName of the DomainConfig MBean */
    public static final String AMX_CONFIG_READY_FEATURE   = "AMXConfigReady";

}








