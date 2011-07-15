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

package javax.security.auth.message.callback;

import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

/**
 * Callback establishing group principals within the argument subject.
 * This callback is intended to be called by a <code>serverAuthModule</code> 
 * during its <code>validateRequest</code> processing. 
 *
 * @version %I%, %G%
 */
public class GroupPrincipalCallback implements Callback {

    private Subject subject;
    private String[] groups;

    /**
     * Create a GroupPrincipalCallback to establish the container's 
     * representation of the corresponding group principals within
     * the Subject.
     *
     * @param s The Subject in which the container will create
     * group principals.
     *
     * @param g An array of Strings, where each element contains
     * the name of a group that will be used to create a 
     * corresponding group principal within the Subject.
     * <p> 
     * When a null value is passed to the g argument, the handler will 
     * establish the container's representation of no group principals within
     * the Subject. 
     * Otherwise, the handler's processing of this callback is
     * additive, yielding the union (without duplicates) of the principals 
     * existing within the Subject, and those created with the names occuring 
     * within the argument array. The CallbackHandler will define the type 
     * of the created principals. 
     */
    public GroupPrincipalCallback(Subject s, String[] g) {
	subject = s;
	groups = g;
    }

    /**
     * Get the Subject in which the handler will establish the 
     * group principals.
     *
     * @return The subject.
     */
    public Subject getSubject() {
	return subject;
    }

    /**
     * Get the array of group names. 
     *
     * @return Null, or an array containing 0 or more String group names. 
     * <p>
     * When the return value is null, the handler will 
     * establish the container's representation of no group principals within
     * the Subject. 
     *
     * Otherwise, the handler's processing of this callback is
     * additive, yielding the union (without duplicates) of the principals 
     * created with the names in the returned array and those existing 
     * within the Subject.
     */
    public String[] getGroups() {
	return groups;
    }

}
