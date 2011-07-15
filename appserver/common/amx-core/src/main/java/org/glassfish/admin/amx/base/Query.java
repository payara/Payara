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

import javax.management.ObjectName;
import javax.management.MBeanOperationInfo;
import java.util.Set;


import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.external.arc.Stability;

import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
Supports various types of queries to find MBeans in the AMX domain only; does not
query all MBeans in all domains, only those in the AMX domain.
<p>
Note that the methods as declared return AMXProxy or collections thereof, but the
actual result consists only of ObjectName; it is the proxy code that auto-converts
to AMXProxy eg invoking with MBeanServerConnection.invoke() will return Set<ObjectName>
but using QueryMgr (as a client-side proxy) will return Set<AMXProxy>.  If ObjectNames
are desirable, use {@link Util#toObjectNames}.
 */
@Taxonomy(stability = Stability.COMMITTED)
@AMXMBeanMetadata(singleton=true, globalSingleton=true, leaf=true)
public interface Query extends AMXProxy, Utility, Singleton
{
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans having any of the specified types")
    public Set<AMXProxy> queryTypes(@Param(name = "type") Set<String> type);

    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans having the specified type")
    public Set<AMXProxy> queryType( @Param(name = "type") String type);

    /**
    Return all {@link AMXProxy} having the specified name.
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans having the specified name")
    public Set<AMXProxy> queryName(  @Param(name = "name") String name);

    /**
    Return all AMX whose type and name matches. Note that the resulting items will necessarily
    have a different Parent (uniqueness invariant within any parent).
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans having the specified type and name")
    public Set<AMXProxy> queryTypeName( @Param(name = "type") String type, @Param(name = "name") String name);

    /**
    Return all AMX whose ObjectName matches the supplied pattern, as defined by the JMX specification.

    @param pattern  an ObjectName containing a pattern as defined by JMX
    @return Set of AMX or empty Set if none
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans matching the specfied pattern")
    public Set<AMXProxy> queryPattern( @Param(name = "pattern") ObjectName pattern);

    /**
    Return all AMX MBeans matching the specfied ObjectName properties
    @param props a String containing one or more name/value ObjectName properties
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans matching the specfied ObjectName properties")
    public Set<AMXProxy> queryProps( @Param(name = "props") String props);

    /**
    Return all AMX MBeans whose whose ObjectName matches all property
    expressions.  Each property expression consists of a key expression, and a value
    expression; an expression which is null is considered a "*" (match all).
    <p>
    Both key and value expressions may be wildcarded with the "*" character,
    which matches 0 or more characters.
    <p>
    Each property expression is matched in turn against the ObjectName. If a match
    fails, the ObjectName is not included in the result.  If all matches succeed, then
    the ObjectName is included.
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Return all AMX MBeans matching all specified ObjectName properties, wildcarded by key and/or value")
    public Set<AMXProxy> queryWildAll( @Param(name = "wildKeys") String[] wildKeys, @Param(name = "wildValues") String[] wildValues);

    /**  Return all AMX MBeans */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public Set<AMXProxy> queryAll();


    @Description("Return  all MBeans that are global singletons")
    @ManagedAttribute()
    public AMXProxy[] getGlobalSingletons();
    
    @Description("Return the global singleton of the specified type, or null if not found")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public AMXProxy getGlobalSingleton( @Param(name="type") String type);
    
    @Description("List the parent followed by all descendants, depth-first traversal")
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public AMXProxy[] queryDescendants( @Param(name="parentObjectName") ObjectName parentObjectName);
}






