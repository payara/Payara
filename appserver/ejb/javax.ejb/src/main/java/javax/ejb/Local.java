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

package javax.ejb;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/**
 * Declares the local business interface(s) for a session bean.
 * <p>
 * The <code>Local</code> annotation is applied to the session bean class or local
 * business interface to designate a local interface of the bean.
 * <p>
 * When used on the bean class, declares the local business interface(s) 
 * for a session bean.  When used on an interface, designates that interface
 * as a local business interface.  In this case, no <code>value</code> element
 * should be provided.
 * <p>
 * The <code>Local</code> annotation applies only to session beans and 
 * their interfaces.
 * <p>
 * Use of the <code>Local</code> annotation is required when the bean class 
 * exposes one or more local business interface and any of the following is true:
 * <ul>
 * <li>the bean class does not implement its business interfaces
 * <li>the bean exposes a no-interface view
 * <li>the bean class implements more than one interface and at least one of the interfaces 
 * is explicitly designated as a business interface by using the <code>Local</code> 
 * or <code>Remote</code> annotation on the interface, or in the deployment descriptor
 * </ul>
 * <p>
 * The following interfaces are excluded when determining whether the bean class 
 * has business interfaces: <code>java.io.Serializable</code>; 
 * <code>java.io.Externalizable</code>; any of the interfaces defined in <code>javax.ejb</code>.
 *
 * @since EJB 3.0
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Local {

    /**
     * Specifies the local business interface(s) of the bean.  The <code>value</code>
     * element is specified only when the annotation is applied to the bean class. 
     * It is required to be specified if any of the following is true:
     * <ul>
     * <li>the bean class does not implement its local business interface
     * <li>any of the implemented interfaces is designated as a remote interface
     * <li>some of the implemented interfaces are designated
     * as local business interfaces using <code>Local</code> annotation on the interface,
     * while others (excluding <code>java.io.Serializable</code>, 
     * <code>java.io.Externalizable</code>, and any of the interfaces 
     * defined by the <code>javax.ejb</code> package) are not designated as such.
     * </ul>
     */
    Class[] value() default {};
} 
