/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.permissionsxml;

import java.io.FilePermission;
import java.net.URL;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GlobalPolicyUtilTest {

    private static final String plfile = "server.policy";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        URL serverPF = GlobalPolicyUtilTest.class.getResource(plfile);
        System.out.println("policy file url = " + serverPF + ", path = " + serverPF.getPath());
        System.setProperty(GlobalPolicyUtil.SYS_PROP_JAVA_SEC_POLICY, serverPF.getPath());
    }

    @Test
    public void testSystemPolicyPath() {
        System.out.println("path= " + GlobalPolicyUtil.domainCfgFolder);

        Assert.assertNotNull(GlobalPolicyUtil.domainCfgFolder);
    }

    @Test
    public void testTYpeConvert() {
        for (CommponentType commponentType : CommponentType.values()) {
            CommponentType convertedType = GlobalPolicyUtil.convertComponentType(commponentType.name());
            System.out.println("Converted type = " + convertedType);
            Assert.assertEquals("Converted type should be " + commponentType.name(), commponentType, convertedType);
        }
        
        try {
            GlobalPolicyUtil.convertComponentType("");
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            GlobalPolicyUtil.convertComponentType("bla");
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        try {
            GlobalPolicyUtil.convertComponentType(null);
            Assert.fail();
        } catch (NullPointerException e) {

        }
    }

//    @Test
    public void testPolicyLoading() {
        System.out.println("Starting testDefPolicy loading - ee");

        PermissionCollection defEjbGrantededPC = GlobalPolicyUtil.getEECompGrantededPerms(CommponentType.ejb);
        int count = dumpPermissions("Grant", "Ejb", defEjbGrantededPC);
        Assert.assertEquals(5, count);

        PermissionCollection defWebGrantededPC = GlobalPolicyUtil.getEECompGrantededPerms(CommponentType.war);
        count = dumpPermissions("Grant", "Web", defWebGrantededPC);
        Assert.assertEquals(6, count);

        PermissionCollection defRarGrantededPC = GlobalPolicyUtil.getEECompGrantededPerms(CommponentType.rar);
        count = dumpPermissions("Grant", "Rar", defRarGrantededPC);
        Assert.assertEquals(5, count);

        PermissionCollection defClientGrantededPC = GlobalPolicyUtil.getEECompGrantededPerms(CommponentType.car);
        count = dumpPermissions("Grant", "Client", defClientGrantededPC);
        Assert.assertEquals(10, count);

        System.out.println("Starting testDefPolicy loading - ee restrict");

        PermissionCollection defEjbRestrictedPC = GlobalPolicyUtil.getCompRestrictedPerms(CommponentType.ejb);
        count = dumpPermissions("Restricted", "Ejb", defEjbRestrictedPC);
        Assert.assertEquals(2, count);

        PermissionCollection defWebRestrictedPC = GlobalPolicyUtil.getCompRestrictedPerms(CommponentType.war);
        count = dumpPermissions("Restricted", "Web", defWebRestrictedPC);
        Assert.assertEquals(2, count);

        PermissionCollection defRarRestrictedPC = GlobalPolicyUtil.getCompRestrictedPerms(CommponentType.rar);
        count = dumpPermissions("Restricted", "Rar", defRarRestrictedPC);
        Assert.assertEquals(1, count);

        PermissionCollection defClientRestrictedPC = GlobalPolicyUtil.getCompRestrictedPerms(CommponentType.car);
        count = dumpPermissions("Restricted", "Client", defClientRestrictedPC);
        Assert.assertEquals(2, count);
    }

    @Test
    public void testFilePermission() {
        System.out.println("Starting testFilePermission");

        FilePermission fp1 = new FilePermission("-", "delete");
        FilePermission fp2 = new FilePermission("a/file.txt", "delete");

        Assert.assertTrue(fp1.implies(fp2));

        FilePermission fp3 = new FilePermission("*", "delete");
        FilePermission fp4 = new FilePermission("file.txt", "delete");

        Assert.assertTrue(fp3.implies(fp4));

        FilePermission fp5 = new FilePermission("/scratch/xyz/*", "delete");
        FilePermission fp6 = new FilePermission("/scratch/xyz/deleteit.txt", "delete");

        Assert.assertTrue(fp5.implies(fp6));

        FilePermission fp7 = new FilePermission("/scratch/xyz/", "delete");
        FilePermission fp8 = new FilePermission("/scratch/xyz", "delete");

        Assert.assertTrue(fp7.implies(fp8));

        Permission fp9 = new java.security.UnresolvedPermission("VoidPermission", "", "", null);
        Permission fp10 = new java.security.AllPermission();

        Assert.assertTrue(fp10.implies(fp9));
        Assert.assertTrue(!fp9.implies(fp10));
    }

    private int dumpPermissions(String type, String component, PermissionCollection pc) {
        int count = 0;

        if (pc == null) {
            System.out.println("Type= " + type + ", compnent= " + component + ", Permission is empty ");
            return count;
        }

        Enumeration<Permission> pen = pc.elements();
        while (pen.hasMoreElements()) {
            Permission p = pen.nextElement();
            System.out.println("Type= " + type + ", compnent= " + component + ", Permission p= " + p);
            count += 1;
        }

        return count;
    }

}