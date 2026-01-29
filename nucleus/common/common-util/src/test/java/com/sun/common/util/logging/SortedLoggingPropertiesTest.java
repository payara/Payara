/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package com.sun.common.util.logging;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class SortedLoggingPropertiesTest {

    @Test
    public void testSortedKeys() throws IOException {
        Properties properties = new Properties();

        InputStream inStream = SortedLoggingPropertiesTest.class.getResourceAsStream("/config/logging.properties");
        Assert.assertNotNull("logging.properties file is found", inStream);
        properties.load(inStream);
        inStream.close();

        SortedLoggingProperties sortedProperties = new SortedLoggingProperties(properties);
        OutputStream outStream = new ByteArrayOutputStream();
        sortedProperties.store(outStream, "Test");
        outStream.flush();

        String[] lines = outStream.toString().split("\n");
        // First line is the comments, Second line the date. So first real line is lines[2]
        Assert.assertTrue("First properties line must be 'handlers' ",lines[2].startsWith("handlers="));
        Assert.assertTrue("Second properties line must be 'handlerServices' ",lines[3].startsWith("handlerServices="));
        Assert.assertTrue("Third properties line must be 'java.util.logging.ConsoleHandler.formatter' ",lines[4].startsWith("java.util.logging.ConsoleHandler.formatter="));
        Assert.assertTrue("Fifth properties line must be 'java.util.logging.FileHandler.count' ",lines[5].startsWith("java.util.logging.FileHandler.count="));
        Assert.assertTrue("9th properties line must be 'com.sun.enterprise.server.logging.GFFileHandler.compressOnRotation' ",lines[9].startsWith("com.sun.enterprise.server.logging.GFFileHandler.compressOnRotation="));
        Assert.assertTrue("24th properties line must be 'com.sun.enterprise.server.logging.SyslogHandler.level' ",lines[24].startsWith("com.sun.enterprise.server.logging.SyslogHandler.level="));
        Assert.assertTrue("27th properties line must be 'com.sun.enterprise.server.logging.UniformLogFormatter.ansiColor' ",lines[27].startsWith("com.sun.enterprise.server.logging.UniformLogFormatter.ansiColor="));
        Assert.assertTrue("28th properties line must be 'fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.compressOnRotation' ",lines[28].startsWith("fish.payara.enterprise.server.logging.PayaraNotificationFileHandler.compressOnRotation="));
        Assert.assertTrue("37th properties line must be '.level' ",lines[37].startsWith(".level="));

        Assert.assertEquals("Total number of lines is 89 ", 89, lines.length);

        Arrays.stream(lines).forEach(System.out::println);

    }
}