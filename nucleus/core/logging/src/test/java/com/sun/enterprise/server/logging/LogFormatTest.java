/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.server.logging;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test to ensure that the LogForatHelper correctly works out the log type
 * @author jonathan coustick
 */
public class LogFormatTest {
    
    private static final String JSON_RECORD = "{\"Timestamp\":\"2020-04-20T21:53:40.248+0100\",\"Level\":\"INFO\","
            + "\"Version\":\"Payara 5.202\",\"LoggerName\":\"javax.enterprise.logging\",\"ThreadID\":\"22\",\"ThreadName\":"
            + "\"RunLevelControllerThread-1587416020198\",\"TimeMillis\":\"1587416020248\",\"LevelValue\":\"800\","
            + "\"MessageID\":\"NCLS-LOGGING-00009\",\"LogMessage\":\"Running Payara Version: Payara Server  5.202"
            + " (build ${build.number})\"}";
    private static final String ODL_RECORD = "[2020-04-20T22:05:43.203+0100] [Payara 5.202] [INFO] [NCLS-LOGGING-00009] "
            + "[javax.enterprise.logging] [tid: _ThreadID=21 _ThreadName=RunLevelControllerThread-1587416743113] "
            + "[timeMillis: 1587416743203] [levelValue: 800] [[";
    private static final String ULF_RECORD = "[#|2020-04-20T22:02:35.314+0100|INFO|Payara 5.202|javax.enterprise.logging|_ThreadID=21;"
            + "_ThreadName=RunLevelControllerThread-1587416555246;_TimeMillis=1587416555314;_LevelValue=800;"
            + "_MessageID=NCLS-LOGGING-00009;|";

    
    @Test
    public void JSONFormatTest() {
        Assert.assertTrue(LogFormatHelper.isJSONFormatLogHeader(JSON_RECORD));
        Assert.assertFalse(LogFormatHelper.isODLFormatLogHeader(JSON_RECORD));
        Assert.assertFalse(LogFormatHelper.isUniformFormatLogHeader(JSON_RECORD));
    }
    
    @Test
    public void ODLFormatTest() {
        Assert.assertFalse(LogFormatHelper.isJSONFormatLogHeader(ODL_RECORD));
        Assert.assertTrue(LogFormatHelper.isODLFormatLogHeader(ODL_RECORD));
        Assert.assertFalse(LogFormatHelper.isUniformFormatLogHeader(ODL_RECORD));
    }
    
    @Test
    public void UniformFormatTest() {
        Assert.assertFalse(LogFormatHelper.isJSONFormatLogHeader(ULF_RECORD));
        Assert.assertFalse(LogFormatHelper.isODLFormatLogHeader(ULF_RECORD));
        Assert.assertTrue(LogFormatHelper.isUniformFormatLogHeader(ULF_RECORD));
    }
    
}
