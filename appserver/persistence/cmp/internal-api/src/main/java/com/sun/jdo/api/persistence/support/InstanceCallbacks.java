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

/*
 * InstanceCallbacks.java
 *
 * Created on February 25, 2000
 */
 
package com.sun.jdo.api.persistence.support;

/**
 *
 * @author Craig Russell
 * @version 0.1
 */

/**
 * A PersistenceCapable class that provides callback methods for life
 * cycle events implements this interface.
 *
 * <P>Classes which include derived fields (transient fields whose values depend
 * on the values of persistent fields) require callbacks on specific
 * JDO Instance life cycle events in order to correctly populate the
 * values in these fields.  
 * 
 * <P>This interface defines the methods executed
 * by the PersistenceManager for these life cycle events.  If the class
 * implements InstanceCallbacks, it must explicitly declare it in the
 * class definition.  The Reference Enhancer does not modify the declaration or
 * any of the methods in the interface.
 */
public interface InstanceCallbacks 
{
    /**
     * Called after the values are loaded from the data store into
     * this instance.
     *
     * <P>Derived fields should be initialized in this method.
     *
     * <P>This method is never modified by the Reference Enhancer.
     */
    void jdoPostLoad();

    /**
     * Called before the values are stored from this instance to the
     * data store.
     *
     * <P>Database fields that might have been affected by modified derived
     * fields should be updated in this method.
     *
     * <P>This method is never modified by the Reference Enhancer.
     */
    void jdoPreStore();

    /**
     * Called before the values in the instance are cleared.
     *
     * <P>Transient fields should be cleared in this method, as they will
     * not be affected by the jdoClear method.  Associations between this
     * instance and others in the runtime environment should be cleared.
     *
     * <P>This method is never modified by the Reference Enhancer.
     */
    void jdoPreClear();
}
