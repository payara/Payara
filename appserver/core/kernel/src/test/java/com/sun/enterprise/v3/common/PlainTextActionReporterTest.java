/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.enterprise.v3.common;

import org.glassfish.api.ActionReport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class PlainTextActionReporterTest {

    public PlainTextActionReporterTest() {
    }

    @Before
    public void beforeTest() throws Exception {
        System.out.println(
            "\n-------------------------------------------------------------------------------");
    }
    @AfterClass
    public static void afterTest() throws Exception {
        System.out.println(
            "-------------------------------------------------------------------------------");
    }

    @Test
    public void failureTest() throws Exception {
        ActionReport report = new PlainTextActionReporter();
        report.setActionDescription("My Action Description");
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage("FailureTest Message Here!!");
        report.setFailureCause(new IndexOutOfBoundsException("Hi I am a phony Exception!!"));
        report.writeReport(System.out);
    }
    @Test
    public void babyTest() throws Exception {
        ActionReport report = new PlainTextActionReporter();
        report.setActionDescription("My Action Description");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage("BabyTest Message Here!!");
        report.writeReport(System.out);
    }

    @Test
    public void mamaTest() throws Exception {
        ActionReport report = new PlainTextActionReporter();
        report.setActionDescription("My Action Description");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage("Mama Test Top Message");
        top.setChildrenType("Module");

        for(int i = 0; i < 8; i++) {
            ActionReport.MessagePart childPart = top.addChild();
            childPart.setMessage("child" + i + " Message here");
            childPart.addProperty("ChildKey" + i, "ChildValue" + i);
            childPart.addProperty("AnotherChildKey" + i, "AnotherChildValue" + i);

            ActionReport.MessagePart grandkids = childPart.addChild();
            grandkids.setMessage("Grand Kids #" + i + " Top Message");
        }
        report.writeReport(System.out);
    }

    @Test
    public void papaTest() throws Exception {
        ActionReport report = new PlainTextActionReporter();
        report.setActionDescription("My Action Description");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage("Papa Test Top Message");
        top.setChildrenType("Module");

        for(int i = 0; i < 8; i++) {
            ActionReport.MessagePart childPart = top.addChild();
            childPart.setMessage("child" + i + " Message here");
            childPart.addProperty("ChildKey" + i, "ChildValue" + i);
            childPart.addProperty("AnotherChildKey" + i, "AnotherChildValue" + i);

            for(int j = 0; j < 3; j++) {
                ActionReport.MessagePart grandkids = childPart.addChild();
                grandkids.setMessage("Grand Kid#" + j + " from child#" + i + " Top Message");
                grandkids.addProperty("Grand Kid#" + j + " from child#" + i + "key", "value");
            }
        }
        report.writeReport(System.out);
    }

    @Test
    public void aggregateTest() {
        ActionReporter successfulRoot = new PlainTextActionReporter();
        assert successfulRoot.hasSuccesses();
        assert !successfulRoot.hasFailures();
        assert !successfulRoot.hasWarnings();
        ActionReport failedChild = successfulRoot.addSubActionsReport();
        failedChild.setActionExitCode(ActionReport.ExitCode.FAILURE);
        assert successfulRoot.hasSuccesses();
        assert successfulRoot.hasFailures();
        assert !successfulRoot.hasWarnings();
        assert !failedChild.hasSuccesses();
        assert !failedChild.hasWarnings();
        assert failedChild.hasFailures();
        ActionReport warningChild = failedChild.addSubActionsReport();
        warningChild.setActionExitCode(ActionReport.ExitCode.WARNING);
        assert successfulRoot.hasSuccesses();
        assert successfulRoot.hasFailures();
        assert successfulRoot.hasWarnings();
        assert !failedChild.hasSuccesses();
        assert failedChild.hasWarnings();
        assert failedChild.hasFailures();
        assert warningChild.hasWarnings();
        assert !warningChild.hasSuccesses();
        ActionReport successfulChild = warningChild.addSubActionsReport();
        assert failedChild.hasSuccesses();
        assert warningChild.hasSuccesses();
        assert !warningChild.hasFailures();
        StringBuilder sb = new StringBuilder();
        successfulRoot.setMessage("sr");
        successfulRoot.getCombinedMessages(successfulRoot, sb);
        assertEquals("sr", sb.toString());
        warningChild.setMessage("wc");
        sb = new StringBuilder();
        successfulRoot.getCombinedMessages(successfulRoot, sb);
        assertEquals("sr\nwc", sb.toString());
        failedChild.setMessage("fc");
        sb = new StringBuilder();
        successfulRoot.getCombinedMessages(successfulRoot, sb);
        assertEquals("sr\nfc\nwc", sb.toString());
    }
}
