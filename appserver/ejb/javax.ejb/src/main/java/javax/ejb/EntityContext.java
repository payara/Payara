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

import java.util.*;
import java.security.Identity;

/**
 * The EntityContext interface provides an instance with access to the 
 * container-provided runtime context of an entity bean instance. 
 * The container passes the EntityContext interface to an entity 
 * bean instance after the instance has been created. 
 *
 * <p> The EntityContext interface remains associated with the instance for 
 * the lifetime of the instance. Note that the information that the instance
 * obtains using the EntityContext interface (such as the result of the
 * getPrimaryKey() method) may change, as the container assigns the instance
 * to different EJB objects during the instance's life cycle.
 *
 * @since EJB 2.0
 */
public interface EntityContext extends EJBContext
{
    /**
     * Obtain a reference to the EJB local object that is currently 
     * associated with the instance.
     *
     * <p> An instance of an entity bean can call this method only
     * when the instance is associated with an EJB local object identity, i.e.
     * in the ejbActivate, ejbPassivate, ejbPostCreate, ejbRemove,
     * ejbLoad, ejbStore, and business methods.
     *
     * <p> An instance can use this method, for example, when it wants to
     * pass a reference to itself in a method argument or result.
     *
     * @return The EJB local object currently associated with the instance.
     *
     * @exception IllegalStateException if the instance invokes this
     *    method while the instance is in a state that does not allow the
     *    instance to invoke this method, or if the instance does not have
     *    a local interface.
     *
     * @since EJB 2.0
     */
    EJBLocalObject getEJBLocalObject() throws IllegalStateException;

    /**
     * Obtain a reference to the EJB object that is currently associated with 
     * the instance.
     *
     * <p> An instance of an entity bean can call this method only
     * when the instance is associated with an EJB object identity, i.e.
     * in the ejbActivate, ejbPassivate, ejbPostCreate, ejbRemove,
     * ejbLoad, ejbStore, and business methods.
     *
     * <p> An instance can use this method, for example, when it wants to
     * pass a reference to itself in a method argument or result.
     *
     * @return The EJB object currently associated with the instance.
     *
     * @exception IllegalStateException Thrown if the instance invokes this
     *    method while the instance is in a state that does not allow the
     *    instance to invoke this method, or if the instance does not have
     *    a remote interface.
     */
    EJBObject getEJBObject() throws IllegalStateException;

    /**
     * Obtain the primary key of the EJB object that is currently
     * associated with this instance.
     *
     * <p> An instance of an entity bean can call this method only
     * when the instance is associated with an EJB object identity, i.e.
     * in the ejbActivate, ejbPassivate, ejbPostCreate, ejbRemove,
     * ejbLoad, ejbStore, and business methods.
     *
     * <p><b>Note</b>: The result of this method is that same as the
     * result of getEJBObject().getPrimaryKey().
     *
     * @return The primary key currently associated with the instance.
     *
     * @exception IllegalStateException Thrown if the instance invokes this
     *    method while the instance is in a state that does not allow the
     *    instance to invoke this method.
     */
    Object getPrimaryKey() throws IllegalStateException;
}
