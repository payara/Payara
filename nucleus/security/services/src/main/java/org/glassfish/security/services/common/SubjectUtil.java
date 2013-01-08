/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.common;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.security.auth.Subject;
import java.security.Principal;

import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.common.Group;

public class SubjectUtil {

    /**
     * Utility method to find the user names from a subject. The method assumes the user name is
     * represented by {@link org.glassfish.security.common.PrincipalImpl PrincipalImpl } inside the Subject's principal set.
     * @param subject the subject from which to find the user name
     * @return a list of strings representing the user name. The list may have more than one entry if the subject's principal set
     * contains more than one PrincipalImpl instances, or empty entry (i.e., anonymous user) if the subject's principal set contains no PrincipalImpl instances.
     */
    public static List<String> getUsernamesFromSubject(Subject subject) {

        List<String> userList = new ArrayList<String>();

        Set<Principal> princSet = null;

        if (subject != null) {

            princSet = subject.getPrincipals();
            for (Principal p : princSet) {
                if ((p != null) && (
                  p.getClass().isAssignableFrom(PrincipalImpl.class)  ||
                  "weblogic.security.principal.WLSUserImpl".equals(p.getClass().getCanonicalName())
                		)) {
                    String uName = p.getName();
                    userList.add(uName);
                }
            }
        }

        return userList;
    }


    /**
     * Utility method to find the group names from a subject. The method assumes the group name is
     * represented by {@link org.glassfish.security.common.Group Group } inside the Subject's principal set.
     * @param subject the subject from which to find the username
     * @return a list of strings representing the group names. The list may have more than one entry if the subject's principal set
     * contains more than one Group instances, or empty entry if the subject's principal set contains no Group instances.
     */
    public static List<String> getGroupnamesFromSubject(Subject subject) {

        List<String> groupList = new ArrayList<String>();

        Set<Group> princSet = null;

        if (subject != null) {

            princSet = subject.getPrincipals(Group.class);
            for (PrincipalImpl g : princSet) {
                String gName = g.getName();
                groupList.add(gName);
            }
        }

        return groupList;
    }

}
