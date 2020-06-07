/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.rest;

import com.sun.enterprise.admin.report.ActionReporter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.provider.ActionReportJson2Provider;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author mmares
 */
public class ActionReportTest {
    private ActionReportJson2Provider provider = new ActionReportJson2Provider();
    
    private String marshall(RestActionReporter ar) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        provider.writeTo(ar, ar.getClass(), ActionReporter.class, null, new MediaType("application", "actionreport"), null, baos);
        return baos.toString("UTF-8");
    }
    
    private String basicMarshallingTest(RestActionReporter ar) throws IOException {
        String str = marshall(ar);
        assertNotNull(str);
        assertFalse(str.isEmpty());
        //System.out.println(str);
        return str;
    }
    
    @Test
    public void actionReportMarshallingTest() throws IOException {
        RestActionReporter ar = new RestActionReporter();
        ar.setActionDescription("Some description");
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setExtraProperties(null);
        basicMarshallingTest(ar);
        ar.getTopMessagePart().setMessage("First message in First report");
        basicMarshallingTest(ar);
        ar.getTopMessagePart().addProperty("AR1-MSG1-PROP1", "1.1.1.");
        basicMarshallingTest(ar);
        ar.getTopMessagePart().addProperty("AR1-MSG1-PROP2", "1.1.2.");
        basicMarshallingTest(ar);
        MessagePart part1 = ar.getTopMessagePart().addChild();
        basicMarshallingTest(ar);
        part1.setMessage("Second message in First report");
        basicMarshallingTest(ar);
        part1.addProperty("AR1-MSG2-PROP1", "1.2.1.");
        part1.addProperty("AR1-MSG2-PROP2", "1.2.2.");
        basicMarshallingTest(ar);
        MessagePart part2 = part1.addChild();
        part2.setMessage("Third message in First report");
        part2.addProperty("AR1-MSG3-PROP1", "1.3.1.");
        part2.addProperty("AR1-MSG3-PROP2", "1.3.2.");
        basicMarshallingTest(ar);
        MessagePart part3 = ar.getTopMessagePart().addChild();
        part3.setMessage("Fourth message in First report");
        part3.addProperty("AR1-MSG4-PROP1", "1.4.1.");
        part3.addProperty("AR1-MSG4-PROP2", "1.4.2.");
        basicMarshallingTest(ar);
        Properties extra = new Properties();
        extra.setProperty("EP1-PROP1", "1.1");
        extra.setProperty("EP1-PROP2", "1.2");
        ar.setExtraProperties(extra);
        ActionReport ar2 = ar.addSubActionsReport();
        ar2.setActionExitCode(ActionReport.ExitCode.WARNING);
        ar2.setActionDescription("Description 2");
        ar2.getTopMessagePart().setMessage("First Message in Second Report");
        MessagePart subPart2 = ar2.getTopMessagePart().addChild();
        subPart2.addProperty("AR2-MSG2-PROP1", "2.2.1.");
        subPart2.setMessage("Second Message in Second Report");
        basicMarshallingTest(ar);
        ActionReport ar3 = ar.addSubActionsReport();
        ar3.setActionExitCode(ActionReport.ExitCode.FAILURE);
        ar3.setActionDescription("Description 3");
        ar3.setFailureCause(new Exception("Some exception message"));
        basicMarshallingTest(ar);
    }
    
}
