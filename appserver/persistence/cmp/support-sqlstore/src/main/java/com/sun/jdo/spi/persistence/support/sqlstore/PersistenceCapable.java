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
 * PersistenceCapable.java
 *
 * Created on February 28, 2000
 */
 
package com.sun.jdo.spi.persistence.support.sqlstore;


/**
 * Internal PersistenceCapable interface.
 */
public interface PersistenceCapable
    extends com.sun.jdo.api.persistence.support.PersistenceCapable
{
    /**
     * Returns the associated state manager. 
     */
    StateManager jdoGetStateManager();

    /**
     * Sets the associated state manager.
     */
    void jdoSetStateManager(StateManager sm);

    /**
     * Returns the value of the JDO flags. 
     */
    byte jdoGetFlags();

    /**
     * Sets the value of the JDO flags, and returns the previous value.
     */
    void jdoSetFlags(byte flags);

    /**
     * Returns the value of the specified field.
     *
     * Primitive valued fields are wrapped with the corresponding
     * Object wrapper type.
     */
    Object jdoGetField(int fieldNumber);

    /**
     * Sets the value of the specified field.
     *
     * Primitive valued fields are wrapped with the corresponding
     * Object wrapper type.
     */
    void jdoSetField(int fieldNumber, Object value);

    /**
     * Creates an instance of the same class as this object.
     */
    // added new method
    Object jdoNewInstance(StateManager statemanager);

    /**
     * Clears the fields of each persistent field.
     * 
     * This method stores zero or null values into each persistent
     * field of the instance, in effect reverting it to its initial
     * state. Clearing fields allows objects referred to by this
     * instance to be garbage collected. The associated StateManager
     * calls this method when transitioning an instance to the hollow
     * state. This will normally be during post completion. 
     */
    // removed parameter: StateManager sm
    void jdoClear();

    /**
     * Copies values from each transient, derived, or persistent field
     * from the target instance.
     *
     * The target instance must have exactly the same type as this instance.
     *
     * This method might be used by the StateManager to make a shallow
     * copy of an instance, or might be used to restore values of an
     * instance after transaction rollback. It might be used by the
     * application to make a shallow copy (clone) of a transient or
     * persistent instance.
     *
     * The enhancement-added fields (jdoFlags and jdoStateManager) are not
     * affected by jdoCopy(). 
     */
    //@olsen: fix 4435059: this method is not generated anymore
    // additional parameter: boolean cloneSCOs
    //void jdoCopy(Object o, boolean cloneSCOs);
}
