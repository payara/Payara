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

package com.sun.enterprise.util;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Tests for JavaVersion class's methods.
 * @author Yamini K B
 */
public class JavaVersionTest {    
    private static final String[] JAVA_VERSIONS = new String[] {
        "1.5.1-beta",
        "1.6.0",
        "1.7.0_10-ea",
        "1.7.0_17",
        "1.7.0_17-rc1"
    };

    private static final String[] INVALID_JAVA_VERSIONS = new String[] {
        "1a.7.0",
        "a.b.c",
        "1.7beta",
    };
    
    
    @Test
    public void testMatchRegex() {
        for (String st: JAVA_VERSIONS) {
            System.out.println("Test Java Version String " + st);
            JavaVersion jv = JavaVersion.getVersion(st);
            assertTrue(jv != null);
            System.out.println("Java Version = " + jv.toJdkStyle());
        }
        
        for (String st: INVALID_JAVA_VERSIONS) {
            System.out.println("Test Invalid Java Version String " + st);
            JavaVersion jv = JavaVersion.getVersion(st);
            assertTrue(jv == null);
        }
    }
    
    @Test
    public void testNewerThan() {
        JavaVersion jv1 = JavaVersion.getVersion("1.7.0_10-ea");
        JavaVersion jv2 = JavaVersion.getVersion("1.7.0_11");
        assertTrue(jv2.newerThan(jv1));
    }
    
    @Test
    public void testNewerOrEQuals() {
        JavaVersion jv1 = JavaVersion.getVersion("1.7.0_11");
        assertTrue(jv1.newerOrEquals(jv1));
        JavaVersion jv2 = JavaVersion.getVersion("1.7.0_12");
        JavaVersion jv3 = JavaVersion.getVersion("1.7.0_11-ea");
        assertTrue(jv2.newerOrEquals(jv3));
    }
    
    @Test
    public void testOlderThan() {
        JavaVersion jv1 = JavaVersion.getVersion("1.3.0");
        JavaVersion jv2 = JavaVersion.getVersion("1.3.0_11");
        assertTrue(jv1.olderThan(jv2));
        JavaVersion jv3 = JavaVersion.getVersion("1.3.1");
        assertTrue(jv1.olderThan(jv3));
    }
    
    @Test
    public void testOlderOrEquals() {
        JavaVersion jv1 = JavaVersion.getVersion("1.6.0_31");
        JavaVersion jv2 = JavaVersion.getVersion("1.7.0");
        assertTrue(jv1.olderOrEquals(jv2));
    }
}

