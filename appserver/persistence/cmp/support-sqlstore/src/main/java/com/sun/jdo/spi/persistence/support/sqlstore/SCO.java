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
 * SCO.java
 *
 * created May 9, 2000
 *
 * @author Marina Vatkina
 * @version 1.0
 */

package com.sun.jdo.spi.persistence.support.sqlstore;

public interface SCO
{
	/**
         * Returns the field name
	 * 
         * @return field name as java.lang.String
         */
	String getFieldName();

	/**
         * Returns the owner object of the SCO instance
	 * 
         * @return owner object
         */
	Object getOwner();

	/**
         * Nullifies references to the owner Object and Field
         */
        void unsetOwner();

	/**
         * Apply changes (can be a no-op)
	 */
	void applyUpdates(StateManager sm, boolean modified);

	/**
         * Marks object dirty
	 *
	 * @return StateManager associated with the owner
	 */
	StateManager makeDirty();

        /**
         * Creates clone with the same owner and field value.
         * Used for the beforeImage.
         */
        Object cloneInternal();
}
