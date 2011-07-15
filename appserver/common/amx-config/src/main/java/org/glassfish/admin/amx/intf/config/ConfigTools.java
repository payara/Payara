/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.admin.amx.base.Singleton;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.admin.amx.core.AMXProxy;

import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Server-side helper methods for config MBeans.
 */
@AMXMBeanMetadata(leaf = true, singleton = true)
@Deprecated
public interface ConfigTools extends AMXProxy, Singleton {
    /**
     * Create Property sub-elements on the specified MBean, transactionally (all or none).
     * <p/>
     * Each Map correspond to one property, and its keys should be Name/Value/Description,
     * though the Description may be omitted.
     *
     * @param parent   Parent MBean in which the new elements should reside
     * @param props    List of Maps, each Map keying Name/Value/Description
     * @param clearAll whether to remove all existing properties first
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    @Description("Create Property sub-elements, transactionally (all or none)")
    public void setProperties(
            @Description("Parent MBean in which the new elements should reside")
            @Param(name = "parent")
            final ObjectName parent,

            @Description("List of Maps, each Map keying Name/Value/Description")
            @Param(name = "props")
            final List<Map<String, String>> props,

            @Description("List of Maps, each Map keying Name/Value/Description")
            @Param(name = "clearAll")
            final boolean clearAll);

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    @Description("transactionally remove all 'property' children")
    public void clearProperties(final ObjectName objectName);

    /**
     * Create SystemProperty sub-elements, transactionally (all or none).
     * <p/>
     * Each Map correspond to one property, and its keys should be Name/Value/Description,
     * though the Description may be omitted.
     *
     * @param parent   Parent MBean in which the new elements should reside
     * @param props    List of Maps, each Map keying Name/Value/Description
     * @param clearAll whether to remove all existing properties first
     */
    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    @Description("Create SystemProperty sub-elements, transactionally (all or none)")
    public void setSystemProperties(
            @Description("Parent MBean in which the new elements should reside")
            @Param(name = "parent")
            final ObjectName parent,

            @Description("List of Maps, each Map keying Name/Value/Description")
            @Param(name = "props")
            final List<Map<String, String>> props,

            @Description("List of Maps, each Map keying Name/Value/Description")
            @Param(name = "clearAll")
            final boolean clearAll);

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    @Description("remove all 'system-property' children")
    public void clearSystemProperties(final ObjectName objectName);


    @ManagedAttribute
    @Description("return all element types that are Named")
    public String[] getConfigNamedTypes();

    @ManagedAttribute
    @Description("return all element types that are Resource")
    public String[] getConfigResourceTypes();

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    public Object test();
}







