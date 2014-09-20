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
package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.xml.stringsubs.ModeType;

/**
 * Unit test for {@link ModeProcessor} functionality.
 */
public class TestModeProcessor {

    /**
     * Test for <code>null</code> input string.
     */
    @Test
    public void testNullInput() {
        String outputStr = ModeProcessor.processModeType(ModeType.DOUBLE, null);
        Assert.assertNull(outputStr);
    }

    /**
     * Test for <code>null</code> mode.
     */
    @Test
    public void testInvalidMode() {
        String inputStr = "TEST";
        Assert.assertEquals(inputStr, ModeProcessor.processModeType(null, inputStr));
    }

    /**
     * Test for the empty input string.
     */
    @Test
    public void testEmptyInput() {
        String outputStr = ModeProcessor.processModeType(ModeType.DOUBLE, "");
        Assert.assertTrue(outputStr.isEmpty());
    }

    /**
     * Test the processing for FORWARD ModeType.
     */
    @Test
    public void testForwardMode() {
        String inputStr = "First Slash \\ Second Double Slash \\\\";
        String expectedOutput = "First Slash / Second Double Slash //";
        String outputStr = ModeProcessor.processModeType(ModeType.FORWARD, inputStr);
        Assert.assertEquals(outputStr, expectedOutput);
    }

    /**
     * Test the processing for DOUBLE ModeType.
     */
    @Test
    public void testDoubleMode() {
        String inputStr = "First Slash \\ First Colon : Second Double Slash \\\\ Second Double Colon ::";
        String expectedOutput = "First Slash \\\\ First Colon \\: Second Double Slash \\\\\\\\ Second Double Colon \\:\\:";
        String outputStr = ModeProcessor.processModeType(ModeType.DOUBLE, inputStr);
        Assert.assertEquals(outputStr, expectedOutput);
    }

    /**
     * Test the processing for POLICY ModeType.
     */
    @Test
    public void testPolicyMode() {
        StringBuilder builder = new StringBuilder();
        builder.append("First Separator ");
        builder.append(File.separator);
        builder.append(" Second Double Separator ");
        builder.append(File.separator);
        builder.append(File.separator);
        String expectedOutput = "First Separator ${/} Second Double Separator ${/}${/}";
        String outputStr = ModeProcessor.processModeType(ModeType.POLICY, builder.toString());
        Assert.assertEquals(outputStr, expectedOutput);
    }
}