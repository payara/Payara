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

package org.glassfish.admin.amx.core;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
    Holds meta information useful in generating and/or supplementing the default
    MBeanInfo as well as other runtime fields or optimizations.
    
    Depending on how the implementor generates MBeans, not all of this information is
    necessarily used; it could be ignored if there is a more authoritative source (eg
    internal @Configured interfaces that also have AMXConfig proxy interfaces).
    <p>
    In general, this annotation is used only by amx-core, amx-config and related built-in
    AMX modules.
 
   @author Lloyd Chambers
 */
@Retention(RUNTIME)
@Target({TYPE, ANNOTATION_TYPE})
@Documented
@org.glassfish.external.arc.Taxonomy(stability = org.glassfish.external.arc.Stability.UNCOMMITTED)
public @interface AMXMBeanMetadata {
    /**
       If true, states that the MBeanInfo is immutable; that once MBeanInfo is
       obtained it may be cached, avoiding needless/repeated invocations of getMBeanInfo().
       Very few MBeans have mutable MBeanInfo, so this defaults to 'true'.
       The term is a misnomer; it should be invariantMBeanInfo(), but this name
       is used go be consistent with the JMX standard.
     */
    boolean immutableMBeanInfo() default true;
    
    public static final String NULL = "\u0000";
    
    /** overrides default type to be used in ObjectName=, ignored if null or empty */
    public String type() default NULL;
    
    /** If true, no children are allowed. */
    public boolean leaf() default false;
    
    /** if true, the MBean is a singleon within its parent's scope */
    public boolean singleton() default false;
    
    /** if true, the MBean is a global singleton, unique in type among all AMX MBeans.
        Being a globalSingleton implies being a singleton
     */
    public boolean globalSingleton() default false;

}





