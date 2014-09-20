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

package org.glassfish.admin.amx.base;

import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
Useful informational tools.

@since GlassFish V3
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public interface Tools extends AMXProxy, Utility, Singleton
{
    /** emit information about all MBeans */
    @ManagedAttribute
    @Description("emit information about all MBeans")
    public String getInfo();

    /** emit information about all MBeans of the specified type, or path */
    @Description("emit information about all MBeans of the specified type, or path")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    String infoType(
            @Param(name = "type")
            final String type);

    /** emit information about all MBeans of the specified type, or path */
    @Description("emit information about all MBeans of the specified type, or path")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    String infoPath(
            @Param(name = "path")
            final String path);

    /** emit information about all MBeans having the specified parent path (PP), recursively or not */
    @Description("emit information about all MBeans having the specified parent path (PP), recursively or not")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    String infoPP(
            @Description("path of the *parent* MBean")
            @Param(name = "parentPath")
            final String parentPath,
            @Param(name = "recursive")
            final boolean recursive);

    /** emit information about MBeans, loosey-goosey seach string eg type alone, pattern, etc */
    @Description("emit information about MBeans, loosey-goosey seach string eg type alone")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    String info(
            @Description("loosey-goosey seach string")
            @Param(name = "searchString")
            final String searchString);

    /** Get a compilable java interface for the specified MBean */
    @Description("Get a compilable java interface for the specified MBean")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    String java(
            @Description("Get a compilable java interface for the specified MBean")
            @Param(name = "objectName")
            final ObjectName objectName);

    /** Validate all AMX MBeans. */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Validate all AMX MBeans and return status")
    public String validate();

    /** Validate a single MBean or MBeans specified by a pattern. */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Validate a single MBean or MBeans specified by a pattern")
    public String validate(
            @Param(name = "objectNameOrPattern")
            final ObjectName objectNameOrPattern);

    /** Validate MBeans: specific ObjectNames and/or patterns. */
    @Description("Validate MBeans: specific ObjectNames and/or patterns")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public String validate(
            @Param(name = "mbeans")
            final ObjectName[] mbeans);

    @Description("Dump the hierarchy of AMX MBeans by recursive descent")
    @ManagedAttribute
    public String getHierarchy();
    
}
















