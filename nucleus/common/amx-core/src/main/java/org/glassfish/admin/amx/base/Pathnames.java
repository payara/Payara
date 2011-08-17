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
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.PathnameConstants;
import org.glassfish.admin.amx.core.PathnameParser;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
    The Pathnames MBean--utilities for working with pathnames and MBeans.
    @since GlassFish V3
    @see PathnameConstants
    @see PathnameParser
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton=true, globalSingleton=true, leaf=true)
public interface Pathnames extends AMXProxy, Utility, Singleton
{
    /** Resolve a path to an ObjectName.  Any aliasing, etc is dealt with.  Return null if failure. */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public ObjectName  resolvePath( final String path );
    
    /** Paths that don't resolve result in a null entry */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public ObjectName[]  resolvePaths( final String[] paths );
    
    /**
        An efficient way to get the list of MBeans from DomainRoot on down to the specified
        MBean.  The last entry will be the same as the parameter.
        From the ObjectNames one can obtain the path of every ancestor.
        If the MBean does not exist, null will be returned.
     */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public ObjectName[] ancestors( final ObjectName objectName );

    /**
        Resolves the path to an ObjectName, then calls ancestors(objectName).
        Any aliasing or special handling will be dealt with.
     */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public ObjectName[] ancestors( final String path );
    
    /**
        List descendant ObjectNames.
     */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public ObjectName[]  listObjectNames( final String path, final boolean recursive);
    
    /**
        List descendant paths.
     */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public String[] listPaths( final String path, boolean recursive );
    
    @ManagedAttribute
    public String[] getAllPathnames();

    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public String[] dump( final String path );
}







