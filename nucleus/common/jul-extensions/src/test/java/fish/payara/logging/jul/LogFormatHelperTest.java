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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.logging.jul;

import fish.payara.logging.jul.formatter.ODLLogFormatter;
import fish.payara.logging.jul.formatter.UniformLogFormatter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to ensure that the LogForatHelper correctly works out the log type
 * @author jonathan coustick
 */
public class LogFormatHelperTest {

    private static final String ODL_RECORD = "[2020-04-20T22:05:43.203+0100] [Payara 5.202] [INFO] [NCLS-LOGGING-00009] "
            + "[javax.enterprise.logging] [tid: _ThreadID=21 _ThreadName=RunLevelControllerThread-1587416743113] "
            + "[timeMillis: 1587416743203] [levelValue: 800] [[";
    private static final String ULF_RECORD = "[#|2020-04-20T22:02:35.314+0100|INFO|Payara 5.202|javax.enterprise.logging|_ThreadID=21;"
            + "_ThreadName=RunLevelControllerThread-1587416555246;_TimeMillis=1587416555314;_LevelValue=800;"
            + "_MessageID=NCLS-LOGGING-00009;|";

    private static final String RANDOM_RECORD = "liuasudhfuk fhuashfu hiufh fueqrhfuqrehf qufhr uihuih uih jj";

    private final LogFormatHelper helper = new LogFormatHelper();


    @Test
    public void odl() {
        assertTrue(helper.isODLFormatLogHeader(ODL_RECORD));
        assertFalse(helper.isUniformFormatLogHeader(ODL_RECORD));
        assertEquals(ODLLogFormatter.class.getName(), helper.detectFormatter(ODL_RECORD));
    }

    @Test
    public void uniform() {
        assertFalse(helper.isODLFormatLogHeader(ULF_RECORD));
        assertTrue(helper.isUniformFormatLogHeader(ULF_RECORD));
        assertEquals(UniformLogFormatter.class.getName(), helper.detectFormatter(ULF_RECORD));
    }


    @Test
    public void unknown() {
        assertFalse(helper.isODLFormatLogHeader(RANDOM_RECORD));
        assertFalse(helper.isUniformFormatLogHeader(RANDOM_RECORD));
        assertEquals(LogFormatHelper.UNKNOWN_FORMAT, helper.detectFormatter(RANDOM_RECORD));
    }
}
