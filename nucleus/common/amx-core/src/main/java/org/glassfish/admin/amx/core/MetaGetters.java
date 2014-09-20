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

package org.glassfish.admin.amx.core;

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;


/**
	@deprecated Convenience getters for Descriptor values and other metadata from the MBeanInfo.
    These operations do not make a trip to the server.
    See {@link AMXProxy#extra}.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@Deprecated
public interface MetaGetters
{
    public MBeanInfo mbeanInfo();
    
    /**
        From Descriptor: get the Java classname of the interface for this MBean.
        If it has not been specified, a default generic interface is returned  eg 'AMX'.
		There is no guarantee that the interface exists on the client.
    */
    public String interfaceName();
    
    /**
        From Descriptor: get the generic interface for this MBean
        eg AMXProxy or AMXConfigProxy or (possibly) something else.
        The generic interface is always part of amx-core.
    */
    public Class<? extends AMXProxy> genericInterface();
    
    /** From Descriptor:  true if the MBeanInfo is invariant ("immutable") */
    public boolean isInvariantMBeanInfo();
    
    /** From Descriptor: true if this MBean is a singleton (1 instance within its parent scope) */
    public boolean singleton();
    
    /** From Descriptor: true if this MBean is a global singleton (1 instance within entire domain) */
    public boolean globalSingleton();
    
    /** From Descriptor: Get the *potential* sub types this MBean expects to have */
    public String[]  subTypes();
    
    /** From Descriptor: return true if new children are allowed by external subsystems */
    public boolean  supportsAdoption();
    
    /** From Descriptor: return the group value */
    public String  group();
    
    /** MBeanInfo descriptor */
    public Descriptor  descriptor();
    
    /** Get MBeanOperationInfo for specified attribute name. */
    public MBeanAttributeInfo attributeInfo(  final String attrName);
    
    /** Get MBeanOperationInfo for specified operation. */
    public MBeanOperationInfo operationInfo(final String operationName);
}








