/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;
import javax.security.auth.Subject;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.glassfish.security.common.PrincipalImpl;

import junit.framework.Assert;

public class SubjectUtilTest {

    private static final String USERNAME = "john";
    private static final String USERNAME2 = "John";
    private static final String[] GROUPS = {"g1", "g2"}; 
    
    private boolean debug = false;
    
    @Test
    public void testUserNameUtil() {
        
        Subject sub = createSub(USERNAME, GROUPS);
        
        List<String> usernames = SubjectUtil.getUsernamesFromSubject(sub);
        
        if (debug)
            System.out.println("user list =" + usernames);
        
        Assert.assertEquals(1, usernames.size());
    }

    
    @Test
    public void testGroupNameUtil() {

        Subject sub = createSub(USERNAME, GROUPS);
        
        List<String> groupnames = SubjectUtil.getGroupnamesFromSubject(sub);
        
        if (debug)
            System.out.println("group list =" + groupnames);

        Assert.assertEquals(2, groupnames.size());
    }

    @Test
    public void testUserNameUtil_empty() {
        
        Subject sub = createSub(null, GROUPS);
        
        List<String> usernames = SubjectUtil.getUsernamesFromSubject(sub);
        
        Assert.assertEquals(0, usernames.size());
    }

    
    @Test
    public void testGroupNameUtil_empty() {

        Subject sub = createSub(USERNAME, null);
        
        List<String> groupnames = SubjectUtil.getGroupnamesFromSubject(sub);
        
        Assert.assertEquals(0, groupnames.size());

    }
    
    @Test
    public void testUserNameUtil_multi() {
        
        Subject sub = createSub(USERNAME, GROUPS);
        sub.getPrincipals().add(new PrincipalImpl(USERNAME2));
        
        List<String> usernames = SubjectUtil.getUsernamesFromSubject(sub);
        
        if (debug)
            System.out.println("user list =" + usernames);
        
        Assert.assertEquals(2, usernames.size());
    }


    
    public static Subject createSub(String username, String[] groups) {
        
        Set<Principal> pset = new HashSet<Principal>();
        
        if (username != null) {
            Principal u = new PrincipalImpl(username);
            pset.add(u);
        }
        
        if (groups != null) {
            for (String g : groups) {
                if (g != null) {
                    Principal p = new org.glassfish.security.common.Group(g);
                    pset.add(p);
                }
            }
        }
        
        
        Set prvSet = new HashSet();

        Set<Object> pubSet = new HashSet<Object>();
        
        Subject sub = new Subject(false, pset, pubSet, prvSet);
        
        return sub;

    }

}
