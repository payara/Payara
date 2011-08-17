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
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

import javax.management.AttributeList;
import javax.management.MBeanOperationInfo;

/**
@deprecated  Interface implemented by MBeans which can resolve a variable to a value.
Variable attributes are strings  of the form ${...} and
are returned as the values of certain Attributes.  This interface is intended for use
only with config MBeans.
<p>
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@Deprecated
public interface AttributeResolver
{
    /**
    Resolve an attribute <em>value</em> to a literal.  The value should have been
    previously obtained from an Attribute of the same AMXConfig MBean.
    <p>
    If the String is not a template string, return the string unchanged.
    <p>
    If the String is a template string, resolve its value if it can be resolved, or 'null'
    if it cannot be resolved.
    <p>
    Examples:</br>
    <pre>
    "${com.sun.aas.installRoot}" => "/glassfish"
    "${does-not-exist}" => null
    "${com.myco.moonIsBlue}" => "true"
    "8080" => "8080"
    "hello" => "hello"
    </pre>

    @param value	any String
    @return resolved value
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Resolve a (possible) ${...} attribute *value* to a real value")
    public String resolveAttributeValue(@Param(name = "value") String value);

    /** calls getAttribute(), then returns the resolved value or null */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get and resolve a (possible) ${...} attribute to a real value")
    public String resolveAttribute(@Param(name = "attributeName") String attributeName);

    /** Get the Attribute and resolve it to a Boolean or null */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get and resolve a (possible)  ${...} attribute to a Boolean, returns null if not found")
    public Boolean resolveBoolean(@Param(name = "attributeName") String attributeName);

    /** Get the Attribute and resolve it to a Long or null */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get and resolve a (possible)  ${...} attribute to a Long, returns null if not found")
    public Long resolveLong(@Param(name = "attributeName") String attributeName);

    /**
    Calls getAttributes(), then returns all resolved values.  If the attributes
    have been annotated with @ResolveTo, then the value is of the correct type
    (eg String, Boolean, Integer).
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    @Description("Get and resolve attributes to values")
    public AttributeList resolveAttributes(@Param(name = "attributeNames") String[] attributeNames);

}







