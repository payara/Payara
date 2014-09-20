/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.common;

import java.security.Principal;

/**
 * This class implements the principal interface.
 *
 * @author Harish Prabandham
 */
public class PrincipalImpl implements Principal, java.io.Serializable {

    /**
     * @serial
     */
    private String name;

     /**
     * Construct a principal from a string user name.
     * @param user The string form of the principal name.
     */
    public PrincipalImpl(String user) {
	this.name = user;
    }

    /**
     * This function returns true if the object passed matches 
     * the principal represented in this implementation
     * @param another the Principal to compare with.
     * @return true if the Principal passed is the same as that 
     * encapsulated in this object, false otherwise
     */
    public boolean equals(Object another) {
        // XXX for bug 4889642: if groupA and userA have
        // the same name, then groupA.equals(userA) return false
        // BUT userA.equals(groupA) return "true"
        if (another instanceof Group) {
            return false;
        } else if (another instanceof PrincipalImpl) {
	    Principal p = (Principal) another;
	    return getName().equals(p.getName());
	} else
	    return false;
    }
    
    /**
     * Prints a stringified version of the principal.
     * @return A java.lang.String object returned by the method getName()
     */
    @Override
    public String toString() {
	return getName();
    }

    /**
     * Returns the hashcode for this Principal object
     * @return a hashcode for the principal.
     */
    @Override
    public int hashCode() {
	return name.hashCode();
    }

    /**
     * Gets the name of the Principal as a java.lang.String
     * @return the name of the principal.
     */
    public String getName() {
	return name;
    }

}
